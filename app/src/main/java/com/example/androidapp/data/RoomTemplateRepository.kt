package com.example.androidapp.data

import com.example.androidapp.data.local.SetEntryEntity
import com.example.androidapp.data.local.SessionExerciseEntity
import com.example.androidapp.data.local.TemplateDao
import androidx.room.withTransaction
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.toDomain
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.Load
import com.example.androidapp.domain.model.RungRun
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.runAt
import com.example.androidapp.domain.model.rungLoad
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Room-backed [TemplateRepository] (ROADMAP N3).
 *
 * Read and write access to templates is entirely here; the only other place that
 * touches the tables is starting a workout from one, which lives in
 * `RoomWorkoutRepository` and reads the ordered exercise ids.
 */
@Singleton
class RoomTemplateRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : TemplateRepository {

    private val dao = database.templateDao()

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        dao.observeTemplates().map { rows -> rows.map { it.toDomain() } }

    override fun observeTemplate(templateId: String): Flow<WorkoutTemplate?> =
        dao.observeTemplate(templateId).map { it?.toDomain() }

    override fun observeExercises(templateId: String): Flow<List<TemplateExercise>> =
        combine(
            dao.observeTemplateExercises(templateId),
            dao.observeTemplateSets(templateId),
        ) { exercises, sets ->
            val byExercise = sets.groupBy { it.templateExerciseId }
            exercises.map { row ->
                row.toDomain(byExercise[row.id].orEmpty().map { it.toDomain() })
            }
        }

