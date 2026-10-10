// Schema version numbers are inherently literal — `Migration(2, 3)` reads as
// "from v2 to v3" and naming them would obscure exactly the thing being stated.
@file:Suppress("MagicNumber")

package com.example.androidapp.data.local

import androidx.room.migration.Migration
import com.example.androidapp.data.SeedExercises
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The database's schema history. Every migration lives here and is validated by
 * `WorkoutDatabaseMigrationTest` against the committed JSON in `app/schemas/`.
 *
 * The SQL is copied from Room's own generated `createSql` rather than
 * hand-written to "look equivalent". Room compares the migrated database's
 * structure (columns, foreign keys, indices) against the expected schema, so a
 * typo'd column type or a missing index fails the migration test rather than
 * silently shipping — but there is no reason to make it guess.
 */
private const val CREATE_WORKOUT_SESSIONS =
    "CREATE TABLE IF NOT EXISTS `workout_sessions` (" +
        "`id` TEXT NOT NULL, `startedAt` INTEGER NOT NULL, `finishedAt` INTEGER, " +
        "`notes` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
        "`deletedAt` INTEGER, PRIMARY KEY(`id`))"

private const val CREATE_WORKOUT_SESSIONS_FINISHED_AT_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_workout_sessions_finishedAt` " +
        "ON `workout_sessions` (`finishedAt`)"

private const val CREATE_SESSION_EXERCISES =
    "CREATE TABLE IF NOT EXISTS `session_exercises` (" +
        "`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, " +
        "`position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE RESTRICT )"

private const val CREATE_SESSION_EXERCISES_SESSION_ID_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_session_exercises_sessionId` " +
        "ON `session_exercises` (`sessionId`)"

private const val CREATE_SESSION_EXERCISES_EXERCISE_ID_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_session_exercises_exerciseId` " +
        "ON `session_exercises` (`exerciseId`)"

