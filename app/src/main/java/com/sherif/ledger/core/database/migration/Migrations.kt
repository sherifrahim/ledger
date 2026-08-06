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



/**
 * Separates the captured message from the merchant extracted out of it.
 *
 * Until now `raw_text` was overwritten with the merchant name at insert time, so
 * the database held "Kfc", "Transfer", "Income" where the original bank message
 * had been. That destroyed the only durable record of what was actually received:
 * a confirmed capture bug (a card's available limit being booked as an AED
 * 8,225.16 purchase) could not be diagnosed from the database at all, and had to
 * be reconstructed from `dumpsys notification` on the device.
 *
 * Additive and nullable — the safest migration shape, matching MIGRATION_11_12.
 * Every existing row migrates to NULL, and existing rows keep their merchant name
 * in `raw_text` exactly as before; nothing is read, rewritten or backfilled.
 * [com.sherif.ledger.core.domain.model.Transaction.merchantOrRawText] is what makes
 * that safe: it prefers the new column and falls back to the old meaning, so rows
 * written on either side of this migration read identically.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN merchant_text TEXT")
    }
}

/**
 * Credit-card outstanding, derived rather than replayed.
 *
 * A credit card's balance cannot be obtained by summing the purchases Ledger
 * happened to capture: that only ever totals what arrived since the import window
 * opened, which is why the owner's Mashreq card reported AED 23,499.70 of "debt"
 * that was really three months of spending.
 *
 * The bank already does this arithmetic and restates the result in every message.
 * Verified against the owner's real data: the stated available limit falls by
 * exactly the purchase amount, and jumps back up by the payment when the card is
 * paid (+1,184.00 on 9 Jul, +964.25 on 13 Jul). So:
 *
 *     outstanding = credit_limit - available_credit
 *
 * `available_credit_minor` records the bank's own figure per message;
 * `credit_limit_minor` is the card's total limit, which no purchase SMS ever
 * states and which the user therefore supplies once per card. Together they give
 * an exact figure that self-corrects on the very next message, with no payment
 * matching and no accumulated drift.
 *
 * Both columns are additive and nullable, the same shape as MIGRATION_11_12 and
 * MIGRATION_12_13. Existing rows migrate to NULL and every balance keeps being
 * computed exactly as before until a limit is actually known.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN available_credit_minor INTEGER")
        db.execSQL("ALTER TABLE accounts ADD COLUMN credit_limit_minor INTEGER")
    }
}

/**
 * Tags — the first thing in Ledger the user authors rather than the data implies.
 *
 * Two new tables and no change to any existing one. `tags` holds the vocabulary,
 * unique on a normalized name so the same label cannot exist twice in different
 * capitalisation; `transaction_tags` is the many-to-many join, with a composite
 * primary key so the same tag cannot be attached to the same transaction twice,
 * and cascading deletes on both sides so no edge can outlive what it connects.
 *
 * Column names, types, nullability and index names must match TagEntity and
 * TransactionTagEntity exactly — Room validates the migrated schema against the
 * entities the first time the database opens.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tags (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                normalized_name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_normalized_name ON tags(normalized_name)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transaction_tags (
                transaction_id INTEGER NOT NULL,
                tag_id INTEGER NOT NULL,
                tagged_at INTEGER NOT NULL,
                PRIMARY KEY(transaction_id, tag_id),
                FOREIGN KEY(transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
                FOREIGN KEY(tag_id) REFERENCES tags(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_tags_tag_id ON transaction_tags(tag_id)")
    }
}

/**
 * Budgets — one monthly ceiling per spending category.
 *
 * A single new table holding only the user's intention. Progress against a budget
 * is deliberately NOT stored: how much went on groceries this month is already
 * answered by GetFinancialAnalyticsUseCase's category totals, and caching it here
 * would create a second figure to keep in sync with the first. That is the same
 * mistake this codebase refuses to make with balances, none of which are stored.
 *
 * Unique on category, so setting a limit for a category that already has one is
 * an edit rather than a duplicate.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS budgets (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                limit_minor INTEGER NOT NULL,
                currency_code TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category ON budgets(category)")
    }
}

/**
 * Goals — something the user is saving towards, funded by one of their accounts.
 *
 * Progress is deliberately not a column: a goal's "saved so far" IS the funding
 * account's balance, which AccountBalanceService already derives by replaying
 * transactions. Storing it would give the user a number to maintain by hand and
 * Ledger a second figure that can drift from the account it describes.
 *
 * Cascades on account delete, so a goal cannot outlive the account funding it.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goals (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                target_minor INTEGER NOT NULL,
                currency_code TEXT NOT NULL,
                account_id INTEGER NOT NULL,
                target_date_millis INTEGER,
                created_at INTEGER NOT NULL,
                FOREIGN KEY(account_id) REFERENCES accounts(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_goals_account_id ON goals(account_id)")
    }
}

/**
 * Gives the fallback account an explicit identity instead of a position.
 *
 * Before this, "the default account" was `accounts.first().id` inside
 * EnsureDefaultAccountUseCase — whatever account happened to occupy that
 * position, decided purely by insert order. A real, recognised institution's
 * account could end up there by coincidence, which is exactly the shape of a
 * confirmed bug on the owner's real device: their real ADCB balance was seeded
 * onto an untailed "Primary Account" while the real ADCB transactions
 * accumulated on a separate, later-created account, because the untailed one
 * happened to be created first.
 * DeterministicAccountIdentityResolver already refuses to bind an unrecognised
 * institution's transaction to the default account — the fallback and a real
 * bank account are supposed to be two different rows, never the same one by
 * accident. `is_default` is what turns that into a guarantee: from this point
 * on it is set on exactly one account, only by EnsureDefaultAccountUseCase
 * itself, never derived from where a row happens to sit.
 *
 * The backfill marks the SAME account `accounts.first().id` would already have
 * returned (lowest id among non-deleted, non-candidate rows) — deliberately
 * preserving today's live behavior across the upgrade rather than silently
 * redirecting an existing install's fallback captures to a different account.
 * Nothing about already-attributed transactions changes; this only affects
 * which account future unmatched captures land on.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE accounts ADD COLUMN is_default INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            """
            UPDATE accounts SET is_default = 1 WHERE id = (
                SELECT id FROM accounts WHERE is_deleted = 0 AND is_candidate = 0 ORDER BY id ASC LIMIT 1
            )
            """.trimIndent(),
        )
    }
}

/**
 * `financial_events.transfer_direction` — the mirror event never carried this
 * (see EventToTransaction.kt), so every TRANSFER read through the event-sourced
 * path (Dashboard/Transactions/Story recent-activity) rendered as an outflow
 * regardless of its real direction, because `isOutflow` treats a direction-less
 * transfer as conservative-outflow. Additive/nullable column, then backfilled
 * from the authoritative `transactions.transfer_direction` for every existing
 * mirror event that still links to its originating transaction (coexistence
 * events always do) — so already-captured transfers get their correct sign
 * immediately, not only ones captured after this migration.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE financial_events ADD COLUMN transfer_direction TEXT")
        db.execSQL(
            """
            UPDATE financial_events
            SET transfer_direction = (
                SELECT transactions.transfer_direction FROM transactions
                WHERE transactions.id = financial_events.transaction_id
            )
            WHERE transaction_id IS NOT NULL
            """.trimIndent(),
        )
    }
}

/**
 * Re-runs MIGRATION_18_19's backfill. An earlier build of that migration shipped
 * without the backfill UPDATE (column-add only); any device that already opened
 * that build has `user_version = 19` and will never re-run 18→19, so its
 * financial_events rows are stuck at NULL forever without this. No-op (empty
 * UPDATE) on a device that only ever saw the corrected 18→19.
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            UPDATE financial_events
            SET transfer_direction = (
                SELECT transactions.transfer_direction FROM transactions
                WHERE transactions.id = financial_events.transaction_id
            )
            WHERE transaction_id IS NOT NULL AND transfer_direction IS NULL
            """.trimIndent(),
        )
    }
}
