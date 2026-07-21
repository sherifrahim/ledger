# Ledger — Visual Architecture

RC9. Companion to `FINANCIAL_ENGINE.md`, `INTELLIGENCE_ENGINE.md`,
`PIPELINE_ARCHITECTURE.md`, `MERCHANT_ARCHITECTURE.md`, and
`ENGINEERING_STATUS.md` — this document is the visual index into all of them.
Every diagram below reflects the REAL, current code (verified while writing
each companion doc), not an aspirational design.

## 1. Package architecture

```mermaid
flowchart TB
    subgraph presentation["presentation/ + feature/*/presentation"]
        Dashboard["presentation/dashboard"]
        Screens["feature/{accounts,transactions,analytics,onboarding,review,settings}/presentation"]
        Nav["presentation/navigation\n(LedgerNavHost, LedgerRoute)"]
    end

    subgraph debugOnly["app/src/debug (debug builds only)"]
        DevConsole["DebugConsoleScreen\n+ BalanceInspector / AiMetrics / AiDebug / IntelligenceInspector"]
    end

    subgraph featureLayer["feature/* (business logic, not just UI)"]
        Capture["feature/capture\n(sms, notification, parsing, extraction, reconciliation, source)"]
        Merchant["feature/merchant"]
        Relationship["feature/relationship"]
        Semantic["feature/semantic"]
        AI["feature/ai"]
        Diagnostics["feature/diagnostics"]
    end

    subgraph coreDomain["core/domain"]
        Models["model/"]
        RepoIfaces["repository/ (interfaces)"]
        UseCases["usecase/"]
        Services["service/\n(account, transaction, intelligence, diagnostic)"]
    end

    subgraph coreDatabase["core/database (Room)"]
        Entities["entity/"]
        Daos["dao/"]
        RoomRepos["repository/ (Room* impls)"]
        Migrations["migration/"]
    end

    subgraph coreDi["core/di (Hilt modules)"]
        DbModule["DatabaseModule"]
        RepoModule["RepositoryModule"]
        OtherModules["Parsing/Extraction/AccountIdentity/\nDiagnosticCollector/AiProvider modules"]
    end

    subgraph designSystem["core/designsystem (LDL)"]
        Theme["theme, tokens, motion"]
        Components["reusable Compose components"]
    end

    presentation --> featureLayer
    presentation --> coreDomain
    debugOnly --> coreDomain
    debugOnly --> featureLayer
    featureLayer --> coreDomain
    coreDomain -.->|"real, confirmed coupling\n(see ENGINEERING_STATUS.md Phase F)"| featureLayer
    coreDatabase -->|implements| RepoIfaces
    coreDi -->|wires| coreDatabase
    coreDi -->|wires| coreDomain
    coreDi -->|wires| featureLayer
    presentation --> designSystem
    debugOnly --> designSystem
```

**The dotted line is real, not a mistake.** `core/domain` importing from
`feature/*` (e.g. `AccountBalanceService` importing `feature.relationship.RelationshipEngine`,
`DeterministicAccountIdentityResolver` importing `feature.capture.notification.NotificationEnvelope`)
is a confirmed, pervasive, and — per `ENGINEERING_STATUS.md`'s Phase F
findings — an accepted architectural characteristic of this codebase, not a
bug introduced by any single RC. `feature/*` here means "shared domain-adjacent
services" (relationship inference, merchant intelligence, capture pipeline),
not "UI screens" — the actual UI-only code lives in `feature/*/presentation`
and `presentation/*`, which is a one-way dependency into `core/domain`/`feature/*`
as expected.

## 2. Live capture pipeline (every SMS/notification)

```mermaid
flowchart TD
    A["Notification / SMS"] --> B{"NotificationFilter\nAccept or Reject?"}
    B -->|Rejected| Z1(["Discarded — no diagnostics persisted beyond the trace"])
    B -->|Accepted| C["ExtractionRegistry\n(runs ALL applicable extractors,\nranks by confidence, contested-result penalty)"]
    C --> D["DeterministicFinancialIntentClassifier\nSOLE routing authority"]
    D -->|FINANCIAL_CONFIRMATION| E1["ConfirmationMatcher / ConfirmationInterpreter\nNever persists"]
    D -->|FINANCIAL_INFORMATION| Z2(["Discarded — diagnostics only"])
    D -->|UNKNOWN| Z3(["Discarded — diagnostics only"])
    D -->|FINANCIAL_EVENT| F["ReconciliationEngine\nNew / Updated / Duplicate / Ignored"]
    F -->|Duplicate or Ignored| Z4(["No insert"])
    F -->|Updated| Z5(["Existing transaction logically updated\n(no new row)"])
    F -->|New| G["DeterministicAccountIdentityResolver"]
    G --> G1{"InstitutionRegistry\nrecognized?"}
    G1 -->|"No, but LearnedDecisionStore\nhas a prior confirmation"| G2["Bind to the previously-\npromoted account"]
    G1 -->|"No, and never learned"| G3["Create/reuse a\nCandidate Account\n(isCandidate=true)"]
    G1 -->|Yes| G4{"Tail present?"}
    G4 -->|No| G5["FALLBACK_DEFAULT\n(default account)"]
    G4 -->|Yes| G6{"Score >= BIND_THRESHOLD\nagainst an existing account?"}
    G6 -->|Yes| G7["BOUND_EXISTING"]
    G6 -->|No| G8{"Single-shot or repeated\nobservation clears the bar?"}
    G8 -->|Yes| G9["CREATED_NEW"]
    G8 -->|No| G5
    G2 --> H["InsertTransactionUseCase"]
    G3 --> H
    G5 --> H
    G7 --> H
    G9 --> H
    H --> H1["System B: MerchantResolver (brandId)\n+ CategoryResolver (categoryId, always null)"]
    H1 --> I[("transactions table\nfingerprint-unique, indexed")]
```