/**
 * v1 -> v2: workout sessions and the exercises inside them (ROADMAP P1.2, P1.8).
 *
 * Purely additive: no existing table is touched, so an upgrade cannot lose the
 * exercise library that v1 already wrote.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_WORKOUT_SESSIONS)
        db.execSQL(CREATE_WORKOUT_SESSIONS_FINISHED_AT_INDEX)
        db.execSQL(CREATE_SESSION_EXERCISES)
        db.execSQL(CREATE_SESSION_EXERCISES_SESSION_ID_INDEX)
        db.execSQL(CREATE_SESSION_EXERCISES_EXERCISE_ID_INDEX)
    }
}

/**
 * v2 -> v3: logged sets and the persisted rest timer (ROADMAP P1.3, P1.4).
 *
 * [REST_ENDS_AT_COLUMN] is nullable and has no default, so the ALTER is valid on
 * a table that already has rows — existing sessions simply read as "not resting".
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SET_ENTRIES)
        db.execSQL(CREATE_SET_ENTRIES_SESSION_EXERCISE_ID_INDEX)
        db.execSQL(ADD_REST_ENDS_AT)
    }
}

private const val CREATE_SET_ENTRIES =
    "CREATE TABLE IF NOT EXISTS `set_entries` (" +
        "`id` TEXT NOT NULL, `sessionExerciseId` TEXT NOT NULL, " +
        "`setIndex` INTEGER NOT NULL, `reps` INTEGER NOT NULL, " +
        "`weightGrams` INTEGER NOT NULL, `setType` TEXT NOT NULL, " +
        "`completedAt` INTEGER, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_SET_ENTRIES_SESSION_EXERCISE_ID_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_set_entries_sessionExerciseId` " +
        "ON `set_entries` (`sessionExerciseId`)"

private const val ADD_REST_ENDS_AT =
    "ALTER TABLE `workout_sessions` ADD COLUMN `restEndsAt` INTEGER"

/**
 * v3 -> v4: an exercise's own rest and its technique cue (ROADMAP N5).
 *
 * Both columns are nullable and unset by default, so "no opinion" and the app's
 * 90 s default are the same state — which is what makes this a pair of plain
 * ALTERs rather than a backfill. The seeded library therefore keeps its rest and
 * its cues unset until a user edits them.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_EXERCISE_REST_SECONDS)
        db.execSQL(ADD_EXERCISE_TECHNIQUE_NOTE)
    }
}

private const val ADD_EXERCISE_REST_SECONDS =
    "ALTER TABLE `exercises` ADD COLUMN `restSeconds` INTEGER"

private const val ADD_EXERCISE_TECHNIQUE_NOTE =
    "ALTER TABLE `exercises` ADD COLUMN `techniqueNote` TEXT"

/**
 * v4 -> v5: the readiness note on a session (ROADMAP N4).
 *
 * Nullable and unset, so an existing or completed workout simply has no note and
 * nothing is backfilled. `notes` stays untouched: it is reserved for a per-workout
 * note, which is a different question from "what is not recovered today".
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SESSION_READINESS_NOTE)
    }
}

private const val ADD_SESSION_READINESS_NOTE =
    "ALTER TABLE `workout_sessions` ADD COLUMN `readinessNote` TEXT"

/**
 * v5 -> v6: a set's RPE and its comment (ROADMAP N6).
 *
 * Both nullable and unset, so every set logged before this reads as "no RPE, no
 * comment" — which is exactly what the one-tap **Log set** path keeps writing.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SET_RPE)
        db.execSQL(ADD_SET_NOTE)
    }
}

private const val ADD_SET_RPE =
    "ALTER TABLE `set_entries` ADD COLUMN `rpe` INTEGER"

private const val ADD_SET_NOTE =
    "ALTER TABLE `set_entries` ADD COLUMN `note` TEXT"

/**
 * v6 -> v7: when a session exercise was marked done (ROADMAP N7).
 *
 * Nullable and unset, so every exercise in an open or past workout reads as "not
 * done" and nothing is backfilled. Done is a session state, not a delete, so no
 * existing row moves.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SESSION_EXERCISE_FINISHED_AT)
    }
}

private const val ADD_SESSION_EXERCISE_FINISHED_AT =
    "ALTER TABLE `session_exercises` ADD COLUMN `finishedAt` INTEGER"

/**
 * v7 -> v8: how an exercise felt — muscle feel and joint pain (ROADMAP N8).
 *
 * Both nullable and unset, so every exercise in an open or past workout reads as
 * "not rated" and nothing is backfilled. They sit on the session exercise rather
 * than the library entry, because the same movement differs day to day.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SESSION_EXERCISE_MUSCLE_FEEL)
        db.execSQL(ADD_SESSION_EXERCISE_JOINT_PAIN)
    }
}

private const val ADD_SESSION_EXERCISE_MUSCLE_FEEL =
    "ALTER TABLE `session_exercises` ADD COLUMN `muscleFeel` INTEGER"

private const val ADD_SESSION_EXERCISE_JOINT_PAIN =
    "ALTER TABLE `session_exercises` ADD COLUMN `jointPain` INTEGER"

/**
 * v8 -> v9: workout templates and the exercises they hold (ROADMAP N3).
 *
 * Two new sync-shaped tables, so this is a create rather than an ALTER. Nothing
 * existing is touched, which is the same argument migration 1→2 made: an upgrade
 * cannot lose what the previous version already wrote.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_TEMPLATES)
        db.execSQL(CREATE_TEMPLATE_EXERCISES)
        db.execSQL(CREATE_TEMPLATE_EXERCISES_TEMPLATE_ID_INDEX)
        db.execSQL(CREATE_TEMPLATE_EXERCISES_EXERCISE_ID_INDEX)
    }
}

private const val CREATE_TEMPLATES =
    "CREATE TABLE IF NOT EXISTS `templates` (" +
        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))"

private const val CREATE_TEMPLATE_EXERCISES =
    "CREATE TABLE IF NOT EXISTS `template_exercises` (" +
        "`id` TEXT NOT NULL, `templateId` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, " +
        "`position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
        "FOREIGN KEY(`exerciseId`) REFERENCES `exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE RESTRICT )"

private const val CREATE_TEMPLATE_EXERCISES_TEMPLATE_ID_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_template_exercises_templateId` " +
        "ON `template_exercises` (`templateId`)"

private const val CREATE_TEMPLATE_EXERCISES_EXERCISE_ID_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_template_exercises_exerciseId` " +
        "ON `template_exercises` (`exerciseId`)"

/**
 * v9 -> v10: which joints hurt (ROADMAP N9).
 *
 * One additive column, nullable and unset, so every rating already recorded reads
 * back as "no location given" and nothing is backfilled.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_SESSION_EXERCISE_JOINT_PAIN_NOTE)
    }
}

private const val ADD_SESSION_EXERCISE_JOINT_PAIN_NOTE =
    "ALTER TABLE `session_exercises` ADD COLUMN `jointPainNote` TEXT"

/**
 * v10 -> v11: a plan's sets, and the rest and cue it prescribes (ROADMAP N14).
 *
 * A new table plus two additive columns. Nothing existing is touched, so an upgrade
 * cannot lose a template that was already there.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_TEMPLATE_SETS)
        db.execSQL(CREATE_TEMPLATE_SETS_INDEX)
        db.execSQL(ADD_TEMPLATE_EXERCISE_REST_SECONDS)
        db.execSQL(ADD_TEMPLATE_EXERCISE_TECHNIQUE_NOTE)
    }
}

private const val CREATE_TEMPLATE_SETS =
    "CREATE TABLE IF NOT EXISTS `template_sets` (" +
        "`id` TEXT NOT NULL, `templateExerciseId` TEXT NOT NULL, `setIndex` INTEGER NOT NULL, " +
        "`role` TEXT NOT NULL, `targetWeightGrams` INTEGER, `targetRepsMin` INTEGER, " +
        "`targetRepsMax` INTEGER, `targetRpe` INTEGER, `note` TEXT, " +
        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
        "PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`templateExerciseId`) REFERENCES `template_exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_TEMPLATE_SETS_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_template_sets_templateExerciseId` " +
        "ON `template_sets` (`templateExerciseId`)"

private const val ADD_TEMPLATE_EXERCISE_REST_SECONDS =
    "ALTER TABLE `template_exercises` ADD COLUMN `restSeconds` INTEGER"

private const val ADD_TEMPLATE_EXERCISE_TECHNIQUE_NOTE =
    "ALTER TABLE `template_exercises` ADD COLUMN `techniqueNote` TEXT"

/**
 * v11 -> v12: the rest and cue a *plan* prescribed, carried onto the session exercise
 * (ROADMAP N14).
 *
 * Nullable and unset for a workout that was not started from a plan (or was started
 * from a template that prescribes neither), which is what leaves the library's values
 * showing through — the fallback direction the roadmap names.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `restSeconds` INTEGER")
        db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `techniqueNote` TEXT")
    }
}

/**
 * v12 -> v13: assisted load (ROADMAP N15).
 *
 * `set_entries.assistanceGrams` defaults to 0 — "the machine took nothing off" — so
 * every set already recorded keeps meaning exactly what it meant. On a plan's set the
 * target is nullable instead, because a plan may say nothing about assistance, and
 * only the performed set needs a definite answer.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `set_entries` ADD COLUMN `assistanceGrams` INTEGER NOT NULL DEFAULT 0",
        )
        db.execSQL(
            "ALTER TABLE `template_sets` ADD COLUMN `targetAssistanceGrams` INTEGER",
        )
    }
}

/**
 * v13 -> v14: RPE in half steps, so 9.5 can be recorded (ROADMAP N6, extended).
 *
 * The column is *renamed* to `rpeHalves` rather than re-used, because its unit changed:
 * an `rpeHalves` holding 19 would read as nineteen points to anyone who did not know, which
 * is how a silent corruption starts. Values are doubled on the way across, so an 8
 * already recorded becomes 16 halves — still 8.0.
 *
 * This is the create-copy-drop-rename form rather than `ALTER TABLE … DROP COLUMN`
 * because minSdk is 26, whose SQLite predates that statement. The column lists are
 * copied from the exported v13 schema, so a mismatch would fail the migration test
 * rather than reach a phone.
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SET_ENTRIES_V14)
        db.execSQL(
            """
            INSERT INTO `set_entries_v14`
                (id, sessionExerciseId, setIndex, reps, weightGrams, assistanceGrams,
                 setType, rpeHalves, note, completedAt, createdAt, updatedAt, deletedAt)
            SELECT id, sessionExerciseId, setIndex, reps, weightGrams, assistanceGrams,
                   setType, rpe * 2, note, completedAt, createdAt, updatedAt, deletedAt
            FROM `set_entries`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `set_entries`")
        db.execSQL("ALTER TABLE `set_entries_v14` RENAME TO `set_entries`")
        db.execSQL(CREATE_SET_ENTRIES_SESSION_INDEX)

        db.execSQL(CREATE_TEMPLATE_SETS_V14)
        db.execSQL(
            """
            INSERT INTO `template_sets_v14`
                (id, templateExerciseId, setIndex, role, targetWeightGrams,
                 targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRpeHalves,
                 note, createdAt, updatedAt, deletedAt)
            SELECT id, templateExerciseId, setIndex, role, targetWeightGrams,
                   targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRpe * 2,
                   note, createdAt, updatedAt, deletedAt
            FROM `template_sets`
            """.trimIndent(),
        )
        db.execSQL("DROP TABLE `template_sets`")
        db.execSQL("ALTER TABLE `template_sets_v14` RENAME TO `template_sets`")
        db.execSQL(CREATE_TEMPLATE_SETS_EXERCISE_INDEX)
    }
}

private const val CREATE_SET_ENTRIES_V14 =
    "CREATE TABLE IF NOT EXISTS `set_entries_v14` (" +
        "`id` TEXT NOT NULL, `sessionExerciseId` TEXT NOT NULL, `setIndex` INTEGER NOT NULL, " +
        "`reps` INTEGER NOT NULL, `weightGrams` INTEGER NOT NULL, " +
        "`assistanceGrams` INTEGER NOT NULL, `setType` TEXT NOT NULL, `rpeHalves` INTEGER, " +
        "`note` TEXT, `completedAt` INTEGER, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_SET_ENTRIES_SESSION_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_set_entries_sessionExerciseId` " +
        "ON `set_entries` (`sessionExerciseId`)"

private const val CREATE_TEMPLATE_SETS_V14 =
    "CREATE TABLE IF NOT EXISTS `template_sets_v14` (" +
        "`id` TEXT NOT NULL, `templateExerciseId` TEXT NOT NULL, `setIndex` INTEGER NOT NULL, " +
        "`role` TEXT NOT NULL, `targetWeightGrams` INTEGER, " +
        "`targetAssistanceGrams` INTEGER, `targetRepsMin` INTEGER, `targetRepsMax` INTEGER, " +
        "`targetRpeHalves` INTEGER, `note` TEXT, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`templateExerciseId`) REFERENCES `template_exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_TEMPLATE_SETS_EXERCISE_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_template_sets_templateExerciseId` " +
        "ON `template_sets` (`templateExerciseId`)"

/**
 * v14 -> v15: a template can be pinned to a weekday (ROADMAP N16).
 *
 * Additive and nullable: an existing template is unscheduled until it is given a day,
 * which is exactly what it was before.
 */
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `templates` ADD COLUMN `weekday` TEXT")
    }
}

