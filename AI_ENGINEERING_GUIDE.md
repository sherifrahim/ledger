# AI_ENGINEERING_GUIDE.md

# Ledger AI Engineering Guide

**Version:** 1.0  
**Status:** Living Document  
**Applies to:** Every AI and every future contributor

---

# Purpose

This document is the single source of truth for AI contributors.

Every AI must read this document before proposing architecture, generating code, modifying UI, or creating new features.

Ledger is not an Android demo project.

Ledger is a long-term product with its own design language (LDL), engineering standards, and architectural philosophy.

---

# Vision

Ledger is building a privacy-first personal finance operating system.

The goal is not simply expense tracking.

The goal is to create the most premium finance experience available on Android while remaining:

- Offline First
- Privacy First
- Zero Ads
- Calm
- Intentional
- Fast
- Beautiful
- Trustworthy

---

# Product Philosophy

Ledger should feel:

- Calm
- Confident
- Deliberate
- Premium
- Financial
- Minimal
- Information-dense without feeling crowded

Users should immediately trust the product.

Nothing should feel accidental.

Nothing should feel generic.

---

# Ledger Design Language (LDL)

Compose is NOT the design language.

Compose is ONLY the rendering engine.

Material Design is NOT the design language.

Ledger Design Language (LDL) is the product.

LDL defines:

- Surfaces
- Motion
- Typography
- Spacing
- Hierarchy
- Interaction
- Navigation
- Components
- Visual rhythm

Every feature must reinforce LDL.

---

# Engineering Philosophy

Architecture before features.

Primitives before screens.

Reuse before creation.

Consistency before novelty.

Compile correctness before optimization.

Every line of code should make the next feature easier to build.

---

# Architecture

Current architecture:

MVVM

↓

Clean Architecture

↓

Repository Pattern

↓

Offline First

↓

Jetpack Compose UI

↓

Ledger Design Language

Feature modules consume LDL.

Features should never redefine LDL.

---

# LDL Ownership

LDL owns:

- Theme
- Colors
- Typography
- Motion
- Spacing
- Surface hierarchy
- Components
- Interactions
- Navigation primitives

Feature code owns:

- Screen logic
- ViewModels
- Business logic
- State
- Navigation flow
- Repositories

Feature code should never recreate LDL.

---

# Directory Ownership

core/designsystem/

Owns:

- Theme
- Motion
- Components
- Tokens
- Typography
- Surfaces

feature/

Owns:

- Screen implementation
- ViewModels
- UI State
- Domain interaction

presentation/

Owns:

- App navigation
- Global app wiring

---

# Motion Rules

LedgerMotion is the single source of truth for:

- Spring values
- Durations
- Press scale
- Press opacity
- Border alpha
- Surface highlight alpha
- Disabled alpha
- Tween timings
- Stagger timings

Never hardcode these values.

LedgerAnimations owns reusable animation choreography.

LedgerMotion owns animation physics.

---

# Design Tokens

Always use:

LedgerTheme

LedgerSpacing

LedgerMotion

LedgerAnimations

LedgerRadius

Never introduce local equivalents.

---

# Component Rules

Before creating a new component ask:

1.

Does LDL already provide this?

If yes

Reuse it.

2.

Can this become a reusable LDL primitive?

If yes

Create it inside LDL.

3.

Is this feature-specific?

Only then place it inside the feature.

---

# Material Design Policy

Material is allowed only when it is acting as a rendering primitive.

Material must never define Ledger's visual identity.

Avoid introducing:

- Material visual styling
- Material motion
- Material navigation
- Material interaction patterns

Replace Material behavior with LDL behavior whenever practical.

---

# Coding Standards

Never duplicate components.

Never duplicate tokens.

Never duplicate spacing.

Never duplicate motion values.

Never introduce magic numbers.

Never bypass LDL.

Prefer composition over duplication.

Prefer primitives over wrappers.

---

# Compile Safety

Every patch must:

Compile logically.

Verify imports.

Avoid unavailable APIs.

Avoid unavailable Material icons.

Avoid syntax mistakes.

Prefer minimal safe changes over large risky edits.

---

# AI Workflow

Claude

Role:

Product Architect

Responsibilities:

- Vision
- UX
- Sprint planning
- Major architecture
- LDL evolution

---

Gemini

Role:

Lead Android Engineer

Responsibilities:

- Kotlin
- Compose
- Compile correctness
- Refactoring
- Documentation
- Performance
- LDL implementation

---

ChatGPT

Role:

Architecture Reviewer

Responsibilities:

- LDL consistency
- Patch review
- Design review
- Cross-AI synchronization
- Preventing architectural drift

---

# Engineering Workflow

1.

Architecture

↓

2.

Review

↓

3.

Implementation

↓

4.

Compile

↓

5.

Physical device testing

↓

6.

Git commit

Never skip review.

---

# Design Review Checklist

Before submitting code ask:

Does it feel premium?

Does it reinforce LDL?

Does it introduce unnecessary complexity?

Does it duplicate existing primitives?

Can another feature reuse it?

Would this still make sense one year from now?

---

# Current Priorities

Current priority is NOT adding features.

Current priority is strengthening LDL.

Focus areas:

- Surface hierarchy
- Motion
- Typography
- Navigation
- Component quality
- Design consistency
- Premium interactions

---

# Long-Term Roadmap

Foundation

↓

Premium LDL

↓

Transaction Engine

↓

Review Inbox

↓

OCR

↓

AI Parsing

↓

Budgets

↓

Investments

↓

Analytics

↓

Cloud Sync (Optional)

---

# Definition of Done

A task is not complete because it compiles.

A task is complete when:

- Architecture remains clean.
- LDL becomes stronger.
- Code is reusable.
- Compile succeeds.
- Physical device experience improves.
- The application feels more premium than before.

---

# Final Principle

Every contribution should leave Ledger better than it was found.

Do not optimize for speed.

Optimize for craftsmanship.

Ledger is intended to become a premium long-term product, and every engineering decision should reinforce that goal.
