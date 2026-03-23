package com.rekosuo.gymtracker.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// SQLite doesn't support adding foreign keys via ALTER TABLE,
// so we recreate the table with the constraint.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table with foreign key and index
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS performances_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                exerciseId INTEGER NOT NULL,
                date INTEGER NOT NULL,
                sets TEXT NOT NULL,
                notes TEXT NOT NULL,
                FOREIGN KEY (exerciseId) REFERENCES exercises(id) ON DELETE CASCADE
            )
        """.trimIndent())
        // Delete orphaned performances whose exercise was already deleted
        db.execSQL("""
            DELETE FROM performances
            WHERE exerciseId NOT IN (SELECT id FROM exercises)
        """.trimIndent())
        // Copy data
        db.execSQL("""
            INSERT INTO performances_new (id, exerciseId, date, sets, notes)
            SELECT id, exerciseId, date, sets, notes FROM performances
        """.trimIndent())
        // Drop old table and rename
        db.execSQL("DROP TABLE performances")
        db.execSQL("ALTER TABLE performances_new RENAME TO performances")
        // Create index on exerciseId
        db.execSQL("CREATE INDEX IF NOT EXISTS index_performances_exerciseId ON performances(exerciseId)")
    }
}