/**
 * v15 -> v16: an exercise can belong to a superset or circuit (ROADMAP N24).
 *
 * Additive and nullable: an ungrouped exercise is exactly what every exercise was before, and
 * the group is an ordinal rather than a foreign key — "these are done together" needs no more,
 * and `position` already carries the order within the workout. A circuit is the same column
 * with three or more members, which is why there is one concept and not two.
 */
val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `session_exercises` ADD COLUMN `supersetGroup` INTEGER")
        // A plan can prescribe a superset too, and that is the end of the feature the app most
        // wants it at: plans are what a workout is started from, so a grouping that only exists
        // inside a session cannot be written down (ROADMAP B16).
        db.execSQL("ALTER TABLE `template_exercises` ADD COLUMN `supersetGroup` INTEGER")
    }
}

/**
 * A session remembers the zone it was performed in (ROADMAP N25).
 *
 * **The backfill is deliberately left null.** A workout done in Tokyo before this column existed
 * cannot be given an offset after the fact — the data to say where it happened was never captured,
 * and inventing one would be a lie the rows cannot support. Null means "not known", and every screen
 * falls back to the current zone for those, which is exactly what they already showed. The
 * alternative — stamping every old row with today's offset — would look tidier and be wrong for the
 * one case this feature exists for.
 */
val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `workout_sessions` ADD COLUMN `zoneOffsetMinutes` INTEGER")
    }
}

