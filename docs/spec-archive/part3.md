# ============================================================================
# PART III — PLATFORM ARCHITECTURE
# ============================================================================

# Chapter 13 — System Architecture

Ledger follows a layered architecture.

Each layer has a single responsibility.

No layer should contain business logic that belongs elsewhere.

```
                    User Interface
                          │
                          ▼
                  Product Experience
                          │
                          ▼
                Intelligence Layer
                          │
                          ▼
                 Financial Engine
                          │
                          ▼
               Universal Ingestion
                          │
                          ▼
                  External Sources
```

The system is intentionally deterministic.

The UI never owns financial logic.

The Intelligence Layer never mutates financial truth.

The Financial Engine never depends on presentation.

The ingestion layer never contains business rules.

---

## Architectural Layers

### Layer 1 — External Sources

Responsible for acquiring data.

Possible sources:

- Open Banking
- CSV
- PDF
- Email
- SMS
- Notifications
- OCR
- Manual Entry
- Future connectors

Rules

• Never trusted.

• Always normalized.

• Always versioned.

---

### Layer 2 — Universal Data Ingestion

Purpose:

Transform every source into one canonical format.

Responsibilities

• Parsing

• Normalization

• Validation

• Deduplication

• Currency normalization

• Institution identification

• Merchant normalization

• Metadata extraction

Outputs

Financial Events.

Nothing else.

No UI logic.

No AI.

No forecasting.

---

### Layer 3 — Financial Engine

This is Ledger's source of truth.

Responsibilities

• Financial Events

• Accounts

• Balances

• Categories

• Budgets

• Transfers

• Institutions

• Currency

• Reconciliation

Every value displayed anywhere in the application must ultimately originate here.

---

### Layer 4 — Intelligence Layer

Consumes Financial Truth.

Produces Knowledge.

Never mutates financial history directly.

Sub-engines include:

Merchant Intelligence

Relationship Engine

Learning Engine

Forecast Engine

Subscription Detection

Recurring Payment Detection

Anomaly Detection

Recommendation Engine

Explainability Engine

These engines operate independently but communicate through shared financial events.

---

### Layer 5 — Product Experience

Responsible for presentation.

Examples

Dashboard

Financial Story

Review Queue

Merchant Pages

Forecast

Insights

Search

Widgets

Notifications

Settings

No business logic belongs here.

---

# Chapter 14 — Domain Architecture

Ledger models real-world financial concepts.

Not UI concepts.

Core Domains

Institution

Account

Financial Event

Merchant

Category

Budget

Goal

Relationship

Forecast

Insight

Review

Notification

Learning Rule

Evidence

Confidence

Every feature should belong to exactly one primary domain.

Cross-domain communication occurs through the Financial Engine.

---

## Domain Relationships

Institution

↓

Accounts

↓

Financial Events

↓

Merchant Intelligence

↓

Relationships

↓

Insights

↓

Forecasts

↓

Financial Story

The flow is intentionally one-directional.

Financial Story never changes Financial Events.

Forecasts never modify balances.

Insights never overwrite history.

---

# Chapter 15 — Universal Data Ingestion

Universal Data Ingestion is the final foundational backend capability.

Its purpose is to eliminate source-specific behavior.

```
Notification
      │
SMS
      │
Email
      │
CSV
      │
PDF
      │
OCR
      │
Open Banking
      │
Manual Entry
      ▼

Universal Parser

      ▼

Normalizer

      ▼

Validator

      ▼

Deduplicator

      ▼

Institution Resolver

      ▼

Merchant Resolver

      ▼

Financial Event

      ▼

Financial Engine
```

Every ingestion source must satisfy the same contract.

Output:

Financial Events.

Never UI objects.

Never transactions tied to a specific provider.

---

## Benefits

Adding a new ingestion source should require:

Parser

Normalizer mapping

Connector

Nothing else.

Every downstream system continues functioning unchanged.

---

# Chapter 16 — Intelligence Architecture

Ledger Intelligence is modular.

Each engine has one responsibility.

---

Merchant Intelligence

Purpose

Understand merchants.

Produces

Merchant Profiles

Merchant Memory

Merchant Confidence

Merchant Categories

Merchant Relationships

---

Relationship Engine

Purpose

Discover relationships between entities.

Produces

Merchant ↔ Category

Merchant ↔ Institution

Account ↔ Goal

Recurring Relationships

Behavioral Links

---

Learning Engine

Purpose

Observe user corrections.

Produces

Personalized Rules

Confidence Improvements

Review Reduction

Learning History

---

Forecast Engine

Purpose

Project future financial state.

Produces

Cash Flow

Balance Forecasts

Subscription Forecasts

Budget Projections

Safe-to-Spend

---

Explainability Engine

Purpose

Generate reasoning.

Produces

Evidence Chains

Confidence

Decision History

Correction Paths

---

Anomaly Engine

Purpose

Detect unusual activity.

Produces

Alerts

Suspicious Spending

Behavioral Deviations

Unexpected Merchant Activity

---

# Chapter 17 — Backend Capability Matrix

The product should be implemented only after confirming backend readiness.

| Capability | Backend | Status |
|------------|---------|--------|
| Financial Events | Financial Engine | Complete |
| Accounts | Financial Engine | Complete |
| Institutions | Institution Registry | Complete |
| Merchant Intelligence | Merchant Engine | Complete |
| Relationship Discovery | Relationship Engine | Complete |
| Learning | Learning Engine | Complete |
| Explainability | Explainability Engine | Complete |
| Forecasting | Forecast Engine | Complete |
| Dashboard | Product Layer | Pending UI |
| Financial Story | Product Layer | Pending UI |
| Review Queue | Product Layer | Pending UI |
| Merchant Detail | Product Layer | Pending UI |
| Search | Product Layer | Pending UI |
| Widgets | Product Layer | Pending UI |
| Notifications | Product Layer | Pending UI |
| Universal Data Ingestion | RC10 | Planned |

---

# Chapter 18 — Traceability Matrix

Every product feature must map to backend capabilities.

| Product Feature | Required Engine |
|-----------------|----------------|
| Dashboard | Financial Engine + Forecast + Insights |
| Financial Story | Relationship + Forecast + Merchant Intelligence |
| Review Queue | Learning Engine |
| Merchant Detail | Merchant Intelligence |
| Institution Detail | Institution Registry |
| Budget Overview | Financial Engine |
| Spending Insights | Relationship + Intelligence |
| Forecast Screen | Forecast Engine |
| Safe to Spend | Forecast + Financial Engine |
| Explain Button | Explainability Engine |
| Search | Financial Engine + Merchant Index |
| Notifications | Forecast + Rules + Intelligence |

If a feature cannot be traced to backend capabilities,

the feature specification is incomplete.

---

# Chapter 19 — Architectural Rules

The following rules are permanent.

1.

No UI component performs financial calculations.

---

2.

No AI engine directly mutates Financial Truth.

---

3.

Every AI decision is reproducible.

---

4.

Every financial event is immutable.

Corrections generate new history.

Never overwrite.

---

5.

Every backend service has one responsibility.

---

6.

Every product feature must be explainable.

---

7.

Universal Data Ingestion is the only entry point for external financial data.

---

8.

Presentation depends on Engines.

Engines never depend on Presentation.

---

9.

Financial Truth always precedes Intelligence.

---

10.

Intelligence always precedes Presentation.

---

# End of Part III
