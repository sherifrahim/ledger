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