/**
 * Body measurements (ROADMAP N32).
 *
 * A new table rather than a column somewhere: a measurement is its own fact, dated, with weight the
 * only value that is always there. The SQL is Room's own, copied from the exported schema rather than
 * hand-written as an equivalent.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `measurements` (`id` TEXT NOT NULL,
            `measuredAt` INTEGER NOT NULL,
            `weightGrams` INTEGER NOT NULL,
            `bodyFatTenths` INTEGER,
            `muscleTenths` INTEGER,
            `neckMm` INTEGER,
            `chestMm` INTEGER,
            `waistMm` INTEGER,
            `hipsMm` INTEGER,
            `upperArmMm` INTEGER,
            `thighMm` INTEGER,
            `calfMm` INTEGER,
            `createdAt` INTEGER NOT NULL,
            `updatedAt` INTEGER NOT NULL,
            `deletedAt` INTEGER,
            PRIMARY KEY(`id`))
            """.trimIndent(),
        )
    }
}

/**
 * v18 -> v19: an index on the session's start time (ROADMAP B46).
 *
 * The statistics range filter scans `workout_sessions` by `startedAt`, and the record count does too. The
 * existing `finishedAt` index cannot serve either — one filters `IS NOT NULL`, the other wants a range on a
 * different column — so the record query was reading every set ever logged and running a correlated subquery
 * per row. The index has to be created from the entity too, so Room's schema validation still matches.
 */
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workout_sessions_startedAt` " +
                "ON `workout_sessions` (`startedAt`)",
        )
    }
}

/**
 * v19 -> v20: programs, their slots, their recorded skips, and a session's provenance
 * (ROADMAP P3.3).
 *
 * Three new tables plus one additive column, and the column is the one worth stating:
 * `workout_sessions.templateId` is left **null** for every session already recorded. A
 * workout done before this existed cannot be given provenance after the fact — the plan it
 * was started from was never captured — and inventing one would make a session resolve an
 * occurrence it never touched. Null means "unknown", which is the honest answer, and the
 * occurrence matcher simply finds no session for those weeks.
 *
 * The program tables are new, so an upgrade cannot lose a schedule that did not exist;
 * what it must not disturb is everything else, which is the same additive argument
 * migration 8→9 made for templates.
 */
val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_PROGRAMS)
        db.execSQL(CREATE_PROGRAM_SLOTS)
        db.execSQL(CREATE_PROGRAM_SLOTS_PROGRAM_INDEX)
        db.execSQL(CREATE_PROGRAM_SLOTS_TEMPLATE_INDEX)
        db.execSQL(CREATE_PROGRAM_SKIPS)
        db.execSQL(CREATE_PROGRAM_SKIPS_SLOT_INDEX)
        db.execSQL(CREATE_PROGRAM_SKIPS_WEEK_INDEX)
        db.execSQL(ADD_SESSION_TEMPLATE_ID)
    }
}

/**
 * v20 -> v21: a program gains an authored order (ROADMAP P3.12).
 *
 * `position` arrives NOT NULL with a default of 0 so the ALTER can run against a table that
 * already holds programs, and the rows already there are then given their implicit `rowid` as
 * the position. That is the only order the old rows carry — the list used to sort active-first
 * then by name — and stamping them all 0 would silently reshuffle them into alphabetical order
 * the moment the user reordered one.
 *
 * The `isActive` column is deliberately untouched: it simply stops meaning "the only one".
 * Nothing is deactivated, so a lifter who followed a program still follows it.
 */
val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_PROGRAM_POSITION)
        db.execSQL("UPDATE `programs` SET `position` = `rowid`")
    }
}

/**
 * v21 -> v22: what each program slot prescribes for each exercise (ROADMAP P3.8).
 *
 * Two new tables, so an upgrade cannot lose a prescription that did not exist. A slot that
 * prescribed nothing gets no rows, which is exactly the state it was in before: the template's
 * targets stood then and still do. The SQL is Room's own, copied from the exported schema rather
 * than hand-written as an equivalent.
 */
val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_PROGRAM_SLOT_EXERCISES)
        db.execSQL(CREATE_PROGRAM_SLOT_EXERCISES_SLOT_INDEX)
        db.execSQL(CREATE_PROGRAM_SLOT_SETS)
        db.execSQL(CREATE_PROGRAM_SLOT_SETS_EXERCISE_INDEX)
    }
}

/**
 * v22 -> v23: the weeks a program was deliberately backed off (ROADMAP P3.10).
 *
 * One new table, and what it does *not* do is the point: a program with no deload gets no rows,
 * which is exactly the state it was in — every week counted, which is what an unmarked week means.
 * The SQL is Room's own, copied from the exported schema rather than hand-written as an equivalent.
 */
val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_PROGRAM_DELOADS)
        db.execSQL(CREATE_PROGRAM_DELOADS_PROGRAM_INDEX)
        db.execSQL(CREATE_PROGRAM_DELOADS_WEEK_INDEX)
    }
}

/**
 * v24 -> v25: a template loses the weekday pin (ROADMAP N56).
 *
 * The pin was the weaker of two places answering "what am I doing on Tuesday" — a template has no
 * order, no next-up and no adherence to belong to — so a program's slots are the only source of a
 * dated plan now. The loss is accepted rather than mitigated: a template pinned to a day today comes
 * out of this with no day at all, and getting the schedule back means putting it in a program, which
 * is the rule being stated rather than a migration that failed.
 *
 * **The table is rebuilt rather than `ALTER TABLE ... DROP COLUMN`ed.** SQLite has supported the drop
 * since 3.35 and Room's bundled version is newer, but the rebuild is what Room's own schema validation
 * compares against and what the other table-shaped migrations here do; and the copy states the
 * surviving columns explicitly, so a template's name and its timestamps cannot be silently reshaped by
 * the change. The `id` is the primary key and the rows keep it, so every foreign key into `templates`
 * (`template_exercises`, `program_slots`, `program_substitutions`) still resolves.
 */
val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `templates_new` (" +
                "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "INSERT INTO `templates_new` (`id`, `name`, `createdAt`, `updatedAt`, `deletedAt`) " +
                "SELECT `id`, `name`, `createdAt`, `updatedAt`, `deletedAt` FROM `templates`",
        )
        db.execSQL("DROP TABLE `templates`")
        db.execSQL("ALTER TABLE `templates_new` RENAME TO `templates`")
    }
}

/**
 * v23 -> v24: the workouts that stood in for a slot's own, one week at a time (ROADMAP P3.11).
 *
 * One new table. A program with no substitution gets no rows, which is exactly the state it was
 * in: every occurrence trained with what the slot names. The SQL is Room's own, copied from the
 * exported schema rather than hand-written as an equivalent.
 */
val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_PROGRAM_SUBSTITUTIONS)
        db.execSQL(CREATE_PROGRAM_SUBSTITUTIONS_SLOT_INDEX)
        db.execSQL(CREATE_PROGRAM_SUBSTITUTIONS_WEEK_INDEX)
        db.execSQL(CREATE_PROGRAM_SUBSTITUTIONS_TEMPLATE_INDEX)
    }
}

/**
 * v25 -> v26: the muscles a session reported sore, each with its own score (ROADMAP N62).
 *
 * One new table, and what it deliberately does *not* do is the point: the readiness note it sits
 * beside is untouched, and a session that reported nothing gets no rows — which is exactly the
 * state it was in, because one free-text line was all there was to write. The SQL is Room's own,
 * copied from the exported `26.json` rather than hand-written as an equivalent.
 */
val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SESSION_SORE_MUSCLES)
        db.execSQL(CREATE_SESSION_SORE_MUSCLES_SESSION_INDEX)
    }
}

private const val CREATE_SESSION_SORE_MUSCLES =
    "CREATE TABLE IF NOT EXISTS `session_sore_muscles` (" +
        "`id` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `muscle` TEXT NOT NULL, " +
        "`score` INTEGER NOT NULL, `position` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`sessionId`) REFERENCES `workout_sessions`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_SESSION_SORE_MUSCLES_SESSION_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_session_sore_muscles_sessionId` " +
        "ON `session_sore_muscles` (`sessionId`)"

/**
 * v26 -> v27: the joints a session exercise reported painful, each with its side and score
 * (ROADMAP N63).
 *
 * One new table, and what it deliberately does *not* do is the point: the legacy `jointPain` and
 * `jointPainNote` columns on `session_exercises` are untouched — a session rated before the change
 * keeps its number and its free text, and history still reads them — and an exercise rated before
 * the change gets no rows, which is exactly the state it was in. The SQL is Room's own, copied from
 * the exported `27.json` rather than hand-written as an equivalent.
 */
val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CREATE_SESSION_EXERCISE_JOINTS)
        db.execSQL(CREATE_SESSION_EXERCISE_JOINTS_EXERCISE_INDEX)
    }
}

private const val CREATE_SESSION_EXERCISE_JOINTS =
    "CREATE TABLE IF NOT EXISTS `session_exercise_joints` (" +
        "`id` TEXT NOT NULL, `sessionExerciseId` TEXT NOT NULL, `joint` TEXT NOT NULL, " +
        "`side` TEXT NOT NULL, `score` INTEGER NOT NULL, `position` INTEGER NOT NULL, " +
        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
        "PRIMARY KEY(`id`), FOREIGN KEY(`sessionExerciseId`) REFERENCES `session_exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_SESSION_EXERCISE_JOINTS_EXERCISE_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_session_exercise_joints_sessionExerciseId` " +
        "ON `session_exercise_joints` (`sessionExerciseId`)"

