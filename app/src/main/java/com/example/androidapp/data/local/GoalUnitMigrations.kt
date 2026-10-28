package com.example.androidapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * A rating's target is stored in **points**, as every rating series reads them (ROADMAP B270).
 *
 * `MetricUnit.RATING` parsed a typed target by doubling it into half-points — the unit the *tables* store
 * an RPE in — while `format` read the same number as a point on the 1–10 scale. A target of 7 was
 * therefore stored as 14, printed as "14.0", and drawn at 14 on an axis that stops at 10. The unit is one
 * scale now, and this is the row-side half: every rating metric's target is halved, once, because every
 * one of them was written by the doubling parse. A goal with no value (a direction's lose/gain) is left
 * alone, and so is every metric whose unit was already consistent.
 *
 * The metric ids are matched by prefix for the three families that are composed rather than listed — a
 * soreness, joint or per-lift joint series names the muscle or site in its id — and by name for the six
 * that are not. `JOINT:%` does not match `EXERCISE_JOINT:%`, because the prefix is anchored.
 */
internal val MIGRATION_11_12 = object : Migration(FROM_VERSION, TO_VERSION) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(HALVE_RATING_TARGETS)
    }
}

private const val FROM_VERSION = 11
private const val TO_VERSION = 12

private const val HALVE_RATING_TARGETS = """
    UPDATE metric_goals SET value = value / 2.0
    WHERE value IS NOT NULL AND (
        metricId IN (
            'WORKOUT:RPE', 'WORKOUT:MUSCLE_FEEL', 'WORKOUT:JOINT_PAIN',
            'EXERCISE:RPE', 'EXERCISE:MUSCLE_FEEL', 'EXERCISE:JOINT_PAIN'
        )
        OR metricId LIKE 'SORE:%'
        OR metricId LIKE 'JOINT:%'
        OR metricId LIKE 'EXERCISE_JOINT:%'
    )
"""
