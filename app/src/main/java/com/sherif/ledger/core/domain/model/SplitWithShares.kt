package com.sherif.ledger.core.domain.model

/**
 * A split bundled with each share's participant already resolved — read shape
 * for consumers (ViewModels) that need "who owes what" without a separate
 * participant lookup per share.
 */
data class SplitWithShares(
    val split: Split,
    val shares: List<ShareWithParticipant>,
) {
    val totalOwedMinor: Long get() = shares.sumOf { it.share.shareAmountMinor }
    val outstandingMinor: Long get() = shares.filter { !it.share.isSettled }.sumOf { it.share.shareAmountMinor }
}

data class ShareWithParticipant(
    val share: SplitShare,
    val participant: Participant,
)