/**
 * v27 -> v28: a plan's target RPE moves from one per prescribed set to one per exercise
 * (ROADMAP N59, amended).
 *
 * **This is a consolidation, not a copy.** A plan used to name an effort on every planned set; it now
 * names one number for the exercise, beside the rest and cue it already carried, so the two editors
 * each show a single RPE field. The backfill takes **the last set that names one, in `setIndex`
 * order**, warm-ups included — a ramp's last set is the top set, which is what the plan builds to.
 *
 * **The per-set values are left in place rather than cleared.** Nothing reads them once the exercise
 * names a value, but they are what a backup file written before this change carries, and a reader
 * falls back to them where the exercise-level value is absent — which is exactly the state a
 * pre-change backup imported after this is in. Clearing them would make that fallback pointless and
 * would silently discard data the export still holds.
 *
 * Both columns are nullable and unset on an exercise with no prescribed RPE, so "the plan names no
 * effort" stays a state rather than becoming a zero. The SQL is Room's own ALTER shape, and the two
 * backfills are plain correlated subqueries; the migration test validates the result against the
 * exported `28.json`.
 */
val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_TEMPLATE_EXERCISE_TARGET_RPE)
        db.execSQL(ADD_SLOT_EXERCISE_TARGET_RPE)
        db.execSQL(SEED_TEMPLATE_EXERCISE_TARGET_RPE)
        db.execSQL(SEED_SLOT_EXERCISE_TARGET_RPE)
    }
}

private const val ADD_TEMPLATE_EXERCISE_TARGET_RPE =
    "ALTER TABLE `template_exercises` ADD COLUMN `targetRpeHalves` INTEGER"

private const val ADD_SLOT_EXERCISE_TARGET_RPE =
    "ALTER TABLE `program_slot_exercises` ADD COLUMN `targetRpeHalves` INTEGER"

/** The last set that named an effort is what the plan builds to; a ramp's last set is the top set. */
private const val SEED_TEMPLATE_EXERCISE_TARGET_RPE =
    "UPDATE `template_exercises` SET `targetRpeHalves` = (" +
        "SELECT s.`targetRpeHalves` FROM `template_sets` s " +
        "WHERE s.`templateExerciseId` = `template_exercises`.`id` " +
        "AND s.`targetRpeHalves` IS NOT NULL AND s.`deletedAt` IS NULL " +
        "ORDER BY s.`setIndex` DESC LIMIT 1)"

/** The same consolidation for a slot's prescription: its own sets seed its own exercise row. */
private const val SEED_SLOT_EXERCISE_TARGET_RPE =
    "UPDATE `program_slot_exercises` SET `targetRpeHalves` = (" +
        "SELECT s.`targetRpeHalves` FROM `program_slot_sets` s " +
        "WHERE s.`slotExerciseId` = `program_slot_exercises`.`id` " +
        "AND s.`targetRpeHalves` IS NOT NULL AND s.`deletedAt` IS NULL " +
        "ORDER BY s.`setIndex` DESC LIMIT 1)"

/**
 * A warm-up carries no effort (ROADMAP N67).
 *
 * The rule is new, so rows already on disk can hold a number that is no longer a fact about
 * anything: the RPE a logged warm-up recorded, and the legacy per-set target a *planned* warm-up
 * still carries (N59 moved the plan's effort to the exercise, and this column is the fallback a
 * pre-change plan arrives with). Both are cleared, because the editor no longer offers the field
 * and a stored value nothing can show is dead weight rather than history. The exercise-level
 * `targetRpeHalves` is deliberately left alone: that one belongs to the exercise's working sets.
 *
 * The migration test seeds a warm-up and a working set on each side and asserts that only the
 * working one keeps its number, validated against the exported `29.json`.
 */
val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(CLEAR_WARM_UP_RPE)
        db.execSQL(CLEAR_PLANNED_WARM_UP_RPE)
        db.execSQL(CLEAR_SLOT_PLANNED_WARM_UP_RPE)
    }
}

private const val CLEAR_WARM_UP_RPE =
    "UPDATE `set_entries` SET `rpeHalves` = NULL " +
        "WHERE `setType` = 'WARMUP' AND `rpeHalves` IS NOT NULL"

private const val CLEAR_PLANNED_WARM_UP_RPE =
    "UPDATE `template_sets` SET `targetRpeHalves` = NULL " +
        "WHERE `role` = 'WARMUP' AND `targetRpeHalves` IS NOT NULL"

private const val CLEAR_SLOT_PLANNED_WARM_UP_RPE =
    "UPDATE `program_slot_sets` SET `targetRpeHalves` = NULL " +
        "WHERE `role` = 'WARMUP' AND `targetRpeHalves` IS NOT NULL"

/**
 * A program is a schedule over templates, and nothing else (ROADMAP N73).
 *
 * P3.8 let a slot prescribe its own sets, rest, cue and effort over its template's — the same
 * template trained differently on a Monday and a Friday. The lifter's own decision was that a
 * program must always **use** what the template says and never hold a second copy of it, so the two
 * tables that carried the overrides are dropped: the app reads the template, and progression writes
 * the template.
 *
 * This **discards** every prescription already stored, which is the point of the change rather than a
 * side effect of it. Templates, planned sets and every logged workout are untouched, and the child
 * table goes first so the drop cannot fail on a foreign key.
 *
 * The migration test seeds a prescription on each side and asserts the tables are gone while the
 * template and its slot survive, validated against the exported `30.json`.
 */
val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(DROP_PROGRAM_SLOT_SETS)
        db.execSQL(DROP_PROGRAM_SLOT_EXERCISES)
    }
}

private const val DROP_PROGRAM_SLOT_SETS = "DROP TABLE IF EXISTS `program_slot_sets`"

private const val DROP_PROGRAM_SLOT_EXERCISES = "DROP TABLE IF EXISTS `program_slot_exercises`"

/**
 * An exercise may carry its own display unit (ROADMAP N64).
 *
 * A nullable TEXT column holding the enum's name: null is "follow the app setting", which is what
 * every existing row becomes, so nothing changes for anyone until they set one. Presentation only —
 * every weight stays in grams, so this column changes no number on disk.
 *
 * The migration test seeds an exercise, upgrades, and asserts it survives with no unit set,
 * validated against the exported `31.json`.
 */
val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_EXERCISE_WEIGHT_UNIT)
    }
}

private const val ADD_EXERCISE_WEIGHT_UNIT =
    "ALTER TABLE `exercises` ADD COLUMN `weightUnit` TEXT"

