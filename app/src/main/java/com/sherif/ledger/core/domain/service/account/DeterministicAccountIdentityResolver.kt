package com.sherif.ledger.core.domain.service.account

import com.sherif.ledger.core.domain.model.Account
import com.sherif.ledger.core.domain.model.AccountType
import com.sherif.ledger.core.domain.model.CurrencyCode
import com.sherif.ledger.core.domain.model.LedgerResult
import com.sherif.ledger.core.domain.model.Money
import com.sherif.ledger.core.domain.model.TransactionCandidate
import com.sherif.ledger.core.domain.repository.AccountRepository
import com.sherif.ledger.core.domain.repository.TransactionRepository
import com.sherif.ledger.core.domain.service.intelligence.DecisionType
import com.sherif.ledger.core.domain.service.intelligence.LearnedDecisionStore
import com.sherif.ledger.core.domain.usecase.account.EnsureDefaultAccountUseCase
import com.sherif.ledger.feature.capture.notification.NotificationEnvelope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RC7's Candidate Account naming convention, shared (not private) so RC8's
 * [com.sherif.ledger.core.domain.usecase.account.PromoteCandidateAccountUseCase]
 * can recover the raw institution identifier a promoted account was created
 * from, to record a learned mapping ([LearnedDecisionStore]).
 */
object CandidateAccountNaming {
    private val pattern = Regex("^Unrecognized Institution \\((.+?)\\)(?: •(.+))?$")

    fun nameFor(rawIdentifier: String, tail: String?): String =
        "Unrecognized Institution ($rawIdentifier)" + (tail?.let { " •$it" } ?: "")

    /** Null if [accountName] doesn't match the Candidate Account naming convention (e.g. already promoted/renamed). */
    fun parseRawIdentifier(accountName: String): String? = pattern.matchEntire(accountName)?.groupValues?.get(1)
}

/**
 * Deterministic account identity resolution. No AI — every decision traces to
 * explicit, listed evidence. Implements [AccountIdentityResolver] so a future
 * model-assisted implementation can be substituted behind the same interface with
 * zero downstream change.
 *
 * Identity is a tuple — (institution, account-type hint, currency, tail) — never a
 * bare tail. Two accounts that happen to share a tail digit sequence at different
 * institutions, or in different currencies, are never conflated.
 *
 * Binding to an EXISTING account requires a high-confidence match. Creating a NEW
 * account requires either near-certainty in one observation, or the same identity
 * signature independently observed several times, each individually clearing a
 * real bar — never a single moderate-confidence guess. Below both bars, the
 * transaction falls back to the default account: a visible, queryable state, not
 * a silent invention.
 *
 * RC2: @Singleton + an internal [Mutex] serializing the whole resolve-or-create
 * sequence (including the default-account get-or-create inside
 * [EnsureDefaultAccountUseCase], which has the identical race shape). Both parts
 * are required together — a Mutex on a non-singleton class gives every injection
 * site (notification listener, SMS receiver, historical importer) its own,
 * separately useless lock. Without this, notification/SMS processing on
 * Dispatchers.IO's thread pool could run two resolutions for the same identity
 * concurrently: both see "no matching account yet" before either commits, and
 * both create one — a real, confirmed root cause of unstable balances, since a
 * split-account identity has no database-level uniqueness constraint to catch it
 * (unlike transactions, which do, via the fingerprint index).
 */
