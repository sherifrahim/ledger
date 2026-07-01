# ARCHITECTURE_LOCK.md

# Ledger Architectural Invariants
**Status:** Frozen (Release Candidate Baseline)

This document records architectural decisions that are considered immutable. Future changes must preserve these invariants unless explicitly approved.

---

## 1. Edge-to-Edge Implementation
*   **Authority:** `MainActivity` and `styles.xml`.
*   **Mechanism:** `enableEdgeToEdge()` and `WindowCompat.setDecorFitsSystemWindows(window, false)`.
*   **Rule:** The application must own the entire window buffer. System bar transparency is managed via the theme and root-level atmosphere components.

## 2. Hero Composition Model
*   **Authority:** `LedgerCollapsingHero.kt`.
*   **Mechanism:** Single container interpolating height and corner radius based on a 0.0f - 1.0f progress value.
*   **Rule:** The container accepts two slots (`expandedContent` and `compactContent`). The expanded slot fills the container, while the compact slot is pinned to the `collapsedHeight`.

## 3. AnimatedVisibility Gating
*   **Authority:** `LedgerCollapsingHero.kt`.
*   **Rule:** `CompactHero` must be gated by `AnimatedVisibility` (typically `visible = progress >= 0.85f`). 
*   **Rationale:** This removes the compact content from the layout/measurement pass when the hero is expanded, preventing spatial interference (e.g., the "4dp height bug").

## 4. Status Bar Ownership
*   **Authority:** Screen-level Root Content (e.g., `ExpandedHero`).
*   **Rule:** `ExpandedHero` owns `statusBarsPadding()`. `CompactHero` must NEVER own status bar padding.
*   **Rationale:** `ExpandedHero` intentionally bleeds behind the status bar, whereas `CompactHero` occupies a fixed height that does not account for insets.

## 5. Atmosphere & Background Ownership
*   **Authority:** Screen Root `Box`.
*   **Rule:** The screen root owns the global `LedgerAtmosphereGlow`. Hero slots do not own their own backgrounds.
*   **Rationale:** This ensures a continuous atmospheric wash that bleeds edge-to-edge even as the hero container collapses or clips.

## 6. Navigation Architecture
*   **Authority:** `LedgerBottomBar.kt` (Floating Dock).
*   **Rule:** Navigation is implemented as a floating translucent dock. It is a canonical Ledger element.
*   **Constraint:** Root screens must provide `bottom = LedgerSpacing.ScreenBottom + 100.dp` padding to ensure clear scroll termination above the dock.

## 7. Dashboard Layout Hierarchy
*   **Authority:** `DashboardScreen.kt`.
*   **Rule:** Centered Monolith (landmark, amount, currency) distributed via `Arrangement.Center` or deterministic vertical arrangement.
*   **Rule:** Summary Metrics are anchored to the bottom of the hero area.
*   **Rule:** Vertical distribution is driven by a `1.3f` weighted spacer to ensure optical balance across varying device aspect ratios.