/**
 * A planned set remembers where in its rep range the lifter has climbed (ROADMAP N74).
 *
 * The two bounds a plan was authored with are a *range* now, which progression never edits, so the
 * number the session is asked for has to live somewhere of its own. It is backfilled to the range's
 * floor — `from` where the plan wrote one, its `to` otherwise — because that is where a lifter
 * restarts after a weight step, and a ranged plan's first session under this rule asks for the bottom
 * of its range rather than its top.
 *
 * A plan that names no reps at all keeps the column null: there is no range to be inside.
 *
 * The migration test seeds a ranged set and a set with no reps, upgrades, and asserts the floor and
 * the null, validated against the exported `32.json`.
 */
val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_TEMPLATE_SET_CURRENT_REPS)
        db.execSQL(SEED_TEMPLATE_SET_CURRENT_REPS)
    }
}

private const val ADD_TEMPLATE_SET_CURRENT_REPS =
    "ALTER TABLE `template_sets` ADD COLUMN `targetRepsCurrent` INTEGER"

/** The range's floor, or its ceiling where the plan wrote no floor: where the climb starts. */
private const val SEED_TEMPLATE_SET_CURRENT_REPS =
    "UPDATE `template_sets` SET `targetRepsCurrent` = COALESCE(`targetRepsMin`, `targetRepsMax`)"

/**
 * The back group splits three ways, and the seeded library is corrected with it (ROADMAP N75).
 *
 * `BACK` was one muscle; it is now `LATS`, `UPPER_BACK` and `LOWER_BACK`, and `ADDUCTORS` joins the
 * legs. **The legacy value stays in the enum**, so nothing here has to move every row that names it:
 * a reader resolves `BACK` either way and an export written before the split still restores. What the
 * migration moves is the library the *app* wrote, which it can classify, and only where the lifter has
 * not already answered for it — every statement is guarded by the seeded value (`primaryMuscle =
 * 'BACK'`, or a secondary list that still carries `BACK`), so a re-classified exercise keeps their
 * answer. A custom exercise is never in `id IN (...)`, and guessing what a lifter meant by *Back* is
 * what the project refuses.
 *
 * The secondary lists are rewritten **token by token** rather than replaced whole, so a lifter who
 * added `CORE` to one keeps it. The deadlifts are the interesting case: they leave the back group for
 * `HAMSTRINGS`, which the Romanian deadlift already had, and the `HAMSTRINGS` in their secondary list
 * becomes `LOWER_BACK` — the erectors holding a heavy hinge, which is what the retired tag meant.
 *
 * The migration test seeds a v32 library with a Back-tagged row of each shape (a vertical pull, a
 * horizontal pull, a deadlift, the two secondary lists), a row a lifter re-classified and a custom
 * exercise, and is validated against the exported `33.json`.
 */
val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(SPLIT_BACK_INTO_LATS)
        db.execSQL(SPLIT_BACK_INTO_UPPER_BACK)
        db.execSQL(MOVE_DEADLIFTS_TO_HAMSTRINGS)
        db.execSQL(SPLIT_FACE_PULL_SECONDARY)
        db.execSQL(SPLIT_RDL_SECONDARY)
    }
}

/** The vertical pulls are lats. */
private const val SPLIT_BACK_INTO_LATS =
    "UPDATE `exercises` SET `primaryMuscle` = 'LATS' " +
        "WHERE `primaryMuscle` = 'BACK' AND `id` IN ('pull-up', 'lat-pulldown', 'assisted-pull-up')"

/** The horizontal pulls are the upper back. */
private const val SPLIT_BACK_INTO_UPPER_BACK =
    "UPDATE `exercises` SET `primaryMuscle` = 'UPPER_BACK' " +
        "WHERE `primaryMuscle` = 'BACK' AND `id` IN ('barbell-row', 'seated-cable-row', 'machine-row')"

/**
 * One muscle replaced in a comma-separated secondary list, as a whole token (ROADMAP B67).
 *
 * `secondaryMuscles` is a joined list of names, so a bare `replace` is a substring operation:
 * `LOWER_BACK` contains `BACK`, and a rewrite that meant "the retired Back tag" would turn it into
 * `LOWER_UPPER_BACK`, which the reader then throws on. Wrapping the list in commas makes every token a
 * delimited one, and the trim puts the ends back. No legitimately written v32 row can hold a
 * `..._BACK` name — those arrived with the split — so this is hardening rather than a live repair, and
 * it is the shape any future token rewrite here should use.
 */
private fun replaceMuscleToken(from: String, to: String): String =
    "trim(replace(',' || `secondaryMuscles` || ',', ',$from,', ',$to,'), ',')"

/** Whether the list holds [token] as a whole token rather than as part of a longer name (B67). */
private fun hasMuscleToken(token: String): String =
    "(',' || `secondaryMuscles` || ',') LIKE '%,$token,%'"

/** The deadlifts: hamstrings prime, and the erectors take the old tag's place in the secondaries. */
private val MOVE_DEADLIFTS_TO_HAMSTRINGS =
    "UPDATE `exercises` SET `primaryMuscle` = 'HAMSTRINGS', " +
        "`secondaryMuscles` = ${replaceMuscleToken("HAMSTRINGS", "LOWER_BACK")} " +
        "WHERE `primaryMuscle` = 'BACK' AND `id` IN ('deadlift', 'conventional-deadlift')"

/** A face pull's back work is the upper back. */
private val SPLIT_FACE_PULL_SECONDARY =
    "UPDATE `exercises` SET `secondaryMuscles` = ${replaceMuscleToken("BACK", "UPPER_BACK")} " +
        "WHERE `id` = 'face-pull' AND ${hasMuscleToken("BACK")}"

/** A Romanian deadlift's is the erectors. */
private val SPLIT_RDL_SECONDARY =
    "UPDATE `exercises` SET `secondaryMuscles` = ${replaceMuscleToken("BACK", "LOWER_BACK")} " +
        "WHERE `id` = 'romanian-deadlift' AND ${hasMuscleToken("BACK")}"

/**
 * An exercise may name its own weight step (ROADMAP N77).
 *
 * The ± buttons, the warm-up ramp and the progression offer all move a load by a step, and until this
 * column that step came from the unit alone: a machine that jumps 5 kg (or 1 kg) was always edited
 * against a step it did not have. It is nullable with no default, so the ALTER is valid on a table
 * that already holds rows and every one of them reads as the unit's own step — which is exactly what
 * the three call sites meant before this existed.
 *
 * The migration test seeds a row, upgrades, and asserts it survives with the column unset, validated
 * against the exported `34.json`.
 */
val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_EXERCISE_STEP_GRAMS)
    }
}

private const val ADD_EXERCISE_STEP_GRAMS =
    "ALTER TABLE `exercises` ADD COLUMN `stepGrams` INTEGER"