    override fun observeSets(templateId: String): Flow<List<TemplateSet>> =
        dao.observeTemplateSets(templateId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun createTemplate(name: String): DataResult<String> = dataResultOf {
        val trimmed = requireName(name)
        val id = UUID.randomUUID().toString()
        val now = timeSource.nowEpochMillis()
        dao.insertTemplate(
            TemplateEntity(
                id = id,
                name = trimmed,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        id
    }

    override suspend fun createTemplateFromSession(
        sessionId: String,
        name: String,
    ): DataResult<String> = dataResultOf {
        val trimmed = requireName(name)
        database.withTransaction {
            val exercises = database.sessionExerciseDao().findLiveSessionExercises(sessionId)
            if (exercises.isEmpty()) {
                throw InvalidInputException("That workout has no exercises to copy")
            }
            val sets = database.sessionExerciseDao()
                .findLiveSetsForSession(sessionId)
                .groupBy { it.sessionExerciseId }

            val now = timeSource.nowEpochMillis()
            val templateId = UUID.randomUUID().toString()
            dao.insertTemplate(
                TemplateEntity(
                    id = templateId,
                    name = trimmed,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                ),
            )
            val plan = PlanBeingBuilt(dao = dao, templateId = templateId, now = now)
            exercises.forEachIndexed { index, exercise ->
                copyExerciseInto(
                    plan = plan,
                    exercise = exercise,
                    position = index,
                    sets = sets[exercise.id].orEmpty(),
                )
            }
            templateId
        }
    }

    override suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit> =
        dataResultOf {
            if (dao.rename(id = templateId, name = requireName(name), at = timeSource.nowEpochMillis()) == 0) {
                throw NotFoundException("template $templateId")
            }
        }

    override suspend fun deleteTemplate(templateId: String): DataResult<Unit> = dataResultOf {
        // A soft delete, so the rows stay for the export to carry (P1.12).
        if (dao.softDeleteTemplate(id = templateId, at = timeSource.nowEpochMillis()) == 0) {
            throw NotFoundException("template $templateId")
        }
    }

    override suspend fun setSupersetGroup(
        templateExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit> = dataResultOf {
        if (templateExerciseIds.isEmpty()) return@dataResultOf

        val updated = dao.setSupersetGroup(
            templateExerciseIds,
            group,
            timeSource.nowEpochMillis(),
        )
        if (updated != templateExerciseIds.size) {
            throw NotFoundException("$updated of ${templateExerciseIds.size} planned exercises were written")
        }
    }

    override suspend fun addExercise(templateId: String, exerciseId: String): DataResult<Unit> =
        dataResultOf {
            if (dao.findById(templateId) == null) {
                throw NotFoundException("template $templateId")
            }
            val now = timeSource.nowEpochMillis()
            dao.insertTemplateExercise(
                TemplateExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    templateId = templateId,
                    exerciseId = exerciseId,
                    // Appended, so the template is edited in the order it is built.
                    position = dao.maxPosition(templateId) + 1,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                ),
            )
        }

    override suspend fun prependSets(
        templateExerciseId: String,
        edits: List<TemplateSetEdit>,
    ): DataResult<Unit> = dataResultOf {
        if (edits.isEmpty()) return@dataResultOf
        edits.forEach(::validate)
        if (dao.findTemplateExercise(templateExerciseId) == null) {
            throw NotFoundException("template exercise $templateExerciseId")
        }
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            // One statement moves the existing sets down; the ramp then takes indices 0..n-1 in the
            // order it was computed, so the plan reads warm-ups first and work after (ROADMAP B34).
            dao.shiftSetIndexes(templateExerciseId, by = edits.size, at = now)
            edits.forEachIndexed { index, edit ->
                dao.insertTemplateSet(
                    TemplateSetEntity(
                        id = UUID.randomUUID().toString(),
                        templateExerciseId = templateExerciseId,
                        setIndex = index,
                        role = edit.role,
                        targetWeightGrams = edit.targetWeightGrams,
                        targetAssistanceGrams = edit.targetAssistanceGrams,
                        targetRepsMin = edit.targetRepsMin,
                        targetRepsMax = edit.targetRepsMax,
                        targetRepsCurrent = edit.targetRepsCurrent,
                        targetRpeHalves = edit.effortOrNull,
                        note = edit.note,
                        createdAt = now,
                        updatedAt = now,
                        deletedAt = null,
                    ),
                )
            }
        }
    }

    override suspend fun addSet(
        templateExerciseId: String,
        edit: TemplateSetEdit,
    ): DataResult<Unit> = dataResultOf {
        validate(edit)
        val exercise = dao.findTemplateExercise(templateExerciseId)
            ?: throw NotFoundException("template exercise $templateExerciseId")
        val now = timeSource.nowEpochMillis()
        val row = TemplateSetEntity(
            id = UUID.randomUUID().toString(),
            templateExerciseId = templateExerciseId,
            // Appended, so the plan reads in the order it was written.
            setIndex = dao.maxSetIndex(templateExerciseId) + 1,
            role = edit.role,
            targetWeightGrams = edit.targetWeightGrams,
            targetAssistanceGrams = edit.targetAssistanceGrams,
            targetRepsMin = edit.targetRepsMin,
            targetRepsMax = edit.targetRepsMax,
            targetRepsCurrent = edit.targetRepsCurrent,
            targetRpeHalves = edit.effortOrNull,
            dropValueGrams = edit.dropValueGrams,
            note = edit.note?.trim()?.ifEmpty { null },
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
        // The rung rules are about the *run* this row joins, so they are checked with the exercise's
        // own sets in hand rather than on the row alone (ROADMAP N79): the stored list is what says
        // which problems this write *creates* rather than which ones it inherits (B59).
        val sets = plannedSetsOf(exercise)
        validateRun(stored = sets, after = sets + row.toDomain())
        dao.insertTemplateSet(row)
    }

    override suspend fun updateSet(templateSetId: String, edit: TemplateSetEdit): DataResult<Unit> =
        dataResultOf {
            validate(edit)
            val stored = dao.findTemplateSet(templateSetId)
                ?: throw NotFoundException("template set $templateSetId")
            val edited = stored.copy(
                role = edit.role,
                targetWeightGrams = edit.targetWeightGrams,
                targetAssistanceGrams = edit.targetAssistanceGrams,
                targetRepsMin = edit.targetRepsMin,
                targetRepsMax = edit.targetRepsMax,
                targetRepsCurrent = edit.targetRepsCurrent,
                targetRpeHalves = edit.effortOrNull,
                dropValueGrams = edit.dropValueGrams,
                note = edit.note?.trim()?.ifEmpty { null },
                updatedAt = timeSource.nowEpochMillis(),
            )
            val exercise = dao.findTemplateExercise(stored.templateExerciseId)
                ?: throw NotFoundException("template exercise ${stored.templateExerciseId}")
            val sets = plannedSetsOf(exercise)
            validateRun(
                stored = sets,
                after = sets.map { if (it.id == templateSetId) edited.toDomain() else it },
            )
            val updated = dao.updateTemplateSet(edited)
            if (updated == 0) throw NotFoundException("template set $templateSetId")
        }

    /**
     * Removes a planned set, and the run that hangs off it (ROADMAP N79, B60, B72).
     *
     * A rung carries no targets of its own — its load is derived from the set above it — so a run whose
     * anchor is gone has nothing left to read and nothing the editor may write. The whole run goes with
     * the anchor, whichever of its rows the delete was asked for: [runAt] names every rung whose anchor
     * this row is, which is the run's every rung, because adjacency is the only parent link there is.
     *
     * The survivors are then **renumbered to the positions they now hold**. A plan is read by position —
     * a live session matches its logged sets against `setIndex` (`nextIndex = loggedSets.size`) and a
     * run's continuation is keyed by it — so a gap left by a removal points both at the wrong row: a
     * deleted middle set makes the next logged set line up with the one after it (B72). Renumbering
     * keeps one index space rather than two, and it is the same invariant `prependSets` holds from the
     * other direction.
     *
     * Rejected alternative: promoting a stranded first rung to a set of its own. A rung deliberately
     * names no reps and no weight, so promoting it invents both, and the invented numbers would be
     * indistinguishable from ones the lifter authored. Deleting is the rule the shape was planned with.
     */
    override suspend fun removeSet(templateSetId: String): DataResult<Unit> = dataResultOf {
        val stored = dao.findTemplateSet(templateSetId)
            ?: throw NotFoundException("template set $templateSetId")
        val exercise = dao.findTemplateExercise(stored.templateExerciseId)
            ?: throw NotFoundException("template exercise ${stored.templateExerciseId}")
        val sets = plannedSetsOf(exercise)
        val index = sets.indexOfFirst { it.id == templateSetId }
        val going = if (index < 0) {
            listOf(templateSetId)
        } else {
            listOf(templateSetId) +
                sets.indices.filter { sets.runAt(it)?.anchorIndex == index }.map { sets[it].id }
        }
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            going.forEach { dao.softDeleteTemplateSet(id = it, at = now) }
            dao.findTemplateSets(exercise.templateId)
                .filter { it.templateExerciseId == exercise.id }
                .sortedBy { it.setIndex }
                .forEachIndexed { position, row ->
                    if (row.setIndex != position) {
                        dao.updateTemplateSet(row.copy(setIndex = position, updatedAt = now))
                    }
                }
        }
    }

    override suspend fun setExercisePlan(
        templateExerciseId: String,
        restSeconds: Int?,
        techniqueNote: String?,
        targetRpeHalves: Int?,
    ): DataResult<Unit> = dataResultOf {
        // Zero is a value — "this exercise has no rest" — and leaving it unset is how "use the
        // library's" is expressed (the same rule N5 applies to the library itself, amended by N45).
        if (restSeconds != null && restSeconds < RestTimer.MIN_PRESCRIBED_SECONDS) {
            throw InvalidInputException(RestTimer.NEGATIVE_REST_REFUSAL)
        }
        if (!Rpe.isValid(targetRpeHalves)) {
            throw InvalidInputException("Target RPE must be between 1 and 10, in half steps.")
        }
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            // The previous value decides whether a clear also has to reach the legacy per-set column,
            // so it is read before the write that replaces it (N59).
            val previous = dao.findTemplateExercise(templateExerciseId)
                ?: throw NotFoundException("template exercise $templateExerciseId")
            val updated = dao.setExercisePlan(
                id = templateExerciseId,
                restSeconds = restSeconds,
                techniqueNote = techniqueNote?.trim()?.ifEmpty { null },
                targetRpeHalves = targetRpeHalves,
                at = now,
            )
            if (updated == 0) throw NotFoundException("template exercise $templateExerciseId")
            // A clear has to reach the reader: the per-set column is the fallback the exercise's
            // number wins over, so leaving it behind would resurrect the effort the lifter just
            // removed. Only a transition to null clears, so a plan whose effort only ever lived on
            // its sets — an imported pre-change backup — keeps it.
            if (targetRpeHalves == null && previous.targetRpeHalves != null) {
                dao.clearSetTargetRpe(templateExerciseId, at = now)
            }
        }
    }

    override suspend fun removeExercise(templateExerciseId: String): DataResult<Unit> =
        dataResultOf {
            val updated = dao.softDeleteTemplateExercise(
                id = templateExerciseId,
                at = timeSource.nowEpochMillis(),
            )
            if (updated == 0) throw NotFoundException("template exercise $templateExerciseId")
        }

    override suspend fun moveExercise(templateExerciseId: String, delta: Int): DataResult<Unit> =
        dataResultOf {
            val row = dao.findTemplateExercise(templateExerciseId)
                ?: throw NotFoundException("template exercise $templateExerciseId")
            val ordered = dao.observeTemplateExercises(row.templateId).first()
            val index = ordered.indexOfFirst { it.id == templateExerciseId }
            val neighbour = ordered.getOrNull(index + delta)
            // At the top or the bottom: nothing to do, and not an error.
            if (index >= 0 && neighbour != null) {
                dao.swapPositions(
                    firstId = row.id,
                    firstPosition = neighbour.position,
                    secondId = neighbour.id,
                    secondPosition = row.position,
                    at = timeSource.nowEpochMillis(),
                )
            }
        }

    /**
     * A plan's targets have to be usable numbers.
     *
     * Nothing here checks the plan against what the user actually lifts — that is
     * deliberate (N14): a plan describes a shape, and a logged set is expected to
     * differ. What it does check is that the shape is not nonsense: a negative
     * weight, a zero-rep set, or a range that runs backwards.
     */
    private fun validate(edit: TemplateSetEdit) {
        validateLoad(edit)
        validateEffort(edit)
    }

    /** This exercise's planned sets in stored order, for the rules that are about the run (N79). */
    private suspend fun plannedSetsOf(exercise: TemplateExerciseEntity): List<TemplateSet> =
        dao.findTemplateSets(exercise.templateId)
            .filter { it.templateExerciseId == exercise.id }
            .map { it.toDomain() }
            .sortedBy { it.setIndex }

    /**
     * Refuses only the rung problems the write being made **creates** (ROADMAP N79, B59).
     *
     * The rules are about the **run** rather than the row, so they are checked with the exercise's own
     * sets in hand: that is what catches a value that is fine on its own and impossible once it is one
     * rung further down the ladder, and an anchor edited light enough to strand a rung below it.
     *
     * What it does not do is re-judge rows the write never touched. A plan written before these rules
     * existed — a drop run whose value column arrived null, a rung whose anchor is already gone — stays
     * editable, because the alternative is a plan that can be neither read nor corrected. The
     * comparison is by set id and by the problem's own sentence, so a write that leaves an existing
     * problem exactly as it was passes, and one that makes it worse, moves it to another row, or puts a
     * new row into it does not.
     *
     * Rejected alternative: backfilling a value for the runs already on disk in migration 34→35. There
     * is no number the migration could write that the lifter meant — every candidate invents a load and
     * silently moves the whole ladder — so the migration's null stays "this run names no value", which
     * the read path already answers by falling back to what the set itself carries.
     */
    private fun validateRun(stored: List<TemplateSet>, after: List<TemplateSet>) {
        val alreadyWrong = stored.indices.associate { index ->
            stored[index].id to rungProblem(stored, index)
        }
        after.indices.forEach { index ->
            val problem = rungProblem(after, index) ?: return@forEach
            if (alreadyWrong[after[index].id] != problem) throw InvalidInputException(problem)
        }
    }

    /**
     * What is wrong with the rung at [index], or null (ROADMAP N79).
     *
     * The value's own rules come first, because they hold whatever the row is; the rest are about the
     * run it joins.
     */
    private fun rungProblem(sets: List<TemplateSet>, index: Int): String? {
        val set = sets[index]
        val valueProblem = dropValueProblem(set)
        val run = if (set.role.isRung) sets.runAt(index) else null
        return when {
            valueProblem != null -> valueProblem
            !set.role.isRung -> null

            run == null ->
                "A ${set.role.label.lowercase()} set hangs off the set above it, and there is none."

            set.role == SetType.CLUSTER -> null

            // The value belongs to the *run*, and the run writes it once: a later rung inherits it, so
            // carrying one of its own would be a number nothing reads (ROADMAP N79).
            run.rung == 1 && set.dropValueGrams == null ->
                "A drop run needs the value it takes off the set above it."

            run.rung > 1 && set.dropValueGrams != null ->
                "A run's drop value is written on its first rung."

            !hasSomethingToTakeOff(sets, run) ->
                "That drop leaves the set above it nothing to take off."

            else -> null
        }
    }

    /** A value belongs to a drop and is a positive number (ROADMAP N79). */
    private fun dropValueProblem(set: TemplateSet): String? = when {
        set.dropValueGrams == null -> null
        set.role != SetType.DROP -> "Only a drop set takes a value off the set above it."
        set.dropValueGrams <= 0L -> "A drop value must be more than zero."
        else -> null
    }

    /**
     * Whether the run leaves a rung with a load (ROADMAP N79).
     *
     * False for an anchor with no added weight to take the value off, and false once the ladder has run
     * down to nothing — a negative weight being assistance in this app rather than a small weight (N15).
     */
    private fun hasSomethingToTakeOff(sets: List<TemplateSet>, run: RungRun): Boolean {
        val anchor = sets[run.anchorIndex]
        val load = Load(anchor.targetWeightGrams ?: 0L, anchor.targetAssistanceGrams ?: 0L)
        return rungLoad(load, SetType.DROP, run) != null
    }

    /** A load is a weight or a magnitude of assistance, never a negative either way. */
    private fun validateLoad(edit: TemplateSetEdit) {
        val problem = when {
            edit.targetWeightGrams != null && edit.targetWeightGrams < 0 ->
                "A target weight cannot be negative."

            edit.targetAssistanceGrams != null && edit.targetAssistanceGrams < 0 ->
                "Assistance is a magnitude, not a negative weight."

            else -> null
        }
        if (problem != null) throw InvalidInputException(problem)
    }

    /** Reps are a range that can be absent at either end, and RPE sits on the scale. */
    private fun validateEffort(edit: TemplateSetEdit) {
        val problem = when {
            edit.targetRepsMin != null && edit.targetRepsMin < 1 -> "Target reps must be at least 1."
            edit.targetRepsMax != null && edit.targetRepsMax < 1 -> "Target reps must be at least 1."
            edit.targetRepsCurrent != null && edit.targetRepsCurrent < 1 ->
                "The reps the session asks for must be at least 1."
            edit.targetRepsMin != null && edit.targetRepsMax != null &&
                edit.targetRepsMin > edit.targetRepsMax ->
                "The low end of a rep range cannot exceed the high end."

            !Rpe.isValid(edit.targetRpeHalves) ->
                "Target RPE must be between 1 and 10, in half steps."

            else -> null
        }
        if (problem != null) throw InvalidInputException(problem)
    }

    /** A template with no name is a list row nobody can tell apart from the next. */
    private fun requireName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidInputException("Give the template a name.")
        return trimmed
    }
}

