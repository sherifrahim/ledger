package com.sherif.ledger.feature.review.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Transaction
import com.sherif.ledger.core.domain.model.TransactionType
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionReadSource
import com.sherif.ledger.core.domain.usecase.analytics.GetFinancialAnalyticsUseCase
import com.sherif.ledger.core.domain.util.MoneyFormatter
import com.sherif.ledger.feature.merchant.LearnedMerchantCategoryStore
import com.sherif.ledger.feature.merchant.MerchantCategory
import com.sherif.ledger.feature.merchant.MerchantResolution
import com.sherif.ledger.feature.merchant.MerchantResolver
import com.sherif.ledger.feature.review.presentation.ReviewInboxUiState
import com.sherif.ledger.feature.review.presentation.ReviewItemUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Backs the previously-mock `ReviewInboxScreen` with transactions that
 * resolved to "UNKNOWN" (no brand match, no keyword match, no prior learned
 * override — see GetFinancialAnalyticsUseCase's three-tier fallback). This
 * is the deterministic, no-AI alternative to the LLM/auto-categorize idea:
 * the user picks a category once per merchant, and
 * [LearnedMerchantCategoryStore] makes every future transaction from that
 * same merchant auto-categorize without asking again.
 */
@HiltViewModel
class ReviewInboxViewModel @Inject constructor(
    private val transactionReadSource: TransactionReadSource,
    private val accountRepository: AccountRepository,
    private val getFinancialAnalyticsUseCase: GetFinancialAnalyticsUseCase,
    private val merchantResolver: MerchantResolver,
    private val learnedMerchantCategoryStore: LearnedMerchantCategoryStore,
) : ViewModel() {

    // Ignored-for-this-session ids — Review Inbox re-offers them next time it's
    // opened rather than persisting a permanent "never ask again," since the
    // transaction is still genuinely uncategorized.
    private val ignoredIds = MutableStateFlow<Set<Long>>(emptySet())

    // Just-categorized ids — the learned override IS persisted immediately
    // (learnedMerchantCategoryStore.learn), but the combine below only re-emits
    // on transaction/account changes, not on a learn. Tracking the id here gives
    // instant feedback (the card leaves the queue on tap); on the next full read
    // the item is excluded anyway because the story no longer resolves to UNKNOWN.
    private val categorizedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<ReviewInboxUiState> = combine(
        transactionReadSource.observeAllTransactions(),
        accountRepository.observeAllAccounts(),
        ignoredIds,
        categorizedIds,
    ) { transactionsResult, accountsResult, ignored, categorized ->
        val transactions = (transactionsResult as? LedgerResult.Success)?.data ?: emptyList()
        val accounts = (accountsResult as? LedgerResult.Success)?.data ?: emptyList()
        buildState(transactions, accounts, ignored, categorized)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReviewInboxUiState(items = emptyList()),
    )

    private fun buildState(
        transactions: List<Transaction>,
        accounts: List<Account>,
        ignored: Set<Long>,
        categorized: Set<Long>,
    ): ReviewInboxUiState {
        if (transactions.isEmpty()) return ReviewInboxUiState(items = emptyList())

        val accountNames = accounts.associate { it.id to it.name }
        val stories = getFinancialAnalyticsUseCase.transactionStories(transactions)
        val timeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

        val items = transactions
            .filter { stories[it.id]?.category == "UNKNOWN" && it.id !in ignored && it.id !in categorized }
            .sortedByDescending { it.timestamp }
            .map { t ->
                val resolution = merchantResolver.resolve(t.rawText)
                ReviewItemUi(
                    id = t.id.toString(),
                    merchant = resolution.displayName,
                    merchantCategory = "UNKNOWN",
                    merchantAccentHue = 0xFF6E6E6E,
                    amount = MoneyFormatter.format(t.amount, includeSymbol = false),
                    isIncome = t.type == TransactionType.INCOME,
                    suggestedCategory = "Uncategorized",
                    suggestedAccount = accountNames[t.accountId] ?: "Account",
                    confidence = 0,
                    reason = "No matching merchant — choose a category below",
                    timestamp = t.timestamp.atZone(ZoneId.systemDefault()).format(timeFormatter),
                    rawMerchantText = t.rawText,
                )
            }

        return ReviewInboxUiState(
            items = items,
            pendingCount = items.size,
            confirmedTodayCount = 0,
            ignoredTodayCount = ignored.size,
        )
    }

    /** [rawMerchantText] is the ORIGINAL raw text (not the display name) — the same key MerchantResolver normalizes when looking overrides up later. */
    fun categorize(transactionId: String, rawMerchantText: String?, category: MerchantCategory) {
        if (rawMerchantText.isNullOrBlank()) return
        // Instant feedback: the card leaves the queue now. Persist the learned
        // override so every future transaction from this merchant auto-categorizes.
        transactionId.toLongOrNull()?.let { id -> categorizedIds.value = categorizedIds.value + id }
        viewModelScope.launch {
            learnedMerchantCategoryStore.learn(rawMerchantText, category)
        }
    }

    fun ignore(transactionId: String) {
        val id = transactionId.toLongOrNull() ?: return
        ignoredIds.value = ignoredIds.value + id
    }
}
