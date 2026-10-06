package com.example.androidapp.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the committed schema baseline and every migration against it
 * (ROADMAP F5).
 *
 * These tests are the reason migrations are safe to write at all: each one runs
 * the real migration over a database created from the *previous* exported
 * schema, then validates the result against the next one. A column typo or a
 * forgotten index fails here rather than on a user's phone.
 *
 * `app/schemas/` is wired in as instrumented assets by `app/build.gradle.kts`.
 */
@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutDatabase::class.java,
    )

    @Test
    fun version1Schema_matchesTheCommittedBaseline() {
        helper.createDatabase(TEST_DB, 1).close()
    }

    @Test
    fun migration1To2_addsSessionTables_andKeepsTheExerciseLibrary() {
        // A v1 database with a real library row in it.
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // The migration is purely additive, so the library must survive it. This
        // is the assertion that would have caught a careless DROP/recreate.
        migrated.query("SELECT COUNT(*) FROM exercises").use { cursor ->
            cursor.moveToFirst()
            assertEquals("library rows must survive the upgrade", 1, cursor.getInt(0))
        }

        migrated.query("SELECT COUNT(*) FROM workout_sessions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }

        migrated.close()
    }

    @Test
    fun migration2To3_addsSetEntriesAndTheRestColumn() {
        // Start from a real v2 database so the ALTER runs against a table that
        // already has rows — the case a fresh-install test would never cover.
        helper.createDatabase(TEST_DB, 2).apply {
            execSQL("INSERT INTO workout_sessions (id, startedAt, finishedAt, notes, createdAt, updatedAt, deletedAt) VALUES ('s1', 1, NULL, NULL, 1, 1, NULL)")
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        migrated.query("SELECT COUNT(*) FROM set_entries").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        // The pre-existing session survives, and its new column reads as "not resting".
        migrated.query("SELECT restEndsAt FROM workout_sessions WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertTrue("restEndsAt must default to null", cursor.isNull(0))
        }

        migrated.close()
    }

    @Test
    fun migration3To4_addsRestAndTechniqueNote_leavingThemUnset() {
        // Start from a v3 database holding a library row: N5's ALTERs have to run
        // against a table that already has data, which a fresh install never covers.
        helper.createDatabase(TEST_DB, 3).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 4, true, MIGRATION_3_4)

        migrated.query(
            "SELECT restSeconds, techniqueNote FROM exercises WHERE id = 'back-squat'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("the existing row must survive", 1, cursor.count)
            // Unset is the state that means "use the app default", so a backfilled
            // value here would silently change every existing exercise.
            assertTrue("an unset rest must read as null", cursor.isNull(0))
            assertTrue("an unset cue must read as null", cursor.isNull(1))
        }

        migrated.close()
    }

    @Test
    fun migration4To5_addsTheReadinessNote_leavingItUnset() {
        // A v4 database with a finished session in it: the ALTER has to run against
        // a table that already holds history (ROADMAP N4).
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, createdAt, updatedAt, deletedAt)
                VALUES
                    ('s1', 1, 2, NULL, NULL, 1, 2, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

        migrated.query("SELECT readinessNote FROM workout_sessions WHERE id = 's1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("the existing session must survive", 1, cursor.count)
            // Nothing is backfilled: an old workout simply has no note.
            assertTrue("readinessNote must default to null", cursor.isNull(0))
        }

        migrated.close()
    }

    @Test
    fun migration5To6_addsRpeAndComment_leavingThemUnset() {
        // A v5 database with a real logged set in it (ROADMAP N6). Every parent row
        // is inserted too, so the ALTERs run against a table that already holds data.
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, 2, NULL, NULL, NULL, 1, 2, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO set_entries
                    (id, sessionExerciseId, setIndex, reps, weightGrams, setType,
                     completedAt, createdAt, updatedAt, deletedAt)
                VALUES ('set1', 'se1', 0, 5, 100000, 'NORMAL', NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        migrated.query("SELECT rpe, note FROM set_entries WHERE id = 'set1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("the existing set must survive", 1, cursor.count)
            // Unset is what the one-tap log path keeps writing, so nothing is
            // backfilled and an old set reads as "no RPE, no comment".
            assertTrue("rpe must default to null", cursor.isNull(0))
            assertTrue("note must default to null", cursor.isNull(1))
        }

        migrated.close()
    }

    @Test
    fun migration6To7_addsFinishedAt_leavingItUnset() {
        // A v6 database with a real session exercise in it (ROADMAP N7).
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        migrated.query("SELECT finishedAt FROM session_exercises WHERE id = 'se1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("the existing exercise must survive", 1, cursor.count)
            // Unset means "not done": nothing is backfilled, and no past set moves.
            assertTrue("finishedAt must default to null", cursor.isNull(0))
        }

        migrated.close()
    }

    @Test
    fun migration7To8_addsTheFeelRatings_leavingThemUnset() {
        // A v7 database with a real session exercise in it (ROADMAP N8).
        helper.createDatabase(TEST_DB, 7).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, 2, NULL, NULL, NULL, 1, 2, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, finishedAt,
                     createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, 5, 1, 5, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 8, true, MIGRATION_7_8)

        migrated.query(
            "SELECT muscleFeel, jointPain, finishedAt FROM session_exercises WHERE id = 'se1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("the existing exercise must survive", 1, cursor.count)
            // Unrated, not zeroed: nothing is backfilled.
            assertTrue("muscleFeel must default to null", cursor.isNull(0))
            assertTrue("jointPain must default to null", cursor.isNull(1))
            assertEquals("the done state is untouched", 5L, cursor.getLong(2))
        }

        migrated.close()
    }

    @Test
    fun migration8To9_addsTheTemplateTables_leavingExistingWorkoutsAlone() {
        // A v8 database with a real workout in it: the upgrade must add tables
        // without touching what the previous version already wrote (ROADMAP N3).
        helper.createDatabase(TEST_DB, 8).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 9, true, MIGRATION_8_9)

        // The new tables exist and are empty...
        migrated.query("SELECT COUNT(*) FROM templates").use { cursor ->
            cursor.moveToFirst()
            assertEquals("no template is invented by the upgrade", 0, cursor.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM template_exercises").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }

        // ...and the workout that was already there is untouched.
        migrated.query("SELECT id, finishedAt FROM workout_sessions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("s1", cursor.getString(0))
            assertTrue("the open session must stay open", cursor.isNull(1))
        }

        migrated.close()
    }

    @Test
    fun migration9To10_addsTheJointPainLocation_leavingItUnset() {
        // A v9 database with a rating already recorded (ROADMAP N9).
        helper.createDatabase(TEST_DB, 9).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, 2, NULL, NULL, NULL, 1, 2, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, finishedAt, muscleFeel,
                     jointPain, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, NULL, 7, 4, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 10, true, MIGRATION_9_10)

        migrated.query(
            "SELECT jointPainNote, muscleFeel, jointPain FROM session_exercises WHERE id = 'se1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertTrue("no location is invented by the upgrade", cursor.isNull(0))
            // The ratings that were already there are untouched.
            assertEquals(7, cursor.getInt(1))
            assertEquals(4, cursor.getInt(2))
        }

        migrated.close()
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }

    @Test
    fun migration10To11_addsThePlannedSets_leavingAtemplateAlone() {
        // A v10 database with a real template in it: a plan is the user's work, and
        // the upgrade must not disturb one that was already written (ROADMAP N14).
        helper.createDatabase(TEST_DB, 10).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Heavy lower', 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises
                    (id, templateId, exerciseId, position, createdAt, updatedAt, deletedAt)
                VALUES ('te1', 't1', 'back-squat', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 11, true, MIGRATION_10_11)

        // The new table exists and is empty...
        migrated.query("SELECT COUNT(*) FROM template_sets").use { cursor ->
            cursor.moveToFirst()
            assertEquals("no planned set is invented by the upgrade", 0, cursor.getInt(0))
        }

        // ...the plan's two new columns are null rather than zeroed, so the exercise
        // still falls back to the library's rest and cue (N5)...
        migrated.query(
            "SELECT restSeconds, techniqueNote FROM template_exercises WHERE id = 'te1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertTrue("an unset rest must stay unset", cursor.isNull(0))
            assertTrue("an unset cue must stay unset", cursor.isNull(1))
        }

        // ...and the template that was already there is untouched.
        migrated.query("SELECT id, name FROM templates").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("Heavy lower", cursor.getString(1))
        }

        migrated.close()
    }

    @Test
    fun migration11To12_addsThePlansRestAndCue_leavingThemLibraryOwned() {
        // A v11 database with a real session: the new columns are what a *plan*
        // prescribes, so they must arrive unset and let the library's show through
        // (ROADMAP N14).
        helper.createDatabase(TEST_DB, 11).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt,
                     restSeconds, techniqueNote)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL, 90, 'Brace hard')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, finishedAt, muscleFeel,
                     jointPain, jointPainNote, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 12, true, MIGRATION_11_12)

        migrated.query(
            "SELECT restSeconds, techniqueNote FROM session_exercises WHERE id = 'se1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertTrue("the plan prescribed nothing, so the column is unset", cursor.isNull(0))
            assertTrue(cursor.isNull(1))
        }
        // The library's own values are untouched, which is what the session falls back to.
        migrated.query(
            "SELECT restSeconds, techniqueNote FROM exercises WHERE id = 'back-squat'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(90, cursor.getInt(0))
            assertEquals("Brace hard", cursor.getString(1))
        }

        migrated.close()
    }

    @Test
    fun migration12To13_addsAssistance_defaultingToNone() {
        // A v12 database with a real set in it: every set already recorded took no
        // assistance, so the column must arrive as 0 rather than as null or a value
        // that would have to be guessed (ROADMAP N15).
        helper.createDatabase(TEST_DB, 12).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('assisted-pull-up', 'Assisted Pull-Up', 'BACK', '', 'MACHINE',
                     'PULL', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, finishedAt, muscleFeel,
                     jointPain, jointPainNote, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'assisted-pull-up', 0, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO set_entries
                    (id, sessionExerciseId, setIndex, reps, weightGrams, setType,
                     rpe, note, completedAt, createdAt, updatedAt, deletedAt)
                VALUES ('set1', 'se1', 0, 8, 0, 'NORMAL', NULL, NULL, 1, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 13, true, MIGRATION_12_13)

        migrated.query("SELECT assistanceGrams, weightGrams FROM set_entries").use { cursor ->
            cursor.moveToFirst()
            assertEquals("an old set took no assistance", 0, cursor.getInt(0))
            assertEquals("and its weight is untouched", 0, cursor.getInt(1))
        }
        // The plan's target is nullable: a plan may say nothing about assistance.
        migrated.query("SELECT COUNT(*) FROM template_sets").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }

        migrated.close()
    }

    @Test
    fun migration13To14_carriesRpeIntoHalves() {
        // The unit changed, so the column was renamed rather than re-used: an `rpe`
        // holding 19 would read as nineteen points to anyone who did not know. Values
        // are doubled on the way across, so an 8 recorded before this is still 8.0
        // (ROADMAP N6, extended for 9.5).
        helper.createDatabase(TEST_DB, 13).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment,
                     movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES
                    ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL',
                     'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     createdAt, updatedAt, deletedAt)
                VALUES ('s1', 1, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, finishedAt, muscleFeel,
                     jointPain, jointPainNote, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, NULL, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO set_entries
                    (id, sessionExerciseId, setIndex, reps, weightGrams, assistanceGrams,
                     setType, rpe, note, completedAt, createdAt, updatedAt, deletedAt)
                VALUES ('set1', 'se1', 0, 5, 100000, 0, 'NORMAL', 8, 'heavy', 1, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO set_entries
                    (id, sessionExerciseId, setIndex, reps, weightGrams, assistanceGrams,
                     setType, rpe, note, completedAt, createdAt, updatedAt, deletedAt)
                VALUES ('set2', 'se1', 1, 5, 100000, 0, 'NORMAL', NULL, NULL, 1, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Legs', 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises
                    (id, templateId, exerciseId, position, createdAt, updatedAt, deletedAt)
                VALUES ('te1', 't1', 'back-squat', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_sets
                    (id, templateExerciseId, setIndex, role, targetWeightGrams,
                     targetAssistanceGrams, targetRepsMin, targetRepsMax, targetRpe,
                     note, createdAt, updatedAt, deletedAt)
                VALUES ('ts1', 'te1', 0, 'NORMAL', 100000, NULL, 3, 3, 7, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 14, true, MIGRATION_13_14)

        // 8 points is 16 halves; the rest of the row came across untouched.
        migrated.query(
            "SELECT rpeHalves, note, weightGrams FROM set_entries WHERE id = 'set1'",
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(16, cursor.getInt(0))
            assertEquals("heavy", cursor.getString(1))
            assertEquals(100_000L, cursor.getLong(2))
        }
        // A set with no RPE stays unrated rather than becoming 0 halves.
        migrated.query("SELECT rpeHalves FROM set_entries WHERE id = 'set2'").use { cursor ->
            cursor.moveToFirst()
            assertTrue("no RPE stays no RPE", cursor.isNull(0))
        }
        migrated.query("SELECT targetRpeHalves FROM template_sets").use { cursor ->
            cursor.moveToFirst()
            assertEquals(14, cursor.getInt(0))
        }

        migrated.close()
    }

    @Test
    fun migration14To15_addsTheWeekday_leavingPlansUncheduled() {
        // An existing plan has no day until it is given one, which is exactly what it
        // was before this column existed (ROADMAP N16).
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Legs', 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 15, true, MIGRATION_14_15)

        migrated.query("SELECT name, weekday FROM templates").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Legs", cursor.getString(0))
            assertTrue("a plan with no day stays unscheduled", cursor.isNull(1))
        }

        migrated.close()
    }

    @Test
    fun migration15To16_groupsSessionsAndPlans_withoutTouchingTheirRows() {
        // ROADMAP B16, and the reason this test exists at all: the migration was *amended* after
        // it had already run on development devices — a second column joined the same version —
        // so what needs proving is that an upgrade keeps the rows already in the tables AND
        // arrives with both columns. The 1→16 chain in MigrationsTest cannot check the first half.
        helper.createDatabase(TEST_DB, 15).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, createdAt, updatedAt, deletedAt)
                VALUES ('s1', 100, 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO exercises (id, name, primaryMuscle, secondaryMuscles, equipment,
                    movementPattern, isCustom, createdAt, updatedAt, deletedAt)
                VALUES ('e1', 'Back Squat', 'QUADS', '', 'BARBELL', 'SQUAT', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises (id, sessionId, exerciseId, position, createdAt,
                    updatedAt, deletedAt)
                VALUES ('se1', 's1', 'e1', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Legs', 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises (id, templateId, exerciseId, position, createdAt,
                    updatedAt, deletedAt)
                VALUES ('te1', 't1', 'e1', 0, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 16, true, MIGRATION_15_16)

        // The rows are still there, and ungrouped — which is what every exercise was before N24.
        migrated.query("SELECT id, supersetGroup FROM session_exercises").use { cursor ->
            cursor.moveToFirst()
            assertEquals("se1", cursor.getString(0))
            assertTrue("an existing session exercise is ungrouped", cursor.isNull(1))
        }
        migrated.query("SELECT id, supersetGroup FROM template_exercises").use { cursor ->
            cursor.moveToFirst()
            assertEquals("te1", cursor.getString(0))
            assertTrue("and so is an existing planned exercise", cursor.isNull(1))
        }

        migrated.close()
    }

    @Test
    fun migration16To17_keepsTheRows_andLeavesTheirZoneUnknown() {
        // ROADMAP N25. Two things need proving, and the second is the one worth a test: an upgrade
        // keeps the sessions already in the table, and the new column arrives **null** rather than
        // backfilled. A zone that was never captured cannot be reconstructed, and stamping today's
        // onto old rows would look like knowledge the row does not have.
        helper.createDatabase(TEST_DB, 16).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, finishedAt, createdAt, updatedAt, deletedAt)
                VALUES ('s1', 100, 200, 100, 200, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, createdAt, updatedAt, deletedAt)
                VALUES ('s2', 300, 300, 300, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 17, true, MIGRATION_16_17)

        migrated.query("SELECT id, zoneOffsetMinutes FROM workout_sessions ORDER BY id").use { cursor ->
            assertTrue("the finished session survived", cursor.moveToFirst())
            assertEquals("s1", cursor.getString(0))
            assertTrue("its zone is unknown, not invented", cursor.isNull(1))
            assertTrue("and so did the open one", cursor.moveToNext())
            assertEquals("s2", cursor.getString(0))
            assertTrue("with no zone either", cursor.isNull(1))
            assertFalse("exactly the two rows that were there", cursor.moveToNext())
        }

        migrated.close()
    }

    @Test
    fun migration17To18_addsMeasurements_withoutTouchingTheRowsAlreadyThere() {
        // ROADMAP N32: a new table is the easiest migration to get wrong in the quiet direction — an
        // upgrade that dropped what was there would look identical to one that worked, until someone
        // opened a workout.
        helper.createDatabase(TEST_DB, 17).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, finishedAt, createdAt, updatedAt, deletedAt)
                VALUES ('s1', 100, 200, 100, 200, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 18, true, MIGRATION_17_18)

        migrated.query("SELECT id FROM workout_sessions").use { cursor ->
            assertTrue("the workout already there survived", cursor.moveToFirst())
            assertEquals("s1", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM measurements").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("and the new table is there, empty", 0, cursor.getInt(0))
        }

        migrated.close()
    }

    @Test
    fun migration18To19_addsTheStartTimeIndex_leavingTheRowsAlone() {
        // ROADMAP B46. An index is the migration with nothing to show for itself: it changes no column and no
        // row, so only a test can tell an upgrade that added it from one that did nothing. What matters is
        // that the rows survive and the index exists, because a query cannot report a missing index — it
        // just gets slower.
        helper.createDatabase(TEST_DB, 18).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, finishedAt, createdAt, updatedAt, deletedAt)
                VALUES ('s1', 100, 200, 100, 200, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 19, true, MIGRATION_18_19)

        migrated.query("SELECT id, startedAt FROM workout_sessions").use { cursor ->
            assertTrue("the workout already there survived", cursor.moveToFirst())
            assertEquals("s1", cursor.getString(0))
            assertEquals(100L, cursor.getLong(1))
        }
        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'index' AND name = ?",
            arrayOf("index_workout_sessions_startedAt"),
        ).use { cursor ->
            assertTrue("the index the range filter needs is there", cursor.moveToFirst())
        }

        migrated.close()
    }

    @Test
    fun migration19To20_addsPrograms_withoutInventingProvenance() {
        // ROADMAP P3.3. Three new tables and one additive column, and the column is the half
        // worth a test: a workout done before programs existed cannot be given a template it
        // was started from, and inventing one would make it settle an occurrence it never
        // touched.
        helper.createDatabase(TEST_DB, 19).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, finishedAt, zoneOffsetMinutes,
                    createdAt, updatedAt, deletedAt)
                VALUES ('s1', 100, 200, 540, 100, 200, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, createdAt, updatedAt, deletedAt)
                VALUES ('s2', 300, 300, 300, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 20, true, MIGRATION_19_20)

        // The sessions survive, and their provenance is unknown rather than manufactured.
        migrated.query(
            "SELECT id, zoneOffsetMinutes, templateId FROM workout_sessions ORDER BY id",
        ).use { cursor ->
            assertTrue("the finished session survived", cursor.moveToFirst())
            assertEquals("s1", cursor.getString(0))
            assertEquals("its own zone is untouched", 540, cursor.getInt(1))
            assertTrue("and no template is invented for it", cursor.isNull(2))
            assertTrue("the open one survived too", cursor.moveToNext())
            assertEquals("s2", cursor.getString(0))
            assertTrue("with no provenance either", cursor.isNull(2))
            assertFalse("exactly the rows that were there", cursor.moveToNext())
        }

        // The three new tables exist and are empty.
        listOf("programs", "program_slots", "program_skips").forEach { table ->
            migrated.query("SELECT COUNT(*) FROM $table").use { cursor ->
                cursor.moveToFirst()
                assertEquals("no $table is invented by the upgrade", 0, cursor.getInt(0))
            }
        }

        migrated.close()
    }

    @Test
    fun migration20To21_givesProgramsAnOrder_keepingEveryActiveOne() {
        // ROADMAP P3.12. Two things need proving. The new column arrives with a usable order —
        // the rows there get their `rowid`, which is the only order they carry — and **both**
        // programs that were active stay active: the upgrade must not quietly deactivate one to
        // keep the old "at most one" rule true.
        helper.createDatabase(TEST_DB, 20).apply {
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, createdAt, updatedAt, deletedAt)
                VALUES ('p1', 'Upper/Lower', 1, 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, createdAt, updatedAt, deletedAt)
                VALUES ('p2', 'Conditioning', 1, 200, 200, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 21, true, MIGRATION_20_21)

        migrated.query("SELECT id, isActive, position FROM programs ORDER BY position").use { cursor ->
            assertTrue("the first program survived", cursor.moveToFirst())
            assertEquals("p1", cursor.getString(0))
            assertEquals("its active flag is untouched", 1, cursor.getInt(1))
            assertEquals("and it keeps its place", 1L, cursor.getLong(2))
            assertTrue("and so did the second", cursor.moveToNext())
            assertEquals("p2", cursor.getString(0))
            assertEquals("which is still active too", 1, cursor.getInt(1))
            assertEquals(2L, cursor.getLong(2))
            assertFalse("exactly the two rows that were there", cursor.moveToNext())
        }

        migrated.close()
    }

    @Test
    fun migration21To22_addsSlotPrescriptions_withoutInventingOne() {
        // ROADMAP P3.8. Two new tables, and what matters is what they do *not* do: a slot that
        // prescribed nothing before the upgrade gets no rows, so the template's targets still
        // stand exactly as they did. The slot already there has to survive.
        helper.createDatabase(TEST_DB, 21).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Heavy lower', 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt)
                VALUES ('p1', 'Upper/Lower', 1, 1, 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slots
                    (id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt)
                VALUES ('slot1', 'p1', 't1', 0, 'MONDAY', 100, 100, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 22, true, MIGRATION_21_22)

        migrated.query("SELECT id, weekday FROM program_slots").use { cursor ->
            assertTrue("the slot already there survived", cursor.moveToFirst())
            assertEquals("slot1", cursor.getString(0))
            assertEquals("MONDAY", cursor.getString(1))
        }
        listOf("program_slot_exercises", "program_slot_sets").forEach { table ->
            migrated.query("SELECT COUNT(*) FROM $table").use { cursor ->
                cursor.moveToFirst()
                assertEquals("no $table is invented by the upgrade", 0, cursor.getInt(0))
            }
        }

        migrated.close()
    }

    @Test
    fun migration22To23_addsDeloads_withoutInventingOne() {
        // ROADMAP P3.10. A program with no deload gets no rows, which is exactly the state it was
        // in: every week counted, which is what an unmarked week means.
        helper.createDatabase(TEST_DB, 22).apply {
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt)
                VALUES ('p1', 'Upper/Lower', 1, 1, 100, 100, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 23, true, MIGRATION_22_23)

        migrated.query("SELECT id FROM programs").use { cursor ->
            assertTrue("the program already there survived", cursor.moveToFirst())
            assertEquals("p1", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM program_deloads").use { cursor ->
            cursor.moveToFirst()
            assertEquals("no deload is invented by the upgrade", 0, cursor.getInt(0))
        }

        migrated.close()
    }

    @Test
    fun migration23To24_addsSubstitutions_withoutInventingOne() {
        // ROADMAP P3.11. A program with no substitution gets no rows, which is exactly the state it
        // was in: every occurrence trained with what the slot names.
        helper.createDatabase(TEST_DB, 23).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Heavy lower', 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt)
                VALUES ('p1', 'Upper/Lower', 1, 1, 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slots
                    (id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt)
                VALUES ('slot1', 'p1', 't1', 0, 'MONDAY', 100, 100, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 24, true, MIGRATION_23_24)

        migrated.query("SELECT id FROM program_slots").use { cursor ->
            assertTrue("the slot already there survived", cursor.moveToFirst())
            assertEquals("slot1", cursor.getString(0))
        }
        migrated.query("SELECT COUNT(*) FROM program_substitutions").use { cursor ->
            cursor.moveToFirst()
            assertEquals("no substitution is invented by the upgrade", 0, cursor.getInt(0))
        }

        migrated.close()
    }

    @Test
    fun migration24To25_dropsTheTemplateWeekday_andKeepsWhatBelongedToIt() {
        // ROADMAP N56: the column goes, the template stays — its name, its timestamps and every
        // foreign key pointing at its id. A pinned plan comes out with no day at all, which is the
        // rule being stated rather than a migration that failed.
        helper.createDatabase(TEST_DB, 24).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, weekday, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Heavy lower', 'FRIDAY', 100, 100, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises
                    (id, templateId, exerciseId, position, createdAt, updatedAt, deletedAt)
                VALUES ('te1', 't1', 'back-squat', 0, 100, 100, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 25, true, MIGRATION_24_25)

        migrated.query("SELECT id, name, createdAt FROM templates").use { cursor ->
            assertTrue("the template survived with the column", cursor.moveToFirst())
            assertEquals("t1", cursor.getString(0))
            assertEquals("Heavy lower", cursor.getString(1))
            assertEquals(100L, cursor.getLong(2))
        }
        // The foreign key into `templates` still resolves, which is what the rebuild had to preserve.
        migrated.query("SELECT id FROM template_exercises WHERE templateId = 't1'").use { cursor ->
            assertTrue("the planned exercise still points at its template", cursor.moveToFirst())
            assertEquals("te1", cursor.getString(0))
        }

        migrated.close()
    }

    @Test
    fun migration25To26_addsTheSoreMuscles_withoutInventingOne() {
        // ROADMAP N62. A session that reported nothing gets no rows, which is exactly the state it
        // was in — one free-text note was all there was to write — and the session already there
        // survives with its note untouched.
        helper.createDatabase(TEST_DB, 25).apply {
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     zoneOffsetMinutes, createdAt, updatedAt, deletedAt, templateId)
                VALUES ('s1', 100, 200, NULL, NULL, 'Slept badly', 0, 100, 200, NULL, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 26, true, MIGRATION_25_26)

        migrated.query("SELECT id, readinessNote FROM workout_sessions").use { cursor ->
            assertTrue("the session already there survived", cursor.moveToFirst())
            assertEquals("s1", cursor.getString(0))
            assertEquals("and its note is untouched", "Slept badly", cursor.getString(1))
        }
        migrated.query("SELECT COUNT(*) FROM session_sore_muscles").use { cursor ->
            cursor.moveToFirst()
            assertEquals("no sore muscle is invented by the upgrade", 0, cursor.getInt(0))
        }
        // A row the migrated schema accepts, with the name the converter writes and the session it
        // belongs to — the foreign key is what makes the table's rows belong to that workout.
        migrated.execSQL(
            "INSERT INTO session_sore_muscles " +
                "(id, sessionId, muscle, score, position, createdAt, updatedAt, deletedAt) " +
                "VALUES ('sm1', 's1', 'QUADS', 8, 0, 100, 100, NULL)",
        )
        migrated.query("SELECT muscle, score FROM session_sore_muscles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("QUADS", cursor.getString(0))
            assertEquals(8, cursor.getInt(1))
        }

        migrated.close()
    }

    @Test
    fun migration26To27_addsTheJointList_leavingTheLegacyColumnsAlone() {
        // ROADMAP N63. The legacy `jointPain` number and its free text are data a previous version
        // recorded; the migration adds a table and must not rewrite them. An exercise rated before
        // the change gets no joint rows, which is exactly the state it was in.
        helper.createDatabase(TEST_DB, 26).apply {
            execSQL(
                """
                INSERT INTO workout_sessions
                    (id, startedAt, finishedAt, notes, restEndsAt, readinessNote,
                     zoneOffsetMinutes, createdAt, updatedAt, deletedAt, templateId)
                VALUES ('s1', 100, 200, NULL, NULL, NULL, 0, 100, 200, NULL, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern,
                     isCustom, createdAt, updatedAt, deletedAt, restSeconds, techniqueNote)
                VALUES ('back-squat', 'Back Squat', 'QUADS', '', 'BARBELL', 'SQUAT',
                        0, 100, 100, NULL, NULL, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises
                    (id, sessionId, exerciseId, position, restSeconds, techniqueNote, finishedAt,
                     muscleFeel, jointPain, jointPainNote, supersetGroup, createdAt, updatedAt, deletedAt)
                VALUES ('se1', 's1', 'back-squat', 0, NULL, NULL, NULL,
                        8, 7, 'left shoulder', NULL, 100, 100, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 27, true, MIGRATION_26_27)

        migrated.query(
            "SELECT muscleFeel, jointPain, jointPainNote FROM session_exercises WHERE id = 'se1'",
        ).use { cursor ->
            assertTrue("the exercise already there survived", cursor.moveToFirst())
            assertEquals("its muscle feel is untouched", 8, cursor.getInt(0))
            assertEquals("the legacy joint number is not rewritten", 7, cursor.getInt(1))
            assertEquals("nor its free text", "left shoulder", cursor.getString(2))
        }
        migrated.query("SELECT COUNT(*) FROM session_exercise_joints").use { cursor ->
            cursor.moveToFirst()
            assertEquals("no joint is invented by the upgrade", 0, cursor.getInt(0))
        }
        // A row the migrated schema accepts, with the names the converters write and the exercise it
        // belongs to — the foreign key is what makes the rows belong to that session exercise.
        migrated.execSQL(
            "INSERT INTO session_exercise_joints " +
                "(id, sessionExerciseId, joint, side, score, position, createdAt, updatedAt, deletedAt) " +
                "VALUES ('j1', 'se1', 'KNEE', 'LEFT', 6, 0, 100, 100, NULL)",
        )
        migrated.query("SELECT joint, side, score FROM session_exercise_joints").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("KNEE", cursor.getString(0))
            assertEquals("left and right are stored apart", "LEFT", cursor.getString(1))
            assertEquals(6, cursor.getInt(2))
        }

        migrated.close()
    }

    @Test
    fun migration27To28_consolidatesEachExercisesTargetRpe_andLeavesTheSetValuesAlone() {
        // ROADMAP N59, amended: the plan's effort moves from one per prescribed set to one per
        // exercise, and the backfill is a *consolidation* — the last set that names one, in setIndex
        // order, warm-ups included. The per-set values stay for a backup written before the change to
        // carry, and the exercise's row is the only thing rewritten.
        helper.createDatabase(TEST_DB, 27).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt, deletedAt)
                VALUES ('t1', 'Legs', 1, 1, NULL)
                """.trimIndent(),
            )
            // te1: a ramp whose top set is the plan's target — 8, not the ramp's 5. te2: nothing named.
            // te3: only a warm-up names one, which still counts (a ramp's last set is the top set).
            execSQL(
                """
                INSERT INTO template_exercises
                    (id, templateId, exerciseId, position, restSeconds, techniqueNote,
                     supersetGroup, createdAt, updatedAt, deletedAt)
                VALUES ('te1', 't1', 'back-squat', 0, NULL, NULL, NULL, 1, 1, NULL),
                       ('te2', 't1', 'bench-press', 1, NULL, NULL, NULL, 1, 1, NULL),
                       ('te3', 't1', 'overhead-press', 2, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            // te2's only set that named an effort is soft-deleted (deletedAt 200), so the backfill has
            // to skip it: a plan's deleted set is not what the plan builds to (N59).
            execSQL(
                """
                INSERT INTO template_sets
                    (id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams,
                     targetRepsMin, targetRepsMax, targetRpeHalves, note, createdAt, updatedAt, deletedAt)
                VALUES ('ts1', 'te1', 0, 'WARMUP', 40000, NULL, 5, 5, 5, NULL, 1, 1, NULL),
                       ('ts2', 'te1', 1, 'NORMAL', 100000, NULL, 5, 5, 8, NULL, 1, 1, NULL),
                       ('ts3', 'te2', 0, 'NORMAL', 80000, NULL, 5, 5, NULL, NULL, 1, 1, NULL),
                       ('ts4', 'te3', 0, 'WARMUP', 50000, NULL, 5, 5, 6, NULL, 1, 1, NULL),
                       ('ts5', 'te3', 1, 'NORMAL', 90000, NULL, 5, 5, NULL, NULL, 1, 1, NULL),
                       ('ts6', 'te2', 1, 'NORMAL', 80000, NULL, 5, 5, 9, NULL, 1, 1, 200)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt, deletedAt)
                VALUES ('p1', 'Upper/Lower', 0, 0, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slots
                    (id, programId, templateId, position, weekday, createdAt, updatedAt, deletedAt)
                VALUES ('slot1', 'p1', 't1', 0, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slot_exercises
                    (id, slotId, exerciseId, restSeconds, techniqueNote, createdAt, updatedAt, deletedAt)
                VALUES ('pse1', 'slot1', 'back-squat', NULL, NULL, 1, 1, NULL),
                       ('pse2', 'slot1', 'bench-press', NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slot_sets
                    (id, slotExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams,
                     targetRepsMin, targetRepsMax, targetRpeHalves, targetPercentOf1Rm, note,
                     createdAt, updatedAt, deletedAt)
                VALUES ('pss1', 'pse1', 0, 'NORMAL', 100000, NULL, 3, 3, 16, NULL, NULL, 1, 1, NULL),
                       ('pss2', 'pse1', 1, 'NORMAL', 100000, NULL, 3, 3, 18, NULL, NULL, 1, 1, NULL),
                       ('pss3', 'pse2', 0, 'NORMAL', 80000, NULL, 5, 5, NULL, NULL, NULL, 1, 1, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 28, true, MIGRATION_27_28)

        migrated.query(
            "SELECT id, targetRpeHalves FROM template_exercises ORDER BY position",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("te1", cursor.getString(0))
            assertEquals("the top set wins over the ramp's 5", 8, cursor.getInt(1))
            assertTrue(cursor.moveToNext())
            assertEquals("te2", cursor.getString(0))
            assertTrue("an exercise that named nothing stays empty", cursor.isNull(1))
            assertTrue(cursor.moveToNext())
            assertEquals("te3", cursor.getString(0))
            assertEquals("a warm-up's value counts, and it is the last one", 6, cursor.getInt(1))
        }
        // The per-set values are left in place rather than cleared, the soft-deleted one included: a
        // pre-change backup still needs them, and the reader falls back to them where the exercise
        // names nothing. A save that *clears* the exercise's effort is what retires them (N59).
        migrated.query(
            "SELECT id, targetRpeHalves FROM template_sets ORDER BY id",
        ).use { cursor ->
            val values = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getInt(1).takeIf { !cursor.isNull(1) })
            }
            assertEquals(
                listOf("ts1" to 5, "ts2" to 8, "ts3" to null, "ts4" to 6, "ts5" to null, "ts6" to 9),
                values,
            )
        }

        migrated.query(
            "SELECT id, targetRpeHalves FROM program_slot_exercises ORDER BY id",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("pse1", cursor.getString(0))
            assertEquals("the last prescribed set that named one", 18, cursor.getInt(1))
            assertTrue(cursor.moveToNext())
            assertEquals("pse2", cursor.getString(0))
            assertTrue("a slot that named nothing stays empty", cursor.isNull(1))
        }
        migrated.query(
            "SELECT id, targetRpeHalves FROM program_slot_sets ORDER BY id",
        ).use { cursor ->
            val values = buildList {
                while (cursor.moveToNext()) add(cursor.getString(0) to cursor.getInt(1).takeIf { !cursor.isNull(1) })
            }
            assertEquals(listOf("pss1" to 16, "pss2" to 18, "pss3" to null), values)
        }

        migrated.close()
    }

    @Test
    fun migration28To29_clearsEveryWarmUpsEffort_andLeavesTheWorkingSetsAlone() {
        // ROADMAP N67: a warm-up carries no effort. The rule is new, so a logged warm-up can hold an
        // RPE and a *planned* one the legacy per-set target N59 left as a fallback. Both are cleared,
        // because the editor no longer offers the field and a number nothing can show is dead weight.
        // The working sets keep theirs on both sides, and so does the exercise-level target, which
        // belongs to the exercise's working sets rather than to any one row.
        helper.createDatabase(TEST_DB, 28).apply {
            execSQL(
                """
                INSERT INTO workout_sessions (id, startedAt, createdAt, updatedAt)
                VALUES ('s1', 1, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO session_exercises (id, sessionId, exerciseId, position, createdAt, updatedAt)
                VALUES ('se1', 's1', 'back-squat', 0, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO set_entries
                    (id, sessionExerciseId, setIndex, reps, weightGrams, assistanceGrams, setType,
                     rpeHalves, createdAt, updatedAt)
                VALUES ('e1', 'se1', 0, 5, 60000, 0, 'WARMUP', 12, 1, 1),
                       ('e2', 'se1', 1, 5, 100000, 0, 'NORMAL', 18, 1, 1),
                       ('e3', 'se1', 2, 8, 80000, 0, 'WARMUP', NULL, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt)
                VALUES ('t1', 'Legs', 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises
                    (id, templateId, exerciseId, position, targetRpeHalves, createdAt, updatedAt)
                VALUES ('te1', 't1', 'back-squat', 0, 8, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_sets
                    (id, templateExerciseId, setIndex, role, targetRepsMin, targetRepsMax,
                     targetRpeHalves, createdAt, updatedAt)
                VALUES ('ts1', 'te1', 0, 'WARMUP', 5, 5, 5, 1, 1),
                       ('ts2', 'te1', 1, 'NORMAL', 5, 5, 8, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt)
                VALUES ('p1', 'Upper/Lower', 0, 0, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slots (id, programId, templateId, position, createdAt, updatedAt)
                VALUES ('slot1', 'p1', 't1', 0, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slot_exercises
                    (id, slotId, exerciseId, targetRpeHalves, createdAt, updatedAt)
                VALUES ('pse1', 'slot1', 'back-squat', 9, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slot_sets
                    (id, slotExerciseId, setIndex, role, targetRepsMin, targetRepsMax,
                     targetRpeHalves, createdAt, updatedAt)
                VALUES ('pss1', 'pse1', 0, 'WARMUP', 5, 5, 5, 1, 1),
                       ('pss2', 'pse1', 1, 'NORMAL', 3, 3, 16, 1, 1)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 29, true, MIGRATION_28_29)

        migrated.query("SELECT id, rpeHalves FROM set_entries ORDER BY id").use { cursor ->
            val values = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getInt(1).takeIf { !cursor.isNull(1) })
                }
            }
            assertEquals("only the working set keeps an effort", listOf("e1" to null, "e2" to 18, "e3" to null), values)
        }
        migrated.query("SELECT id, targetRpeHalves FROM template_sets ORDER BY id").use { cursor ->
            val values = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getInt(1).takeIf { !cursor.isNull(1) })
                }
            }
            assertEquals(listOf("ts1" to null, "ts2" to 8), values)
        }
        migrated.query("SELECT id, targetRpeHalves FROM program_slot_sets ORDER BY id").use { cursor ->
            val values = buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getInt(1).takeIf { !cursor.isNull(1) })
                }
            }
            assertEquals(listOf("pss1" to null, "pss2" to 16), values)
        }
        migrated.query("SELECT id, targetRpeHalves FROM template_exercises").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(
                "the exercise-level target belongs to the working sets and is left alone",
                8,
                cursor.getInt(1),
            )
        }
        migrated.query("SELECT id, targetRpeHalves FROM program_slot_exercises").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the slot's exercise-level target is left alone too", 9, cursor.getInt(1))
        }

        migrated.close()
    }

    @Test
    fun migration29To30_dropsTheSlotsPrescriptions_andLeavesTheScheduleStanding() {
        // ROADMAP N73: a program is a schedule over templates and nothing else. The two tables that
        // carried a slot's own sets, rest, cue and effort are dropped, and the data in them goes with
        // them — that is the change, not a side effect of it. The program, its slot, the template and
        // the template's own planned set are all untouched.
        helper.createDatabase(TEST_DB, 29).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt)
                VALUES ('t1', 'Legs', 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises (id, templateId, exerciseId, position, createdAt, updatedAt)
                VALUES ('te1', 't1', 'back-squat', 0, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_sets
                    (id, templateExerciseId, setIndex, role, targetWeightGrams, targetRepsMin,
                     targetRepsMax, createdAt, updatedAt)
                VALUES ('ts1', 'te1', 0, 'NORMAL', 100000, 5, 5, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO programs (id, name, isActive, position, createdAt, updatedAt)
                VALUES ('p1', 'Upper/Lower', 0, 0, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slots (id, programId, templateId, position, weekday, createdAt, updatedAt)
                VALUES ('slot1', 'p1', 't1', 0, 'MONDAY', 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slot_exercises
                    (id, slotId, exerciseId, restSeconds, techniqueNote, targetRpeHalves,
                     createdAt, updatedAt)
                VALUES ('pse1', 'slot1', 'back-squat', 150, 'brace hard', 16, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO program_slot_sets
                    (id, slotExerciseId, setIndex, role, targetWeightGrams, targetRepsMin,
                     targetRepsMax, targetRpeHalves, targetPercentOf1Rm, createdAt, updatedAt)
                VALUES ('pss1', 'pse1', 0, 'TOP_SET', 120000, 1, 2, 18, 85, 1, 1)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 30, true, MIGRATION_29_30)

        migrated.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name IN ('program_slot_exercises', 'program_slot_sets')",
        ).use { cursor ->
            assertFalse("both prescription tables must be gone", cursor.moveToFirst())
        }
        migrated.query("SELECT id, templateId, weekday FROM program_slots").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("slot1", cursor.getString(0))
            assertEquals("the slot still names its template", "t1", cursor.getString(1))
            assertEquals("and keeps its day", "MONDAY", cursor.getString(2))
        }
        migrated.query("SELECT id, targetWeightGrams FROM template_sets").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("the template's own planned work is untouched", 100_000L, cursor.getLong(1))
        }

        migrated.close()
    }

    @Test
    fun migration30To31_givesAnExerciseItsOwnUnit_leavingEveryRowOnTheAppSetting() {
        // ROADMAP N64: an exercise may carry its own display unit. The column is nullable with no
        // default, so every existing row reads as unset — which is "follow the app setting" — and
        // nothing changes for anyone until they set one. Presentation only: no weight moves.
        helper.createDatabase(TEST_DB, 30).apply {
            execSQL(
                """
                INSERT INTO exercises
                    (id, name, primaryMuscle, secondaryMuscles, equipment, movementPattern, isCustom,
                     restSeconds, techniqueNote, createdAt, updatedAt)
                VALUES ('e1', 'Back Squat', 'QUADS', '', 'BARBELL', 'SQUAT', 0, NULL, NULL, 1, 1)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 31, true, MIGRATION_30_31)

        migrated.query("SELECT id, weightUnit FROM exercises").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("e1", cursor.getString(0))
            assertTrue("unset means follow the app setting", cursor.isNull(1))
        }
        migrated.close()
    }

    @Test
    fun migration31To32_seedsTheClimbAtTheRangesFloor_andLeavesTheRangeAlone() {
        // ROADMAP N74: a plan's two rep bounds become a range progression never edits, so the number
        // the session asks for moves into a column of its own. It arrives at the range's floor — the
        // bottom of "5 to 8" — because that is where the lifter restarts after a weight step, and it
        // is what a ranged plan's first session under the rule asks for. A set that wrote no reps
        // keeps nothing, and the bounds themselves are untouched.
        helper.createDatabase(TEST_DB, 31).apply {
            execSQL(
                """
                INSERT INTO templates (id, name, createdAt, updatedAt)
                VALUES ('t1', 'Legs', 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_exercises (id, templateId, exerciseId, position, createdAt, updatedAt)
                VALUES ('te1', 't1', 'back-squat', 0, 1, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO template_sets
                    (id, templateExerciseId, setIndex, role, targetWeightGrams, targetAssistanceGrams,
                     targetRepsMin, targetRepsMax, targetRpeHalves, note, createdAt, updatedAt)
                VALUES ('ranged', 'te1', 0, 'NORMAL', 100000, NULL, 5, 8, 16, 'belt on', 1, 1),
                       ('floor-only', 'te1', 1, 'NORMAL', 80000, NULL, 5, NULL, NULL, NULL, 1, 1),
                       ('ceiling-only', 'te1', 2, 'NORMAL', 60000, NULL, NULL, 8, NULL, NULL, 1, 1),
                       ('no-reps', 'te1', 3, 'WARMUP', 40000, NULL, NULL, NULL, NULL, NULL, 1, 1)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 32, true, MIGRATION_31_32)

        migrated.query(
            "SELECT id, targetRepsMin, targetRepsMax, targetRepsCurrent, note FROM template_sets " +
                "ORDER BY setIndex",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("ranged", cursor.getString(0))
            assertEquals("the floor is where the climb starts", 5, cursor.getInt(3))
            assertEquals("and the range itself is untouched", 5, cursor.getInt(1))
            assertEquals(8, cursor.getInt(2))
            assertEquals("the rest of the row came across", "belt on", cursor.getString(4))

            assertTrue(cursor.moveToNext())
            assertEquals("floor-only", cursor.getString(0))
            assertEquals("a plan with only a floor starts there", 5, cursor.getInt(3))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("targetRepsMax")))

            assertTrue(cursor.moveToNext())
            assertEquals("ceiling-only", cursor.getString(0))
            assertEquals("with no floor, its ceiling is the start", 8, cursor.getInt(3))

            assertTrue(cursor.moveToNext())
            assertEquals("no-reps", cursor.getString(0))
            assertTrue("a set that names no reps has no place to be", cursor.isNull(3))
        }

        migrated.close()
    }
}