## 3. Read-time intelligence pipeline (never runs during capture)

```mermaid
flowchart LR
    T[("transactions table")] --> MR["feature.merchant.MerchantResolver\n(canonical name, category, confidence, reason)"]
    MR --> CIE["CategoryIntelligenceEngine\n5 tiers: memory -> registry -> relationship hint\n-> keywords -> on-demand AI"]
    T --> RE["RelationshipEngine.analyze()\n9 resolvers, run ONCE per computation\nconfidence + reasoning + full diagnostics"]
    RE --> RSA["RecurringScheduleAnalyzer\nfrequency, last/next occurrence, variance confidence"]
    RSA --> FE["ForecastEngine\nexpected balance, upcoming bills,\nprojected salary, 6mo history"]
    T --> ABS["AccountBalanceService\nBalanceCalculator replay — the ONLY balance truth"]
    CIE --> GFA["GetFinancialAnalyticsUseCase\nshapes everything above for a screen"]
    RE --> GFA
    ABS --> GFA
    GFA --> Dash["Dashboard / Insights / Transactions / Accounts screens"]
    FE -.->|"not wired to any screen yet\n(INTELLIGENCE_ENGINE.md Suggested RC9"| Dash
```

## 4. AI Orchestration (advisory-only, never a write path)

```mermaid
sequenceDiagram
    participant Caller as CategoryIntelligenceEngine<br/>(only current caller)
    participant Gate as ConfidenceGate
    participant Orch as AIOrchestrator
    participant Cache as AiSuggestionCache
    participant Provider as LLMProvider (primary)
    participant Fallback as LLMProvider (fallback)
    participant Validator as AISuggestionValidator
    participant Audit as AiAuditLogger (DB)
    participant Debug as AiDebugTraceStore (in-memory)

    Caller->>Gate: shouldConsultAi(capability, deterministicConfidence)
    Gate-->>Caller: true (only if below user threshold)
    Caller->>Orch: requestSuggestion(capability, context)
    Orch->>Cache: get(capability, context)
    alt cache hit
        Cache-->>Orch: cached AISuggestion
        Orch-->>Caller: Suggested (providerId="cache")
    else cache miss
        Orch->>Provider: complete(prompt) [up to 2 attempts]
        alt primary fails
            Orch->>Fallback: complete(prompt) [up to 2 attempts]
        end
        Orch->>Validator: validate(capability, suggestion)
        alt invalid
            Orch->>Audit: record(success=false)
            Orch->>Debug: record trace
            Orch-->>Caller: Failed(reason)
        else valid
            Orch->>Cache: put(capability, context, suggestion)
            Orch->>Audit: record(success=true)
            Orch->>Debug: record trace
            Orch-->>Caller: Suggested(suggestion)
        end
    end
    Note over Caller,Debug: The deterministic engine (CategoryIntelligenceEngine)<br/>decides what happens with the opinion — never a DB write.
```

## 5. Developer Console — Explainability surface (RC5-RC9)

