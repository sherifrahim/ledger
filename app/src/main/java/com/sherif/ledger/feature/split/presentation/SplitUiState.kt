package com.sherif.ledger.feature.split.presentation

import com.sherif.ledger.core.domain.model.CurrencyCode

/**
 * Split screen state. V1 supports the common case — an EQUAL split: pick who
 * shared the expense (you are always included in the division), and Ledger tells
 * each person their equal share. Split is isolated from Financial Truth: it never
 * touches a balance, only "who owes whom".
 */
data class SplitUiState(
    val loading: Boolean = true,
    val merchant: String = "",
    val totalMinor: Long = 0,
    val currency: CurrencyCode = CurrencyCode.AED,
    val participants: List<SplitParticipantUi> = emptyList(),
    /** True once a split exists for this transaction — the screen shows shares to settle instead of the picker. */
    val hasSplit: Boolean = false,
    val splitId: String? = null,
    /** Your (self) share — the implicit remainder, never stored/owed. */
    val yourShareMinor: Long = 0,
    val outstandingMinor: Long = 0,
    val error: String? = null,
)

data class SplitParticipantUi(
    val id: String,
    val name: String,
    val isSelf: Boolean,
    /** Picker state (before a split exists): is this person included in the split? */
    val selected: Boolean,
    /** After a split exists: this person's owed amount and settle state. */
    val shareId: String? = null,
    val shareMinor: Long = 0,
    val settled: Boolean = false,
)
