package com.sherif.ledger.core.designsystem.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

/**
 * Screen-level motion, modelled on `UINavigationController`'s push and pop.
 *
 * The behaviour being reproduced has three parts, and the previous implementation
 * had none of them:
 *
 *  1. **The incoming screen travels its full width.** It used to enter from one
 *     fifth of the way in, so a push read as a small nudge rather than a new
 *     surface arriving over the old one. A push should feel like a card being
 *     dealt onto the stack.
 *  2. **The outgoing screen parallaxes.** It drifts left by roughly a third of the
 *     width — never the full width — so it reads as *behind* the incoming screen
 *     rather than as an equal partner sliding out of frame. This depth cue is the
 *     single thing that makes iOS navigation feel layered, and it is why both
 *     screens must move at different rates.
 *  3. **The outgoing screen dims rather than fades.** A screen that fades to
 *     transparent reveals whatever is behind it (here, the window background) and
 *     flickers. iOS darkens it instead, so it recedes without dissolving. Compose's
 *     transition API gives us alpha rather than an overlay, so this is a shallow
 *     fade to a still-visible value — enough to sell depth, not enough to punch a
 *     hole in the frame.
 *
 * Timing is a single decelerate curve shared by every part of the gesture, because
 * the whole point is that the two screens are one movement. [Emphasized] is
 * deliberately close to iOS's own navigation curve: almost all the distance is
 * covered early, then a long quiet settle.
 */
object LedgerNavigationMotion {

    /** iOS-like decelerate: fast commitment, long tail. */
    val Emphasized = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** One push/pop lasts this long end to end. */
    const val DurationMs = 420

    /** How far the screen underneath travels, as a fraction of screen width. */
    private const val ParallaxFraction = 3

    /** How far the outgoing screen dims to. Not zero — see the class note. */
    private const val DimmedAlpha = 0.65f

    private fun <T> spec(): FiniteAnimationSpec<T> = tween(DurationMs, easing = Emphasized)

    /** A new screen pushed onto the stack: full-width travel from the right. */
    val StandardEnter: EnterTransition =
        slideInHorizontally(spec()) { it } + fadeIn(tween(DurationMs / 3))

    /** The screen being covered: parallax left, dimmed, never fully gone. */
    val StandardExit: ExitTransition =
        slideOutHorizontally(spec()) { -it / ParallaxFraction } +
            fadeOut(spec(), targetAlpha = DimmedAlpha)

    /** Going back: the screen underneath returns from its parallax offset. */
    val StandardPopEnter: EnterTransition =
        slideInHorizontally(spec()) { -it / ParallaxFraction } +
            fadeIn(spec(), initialAlpha = DimmedAlpha)

    /** Going back: the top screen leaves the way it came, full width. */
    val StandardPopExit: ExitTransition =
        slideOutHorizontally(spec()) { it } + fadeOut(tween(DurationMs, easing = Emphasized))

    /**
     * Switching between bottom-bar tabs is not a push — there is no hierarchy
     * between siblings, so horizontal travel would imply an order that does not
     * exist. A tab change cross-fades with a whisper of scale, the same way iOS
     * swaps a tab bar's view controllers.
     */
    val TabEnter: EnterTransition =
        fadeIn(tween(LedgerMotion.Short, easing = LedgerMotion.Standard)) +
            androidx.compose.animation.scaleIn(
                tween(LedgerMotion.Short, easing = LedgerMotion.Standard),
                initialScale = 0.98f,
            )

    val TabExit: ExitTransition =
        fadeOut(tween(LedgerMotion.Immediate, easing = LedgerMotion.Standard))
}
