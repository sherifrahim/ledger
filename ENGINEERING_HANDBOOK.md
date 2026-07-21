# ============================================================================
# 02_ENGINEERING_HANDBOOK.md
# ============================================================================

Version: 1.0

Status:
Canonical Engineering Reference

Audience

Engineers

AI Contributors

Maintainers

Architects

====================================================================

# Chapter 1 — Engineering Philosophy

Engineering exists to preserve product quality.

Not simply to deliver features.

Every implementation decision should improve at least one of the following:

• Simplicity

• Maintainability

• Determinism

• Performance

• Testability

• Explainability

Never optimize for short-term convenience at the expense of long-term architecture.

Ledger is expected to exist for many years.

Write code accordingly.

---

## Core Principles

Correctness before speed.

Architecture before shortcuts.

Readability before cleverness.

Deletion before abstraction.

Determinism before automation.

Consistency before novelty.

If a decision violates multiple principles,

it is almost certainly incorrect.

====================================================================

# Chapter 2 — Repository Organization

Repository

```

ledger/

```

Top Level

```

app/

core/

domain/

feature/

shared/

docs/

scripts/

buildSrc/

gradle/

```

Never create miscellaneous folders.

Every directory should communicate ownership.

---

Recommended Feature Structure

```

feature/story/

feature/dashboard/

feature/review/

feature/search/

feature/settings/

feature/import/

feature/forecast/

feature/merchant/

feature/institution/

```

Each feature owns

UI

Navigation

ViewModel

Use Cases

Tests

DI

No feature should depend on another feature's UI.

====================================================================

# Chapter 3 — Module Responsibilities

Core

Contains

Utilities

Logging

Configuration

Base Classes

No business logic.

---

Domain

Contains

Entities

Interfaces

Use Cases

Business Rules

Framework independent.

---

Data

Contains

Repositories

Data Sources

Database

Network

Parsers

Never expose implementation details upward.

---

Presentation

Contains

Compose

Navigation

ViewModels

Animations

UI Models

Never calculate financial values here.

====================================================================

# Chapter 4 — Domain Rules

Domain is sacred.

Nothing external should leak into Domain.

Forbidden

Android Context

Compose

Room

Retrofit

DataStore

Navigation

Framework classes

Allowed

Entities

Interfaces

Business Rules

Value Objects

Use Cases

Domain should compile without Android.

====================================================================

# Chapter 5 — Package Naming

Packages describe capability.

Never implementation.

Good

merchant

forecast

institution

review

story

learning

Bad

helpers

misc

utils2

temp

newcode

old

Package names are architecture.

====================================================================

# Chapter 6 — Naming Rules

Classes

Nouns

Use Cases

Verbs

Interfaces

Capabilities

Repositories

Responsibilities

Avoid

Manager

Processor

Controller

Handler

Utility

Generic names indicate unclear ownership.

====================================================================

# Chapter 7 — Dependency Injection

Dependency Injection exists to remove coupling.

Not increase it.

Inject

Interfaces

Never concrete implementations.

Dependencies should point inward.

Never upward.

Feature modules should expose

only public interfaces.

====================================================================

# Chapter 8 — State Management

Every screen owns one source of truth.

State should be

Immutable

Serializable where appropriate

Observable

Predictable

Recoverable

Avoid

Mutable global state.

Shared mutable objects.

Hidden caches.

====================================================================

# Chapter 9 — Error Handling

Errors are domain concepts.

Not exceptions.

Represent failures explicitly.

Every recoverable error should include

Cause

Recovery

User Message

Technical Details

Never swallow exceptions.

====================================================================

# Chapter 10 — Logging

Logging exists for diagnosis.

Not debugging convenience.

Levels

Error

Warning

Information

Debug

Verbose

Never log

Passwords

Tokens

Personal financial information

Authentication secrets

====================================================================

# Chapter 11 — Performance

Target Budgets

Cold Start

<2 seconds

Warm Start

<500ms

Navigation

<150ms

Compose recomposition

Minimal

Database

Indexed

Background work

Asynchronous

Measure first.

Optimize second.

====================================================================

# Chapter 12 — Memory

Avoid

Large object graphs

Duplicate models

Repeated parsing

Repeated database reads

Prefer

Streaming

Caching

Immutable models

Lazy loading

====================================================================

# Chapter 13 — Testing

Testing Pyramid

Unit

Integration

UI

Manual

Critical Coverage

Financial Engine

Forecast

Merchant

Learning

Review Queue

Import

Synchronization

Currency

Evidence

No financial calculation ships untested.

====================================================================

# Chapter 14 — Documentation

Every public API

Documented.

Every module

README.

Every architecture decision

ADR.

Every feature

Specification.

Documentation is code.

Treat it accordingly.

====================================================================

# Chapter 15 — Code Review

Every review asks

Does this preserve Financial Truth?

Is this deterministic?

Can this be explained?

Does it reduce complexity?

Can another engineer understand it?

Will this still make sense in three years?

====================================================================

# Chapter 16 — Git Workflow

Main

Stable.

Develop

Integration.

Feature Branches

One feature.

One purpose.

Commit messages

Describe intent.

Not implementation.

Example

Good

Implement Merchant Relationship Engine

Bad

Fixed stuff

====================================================================

# Chapter 17 — Release Checklist

Every release verifies

Financial calculations

Forecast correctness

Merchant accuracy

Review Queue

Accessibility

Performance

Localization

Offline behavior

Crash-free startup

Privacy

Security

No release bypasses checklist.

====================================================================

# Chapter 18 — ADRs

Architecture Decision Records

Every major architectural decision

must receive an ADR.

Template

Problem

Decision

Alternatives

Consequences

Status

Related Documents

Examples

ADR-001

Financial Events are immutable.

ADR-002

Universal Data Ingestion.

ADR-003

Explainable Intelligence.

====================================================================

# Chapter 19 — AI Contributor Rules

Future AI systems

must follow

LEDGER_MASTER_SPECIFICATION.md

before generating code.

AI contributors should

Explain architectural conflicts.

Never invent product behavior.

Never remove explainability.

Never bypass Financial Truth.

Never weaken architecture

for implementation convenience.

====================================================================

# Chapter 20 — Engineering Commandments

1.

Architecture before implementation.

2.

Domain before framework.

3.

Financial Truth before AI.

4.

Determinism before convenience.

5.

Readable before clever.

6.

Delete complexity.

7.

Every feature deserves tests.

8.

Documentation evolves with code.

9.

Every architecture decision is recorded.

10.

Future engineers should thank you.

# END
