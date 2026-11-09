package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The exercise a substituted session row originally stood in for (ROADMAP N291).
 *
 * Nullable, so a row written before substitutions existed is an ordinary planned exercise. The value is the
 * **original** exercise id and is never rewritten by a second substitution; it is what lets the review pair the
 * performed row with the plan it answered.
 */
internal val MIGRATION_20_21 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SUBSTITUTED_FROM)
    }
}

private const val FROM_VERSION = 20
private const val TO_VERSION = 21
private const val ADD_SUBSTITUTED_FROM =
    "ALTER TABLE session_exercises ADD COLUMN substitutedFromExerciseId TEXT"