/**
 * The plan's per-set effort, or null for a role that records none (ROADMAP N67).
 *
 * A planned warm-up carries no target effort, so every write path takes the number through here
 * rather than each remembering the rule — the shape the logged set's own write boundary uses. A
 * value written before the rule existed (or restored from a file written then) is dropped the next
 * time the set is written, which is what migration 28→29 did for the rows already on disk.
 */
private val TemplateSetEdit.effortOrNull: Int?
    get() = targetRpeHalves.takeIf { role.recordsEffort }

/**
 * Copies one performed exercise and its sets into a plan being built (ROADMAP N31).
 *
 * File-level rather than a private member, for the same reason the workout repository's append helper
 * is: the class is at its function ceiling, and this keeps the copy a readable sequence rather than one
 * long method.
 */
/** The plan a copy is being written into: what every exercise shares. */
private class PlanBeingBuilt(
    val dao: TemplateDao,
    val templateId: String,
    val now: Long,
)

private suspend fun copyExerciseInto(
    plan: PlanBeingBuilt,
    exercise: SessionExerciseEntity,
    position: Int,
    sets: List<SetEntryEntity>,
) {
    val plannedId = UUID.randomUUID().toString()
    plan.dao.insertTemplateExercise(
        TemplateExerciseEntity(
            id = plannedId,
            templateId = plan.templateId,
            exerciseId = exercise.exerciseId,
            position = position,
            // The rest and the note are part of how it was performed, so the plan carries them; the
            // grouping is what makes a copied superset arrive together (N24).
            restSeconds = exercise.restSeconds,
            techniqueNote = exercise.techniqueNote,
            // One target RPE per exercise now, and the session's last set that named one is what the
            // copy builds to (N59) — the same consolidation migration 27→28 made for plans already
            // stored. The sets keep their own values below, as a plan imported from before does. A
            // warm-up's effort is not a target for the exercise's working sets (N67), so it is not
            // the one this reads.
            targetRpeHalves = sets.lastOrNull { it.rpeHalves != null && it.setType.recordsEffort }
                ?.rpeHalves,
            supersetGroup = exercise.supersetGroup,
            createdAt = plan.now,
            updatedAt = plan.now,
            deletedAt = null,
        ),
    )
    sets.forEachIndexed { setIndex, set ->
        plan.dao.insertTemplateSet(
            TemplateSetEntity(
                id = UUID.randomUUID().toString(),
                templateExerciseId = plannedId,
                setIndex = setIndex,
                role = set.setType,
                targetWeightGrams = set.weightGrams,
                // A set stores assistance as a magnitude and so does a plan; zero means "none", which
                // is what a plan should say rather than zero.
                targetAssistanceGrams = set.assistanceGrams.takeIf { it > 0L },
                targetRepsMin = set.reps,
                targetRepsMax = set.reps,
                // A copied set is one number, so the range's floor and its current target are that
                // same number — the shape a plan written by hand for "3 sets of 5" has (N74).
                targetRepsCurrent = set.reps,
                // A copied warm-up carries no effort, the rule the set itself already holds (N67).
                targetRpeHalves = set.rpeHalves.takeIf { set.setType.recordsEffort },
                note = set.note,
                createdAt = plan.now,
                updatedAt = plan.now,
                deletedAt = null,
            ),
        )
    }
}
