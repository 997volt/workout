package com.example.androidapp.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * The app's local database (ROADMAP F5).
 *
 * `exportSchema = true` writes the schema to `app/schemas/` on every build; those
 * JSON files are committed and are what migration tests validate against.
 *
 * There is deliberately **no** `fallbackToDestructiveMigration()`: it would turn
 * a forgotten migration into silent, total data loss for a user's training
 * history. A missing migration must fail loudly in development instead.
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
    ],
    version = 33,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun measurementDao(): MeasurementDao

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
        const val NAME = "workout.db"
    }
}
