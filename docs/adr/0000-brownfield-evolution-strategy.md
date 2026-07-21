# ADR-0000 — Brownfield Evolution Strategy

Status: Accepted (2026-07-21)

Supersedes: none. Foundational — this ADR governs how every subsequent
implementation ADR and milestone approaches the existing codebase.

Related documents: `LEDGER_MASTER_SPECIFICATION.md` v1.0 (Level-1 authority),
`LEDGER_CONSTITUTION.md` §13 (canonical decisions), `ARCHITECTURE_GAP_ANALYSIS.md`,
`ENGINEERING_STATUS.md`.

## Context

The Ledger specification (`LEDGER_MASTER_SPECIFICATION.md` v1.0) describes a
Financial Intelligence Platform substantially larger than the current
implementation. The current implementation is the product of releases RC4–RC9: a
single-module Android app with a working, audited deterministic financial engine,
an advisory-only AI infrastructure, a mature design system (LDL), and a
comprehensive diagnostics/Developer-Console suite. A full gap analysis
(`ARCHITECTURE_GAP_ANALYSIS.md`) found the engine layer to be ~60–80% present and
spec-aligned, and the product-experience layer ~10–20% present.

A foundational decision was required before implementation: evolve the existing
codebase, or rebuild against the specification.

## Problem

If this decision is left implicit, two engineers could reasonably build in opposite
directions — one preserving and extending RC9, another rewriting subsystems the
spec describes differently (e.g. the Financial Event model, the multi-module
structure). This ambiguity is the highest-severity risk identified in the gap
analysis.

## Decision

**Ledger evolves as a brownfield project. The RC9 codebase is the foundation. We do
not rebuild the project or discard working implementations.**

Concretely:

1. **Preserve working business logic and Financial Truth.** The deterministic
   financial engine (`BalanceCalculator`, `AccountBalanceService`, `CurrencyGuard`),
   `ReconciliationEngine`, `RelationshipEngine`, the capture pipeline, the AI
   infrastructure, and the LDL design system are preserved and extended, not
   rewritten. Frozen files (per `CLAUDE.md`) change only with a specific,
   demonstrated need.
2. **Refactor incrementally.** Architectural convergence toward the specification
   (Financial Event model, module boundaries, merchant/category consolidation,
   navigation/IA) happens through additive, reversible steps — never a big-bang
   rewrite. Each step must leave the app buildable and tests green.
3. **Replace only with clear architectural justification.** A component is replaced
   only when it cannot be evolved to meet the spec (e.g. a stub with hardcoded data
   that has no evolutionary path). Replacements are documented.
4. **Financial Truth and working logic take precedence over rewriting code.**
   Correctness and a working app outrank stylistic or structural purity. "Cleaner
   code" is not, by itself, justification to touch working financial logic.
5. **The specification is the source of truth for architectural *intent*; the
   codebase is the source of truth for implementation *status*** (Constitution
   §13/D3). The Backend Capability Matrix (spec Chapter 17) is a target, not a
   claim about what exists — always verify against the code.
6. **Process for unspecified detail.** Where the frozen specification intentionally
   leaves an implementation *algorithm* unspecified, we do not create an ADR — we
   write a **Technical Design Document (TDD)**: propose an implementation, explain
   tradeoffs, and implement after approval. ADRs are reserved for *architectural*
   decisions. Neither an ADR nor a TDD may block foundation work that does not
   depend on it.

## Alternatives Considered

- **Greenfield rebuild against the spec.** Rejected. It would discard a working,
  audited financial engine and AI infrastructure that already satisfy most of the
  specification's hardest invariants (Financial Truth, explainability,
  advisory-only AI). The cost and risk are disproportionate to the benefit, and it
  violates the spec's own principles ("deletion before abstraction," "architecture
  should become simpler over time," "preserve working logic").
- **Freeze the current app and build the new product beside it.** Rejected. Two
  parallel products fragment effort and duplicate the engine layer, contradicting
  the "One Financial Truth" principle.

## Consequences

Positive:
- The hardest-won, best-tested code (Financial Truth) is retained.
- Implementation can begin immediately on foundation and on wiring existing engines
  to product surfaces, rather than waiting on a rewrite.
- Risk is bounded per-step (additive, reversible, build-green gates).

Negative / accepted:
- Some architectural debt persists longer (e.g. `core/domain → feature/*` coupling,
  the two merchant systems) because it is refactored incrementally rather than
  eliminated up front. This is tracked, not hidden (`ENGINEERING_STATUS.md`).
- Naming/vocabulary migration (e.g. `Transaction` → `Financial Event`) proceeds
  gradually, so both terms coexist during transition (see the forthcoming
  Financial Event ADR).

## Migration Plan

Governed by `ARCHITECTURE_GAP_ANALYSIS.md` §10. Milestone 1 (Foundation Sprint)
performs alignment and cleanup only. The Financial Event model migration and all
feature work follow, incrementally, after the foundation is complete. No milestone
rewrites working business logic.