@Singleton
class DeterministicAccountIdentityResolver @Inject constructor(
    private val institutionRegistry: InstitutionRegistry,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val ensureDefaultAccountUseCase: EnsureDefaultAccountUseCase,
    private val learnedDecisionStore: LearnedDecisionStore,
) : AccountIdentityResolver {

    /** Shared across every caller because this class is @Singleton — serializes
     *  the whole resolve-or-create sequence app-wide. See class KDoc. */
    private val mutex = Mutex()

    companion object {
        /** Binding to an already-established account: institution + tail alone
         *  (75) is sufficient; this is far lower-risk than creating a new one. */
        const val BIND_THRESHOLD = 75

        /** Creating a new account from a single observation: every signal must
         *  agree, with essentially nothing contradicting. */
        const val CREATE_SINGLE_SHOT_THRESHOLD = 95

        /** A single observation must clear this bar to count toward the
         *  repeated-observation path at all — a near-miss, not a guess. */
        const val CREATE_OBSERVATION_MIN_SCORE = 60

        /** Independent sightings of the same identity signature required before
         *  auto-creation via repetition (this call is the Nth; N-1 prior + this
         *  one = the count). */
        const val CREATE_OBSERVATION_COUNT = 3

        // RC1 stabilization decision, not the permanent architecture: "card
        // ending"/"card no" were removed from this list. Both phrases appear in
        // ordinary DEBIT card purchase messages just as often as credit card
        // ones — a resolver that can't yet distinguish debit from credit
        // reliably must not guess CREDIT from generic card-tail mentions alone.
        // A real checking account mistyped as CREDIT gets every transaction's
        // arithmetic inverted (BalanceCalculator.effect() flips sign for
        // liability accounts), corrupting the whole account, not just the
        // purchase that triggered it — so this stays conservative until a
        // proper evidence-based resolver considers bank identity, relationship
        // evidence, payment confirmations, and merchant history together before
        // converging on an account type.
        private val cardPaymentPhrases = listOf(
            "credit card", "card payment", "credit card payment",
            "towards your card", "towards credit card",
        )
    }

    override suspend fun resolve(
        envelope: NotificationEnvelope,
        candidate: TransactionCandidate,
    ): AccountIdentityResult = mutex.withLock { resolveLocked(envelope, candidate) }

    /** Serializes app-wide by [mutex] — every read of existing accounts, every
     *  decision, and every create() call for both this resolver and the default
     *  account happens as one atomic sequence relative to any other caller. */
    private suspend fun resolveLocked(
        envelope: NotificationEnvelope,
        candidate: TransactionCandidate,
    ): AccountIdentityResult {
        val defaultAccountId = ensureDefaultAccountUseCase.execute()
        val tail = candidate.accountHint
        val institution = institutionRegistry.resolve(envelope.packageName)
        val currency = candidate.currencyCode ?: CurrencyCode.AED
        val typeHint = inferTypeHint(candidate.rawText)

        val evidence = mutableListOf<String>()
        institution?.let { evidence += "Institution: ${it.name}" } ?: evidence.add("Institution: unknown package ${envelope.packageName}")
        tail?.let { evidence += "Tail: $it" } ?: evidence.add("Tail: not extracted")
        evidence += "Currency: $currency"
        typeHint?.let { evidence += "Type hint: $it" }

        // RC7 Phase B: an institution InstitutionRegistry has never seen must
        // NEVER silently merge into the default account — that's exactly the
        // shape of the confirmed HDFC Bank currency-mixing bug (RC6): an
        // unrecognized bank's transaction landing in an unrelated-currency
        // account with no visible trace. Park it in a dedicated Candidate
        // Account instead, correctly currency-tagged from this transaction's
        // own data, reviewable in Developer Console.
        //
        // RC8 Phase B ("Ledger must learn"): before parking ANOTHER candidate
        // for the same raw identifier, check whether a user already promoted
        // one to a real account and confirmed its institution name — deterministic
        // memory preferred before repeating the same unresolved state forever.
        if (institution == null) {
            val learnedName = learnedDecisionStore.valueFor(DecisionType.INSTITUTION, envelope.packageName)
            if (learnedName != null) {
                val bound = bindToLearnedInstitution(learnedName, tail, currency, typeHint, evidence)
                if (bound != null) return bound
            }
            return resolveOrCreateCandidate(envelope, tail, currency, typeHint, evidence)
        }

        // Institution known, but no tail to disambiguate accounts within it:
        // still not enough evidence to bind or create against a SPECIFIC
        // account. The default account remains the right fallback here — same
        // institution, and BalanceCalculator's currency guard (RC6) already
        // protects against a currency mismatch if the default account happens
        // to differ, contributing zero effect rather than mixing units.
        if (tail == null) {
            return AccountIdentityResult(defaultAccountId, AccountIdentityDecision.FALLBACK_DEFAULT, 0, typeHint, evidence)
        }

        val existingAccounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val candidateAccounts = existingAccounts.filter { it.id != defaultAccountId }

        // 1. Try to bind to an existing, already-established account.
        val bestMatch = candidateAccounts
            .map { it to scoreAgainstExisting(it, institution, tail, currency, typeHint) }
            .maxByOrNull { it.second }

        if (bestMatch != null && bestMatch.second >= BIND_THRESHOLD) {
            return AccountIdentityResult(
                bestMatch.first.id,
                AccountIdentityDecision.BOUND_EXISTING,
                bestMatch.second,
                typeHint,
                evidence + "Matched existing account '${bestMatch.first.name}' (score ${bestMatch.second})",
            )
        }

        // 2. Consider creating a new account. Score this single observation as if
        // it were being matched against a hypothetical, perfectly-named account.
        val singleShotScore = scoreHypothetical(institution, tail, currency, typeHint)

        if (singleShotScore >= CREATE_SINGLE_SHOT_THRESHOLD) {
            val accountId = createAccount(institution, tail, currency, typeHint)
            return AccountIdentityResult(
                accountId,
                AccountIdentityDecision.CREATED_NEW,
                singleShotScore,
                typeHint,
                evidence + "Single-observation near-certainty (score $singleShotScore) -> created account",
            )
        }

        // 3. Repeated-observation path. Count PRIOR sightings of this exact
        // (package, tail) signature that fell back to the default account. A
        // fallback binding is never itself counted as evidence the default account
        // is correct -- it only counts toward "this identity keeps reappearing and
        // deserves its own account."
        if (singleShotScore >= CREATE_OBSERVATION_MIN_SCORE) {
            val priorFallbackCount = transactionRepository
                .countTransactionsByOrigin(envelope.packageName, tail)
                .filter { it.accountId == defaultAccountId }
                .sumOf { it.count }
            val totalObservations = priorFallbackCount + 1 // including this one
            if (totalObservations >= CREATE_OBSERVATION_COUNT) {
                val accountId = createAccount(institution, tail, currency, typeHint)
                return AccountIdentityResult(
                    accountId,
                    AccountIdentityDecision.CREATED_NEW,
                    singleShotScore,
                    typeHint,
                    evidence + "$totalObservations independent observations of this identity (score $singleShotScore each) -> created account",
                )
            }
            evidence += "Observation $totalObservations/$CREATE_OBSERVATION_COUNT toward account creation (score $singleShotScore)"
        }

        // 4. Insufficient evidence either way. Fall back, visibly.
        return AccountIdentityResult(defaultAccountId, AccountIdentityDecision.FALLBACK_DEFAULT, singleShotScore, typeHint, evidence)
    }

    /** Semantic type hint from wording alone -- a lightweight check, not the full
     *  RelationshipEngine (kept out of the hot ingestion path deliberately). */
    private fun inferTypeHint(rawText: String): AccountType? {
        val lower = rawText.lowercase()
        return if (cardPaymentPhrases.any { lower.contains(it) } && lower.contains("card")) AccountType.CREDIT else null
    }

    // internal (not private) so the scoring logic itself is directly
    // testable, rather than only observable through resolve()'s pass/fail
    // decision — necessary here since the fix below cannot, by itself, change
    // any observable binding outcome given today's threshold configuration
    // (institution+tail alone already clears BIND_THRESHOLD).
    internal fun scoreAgainstExisting(
        account: Account,
        institution: InstitutionIdentity,
        tail: String,
        currency: CurrencyCode,
        typeHint: AccountType?,
    ): Int {
        var score = 0
        if (account.name.contains(institution.name, ignoreCase = true)) score += 40
        if (account.accountNumberTail == tail) score += 35
        if (account.openingBalance.currencyCode == currency) score += 15
        // RC3: was `typeHint == null || account.type == typeHint`, which
        // conflated "no type signal at all" with "confirmed match" and awarded
        // the bonus either way. Absence of evidence is not evidence of a
        // match. Only a genuine, explicit agreement earns these points now.
        // Note this alone does not prevent two different physical cards
        // sharing a tail at the same institution from binding together —
        // institution+tail+currency (90) already clears BIND_THRESHOLD (75)
        // without this bonus. That's a separate, more fundamental limitation
        // of 4-digit tail entropy, not something this fix resolves.
        if (typeHint != null && account.type == typeHint) score += 10
        return score
    }

    /** Same weighting as [scoreAgainstExisting], evaluated as if every signal
     *  matched a hypothetical account exactly -- used only to decide whether THIS
     *  observation is strong enough to justify creating one. */
    private fun scoreHypothetical(
        institution: InstitutionIdentity,
        tail: String,
        currency: CurrencyCode,
        typeHint: AccountType?,
    ): Int {
        var score = 40 + 35 // institution + tail are both present by construction here
        if (institution.defaultCurrency == currency) score += 15
        if (typeHint != null) score += 10 // an explicit type signal, not just an assumed default
        return score
    }

    /**
     * RC8 Phase B: a raw identifier the registry still doesn't recognize, but
     * a user already promoted a Candidate Account for it once and confirmed
     * its real institution name ([LearnedDecisionStore]). Binds directly to
     * that CONFIRMED account (by name-contains + tail + currency, the same
     * convention [AccountMatching]/[scoreAgainstExisting] already use for
     * registry-known institutions) instead of parking yet another candidate
     * for a bank the user has already resolved once. Null (never a partial
     * guess) if no matching confirmed account exists anymore — falls through
     * to ordinary candidate creation, exactly as if nothing had been learned.
     */
    private suspend fun bindToLearnedInstitution(
        learnedName: String,
        tail: String?,
        currency: CurrencyCode,
        typeHint: AccountType?,
        evidence: List<String>,
    ): AccountIdentityResult? {
        val existingAccounts = (accountRepository.observeAllAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val match = existingAccounts.firstOrNull {
            it.name.contains(learnedName, ignoreCase = true) &&
                it.openingBalance.currencyCode == currency &&
                (tail == null || it.accountNumberTail == tail)
        } ?: return null
        return AccountIdentityResult(
            match.id,
            AccountIdentityDecision.BOUND_EXISTING,
            90,
            typeHint,
            evidence + "Learned institution mapping: '$learnedName' -> account '${match.name}' (previously promoted from a Candidate Account)",
        )
    }

    private suspend fun resolveOrCreateCandidate(
        envelope: NotificationEnvelope,
        tail: String?,
        currency: CurrencyCode,
        typeHint: AccountType?,
        evidence: List<String>,
    ): AccountIdentityResult {
        val expectedName = CandidateAccountNaming.nameFor(envelope.packageName, tail)
        val existingCandidates = (accountRepository.observeCandidateAccounts().first() as? LedgerResult.Success)?.data ?: emptyList()
        val existing = existingCandidates.firstOrNull { it.name == expectedName && it.openingBalance.currencyCode == currency }
        if (existing != null) {
            return AccountIdentityResult(
                existing.id,
                AccountIdentityDecision.CANDIDATE,
                0,
                typeHint,
                evidence + "Matched existing Candidate Account '${existing.name}' — institution still unrecognized, awaiting review",
            )
        }

        val account = Account(
            id = 0,
            name = expectedName,
            type = typeHint ?: AccountType.CHECKING,
            openingBalance = Money.zero(currency),
            accountNumberTail = tail,
            bankBrandId = null,
            isCandidate = true,
        )
        val result = accountRepository.insertAccount(account)
        val accountId = (result as? LedgerResult.Success)?.data ?: ensureDefaultAccountUseCase.execute()
        return AccountIdentityResult(
            accountId,
            AccountIdentityDecision.CANDIDATE,
            0,
            typeHint,
            evidence + "Unrecognized institution — created Candidate Account '$expectedName' pending review, currency preserved from the transaction ($currency), never merged into an existing account",
        )
    }

    private suspend fun createAccount(
        institution: InstitutionIdentity,
        tail: String,
        currency: CurrencyCode,
        typeHint: AccountType?,
    ): Long {
        val type = typeHint ?: AccountType.CHECKING
        val name = "${institution.name} ${if (type == AccountType.CREDIT) "Credit Card" else "Account"}"
        val account = Account(
            id = 0,
            name = name,
            type = type,
            openingBalance = Money.zero(currency),
            accountNumberTail = tail,
            bankBrandId = null,
        )
        val result = accountRepository.insertAccount(account)
        return (result as? LedgerResult.Success)?.data ?: ensureDefaultAccountUseCase.execute()
    }
}