```mermaid
flowchart TB
    DC["DebugConsoleScreen\n(the REAL console — DeveloperConsoleScreen\nwas dead code, removed in RC9)"]
    DC --> PD["Pipeline Diagnostics\nPipelineTraceSink"]
    DC --> LD["Ledger Diagnostics\nDiagnosticBundleGenerator\n(9 DiagnosticCollectors)"]
    DC --> BI["Balance Inspector\nFinancialTraceCollector + GetFinancialAnalyticsUseCase\nside-by-side, asserts they agree"]
    DC --> AM["AI Metrics\nAiMetricsService over ai_audit_log"]
    DC --> AD["AI Debug\nAiDebugTraceStore (last 20, in-memory only)"]
    DC --> II["Intelligence Inspector (RC8/RC9)\nMerchant + Category + Relationship + Recurring\n+ Forecast + Learned Decisions + Duplicate Reasoning"]
    II -->|"user-triggered only"| AskAI["Ask AI button\n-> CategoryIntelligenceEngine.resolveWithAiFallback"]
    LD --> Collectors["AccountCollector, RelationshipCollector, PipelineCollector,\nDatabaseHealthCollector, NotificationCollector, AppInfoCollector,\nLiveLogCollector, ImportSummaryCollector, InstitutionDiagnosticsCollector,\nFinancialTraceCollector"]
```

## 6. Database schema (Room, version 10)

```mermaid
erDiagram
    accounts ||--o{ transactions : "account_id"
    accounts {
        long id PK
        string name
        string type
        long opening_balance_minor
        string currency_code
        string account_number_tail
        bool is_deleted
        bool is_candidate "RC7"
    }
    transactions {
        long id PK
        long account_id FK
        long brand_id FK "System B, real, read by TransactionDetailsViewModel"
        long category_id FK "System B, ALWAYS NULL — see MERCHANT_ARCHITECTURE.md"
        long amount_minor
        string currency_code
        string type
        long timestamp_millis
        string fingerprint UK
        string origin_package_name
        string card_tail
    }
    brands ||--o{ transactions : "brand_id (crude, exact-text grouping)"
    brands ||--o{ merchant_aliases : "brand_id"
    categories ||--o{ transactions : "category_id (dead FK — never populated)"
    merchant_category_overrides {
        string merchant_key PK "System A learned memory"
        string category
    }
    learned_decisions {
        string decision_type PK "RC8 generic learned memory"
        string subject_key PK
        string learned_value
    }
    ai_audit_log {
        long id PK
        string capability
        string provider_id
        bool success
    }
    participants ||--o{ split_shares : "participant_id"
    splits ||--o{ split_shares : "split_id"
    transactions ||--o| splits : "transaction_id"
```

**`category_id`'s FK to `categories` is real but permanently unpopulated** —
confirmed in `MERCHANT_ARCHITECTURE.md`. This is why RC9 did NOT drop the
`categories` table when removing its dead application-layer wrapper
(`CategoryRepository`/`RoomCategoryRepository`/domain `Category` model) —
the table itself is still a live FK target.

## 7. Account Resolution decision surface (RC7/RC8, detailed)

```mermaid
flowchart TD
    Start(["New transaction candidate"]) --> Inst{"institutionRegistry.resolve(sender)"}
    Inst -->|null| Learned{"learnedDecisionStore.valueFor\n(INSTITUTION, sender)"}
    Learned -->|"found, matching account\nstill exists"| Bound1["BOUND_EXISTING\nconfidence=90"]
    Learned -->|"not found, or account\nno longer exists"| Candidate["resolveOrCreateCandidate\nCANDIDATE, confidence=0"]
    Inst -->|found| Tail{"candidate.accountHint\npresent?"}
    Tail -->|No| Fallback["FALLBACK_DEFAULT\nconfidence=0"]
    Tail -->|Yes| Score["scoreAgainstExisting()\nper existing account"]
    Score --> Bind{"best score\n>= BIND_THRESHOLD (75)?"}
    Bind -->|Yes| Bound2["BOUND_EXISTING"]
    Bind -->|No| Hyp["scoreHypothetical()"]
    Hyp --> Single{">= CREATE_SINGLE_SHOT\n_THRESHOLD (95)?"}
    Single -->|Yes| Created1["CREATED_NEW\n(single-observation near-certainty)"]
    Single -->|No| Repeat{">= CREATE_OBSERVATION_MIN_SCORE (60)\nAND 3rd independent sighting?"}
    Repeat -->|Yes| Created2["CREATED_NEW\n(repeated observation)"]
    Repeat -->|No| Fallback
```

## Cross-reference

| Doc | Covers |
|---|---|
| `FINANCIAL_ENGINE.md` | Institution Registry, Account Resolver, Currency Rules, Balance Calculation, Reconciliation, AI Boundaries |
| `INTELLIGENCE_ENGINE.md` | Merchant/Learning/Category/Relationship/Recurring/Duplicate/Forecast engines, Confidence Model, Decision Hierarchy |
| `MERCHANT_ARCHITECTURE.md` | The System A vs System B investigation and merge decision (diagram 6 above) |
| `PIPELINE_ARCHITECTURE.md` | Stage-by-stage input/output/confidence/reason/fallback/AI/failure-mode audit (diagrams 2-3 above) |
| `ENGINEERING_STATUS.md` | Technical debt, frozen systems, dependency findings, performance findings, product-readiness scorecard |
