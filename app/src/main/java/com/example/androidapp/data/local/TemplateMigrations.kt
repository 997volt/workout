package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A template can keep a version (ROADMAP N134).
 *
 * One nullable column, and that is the whole of the structure: a version is a template row with
 * `versionOf` naming the template it was kept from, so its exercises and sets need no table of their
 * own and every read path applies unchanged. The migration therefore adds the column and nothing else
 * — no row is copied, because the versions are the rows the feature writes from here on.
 *
 * The column carries no default on purpose: null is what a template is, so every row already on disk
 * migrates to exactly the state it was in.
 */
internal val MIGRATION_3_4 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_VERSION_OF)
    }
}

private const val FROM_VERSION = 3
private const val TO_VERSION = 4

/** Nullable with no default, so a row written before this is not a version of anything. */
private const val ADD_VERSION_OF = "ALTER TABLE templates ADD COLUMN versionOf TEXT"