/**
 * A planned set may hold the drop value its run takes off the anchor (ROADMAP N79).
 *
 * A drop rung's weight is not written down but derived — `anchor − k × value` — so the plan stores
 * the *value*, once for the run, and every rung moves when the anchor does. Nullable with no default,
 * so the ALTER is valid on a table that already holds rows: null means "not a rung", or "another rung
 * of the same run", which is exactly what every row on disk means before the feature is used.
 *
 * The migration test seeds a set, upgrades, and asserts it survives with the column unset, validated
 * against the exported `35.json`.
 */
val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_TEMPLATE_SET_DROP_VALUE)
    }
}

private const val ADD_TEMPLATE_SET_DROP_VALUE =
    "ALTER TABLE `template_sets` ADD COLUMN `dropValueGrams` INTEGER"

/**
 * Gives the library its shape: a row may hang under another, and a row may be a category (ROADMAP N95).
 *
 * Two plain ALTERs and one UPDATE, because both columns are nullable-or-defaulted in Kotlin rather than in
 * SQL. `parentId` is null for every existing row, which reads as "top level" and is exactly what a flat
 * library was; `rowKind` is backfilled to `MOVEMENT` in the same statement, since every row that existed
 * before categories **is** a lift. The default in SQL is written as the empty string rather than the enum's
 * name so the value's one home stays the enum — a name copied into a migration is a second place to change.
 *
 * **No index on `parentId`.** The grouping query reads the whole library, which is a few dozen rows and is
 * already read whole today; an index would be a guess about a size this app has not seen.
 *
 * **No foreign key.** A head is soft-deleted like every other row (P1.12 keeps it for the export), and
 * N58's rule is that a removed head still names its children — a cascade would take the children with it,
 * and a `RESTRICT` would refuse the delete. The children's referent is a row that is still there.
 */
val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(ADD_EXERCISE_PARENT_ID)
        db.execSQL(ADD_EXERCISE_ROW_KIND)
        db.execSQL(BACKFILL_EXERCISE_ROW_KIND)
    }
}

private const val ADD_EXERCISE_PARENT_ID =
    "ALTER TABLE `exercises` ADD COLUMN `parentId` TEXT"

private const val ADD_EXERCISE_ROW_KIND =
    "ALTER TABLE `exercises` ADD COLUMN `rowKind` TEXT NOT NULL DEFAULT ''"

private const val BACKFILL_EXERCISE_ROW_KIND =
    "UPDATE `exercises` SET `rowKind` = 'MOVEMENT'"

/**
 * Files an existing library into the families the seed ships (ROADMAP N95).
 *
 * A data migration rather than something the seeder does, and the reason is the seeder's own rule:
 * `INSERT OR IGNORE` never updates a row that exists, so the movements a database already has would arrive
 * under no head at all — every family empty and every movement loose, which is the feature not working
 * rather than working differently. A first install needs none of this, because the seeder writes the parents
 * with the rows.
 *
 * **The guard is what makes it safe on a database a lifter has organised.** Only an *unfiled* row is filed,
 * so a movement the lifter moved out of a family stays out and one they filed themselves stays filed. The
 * three bench variations are the one place a file can be wrong in a way the guard cannot see: their natural
 * home is *under the barbell bench*, not under the family, and a development build between the two
 * migrations could have filed them flat under the family head. That case is corrected below, and the
 * ordinary upgrade — where `parentId` is still NULL because its predecessor only adds the columns — is left
 * to the general loop, whose map already sends them to the barbell bench (B80).
 *
 * The SQL is generated from [SeedExercises.parentOf] rather than written out here, for the reason the seeder
 * reads the same map: two copies of "which family is this movement in" disagree the first time the seed
 * changes, and this one could not be caught by a compile error.
 */
val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(db: SupportSQLiteDatabase) {
        SeedExercises.categories.forEach { category ->
            db.execSQL(
                "INSERT OR IGNORE INTO `exercises` " +
                    "(id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, " +
                    "isCustom, parentId, rowKind, createdAt, updatedAt) " +
                    "VALUES (?, ?, 'OTHER', '', 'OTHER', 'OTHER', 0, NULL, 'CATEGORY', ?, ?)",
                arrayOf<Any?>(category.id, category.name, MIGRATION_SEEDED_AT, MIGRATION_SEEDED_AT),
            )
        }

        // The movements already filed under the *family head* by an intermediate build keep their place at
        // that level; only the bench variations' own level is corrected, and only from that flat file.
        db.execSQL(
            UPDATE_BENCH_VARIATIONS_FOR_N95,
            arrayOf<Any?>("barbell-bench-press", "bench-press"),
        )
        // Everything still unfiled takes the family the seed says it belongs to — including the three bench
        // variations on the ordinary upgrade, where this update above matched nothing because their
        // `parentId` was NULL rather than the family head. Skipping them here was the defect (B80).
        SeedExercises.parentOf.forEach { (movementId, familyId) ->
            db.execSQL(
                "UPDATE `exercises` SET `parentId` = ?, `updatedAt` = ? " +
                    "WHERE id = ? AND `parentId` IS NULL AND `rowKind` = 'MOVEMENT'",
                arrayOf<Any?>(familyId, MIGRATION_SEEDED_AT, movementId),
            )
        }
    }
}

/**
 * The movement pattern moves onto the category (ROADMAP N96).
 *
 * A head was seeded with `OTHER` because it had nothing to say, and it is the one home for its family's
 * pattern now — so each seeded family is given the pattern the seed says it has. **The map is the seed's own**,
 * for [SeedExercises.parentOf]'s reason: a second copy in SQL would be a second thing to keep in step, and the
 * two would disagree the first time a family's pattern changed.
 *
 * **The four names the merge retired are not rewritten here**, deliberately: this sets the *category's*
 * pattern, and a movement's own stale value stops being read once the head states one. It goes with the
 * column, in the rebuild that follows; until then a row that still says `HORIZONTAL_PUSH` reads through the
 * legacy enum value, which is what that value is for.
 */
val MIGRATION_37_38 = object : Migration(37, 38) {
    override fun migrate(db: SupportSQLiteDatabase) {
        SeedExercises.categories.forEach { category ->
            db.execSQL(
                "UPDATE `exercises` SET `movementPattern` = ?, `updatedAt` = ? " +
                    "WHERE id = ? AND `rowKind` = 'CATEGORY'",
                arrayOf<Any?>(category.pattern.name, MIGRATION_SEEDED_AT, category.id),
            )
        }
    }
}

