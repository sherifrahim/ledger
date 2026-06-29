package com.sherif.ledger.feature.review.presentation.preview

import com.sherif.ledger.core.preview.PreviewAccounts
import com.sherif.ledger.core.preview.PreviewMerchants
import com.sherif.ledger.feature.review.presentation.ReviewFilter
import com.sherif.ledger.feature.review.presentation.ReviewInboxUiState
import com.sherif.ledger.feature.review.presentation.ReviewItemUi

object ReviewInboxPreviewData {

    val state = ReviewInboxUiState(
        pendingCount = 5,
        confirmedTodayCount = 3,
        ignoredTodayCount = 1,
        items = listOf(
            ReviewItemUi(
                id = "r1", merchant = PreviewMerchants.noon.name,
                merchantCategory = PreviewMerchants.noon.category,
                merchantAccentHue = PreviewMerchants.noon.accentHue,
                amount = "245", suggestedCategory = "Shopping",
                suggestedAccount = "${PreviewAccounts.adcb.name} ${PreviewAccounts.adcb.accountNumber}",
                confidence = 72, reason = "Low confidence parse",
                timestamp = "Yesterday, 1:20 PM",
            ),
            ReviewItemUi(
                id = "r2", merchant = "Unknown Merchant",
                merchantCategory = "Unknown",
                merchantAccentHue = 0xFF6B7280,
                amount = "89.50", suggestedCategory = "Others",
                suggestedAccount = "${PreviewAccounts.adcb.name} ${PreviewAccounts.adcb.accountNumber}",
                confidence = 34, reason = "Merchant not recognized",
                timestamp = "Yesterday, 10:15 AM",
            ),
            ReviewItemUi(
                id = "r3", merchant = PreviewMerchants.careem.name,
                merchantCategory = PreviewMerchants.careem.category,
                merchantAccentHue = PreviewMerchants.careem.accentHue,
                amount = "28", suggestedCategory = "Transport",
                suggestedAccount = "${PreviewAccounts.wio.name} ${PreviewAccounts.wio.accountNumber}",
                confidence = 91, reason = "Amount differs from usual",
                timestamp = "Mon, 6:30 PM",
            ),
            ReviewItemUi(
                id = "r4", merchant = PreviewMerchants.lulu.name,
                merchantCategory = PreviewMerchants.lulu.category,
                merchantAccentHue = PreviewMerchants.lulu.accentHue,
                amount = "312", suggestedCategory = "Groceries",
                suggestedAccount = "${PreviewAccounts.adcb.name} ${PreviewAccounts.adcb.accountNumber}",
                confidence = 85, reason = "New merchant variant",
                timestamp = "Mon, 2:45 PM",
            ),
            ReviewItemUi(
                id = "r5", merchant = PreviewMerchants.etisalat.name,
                merchantCategory = PreviewMerchants.etisalat.category,
                merchantAccentHue = PreviewMerchants.etisalat.accentHue,
                amount = "349", suggestedCategory = "Bills",
                suggestedAccount = "${PreviewAccounts.fab.name} ${PreviewAccounts.fab.accountNumber}",
                confidence = 58, reason = "Duplicate transaction detected",
                timestamp = "Sun, 12:00 AM",
            ),
        ),
    )

    val emptyState = ReviewInboxUiState(items = emptyList())
}
