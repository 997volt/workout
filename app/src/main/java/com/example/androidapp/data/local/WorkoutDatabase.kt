package com.example.androidapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's local database (ROADMAP F5, F14).
 *
 * `exportSchema = true` writes the schema to `app/schemas/` on every build; those
 * JSON files are committed and are what migration tests validate against.
 *
 * **This is 2.0's store, and it starts at schema 1** (ROADMAP N104). 1.x's file — `workout.db` — is
 * deliberately never opened by this build: it is left untouched on the device, where the 1.18 APK is
 * still the way to read it, and the migration chain that would have carried it here went with the
 * 1.x store. See [com.example.androidapp.data.RoomBackupRepository] for what "delete everything" owes
 * a second store.
 *
 * **Schema 2 is N110**, and it is a data migration rather than a structural one: the library's shape became
 * a category over movements and nothing deeper, so a database written under the old shape has to be
 * re-filed. See [MIGRATION_1_2].
 *
 * **Schema 4 is N134**, one nullable column: a template can keep a version, and a version is a template row
 * naming the template it was kept from. See [MIGRATION_3_4].
 *
 * **Schema 5 is N137**, one new table: a metric's goal keeps its past, so a goal is a row per **period**
 * rather than a value in a settings string that was rewritten on every change. See [MIGRATION_4_5].
 *
 * **Schema 6 is N141**, one nullable column: a session can be an **activity** — cycling, a swim — which is
 * a session that holds no sets. See [MIGRATION_5_6].
 *
 * **Schema 7 is N147**, one new table: a marked deload week can name the workouts it prescribes, so the
 * week's own days are rows hanging off the `program_deloads` mark P3.10 already keeps. See [MIGRATION_6_7].
 *
 * 8 adds a program's kind and its authored start date (N174): every row already on disk is WEEKLY,
 * which is the rule it was written under. See [MIGRATION_7_8].
 *
 * **Schema 9 is N214**, one nullable column: a workout can be **hidden** from the choices without being
 * deleted, and every row already on disk is visible, which is what it was. See [MIGRATION_8_9].
 *
 * **Schema 10 is N219**, two columns and no backfill: a movement can say it is **loaded by the body** (not
 * null, defaulting to ordinary) and a set can carry the lifter's weight at the moment it was logged
 * (nullable, because a set written before this had only its added weight — which is what it was). See
 * [MIGRATION_9_10].
 *
 * **Schema 11 is N220**, the same shape again: a movement can say its sets are **held** (not null, defaulting
 * to counted) and a set can carry a duration (nullable, because every set on disk was a counted one). See
 * [MIGRATION_10_11].
 *
 * There is deliberately **no** `fallbackToDestructiveMigration()`: it would turn
 * a forgotten migration into silent, total data loss for a user's training
 * history. A missing migration must fail loudly in development instead — which is exactly why 2.0
 * starts a *new file* rather than truncating the old one.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SetEntryEntity::class,
        TemplateEntity::class,
        TemplateExerciseEntity::class,
        TemplateSetEntity::class,
        MeasurementEntity::class,
        ProgramEntity::class,
        ProgramSlotEntity::class,
        ProgramSkipEntity::class,
        ProgramDeloadEntity::class,
        ProgramDeloadDayEntity::class,
        ProgramSubstitutionEntity::class,
        SessionSoreMuscleEntity::class,
        SessionExerciseJointEntity::class,
        MeasurementSoreMuscleEntity::class,
        MetricGoalEntity::class,
    ],
    version = 11,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun measurementDao(): MeasurementDao

    /** The muscles a day's measurement reported sore, each with its own score (ROADMAP N113). */
    abstract fun measurementSoreMuscleDao(): MeasurementSoreMuscleDao

    /** The periods each metric's goal went through (ROADMAP N137). */
    abstract fun metricGoalDao(): MetricGoalDao

    abstract fun statisticsDao(): StatisticsDao

    abstract fun sessionExerciseDao(): SessionExerciseDao

    abstract fun workoutDao(): WorkoutDao

    /** Templates and their exercises (ROADMAP N3). */
    abstract fun templateDao(): TemplateDao

    /** A plan's planned sets, split out of [TemplateDao] at schema 9 (ROADMAP N3, N14). */
    abstract fun templateSetDao(): TemplateSetDao

    /** Programs and their slots (ROADMAP P3.3). */
    abstract fun programDao(): ProgramDao

    /** The occurrences a lifter consciously passed over (ROADMAP P3.3, P3.13). */
    abstract fun programSkipDao(): ProgramSkipDao

    /** The sessions and skips a program's run is derived from (ROADMAP P3.9). */
    abstract fun programRunDao(): ProgramRunDao

    /** The weeks a program was deliberately backed off (ROADMAP P3.10). */
    abstract fun programDeloadDao(): ProgramDeloadDao

    /** The workouts that stood in for a slot's own, one week at a time (ROADMAP P3.11). */
    abstract fun programSubstitutionDao(): ProgramSubstitutionDao

    /** The muscles a session reported sore, each with its own score (ROADMAP N62). */
    abstract fun sessionSoreMuscleDao(): SessionSoreMuscleDao

    /** The joints a session exercise reported painful, each with its own side and score (ROADMAP N63). */
    abstract fun sessionExerciseJointDao(): SessionExerciseJointDao

    /** Whole-table reads and additive inserts for backup/restore (P1.12). */
    abstract fun backupDao(): BackupDao

    /** The newest tables' half of that (ROADMAP P3.3), split to keep both under the ceiling. */
    abstract fun programBackupDao(): ProgramBackupDao

    /** Updates and soft deletes a program edit needs (ROADMAP N171), apart from the insert-only path. */
    abstract fun programEditDao(): ProgramEditDao

    /** Read-only per-workout aggregates for the trends screen (ROADMAP N13). */
    abstract fun trendsDao(): TrendsDao

    companion object {
        /** This store's file (ROADMAP N104). 1.x's `workout.db` is a different file on purpose. */
        const val NAME = "workout-2.db"

        /** The 1.x store, kept only so "delete everything" can remove it (ROADMAP N104). */
        const val LEGACY_NAME = "workout.db"
    }
}
