package com.sherif.ledger.core.domain.model

import java.time.Instant

/**
 * A person who can be part of a split. Local-only in V1 — no account, no login,
 * no cloud identity. [id] is a UUID rather than an auto-incrementing key
 * specifically so a future cross-device sync (V2) can reference the same
 * participant consistently across devices without an ID-remapping migration —
 * that's the one concrete schema decision made now for V2, everything else
 * (groups, invitations, remote identity) is deliberately left unbuilt.
 *
 * [isSelf] marks exactly one reserved participant representing the app's own
 * user — needed so equal-split arithmetic knows the true participant count
 * (dividing by 3 for "you, Ahmed, Ali", not 2), without that participant ever
 * generating a SplitShare of their own. Created once, reused across every split.
 */
data class Participant(
    val id: String,
    val name: String,
    val isSelf: Boolean = false,
    val createdAt: Instant,
)

