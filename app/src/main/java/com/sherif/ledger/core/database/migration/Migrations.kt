package com.sherif.ledger.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds Transaction Notes and Ledger Split. Purely additive — two nullable
 * columns on the existing transactions table, three new tables — no existing
 * row is touched or transformed, so this is a real, safe migration rather than
 * a destructive fallback. The database now has real user financial history in
 * it; wiping it on every schema change stopped being an acceptable tradeoff.
 *
 * Every column, type, nullability, default, and index name below must match
 * what TransactionEntity/ParticipantEntity/SplitEntity/SplitShareEntity declare
 * exactly, including Room's own index-naming convention
 * (index_<table>_<col1>_<col2>...) — Room validates the resulting schema
 * against the entity annotations at first open after migrating, and throws if
 * they disagree.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT")
        db.execSQL("ALTER TABLE transactions ADD COLUMN note_updated_at INTEGER")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS participants (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                is_self INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS splits (
                id TEXT NOT NULL PRIMARY KEY,
                transaction_id INTEGER NOT NULL,
                split_type TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_splits_transaction_id ON splits(transaction_id)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS split_shares (
                id TEXT NOT NULL PRIMARY KEY,
                split_id TEXT NOT NULL,
                participant_id TEXT NOT NULL,
                share_amount_minor INTEGER NOT NULL,
                percentage REAL,
                is_settled INTEGER NOT NULL DEFAULT 0,
                settled_at INTEGER,
                FOREIGN KEY(split_id) REFERENCES splits(id) ON DELETE CASCADE,
                FOREIGN KEY(participant_id) REFERENCES participants(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_split_shares_split_id ON split_shares(split_id)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_split_shares_participant_id ON split_shares(participant_id)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_split_shares_split_id_participant_id " +
                "ON split_shares(split_id, participant_id)"
        )
    }
}

/**
 * Adds the learned merchant-category override table (Review Inbox teaching
 * a category for a specific merchant string) — one new table, no existing
 * data touched.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS merchant_category_overrides (
                merchant_key TEXT NOT NULL PRIMARY KEY,
                category TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * RC5 Part 9/"AI Audit Log" — one new table recording every AI call
 * (provider/model/latency/tokens/confidence/outcome, never a prompt,
 * response, or API key). Additive only.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_audit_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                capability TEXT NOT NULL,
                provider_id TEXT NOT NULL,
                model TEXT NOT NULL,
                latency_ms INTEGER NOT NULL,
                tokens_used INTEGER,
                success INTEGER NOT NULL,
                confidence_percent INTEGER,
                error_summary TEXT
            )
            """.trimIndent()
        )
    }
}

/**
 * RC7 Phase B — one new nullable-default column: whether an account is a
 * Candidate Account (created by the Account Resolver for an institution it
 * could not recognize, per InstitutionRegistry). Additive only, defaults to 0
 * (false) for every existing row, so no existing account silently becomes a
 * candidate. See Account.isCandidate / AccountIdentityDecision.CANDIDATE.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN is_candidate INTEGER NOT NULL DEFAULT 0")
    }
}

/**
 * RC8 Phase B: generic learned-decision memory table. Additive only — a new
 * table, no existing data touched. See LearnedDecisionEntity's doc comment
 * for why this is generic rather than one table per decision type.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS learned_decisions (
                decision_type TEXT NOT NULL,
                subject_key TEXT NOT NULL,
                learned_value TEXT NOT NULL,
                confidence INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY(decision_type, subject_key)
            )
            """.trimIndent()
        )
    }
}

/**
 * Milestone 2 / ADR-0001 — the canonical `financial_events` table, introduced
 * ADDITIVELY alongside `transactions`. One new table with a single foreign key to
 * `transactions` (the originating record during coexistence), plus indices. No
 * existing row is read, touched, or transformed, and nothing writes to this table
 * yet — this is pure, reversible foundation. Column names/types/nullability and
 * index names must match FinancialEventEntity exactly (Room validates the migrated
 * schema against the entity at first open).
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS financial_events (
                id TEXT NOT NULL PRIMARY KEY,
                transaction_id INTEGER,
                account_id INTEGER NOT NULL,
                brand_id INTEGER,
                category_id INTEGER,
                amount_minor INTEGER NOT NULL,
                currency_code TEXT NOT NULL,
                type TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                source TEXT NOT NULL,
                confidence INTEGER NOT NULL,
                status TEXT NOT NULL,
                supersedes_event_id TEXT,
                fingerprint TEXT NOT NULL,
                raw_text TEXT,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_events_transaction_id ON financial_events(transaction_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_events_account_id ON financial_events(account_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_events_timestamp_millis ON financial_events(timestamp_millis)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_financial_events_fingerprint ON financial_events(fingerprint)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_financial_events_status ON financial_events(status)")
    }
}

/**
 * Dated opening-balance anchor (ADR-0009 follow-up / balance reconciliation).
 * One new nullable column on `accounts` recording the instant the opening balance
 * is anchored to. Additive only — the safest migration shape (same as
 * MIGRATION_8_9's is_candidate): every existing row migrates to NULL, no data is
 * read, touched, or transformed, and the balance arithmetic never reads this
 * column. Type/nullability must match AccountEntity.openingBalanceAsOfMillis
 * (INTEGER, nullable) exactly — Room validates on first open after migrating.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN opening_balance_as_of INTEGER")
    }
}


