package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * LDL navigation motion vocabulary.
 *
 * Centralizes all screen-level transitions to ensure spatial
 * consistency across the application.
 */
object LedgerNavigationMotion {

    val StandardEnter: EnterTransition =
        slideInHorizontally(tween(LedgerMotion.StandardTweenMs)) { it / 5 } +
            fadeIn(tween(LedgerMotion.StandardTweenMs))

    val StandardExit: ExitTransition =
        slideOutHorizontally(tween(LedgerMotion.StandardTweenMs)) { -it / 5 } +
            fadeOut(tween(LedgerMotion.FastTweenMs))

    val StandardPopEnter: EnterTransition =
        slideInHorizontally(tween(LedgerMotion.StandardTweenMs)) { -it / 5 } +
            fadeIn(tween(LedgerMotion.StandardTweenMs))

    val StandardPopExit: ExitTransition =
        slideOutHorizontally(tween(LedgerMotion.StandardTweenMs)) { it / 5 } +
            fadeOut(tween(LedgerMotion.FastTweenMs))
}
