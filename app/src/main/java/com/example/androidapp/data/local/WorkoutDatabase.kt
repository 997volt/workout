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
        ProgramSubstitutionEntity::class,
        SessionSoreMuscleEntity::class,
        SessionExerciseJointEntity::class,
        MeasurementSoreMuscleEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun measurementDao(): MeasurementDao

    /** The muscles a day's measurement reported sore, each with its own score (ROADMAP N113). */
    abstract fun measurementSoreMuscleDao(): MeasurementSoreMuscleDao

    abstract fun statisticsDao(): StatisticsDao

    abstract fun sessionExerciseDao(): SessionExerciseDao

    abstract fun workoutDao(): WorkoutDao

    /** Templates and their exercises (ROADMAP N3). */
    abstract fun templateDao(): TemplateDao

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

    /** Read-only per-workout aggregates for the trends screen (ROADMAP N13). */
    abstract fun trendsDao(): TrendsDao

    companion object {
        /** This store's file (ROADMAP N104). 1.x's `workout.db` is a different file on purpose. */
        const val NAME = "workout-2.db"

        /** The 1.x store, kept only so "delete everything" can remove it (ROADMAP N104). */
        const val LEGACY_NAME = "workout.db"
    }
}