/**
 * The timestamp this migration stamps the rows it writes.
 *
 * A constant rather than the clock: a migration's effect has to be the same whenever it runs, and a test
 * that read a moving timestamp could not assert that these rows are the seed's.
 */
private const val MIGRATION_SEEDED_AT = 1_700_000_000_000L

/** The three bench movements moved from the family head down to the barbell bench they are versions of. */
private const val UPDATE_BENCH_VARIATIONS_FOR_N95 =
    "UPDATE `exercises` SET `parentId` = ?, `updatedAt` = " + MIGRATION_SEEDED_AT + " " +
        "WHERE `parentId` = ? AND id IN ('competition-bench-press', 'bench-press-speed-day', " +
        "'paused-bench-press-3s')"

private const val CREATE_PROGRAMS =
    "CREATE TABLE IF NOT EXISTS `programs` (" +
        "`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isActive` INTEGER NOT NULL, " +
        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
        "PRIMARY KEY(`id`))"

private const val CREATE_PROGRAM_SLOTS =
    "CREATE TABLE IF NOT EXISTS `program_slots` (" +
        "`id` TEXT NOT NULL, `programId` TEXT NOT NULL, `templateId` TEXT NOT NULL, " +
        "`position` INTEGER NOT NULL, `weekday` TEXT, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
        "FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE RESTRICT )"

private const val CREATE_PROGRAM_SLOTS_PROGRAM_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_slots_programId` " +
        "ON `program_slots` (`programId`)"

private const val CREATE_PROGRAM_SLOTS_TEMPLATE_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_slots_templateId` " +
        "ON `program_slots` (`templateId`)"

private const val CREATE_PROGRAM_SKIPS =
    "CREATE TABLE IF NOT EXISTS `program_skips` (" +
        "`id` TEXT NOT NULL, `slotId` TEXT NOT NULL, `weekStart` INTEGER NOT NULL, " +
        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
        "PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`slotId`) REFERENCES `program_slots`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_PROGRAM_SKIPS_SLOT_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_skips_slotId` " +
        "ON `program_skips` (`slotId`)"

private const val CREATE_PROGRAM_SKIPS_WEEK_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_skips_weekStart` " +
        "ON `program_skips` (`weekStart`)"

private const val ADD_SESSION_TEMPLATE_ID =
    "ALTER TABLE `workout_sessions` ADD COLUMN `templateId` TEXT"

private const val ADD_PROGRAM_POSITION =
    "ALTER TABLE `programs` ADD COLUMN `position` INTEGER NOT NULL DEFAULT 0"

private const val CREATE_PROGRAM_SLOT_EXERCISES =
    "CREATE TABLE IF NOT EXISTS `program_slot_exercises` (" +
        "`id` TEXT NOT NULL, `slotId` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, " +
        "`restSeconds` INTEGER, `techniqueNote` TEXT, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`slotId`) REFERENCES `program_slots`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_PROGRAM_SLOT_EXERCISES_SLOT_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_slot_exercises_slotId` " +
        "ON `program_slot_exercises` (`slotId`)"

private const val CREATE_PROGRAM_SLOT_SETS =
    "CREATE TABLE IF NOT EXISTS `program_slot_sets` (" +
        "`id` TEXT NOT NULL, `slotExerciseId` TEXT NOT NULL, `setIndex` INTEGER NOT NULL, " +
        "`role` TEXT NOT NULL, `targetWeightGrams` INTEGER, `targetAssistanceGrams` INTEGER, " +
        "`targetRepsMin` INTEGER, `targetRepsMax` INTEGER, `targetRpeHalves` INTEGER, " +
        "`targetPercentOf1Rm` INTEGER, `note` TEXT, `createdAt` INTEGER NOT NULL, " +
        "`updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`slotExerciseId`) REFERENCES `program_slot_exercises`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_PROGRAM_SLOT_SETS_EXERCISE_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_slot_sets_slotExerciseId` " +
        "ON `program_slot_sets` (`slotExerciseId`)"

private const val CREATE_PROGRAM_DELOADS =
    "CREATE TABLE IF NOT EXISTS `program_deloads` (" +
        "`id` TEXT NOT NULL, `programId` TEXT NOT NULL, `weekStart` INTEGER NOT NULL, " +
        "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `deletedAt` INTEGER, " +
        "PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`programId`) REFERENCES `programs`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE )"

private const val CREATE_PROGRAM_DELOADS_PROGRAM_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_deloads_programId` " +
        "ON `program_deloads` (`programId`)"

private const val CREATE_PROGRAM_DELOADS_WEEK_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_deloads_weekStart` " +
        "ON `program_deloads` (`weekStart`)"

private const val CREATE_PROGRAM_SUBSTITUTIONS =
    "CREATE TABLE IF NOT EXISTS `program_substitutions` (" +
        "`id` TEXT NOT NULL, `slotId` TEXT NOT NULL, `weekStart` INTEGER NOT NULL, " +
        "`templateId` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
        "`deletedAt` INTEGER, PRIMARY KEY(`id`), " +
        "FOREIGN KEY(`slotId`) REFERENCES `program_slots`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE CASCADE , " +
        "FOREIGN KEY(`templateId`) REFERENCES `templates`(`id`) " +
        "ON UPDATE NO ACTION ON DELETE RESTRICT )"

private const val CREATE_PROGRAM_SUBSTITUTIONS_SLOT_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_substitutions_slotId` " +
        "ON `program_substitutions` (`slotId`)"

private const val CREATE_PROGRAM_SUBSTITUTIONS_WEEK_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_substitutions_weekStart` " +
        "ON `program_substitutions` (`weekStart`)"

private const val CREATE_PROGRAM_SUBSTITUTIONS_TEMPLATE_INDEX =
    "CREATE INDEX IF NOT EXISTS `index_program_substitutions_templateId` " +
        "ON `program_substitutions` (`templateId`)"

/** Applied in order by the database builder. */
val ALL_MIGRATIONS = arrayOf(    MIGRATION_1_2,
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
    MIGRATION_16_17,
    MIGRATION_17_18,
    MIGRATION_18_19,
    MIGRATION_19_20,
    MIGRATION_20_21,
    MIGRATION_21_22,
    MIGRATION_22_23,
    MIGRATION_23_24,
    MIGRATION_24_25,
    MIGRATION_25_26,
    MIGRATION_26_27,
    MIGRATION_27_28,
    MIGRATION_28_29,
    MIGRATION_29_30,
    MIGRATION_30_31,
    MIGRATION_31_32,
    MIGRATION_32_33,
    MIGRATION_33_34,
    MIGRATION_34_35,
    MIGRATION_35_36,
    MIGRATION_36_37,
    MIGRATION_37_38,
)
