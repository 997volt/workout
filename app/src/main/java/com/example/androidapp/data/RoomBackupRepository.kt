package com.example.androidapp.data

import android.content.Context
import androidx.room.withTransaction
import com.example.androidapp.data.local.MetricGoalEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.seedMissingExercises
import com.example.androidapp.data.transfer.BackupCodec
import com.example.androidapp.data.transfer.BackupFile
import com.example.androidapp.data.transfer.toDto
import com.example.androidapp.data.transfer.toEntity
import com.example.androidapp.data.transfer.withValidLibraryShape
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.BackupRepository
import com.example.androidapp.domain.repository.ImportSummary
import com.example.androidapp.platform.CrashLogStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed backup and restore (ROADMAP P1.12).
 *
 * Both directions go through [dataResultOf], so a malformed file becomes a typed
 * `DataError.Invalid` carrying a message written for the user rather than an
 * exception that takes the screen down.
 */
@Singleton
class RoomBackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
    private val crashLogStore: CrashLogStore,
) : BackupRepository {

    private val dao = database.backupDao()

    /** The program tables' half of the same job, split out to keep both DAOs under the ceiling. */
    private val programBackup = database.programBackupDao()

    /** The sore-muscle rows (ROADMAP N62), whose own DAO carries their backup queries. */
    private val soreMuscles = database.sessionSoreMuscleDao()

    /** The per-exercise joint rows (ROADMAP N63), the same shape. */
    private val joints = database.sessionExerciseJointDao()

    /** The daily record's sore rows (ROADMAP N113), the same shape again. */
    private val measurementSore = database.measurementSoreMuscleDao()

    /** The body's record, whose own DAO carries its backup queries (ROADMAP N32, N113). */
    private val measurements = database.measurementDao()

    /** The goal history, whose own DAO carries its backup queries (ROADMAP N137). */
    private val metricGoals = database.metricGoalDao()

    override suspend fun export(): DataResult<String> = dataResultOf {
        // The goal that is open for each metric, read once: the file carries the periods *and* the two maps a
        // build that predates them reads, and both come from the same rows (N137).
        val openGoals = metricGoals.observeCurrent().first()
        BackupCodec.encode(
            BackupFile(
                schemaVersion = BackupCodec.CURRENT_SCHEMA_VERSION,
                exportedAt = timeSource.nowEpochMillis(),
                exercises = dao.allExercises().map { it.toDto() },
                sessions = dao.allSessions().map { it.toDto() },
                sessionExercises = dao.allSessionExercises().map { it.toDto() },
                sets = dao.allSets().map { it.toDto() },
                // The muscles each session reported sore (ROADMAP N62): a fact the lifter wrote,
                // so a restore that dropped it would lose work in silence.
                sessionSoreMuscles = soreMuscles.allForBackup().map { it.toDto() },
                // The joints each exercise reported painful (ROADMAP N63): the same fact about the
                // body, and a restore that dropped it would lose what the lifter recorded.
                sessionExerciseJoints = joints.allForBackup().map { it.toDto() },
                // A template is a plan, and the plan is the user's work too (N3).
                templates = dao.allTemplates().map { it.toDto() },
                templateExercises = dao.allTemplateExercises().map { it.toDto() },
                // A plan's sets are the plan (ROADMAP N14).
                templateSets = dao.allTemplateSets().map { it.toDto() },
                // Programs, their slots and their recorded skips (ROADMAP P3.3): the schedule is
                // authored setup, and a skip is history — both are the user's data.
                programs = programBackup.allPrograms().map { it.toDto() },
                programSlots = programBackup.allProgramSlots().map { it.toDto() },
                programSkips = programBackup.allProgramSkips().map { it.toDto() },
                // The weeks a program was backed off (ROADMAP P3.10): authored setup the app cannot
                // recompute, so dropping them would read every deload week as a miss.
                programDeloads = programBackup.allProgramDeloads().map { it.toDto() },
                // The workouts that stood in for a slot's own (ROADMAP P3.11): without them a
                // restored week reads as missed even though it was trained, with something else.
                programSubstitutions = programBackup.allProgramSubstitutions().map { it.toDto() },
                measurements = measurements.allForExport().map { it.toDto() },
                // The soreness a daily entry reported (ROADMAP N113): a fact the lifter wrote, so a
                // restore that dropped it would lose work in silence.
                measurementSoreMuscles = measurementSore.allForBackup().map { it.toDto() },
                // The user's metric targets (ROADMAP N39) and the kind each has (N135), as the two maps a
                // build that predates periods reads. They are filled from the period that is open, so the two
                // halves of the file cannot disagree.
                goals = openGoals.mapNotNull { row -> row.value?.let { row.metricId to it } }.toMap(),
                goalKinds = openGoals.mapNotNull { row -> row.kind?.let { row.metricId to it } }.toMap(),
                // The periods themselves (ROADMAP N137): the past, which is what the table is for. The codec
                // drops whatever it is not told about, and a restore that lost these would lose the answer to
                // "what was I asking for in March".
                goalPeriods = metricGoals.allForBackup().map { it.toDto() },
                // Diagnostics ride along so they are reachable on a release
                // build; import ignores them, deliberately.
                crashLogs = crashLogStore.all(),
            ),
        )
    }

    override suspend fun clearAllUserData(): DataResult<Unit> = dataResultOf {
        // Off the main thread: `clearAllTables` is a blocking call and Room asserts where
        // it runs. The DAO's own suspending methods dispatch themselves; this one does not.
        withContext(Dispatchers.IO) {
            database.clearAllTables()

            // The seeder runs in `RoomDatabase.Callback.onOpen`, which fires when the
            // database is *opened*, not after its tables are emptied — so without this the
            // library would stay empty until the process restarted, and a clean start would
            // look broken (N18's trap).
            seedMissingExercises(
                db = database.openHelper.writableDatabase,
                seededAt = timeSource.nowEpochMillis(),
            )

            // Crash logs are diagnostics *about* the data that just went, and they live
            // outside the database, so nothing above touches them.
            if (!crashLogStore.clear()) {
                throw IOException("the crash logs could not be removed")
            }

            // **The 1.x store is user data too** (ROADMAP N104), and 2.0 leaves it on the device rather than
            // migrating it — so "delete everything" has to take it, or a lifter's training history survives
            // on disk in a file no screen mentions. `deleteDatabase` removes the file and its `-wal`/`-shm`
            // companions; a device that never had 1.x reports false, which is not a failure.
            context.deleteDatabase(WorkoutDatabase.LEGACY_NAME)

            // Metric targets are the one setting the user *authored* rather than chose as a display
            // preference, which is why they are exported — and they are **rows** now (N137), so
            // `clearAllTables` above is what takes them. The loop that emptied the settings string went with
            // the string.
        }
    }

    override suspend fun import(text: String): DataResult<ImportSummary> = dataResultOf {
        // Throws InvalidInputException for a file we cannot use, before touching
        // anything — a rejected file must leave the database exactly as it was.
        //
        // The library's shape is enforced before anything is written (B92, N110): a file can carry a link the
        // app would never write — a cycle, a category filed under something, a movement filed under a
        // movement, which is what every variation in an older file is — and the grouped list would then hide
        // or misplace the row rather than show it. A movement under a movement is **flattened onto its
        // category** rather than cleared, so the family survives the trip. A legal library is unchanged.
        val file = BackupCodec.decode(text).let { decoded ->
            decoded.copy(exercises = decoded.exercises.withValidLibraryShape())
        }

        // One transaction, so a failure part-way cannot leave sessions without their sets.
        // The shape is: a soft delete keeps the row under its id, so an insert-only import
        // skips exactly what a restore is meant to bring back — restore the hidden rows from
        // the file (which also clears `deletedAt`), then insert what is genuinely missing.
        //
        // `insertMissing` runs **first**, because the order is parent-before-child across the
        // two helpers and not just inside each one: `program_slots.templateId` and
        // `program_substitutions.templateId` reference `templates`, and running the program
        // block first made every restore into a fresh database fail with
        // `FOREIGN KEY constraint failed` and roll the whole import back.
        val summary = database.withTransaction {
            val inserted = insertMissing(file)
            val programs = importPrograms(file)
            ImportSummary(
                added = inserted + programs.added,
                restored = restoreSoftDeleted(file) + programs.restored,
            )
        }

        restoreGoalPeriods(file)

        summary
    }

    /**
     * Brings back the rows this device holds as soft-deleted, and counts what genuinely came
     * back: only a row the file itself has *live* did, because one the file also records as
     * deleted is still deleted.
     *
     * Extracted from [import] because that function had reached the length this project
     * enforces, and the seven tables here are one shape repeated — updates, so no foreign-key
     * ordering is involved: every row already exists and only its own columns change.
     */
    private suspend fun restoreSoftDeleted(file: BackupFile): Int {
        // Read once into sets: asking the database per row would be a query per element.
        val hiddenExercises = dao.softDeletedExerciseIds().toSet()
        val hiddenSessions = dao.softDeletedSessionIds().toSet()
        val hiddenSessionExercises = dao.softDeletedSessionExerciseIds().toSet()
        val hiddenSets = dao.softDeletedSetIds().toSet()
        val hiddenTemplates = dao.softDeletedTemplateIds().toSet()
        val hiddenTemplateExercises = dao.softDeletedTemplateExerciseIds().toSet()
        val hiddenTemplateSets = dao.softDeletedTemplateSetIds().toSet()
        val hiddenSoreMuscles = soreMuscles.softDeletedIds().toSet()
        val hiddenJoints = joints.softDeletedIds().toSet()
        val hiddenMeasurements = measurements.softDeletedIds().toSet()
        val hiddenMeasurementSore = measurementSore.softDeletedIds().toSet()

        val exercises = file.exercises.filter { it.id in hiddenExercises }
        val sessions = file.sessions.filter { it.id in hiddenSessions }
        val sessionExercises = file.sessionExercises.filter { it.id in hiddenSessionExercises }
        val sets = file.sets.filter { it.id in hiddenSets }
        val templates = file.templates.filter { it.id in hiddenTemplates }
        val templateExercises = file.templateExercises.filter { it.id in hiddenTemplateExercises }
        val templateSets = file.templateSets.filter { it.id in hiddenTemplateSets }
        val soreMuscleRows = file.sessionSoreMuscles.filter { it.id in hiddenSoreMuscles }
        val jointRows = file.sessionExerciseJoints.filter { it.id in hiddenJoints }
        val measurementRows = file.measurements.filter { it.id in hiddenMeasurements }
        val measurementSoreRows = file.measurementSoreMuscles.filter { it.id in hiddenMeasurementSore }

        dao.restoreExercises(exercises.map { it.toEntity() })
        dao.restoreSessions(sessions.map { it.toEntity() })
        dao.restoreSessionExercises(sessionExercises.map { it.toEntity() })
        dao.restoreSets(sets.map { it.toEntity() })
        dao.restoreTemplates(templates.map { it.toEntity() })
        dao.restoreTemplateExercises(templateExercises.map { it.toEntity() })
        dao.restoreTemplateSets(templateSets.map { it.toEntity() })
        soreMuscles.restore(soreMuscleRows.map { it.toEntity() })
        joints.restore(jointRows.map { it.toEntity() })
        // The parent before the child: a sore row un-hidden onto a measurement that stayed hidden is
        // exactly the state this ordering exists to prevent (N113).
        measurements.restore(measurementRows.map { it.toEntity() })
        measurementSore.restore(measurementSoreRows.map { it.toEntity() })

        return exercises.count { it.deletedAt == null } +
            sessions.count { it.deletedAt == null } +
            sessionExercises.count { it.deletedAt == null } +
            sets.count { it.deletedAt == null } +
            templates.count { it.deletedAt == null } +
            templateExercises.count { it.deletedAt == null } +
            templateSets.count { it.deletedAt == null } +
            soreMuscleRows.count { it.deletedAt == null } +
            jointRows.count { it.deletedAt == null } +
            measurementRows.count { it.deletedAt == null } +
            measurementSoreRows.count { it.deletedAt == null }
    }

    /**
     * Inserts what the file has and this device does not, counting only the genuinely new
     * rows: `IGNORE` skips live rows and the ones just restored, and Room returns -1 for a
     * skipped row.
     *
     * Parents before children, so the foreign keys hold as rows go in.
     */
    private suspend fun insertMissing(file: BackupFile): Int =
        dao.insertExercises(file.exercises.map { it.toEntity() }).count { it != SKIPPED } +
            dao.insertSessions(file.sessions.map { it.toEntity() }).count { it != SKIPPED } +
            dao.insertSessionExercises(file.sessionExercises.map { it.toEntity() }).count { it != SKIPPED } +
            // The joint rows go in after the exercises they hang off, which the line above wrote (N63).
            joints.insert(file.sessionExerciseJoints.map { it.toEntity() }).count { it != SKIPPED } +
            // The sore-muscle rows go in after their sessions, which the lines above already wrote (N62).
            soreMuscles.insert(file.sessionSoreMuscles.map { it.toEntity() }).count { it != SKIPPED } +
            dao.insertSets(file.sets.map { it.toEntity() }).count { it != SKIPPED } +
            dao.insertTemplates(file.templates.map { it.toEntity() }).count { it != SKIPPED } +
            dao.insertTemplateExercises(file.templateExercises.map { it.toEntity() }).count { it != SKIPPED } +
            dao.insertTemplateSets(file.templateSets.map { it.toEntity() }).count { it != SKIPPED } +
            measurements.insertAll(file.measurements.map { it.toEntity() }).count { it != SKIPPED } +
            // The sore rows hang off a daily measurement, so they go in after the entries above (N113).
            measurementSore.insert(file.measurementSoreMuscles.map { it.toEntity() })
                .count { it != SKIPPED }

    /**
     * Restores and adds the program tables (ROADMAP P3.3), returning what each step did.
     *
     * The same hidden-then-insert shape as everything else in [import], extracted because the
     * enclosing function had reached the length this project enforces — and because the three
     * tables are a self-contained group that has to move together.
     */
    private suspend fun importPrograms(file: BackupFile): Imported {
        val hiddenPrograms = programBackup.softDeletedProgramIds().toSet()
        val hiddenSlots = programBackup.softDeletedProgramSlotIds().toSet()
        val hiddenSkips = programBackup.softDeletedProgramSkipIds().toSet()
        val hiddenDeloads = programBackup.softDeletedProgramDeloadIds().toSet()
        val hiddenSubstitutions = programBackup.softDeletedProgramSubstitutionIds().toSet()

        val programsToRestore = file.programs.filter { it.id in hiddenPrograms }
        val slotsToRestore = file.programSlots.filter { it.id in hiddenSlots }
        val skipsToRestore = file.programSkips.filter { it.id in hiddenSkips }
        val deloadsToRestore = file.programDeloads.filter { it.id in hiddenDeloads }
        val substitutionsToRestore = file.programSubstitutions.filter { it.id in hiddenSubstitutions }

        programBackup.restorePrograms(programsToRestore.map { it.toEntity() })
        programBackup.restoreProgramSlots(slotsToRestore.map { it.toEntity() })
        programBackup.restoreProgramSkips(skipsToRestore.map { it.toEntity() })
        programBackup.restoreProgramDeloads(deloadsToRestore.map { it.toEntity() })
        programBackup.restoreProgramSubstitutions(substitutionsToRestore.map { it.toEntity() })

        val restored = programsToRestore.count { it.deletedAt == null } +
            slotsToRestore.count { it.deletedAt == null } +
            skipsToRestore.count { it.deletedAt == null } +
            deloadsToRestore.count { it.deletedAt == null } +
            substitutionsToRestore.count { it.deletedAt == null }

        val added = programBackup.insertPrograms(file.programs.map { it.toEntity() })
            .count { it != SKIPPED } +
            programBackup.insertProgramSlots(file.programSlots.map { it.toEntity() })
                .count { it != SKIPPED } +
            programBackup.insertProgramSkips(file.programSkips.map { it.toEntity() })
                .count { it != SKIPPED } +
            programBackup.insertProgramDeloads(file.programDeloads.map { it.toEntity() })
                .count { it != SKIPPED } +
            programBackup.insertProgramSubstitutions(file.programSubstitutions.map { it.toEntity() })
                .count { it != SKIPPED }

        return Imported(added = added, restored = restored)
    }

    /**
     * Restores the goal periods in [file] (ROADMAP N137).
     *
     * The rows merge like every other table — `IGNORE` by id — and the goals' old **missing-only** rule went
     * with the preferences string it existed for: a prefs write could not tell one row from another, while a
     * period is a row with an id, so a file's older periods add to the local history instead of being refused.
     *
     * A file written before periods existed holds the two maps instead, and they are read as **one undated
     * period per metric** — the same reading the one-time move gives the goals already on the device, and the
     * only honest one, because that file never recorded when a goal was set. The id is derived from the metric
     * so importing the same file twice inserts nothing.
     */
    private suspend fun restoreGoalPeriods(file: BackupFile) {
        if (file.goalPeriods.isNotEmpty()) {
            metricGoals.insert(file.goalPeriods.map { it.toEntity() })
            return
        }
        val now = timeSource.nowEpochMillis()
        val metrics = file.goals.keys + file.goalKinds.keys
        metricGoals.insert(
            metrics.map { metricId ->
                MetricGoalEntity(
                    id = "legacy:$metricId",
                    metricId = metricId,
                    kind = file.goalKinds[metricId],
                    value = file.goals[metricId],
                    startedAt = null,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )
    }

    private companion object {
        /** Room's rowid for a row an `OnConflictStrategy.IGNORE` insert skipped. */
        const val SKIPPED = -1L
    }
}

/** What importing the program tables did: rows that genuinely came back, and rows added (P3.3). */
private data class Imported(val added: Int, val restored: Int)
