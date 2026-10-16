package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A goal's past is kept (ROADMAP N137).
 *
 * One new table and nothing moved: the periods are the rows the feature writes from here on, and the goal a
 * lifter already has is carried over by [moveLegacyGoals] on the first open — with an **unknown** start, because
 * the moment they set it is not recorded anywhere and inventing it would be inventing history.
 *
 * Nullable `kind`, `value` and `startedAt` on purpose: the first is "a goal written before kinds existed", the
 * second is "a direction-only goal", and the third is "we do not know when this began". A row with the first
 * two null is a period of **no goal**, which is how clearing one keeps the past instead of deleting it.
 */
internal val MIGRATION_4_5 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_GOALS)
        db.execSQL(CREATE_METRIC_INDEX)
    }
}

private const val FROM_VERSION = 4
private const val TO_VERSION = 5

private const val CREATE_GOALS = "CREATE TABLE IF NOT EXISTS metric_goals (" +
    "id TEXT NOT NULL PRIMARY KEY, " +
    "metricId TEXT NOT NULL, " +
    "kind TEXT, " +
    "value REAL, " +
    "startedAt INTEGER, " +
    "createdAt INTEGER NOT NULL, " +
    "updatedAt INTEGER NOT NULL)"

private const val CREATE_METRIC_INDEX =
    "CREATE INDEX IF NOT EXISTS index_metric_goals_metricId ON metric_goals (metricId)"
