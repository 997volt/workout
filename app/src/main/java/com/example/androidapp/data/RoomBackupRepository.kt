package com.example.androidapp.data

import android.content.Context
import androidx.room.withTransaction
import com.example.androidapp.data.local.MeasurementSoreMuscleEntity
import com.example.androidapp.data.local.MetricGoalEntity
import com.example.androidapp.data.local.SessionExerciseJointEntity
import com.example.androidapp.data.local.SessionSoreMuscleEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.seedMissingExercises
import com.example.androidapp.data.transfer.BackupCodec
import com.example.androidapp.data.transfer.BackupFile
import com.example.androidapp.data.transfer.BackupValidator
import com.example.androidapp.data.transfer.MeasurementDto
import com.example.androidapp.data.transfer.MeasurementSoreMuscleDto
import com.example.androidapp.data.transfer.SessionExerciseJointDto
import com.example.androidapp.data.transfer.SessionSoreMuscleDto
import com.example.androidapp.data.transfer.toDto
import com.example.androidapp.data.transfer.toEntity
import com.example.androidapp.data.transfer.withValidLibraryShape
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.localDayBounds
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.domain.repository.BackupRepository
import com.example.androidapp.domain.repository.ImportSummary
import com.example.androidapp.domain.repository.PreviewCounts
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.platform.CrashLogStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
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
    /** The automatic-backup switch, turned off before a clear so the folder copies are not overwritten (B343). */
    private val settings: SettingsRepository,
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
        // **The encoding runs off the caller's thread** (B158). The DAO reads suspend and Room moves them to
        // its own executors, but everything between them here does not: `crashLogStore.all()` lists and reads a
        // file per crash, and `encode` serialises the whole database in one go. Both used to run on whatever
        // thread asked — the composition's scope — so an export janked the UI in proportion to the history.
        // The same reasoning `clearAllUserData` already states further down this file.
        withContext(Dispatchers.IO) {
            // **One snapshot** (B346): the reads below used to be independent, so a set logged between the
            // session-exercise read and the set read was written without its parent and the file then failed
            // the foreign key on import. A read transaction makes the copy internally consistent.
            val file = database.withTransaction {
                // The goal that is open for each metric, read once: the file carries the periods *and* the two
                // maps a build that predates them reads, and both come from the same rows (N137).
                val openGoals = metricGoals.observeCurrent().first()
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
                    // The days a marked deload week prescribed (ROADMAP N147): the templates travel with
                    // the plan, so a restore keeps the week exactly as it was built rather than losing it.
                    programDeloadDays = programBackup.allProgramDeloadDays().map { it.toDto() },
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
                )
            }
            BackupCodec.encode(file)
        }
    }

    override suspend fun clearAllUserData(): DataResult<Unit> = dataResultOf {
        // Off the main thread: `clearAllTables` is a blocking call and Room asserts where
        // it runs. The DAO's own suspending methods dispatch themselves; this one does not.
        withContext(Dispatchers.IO) {
            // The scheduled writer goes off **before** the rows do (B343). Left on, the next due run writes an
            // almost-empty copy and the prune keeps only the newest five, so five periods later the copies
            // taken before the clear are gone — the opposite of what the dialog promises. A failed switch is a
            // storage failure, so the clear does not proceed on an unconfirmed setting.
            if (settings.setBackupEnabled(false) is DataResult.Failure) {
                throw IOException("the automatic backup switch could not be turned off")
            }
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

    override suspend fun preview(text: String): DataResult<BackupPreview> = dataResultOf {
        // The switch export already makes (B158): decoding a backup and inventorying this device are CPU and
        // memory work, and every caller is a UI scope, so they run off that thread (N257).
        withContext(Dispatchers.IO) {
            val file = BackupCodec.decode(text)
            // A set is drawn only inside its exercise inside its session (N261): one whose parents stay hidden
            // is a live row the merge writes and the history shows nowhere, so it is not one of the counts.
            val drawnSets = visibleSetIds(file)
            val problems = BackupValidator.problems(file)
            BackupPreview(
                sessions = previewCounts(
                    file.sessions.map { FileRow(it.id, it.deletedAt) },
                    dao.allSessions().associate { it.id to it.deletedAt },
                ),
                sets = previewCounts(
                    file.sets.filter { it.id in drawnSets }.map { FileRow(it.id, it.deletedAt) },
                    dao.allSets().associate { it.id to it.deletedAt },
                ),
                measurements = previewMeasurements(file.measurements),
                exercises = previewCounts(
                    file.exercises.map { FileRow(it.id, it.deletedAt) },
                    dao.allExercises().associate { it.id to it.deletedAt },
                ),
                problems = problems,
            )
        }
    }

    /**
     * One row the file carries, as the question about it asks: its id, and whether the file holds it deleted
     * (B330).
     */
    private data class FileRow(val id: String, val deletedAt: Long?)

    /**
     * Splits the file's live rows into the merge's three cases against the local rows (N248, B330).
     *
     * A row the file records as deleted is carried across so a later restore can bring it back, and it stays
     * hidden: on a device that lacks it the merge writes a hidden row, and on one that holds it nothing
     * changes. Neither is a row that becomes live, so the question does not count it — the counts are the rows
     * the merge makes live, which is the same rule [ImportSummary] reports after the write.
     *
     * The local map carries every row, hidden ones included: a live file row this device holds hidden is the
     * restore case, and an absent one is the insert case. That is the import's own split.
     */
    private fun previewCounts(rows: List<FileRow>, local: Map<String, Long?>): PreviewCounts {
        var live = 0
        var here = 0
        var restored = 0
        var added = 0
        rows.distinctBy { it.id }.forEach { row ->
            if (row.deletedAt != null) return@forEach
            live++
            when {
                !local.containsKey(row.id) -> added++
                local[row.id] == null -> here++
                else -> restored++
            }
        }
        return PreviewCounts(inFile = live, alreadyHere = here, restored = restored, added = added)
    }

    /**
     * The daily record's counts, by **day** rather than only by id (ROADMAP N113, B178, N248, B330).
     *
     * A measurement is one entry of each kind per day, and the merge yields to a day this device already has:
     * a file row whose day is taken changes nothing, so it counts as here. The rows are walked in the import's
     * own two steps — the restores, then the inserts — because each step reserves the days it takes, and one
     * order would disagree with the other about a file that names a day twice.
     *
     * A row the file records as deleted is carried across hidden and changes nothing the lifter can see, so it
     * is not one of the rows this counts (B330).
     */
    private suspend fun previewMeasurements(file: List<MeasurementDto>): PreviewCounts {
        val local = measurements.allForExport()
        val zone = ZoneId.systemDefault()
        val byId = local.associateBy { it.id }
        val taken = local.filter { it.deletedAt == null }
            .mapTo(mutableSetOf()) { row ->
                Instant.ofEpochMilli(row.measuredAt).localDayBounds(zone).first to row.kind
            }
        fun day(row: MeasurementDto) =
            Instant.ofEpochMilli(row.measuredAt).localDayBounds(zone).first to row.kind

        val live = file.filter { it.deletedAt == null }.distinctBy { it.id }
        var inFile = 0
        var here = 0
        var restored = 0
        var added = 0
        // The restore step: a hidden local row comes back only when no live entry already owns its day.
        live.forEach { row ->
            val sameId = byId[row.id] ?: return@forEach
            if (sameId.deletedAt == null) return@forEach
            inFile++
            if (taken.add(day(row))) restored++ else here++
        }
        // The insert step: an absent row is added only when its day is free after the restores.
        live.forEach { row ->
            val sameId = byId[row.id]
            if (sameId != null && sameId.deletedAt != null) return@forEach
            inFile++
            val free = taken.add(day(row))
            if (sameId != null || !free) here++ else added++
        }
        return PreviewCounts(inFile = inFile, alreadyHere = here, restored = restored, added = added)
    }

    /**
     * The sets the history will draw after the merge (ROADMAP N261).
     *
     * A set is only seen inside its session exercise inside its session: one whose exercise or session stays
     * hidden is written by the merge but drawn nowhere, so neither the question nor the summary may count it. A
     * parent is live afterwards when it is live in the file — inserted, or restored — or already live here,
     * which the merge leaves alone.
     */
    private suspend fun visibleSetIds(file: BackupFile): Set<String> {
        val localExercises = dao.allSessionExercises()
        val sessions = LiveAfterMerge(
            file = file.sessions.associate { it.id to it.deletedAt },
            local = dao.allSessions().associate { it.id to it.deletedAt },
        )
        val exercises = LiveAfterMerge(
            file = file.sessionExercises.associate { it.id to it.deletedAt },
            local = localExercises.associate { it.id to it.deletedAt },
        )
        val sessionOf = buildMap {
            file.sessionExercises.forEach { put(it.id, it.sessionId) }
            localExercises.forEach { if (it.id !in this) put(it.id, it.sessionId) }
        }
        return file.sets
            .filter { set ->
                val sessionId = sessionOf[set.sessionExerciseId]
                set.deletedAt == null &&
                    set.sessionExerciseId in exercises &&
                    sessionId != null &&
                    sessionId in sessions
            }
            .mapTo(mutableSetOf()) { it.id }
    }

    /**
     * Which rows of one kind are live after the merge, asked by a child that is only drawn through its parents
     * (N261).
     *
     * Live in the file means inserted or restored; live here means the merge leaves it alone; hidden in both
     * means it stays hidden. An id in neither is not a row the merge can produce — the insert would fail its
     * foreign key — and answers false.
     */
    private data class LiveAfterMerge(
        private val file: Map<String, Long?>,
        private val local: Map<String, Long?>,
    ) {
        operator fun contains(id: String): Boolean =
            (file.containsKey(id) && file[id] == null) || (local.containsKey(id) && local[id] == null)
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
        // The decode and the shape work are CPU and memory work, and every caller is a UI scope; the
        // export and preview already made this switch (B158, B346). The validation is here too, before the
        // transaction, so a bad file is refused without opening one.
        val (file, problems) = withContext(Dispatchers.IO) {
            val decoded = BackupCodec.decode(text).let {
                it.copy(exercises = it.exercises.withValidLibraryShape())
            }
            decoded to BackupValidator.problems(decoded)
        }
        if (problems.isNotEmpty()) {
            val first = problems.first()
            throw InvalidInputException(
                "That backup cannot be merged: ${first.where.lowercase()} — ${first.message}",
            )
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
        database.withTransaction {
            // The sets both sides will count (N261): read inside the transaction so the inventory and the
            // write see the same store (B346).
            val drawnSets = visibleSetIds(file)
            val inserted = insertMissing(file, drawnSets)
            val programs = importPrograms(file)
            // The goal periods merge **inside** the transaction like every other table (B144). They used to run
            // after it, as the last trace of the seam the preferences store needed: a goal is a row now, and a
            // failure part-way could leave it written while the import reported that nothing had changed.
            val goals = restoreGoalPeriods(file)
            ImportSummary(
                added = inserted + programs.added + goals,
                restored = restoreSoftDeleted(file, drawnSets) + programs.restored,
            )
        }
    }

    /**
     * Brings back the rows this device holds as soft-deleted, and counts what genuinely came
     * back: only a row the file itself has *live* did, because one the file also records as
     * deleted is still deleted. A set counts only when the exercise and session it is drawn
     * through will be live too (N261) — the row comes back either way, for a later restore of
     * the exercise to find.
     *
     * Extracted from [import] because that function had reached the length this project
     * enforces, and the seven tables here are one shape repeated — updates, so no foreign-key
     * ordering is involved: every row already exists and only its own columns change.
     */
    private suspend fun restoreSoftDeleted(file: BackupFile, drawnSets: Set<String>): Int {
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
            .withoutAnExistingDay()
        val measurementSoreRows = file.measurementSoreMuscles.filter { it.id in hiddenMeasurementSore }

        dao.restoreExercises(exercises.map { it.toEntity() })
        dao.restoreSessions(sessions.map { it.toEntity() })
        dao.restoreSessionExercises(sessionExercises.map { it.toEntity() })
        dao.restoreSets(sets.map { it.toEntity() })
        dao.restoreTemplates(templates.map { it.toEntity() })
        dao.restoreTemplateExercises(templateExercises.map { it.toEntity() })
        dao.restoreTemplateSets(templateSets.map { it.toEntity() })
        // The parent before the child: a sore row un-hidden onto a parent that stayed hidden is
        // exactly the state this ordering exists to prevent (N113) — and B149 closed the other half
        // of the same ordering, which is a child un-hidden onto a parent that stayed live.
        val liveSessions = sessions.mapTo(mutableSetOf()) { it.id }
        val liveSessionExercises = sessionExercises.mapTo(mutableSetOf()) { it.id }
        val liveMeasurements = measurementRows.mapTo(mutableSetOf()) { it.id }

        soreMuscles.restore(soreMuscleRows.restorableSoreRows(liveSessions))
        joints.restore(jointRows.restorableJointRows(liveSessionExercises))
        measurements.restore(measurementRows.map { it.toEntity() })
        measurementSore.restore(measurementSoreRows.restorableMeasurementSoreRows(liveMeasurements))

        return exercises.count { it.deletedAt == null } +
            sessions.count { it.deletedAt == null } +
            sessionExercises.count { it.deletedAt == null } +
            sets.count { it.deletedAt == null && it.id in drawnSets } +
            templates.count { it.deletedAt == null } +
            templateExercises.count { it.deletedAt == null } +
            templateSets.count { it.deletedAt == null } +
            soreMuscleRows.count { it.deletedAt == null } +
            jointRows.count { it.deletedAt == null } +
            measurementRows.count { it.deletedAt == null } +
            measurementSoreRows.count { it.deletedAt == null }
    }

    /**
     * The child rows an import may un-hide, which is only the ones whose parent comes back with them (B149).
     *
     * `softDeletedIds` cannot tell **why** a row is hidden, and there are two reasons a write hides one: it
     * deleted the parent, or it *replaced* the child list — a save rewrites a measurement's sore rows and an
     * exercise's joint rows wholesale. An import that restored by id alone therefore un-hid the rows a
     * replacement had put aside, and the parent came back holding both the old list and the new one: the same
     * muscle scored twice on one card, which is the state the editor's own rule forbids. Correlating the
     * timestamps, the way the undo does, is not enough here: an import acts on a file written before either
     * write, so it has no marker to correlate against. What it does know is whether the **parent** is coming
     * back, which is the question a delete and a replacement answer differently — a delete hides the parent
     * too, a replacement leaves it live.
     *
     * One function per child kind rather than one generic over the three DTOs: each parent column is read by
     * the same name, and a shared rule that needs a reified target type at the call site is more machinery
     * than three readable lines. The count the caller reports is left alone on purpose: a child whose parent
     * stayed live is not a row the import brought back, so counting it would overstate what happened.
     */
    private fun List<SessionSoreMuscleDto>.restorableSoreRows(
        liveSessions: Set<String>,
    ): List<SessionSoreMuscleEntity> = filter { it.sessionId in liveSessions }.map { it.toEntity() }

    private fun List<SessionExerciseJointDto>.restorableJointRows(
        liveSessionExercises: Set<String>,
    ): List<SessionExerciseJointEntity> =
        filter { it.sessionExerciseId in liveSessionExercises }.map { it.toEntity() }

    private fun List<MeasurementSoreMuscleDto>.restorableMeasurementSoreRows(
        liveMeasurements: Set<String>,
    ): List<MeasurementSoreMuscleEntity> =
        filter { it.measurementId in liveMeasurements }.map { it.toEntity() }

    /**
     * The measurements in this list whose local day is not already recorded here, one per day (B178).
     *
     * The app keeps **one entry of each kind per day** (N113), and `save` enforces that with `findOnDay` —
     * but an import inserts by id, and a Waistline row's id is a hash of its day while a hand-entered one
     * is a UUID. Importing body data over days the lifter had recorded by hand therefore left two live
     * daily entries on one day, which every read lists and every chart plots twice. **The day the device
     * already has wins**: the import adds and never overwrites (P1.12), and a file is a second opinion
     * about a day this device has already answered.
     *
     * One read of the live rows rather than a query per row. The set is filled as the list is walked, so a
     * file carrying the same day twice also yields one row — and the day a row was just inserted under is
     * reserved for the restore step that runs after this one.
     */
    private suspend fun List<MeasurementDto>.withoutAnExistingDay(): List<MeasurementDto> {
        val zone = ZoneId.systemDefault()
        val taken = measurements.allForExport()
            .filter { it.deletedAt == null }
            .mapTo(mutableSetOf()) {
                Instant.ofEpochMilli(it.measuredAt).localDayBounds(zone).first to it.kind
            }
        return filter { row ->
            // A soft-deleted row cannot be the second *live* entry on a day, so it never reserves one
            // (B269). It used to: the hidden row came first in rowid order, took the day, and filtered out
            // the live row beside it — so restoring a save/delete/re-enter history left the day empty.
            if (row.deletedAt != null) return@filter true
            val day = Instant.ofEpochMilli(row.measuredAt).localDayBounds(zone).first to row.kind
            // `add` answers "was this day new": a false is the row that would have been the second entry.
            taken.add(day)
        }
    }

    /**
     * Inserts what the file has and this device does not, counting only the rows that became live and can be
     * drawn (B330, N261).
     *
     * `IGNORE` skips live rows and the ones just restored, and Room returns -1 for a skipped row; a row the
     * file records as deleted goes in hidden and did not become live, so it is not counted. A set also needs the
     * exercise and session it is drawn through to be live — [drawnSets] is that answer, computed once by
     * [import] for both sides.
     *
     * Parents before children, so the foreign keys hold as rows go in.
     */
    private suspend fun insertMissing(file: BackupFile, drawnSets: Set<String>): Int {
        val attemptedMeasurements = file.measurements.withoutAnExistingDay()
        val measurementParents = attemptedMeasurements.mapTo(mutableSetOf()) { it.id }
        measurementParents += measurements.allForExport()
            .filter { it.deletedAt == null }
            .map { it.id }
        val measurementSoreRows = file.measurementSoreMuscles.filter { it.measurementId in measurementParents }
        return dao.insertExercises(file.exercises.map { it.toEntity() })
            .countLive(file.exercises) { it.deletedAt == null } +
            dao.insertSessions(file.sessions.map { it.toEntity() })
                .countLive(file.sessions) { it.deletedAt == null } +
            dao.insertSessionExercises(file.sessionExercises.map { it.toEntity() })
                .countLive(file.sessionExercises) { it.deletedAt == null } +
            // The joint rows go in after the exercises they hang off, which the line above wrote (N63).
            joints.insert(file.sessionExerciseJoints.map { it.toEntity() })
                .countLive(file.sessionExerciseJoints) { it.deletedAt == null } +
            // The sore-muscle rows go in after their sessions, which the lines above already wrote (N62).
            soreMuscles.insert(file.sessionSoreMuscles.map { it.toEntity() })
                .countLive(file.sessionSoreMuscles) { it.deletedAt == null } +
            dao.insertSets(file.sets.map { it.toEntity() })
                .countLive(file.sets) { it.deletedAt == null && it.id in drawnSets } +
            dao.insertTemplates(file.templates.map { it.toEntity() })
                .countLive(file.templates) { it.deletedAt == null } +
            dao.insertTemplateExercises(file.templateExercises.map { it.toEntity() })
                .countLive(file.templateExercises) { it.deletedAt == null } +
            dao.insertTemplateSets(file.templateSets.map { it.toEntity() })
                .countLive(file.templateSets) { it.deletedAt == null } +
            // One entry of each kind per day, so a file's row yields to the day this device already has (B178).
            measurements.insertAll(attemptedMeasurements.map { it.toEntity() })
                .countLive(attemptedMeasurements) { it.deletedAt == null } +
            // The sore rows hang off a daily measurement, so they go in after the entries above, and only
            // for a measurement that is actually here afterwards (B269): inserting one whose entry yielded
            // to the device's own day violates the foreign key, and the whole import rolls back with it.
            measurementSore.insert(measurementSoreRows.map { it.toEntity() })
                .countLive(measurementSoreRows) { it.deletedAt == null }
    }

    /**
     * Restores and adds the program tables (ROADMAP P3.3), returning what each step did.
     *
     * The same hidden-then-insert shape as everything else in [import], extracted because the
     * enclosing function had reached the length this project enforces — and because the program
     * tables are a self-contained group that has to move together.
     */
    private suspend fun importPrograms(file: BackupFile): Imported {
        val hiddenPrograms = programBackup.softDeletedProgramIds().toSet()
        val hiddenSlots = programBackup.softDeletedProgramSlotIds().toSet()
        val hiddenSkips = programBackup.softDeletedProgramSkipIds().toSet()
        val hiddenDeloads = programBackup.softDeletedProgramDeloadIds().toSet()
        val hiddenDeloadDays = programBackup.softDeletedProgramDeloadDayIds().toSet()
        val hiddenSubstitutions = programBackup.softDeletedProgramSubstitutionIds().toSet()

        val programsToRestore = file.programs.filter { it.id in hiddenPrograms }
        val slotsToRestore = file.programSlots.filter { it.id in hiddenSlots }
        val skipsToRestore = file.programSkips.filter { it.id in hiddenSkips }
        val deloadsToRestore = file.programDeloads.filter { it.id in hiddenDeloads }
        val deloadDaysToRestore = file.programDeloadDays.filter { it.id in hiddenDeloadDays }
        val substitutionsToRestore = file.programSubstitutions.filter { it.id in hiddenSubstitutions }

        programBackup.restorePrograms(programsToRestore.map { it.toEntity() })
        programBackup.restoreProgramSlots(slotsToRestore.map { it.toEntity() })
        programBackup.restoreProgramSkips(skipsToRestore.map { it.toEntity() })
        programBackup.restoreProgramDeloads(deloadsToRestore.map { it.toEntity() })
        programBackup.restoreProgramDeloadDays(deloadDaysToRestore.map { it.toEntity() })
        programBackup.restoreProgramSubstitutions(substitutionsToRestore.map { it.toEntity() })

        val restored = programsToRestore.count { it.deletedAt == null } +
            slotsToRestore.count { it.deletedAt == null } +
            skipsToRestore.count { it.deletedAt == null } +
            deloadsToRestore.count { it.deletedAt == null } +
            deloadDaysToRestore.count { it.deletedAt == null } +
            substitutionsToRestore.count { it.deletedAt == null }

        val added = programBackup.insertPrograms(file.programs.map { it.toEntity() })
            .countLive(file.programs) { it.deletedAt == null } +
            programBackup.insertProgramSlots(file.programSlots.map { it.toEntity() })
                .countLive(file.programSlots) { it.deletedAt == null } +
            programBackup.insertProgramSkips(file.programSkips.map { it.toEntity() })
                .countLive(file.programSkips) { it.deletedAt == null } +
            programBackup.insertProgramDeloads(file.programDeloads.map { it.toEntity() })
                .countLive(file.programDeloads) { it.deletedAt == null } +
            programBackup.insertProgramDeloadDays(file.programDeloadDays.map { it.toEntity() })
                .countLive(file.programDeloadDays) { it.deletedAt == null } +
            programBackup.insertProgramSubstitutions(file.programSubstitutions.map { it.toEntity() })
                .countLive(file.programSubstitutions) { it.deletedAt == null }

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
     *
     * Returns the rows that were genuinely added (B144), because they contribute to [ImportSummary]: a restore
     * whose only new content was a goal used to report that everything had already been here.
     */
    private suspend fun restoreGoalPeriods(file: BackupFile): Int {
        if (file.goalPeriods.isNotEmpty()) {
            return metricGoals.insert(file.goalPeriods.map { it.toEntity() }).count { it != SKIPPED }
        }
        val now = timeSource.nowEpochMillis()
        val metrics = file.goals.keys + file.goalKinds.keys
        return metricGoals.insert(
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
        ).count { it != SKIPPED }
    }

    /**
     * How many of these inserts Room actually wrote became live rows (B330).
     *
     * `@Insert` answers one rowid per input row — [SKIPPED] for one it left alone — so the result lines up with
     * the file's rows. A row the file records as deleted is written hidden: it is carried so a later restore can
     * bring it back, which is fidelity rather than a row that became live, so [ImportSummary] does not count it.
     */
    private fun <T> List<Long>.countLive(rows: List<T>, isLive: (T) -> Boolean): Int =
        indices.count { this[it] != SKIPPED && isLive(rows[it]) }

    private companion object {
        /** Room's rowid for a row an `OnConflictStrategy.IGNORE` insert skipped. */
        const val SKIPPED = -1L
    }
}

/** What importing the program tables did: rows that genuinely came back, and rows added (P3.3). */
private data class Imported(val added: Int, val restored: Int)
