package com.example.androidapp.data.local

import androidx.room.migration.Migration

/**
 * Every migration this store has, in the order they apply (ROADMAP B286).
 *
 * A list rather than ten arguments at the builder, because a call site cannot be inspected and a
 * list can: a test asserts that the chain starts at 1, ends at [WorkoutDatabase]'s version, and that
 * each step's declared pair is the next one, so a forgotten migration is a red test rather than
 * `A migration from 11 to 12 was required but not found` on every upgrading install.
 *
 * The schema-by-schema story of what each step did is on [WorkoutDatabase], and each migration's own
 * KDoc carries its argument; this file holds only the order.
 */
internal val ALL_MIGRATIONS: List<Migration> = listOf(
    MIGRATION_1_2,
    MIGRATION_2_3,
    MIGRATION_3_4,
    MIGRATION_4_5,
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8,
    MIGRATION_8_9,
    MIGRATION_9_10,
    MIGRATION_10_11,
    MIGRATION_11_12,
    MIGRATION_12_13,
    MIGRATION_13_14,
    MIGRATION_14_15,
    MIGRATION_15_16,
)
