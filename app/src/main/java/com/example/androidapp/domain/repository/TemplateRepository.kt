package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

/**
 * Reading and writing workout templates (ROADMAP N3).
 *
 * Writes return [DataResult] for the same reason every other repository's do (F7):
 * a template that silently failed to save is a plan the user thinks they have.
 */
interface TemplateRepository {

    /**
     * Every live template, name-ordered, with its exercise count.
     *
     * [includeHidden] is the one place N214's rule is stated: a surface that **offers** a workout — the
     * picker a program's day chooses from — takes the default and never sees a hidden one, while the
     * two that legitimately show them ask for them: the templates list (its filter) and home's
     * substitute picker (a stand-in is what you train today, not what a program is composed from).
     * Nothing that resolves a workout **by id** goes through here, which is why a program slot naming a
     * hidden template still schedules and starts it.
     */
    fun observeTemplates(includeHidden: Boolean = false): Flow<List<WorkoutTemplate>>

    /**
     * The workouts this device deleted, name-ordered, with their planned exercise counts (ROADMAP N216).
     *
     * Its own read rather than `includeDeleted` beside `includeHidden` in [observeTemplates]: the list
     * that can bring a deleted one back is the one surface that wants them, and a caller that asked for
     * the deleted ones would be asking for workouts no read by id resolves — they can be neither
     * edited, started, nor offered.
     */
    fun observeDeletedTemplates(): Flow<List<WorkoutTemplate>>

    /** One template, re-emitting when it is renamed. Null when it is gone. */
    fun observeTemplate(templateId: String): Flow<WorkoutTemplate?>

    /**
     * Every live kept version of [templateId], newest first (ROADMAP N134).
     *
     * The rows the templates list hides, read back where they belong: a version is attached to the
     * template it was kept from rather than loose in the list.
     */
    fun observeVersions(templateId: String): Flow<List<WorkoutTemplate>>

    /**
     * Keeps [templateId]'s state **as it stands**, as a version attached to it (ROADMAP N134).
     *
     * The lifter presses this at the moment they decide to change something, and what is kept is the
     * live plan — name, ordered exercises, sets, targets, grouping — copied into a template row of its
     * own flagged `versionOf`. Nothing about the live template moves: keeping a version renames nothing
     * and repoints nothing, which is why history, which reads a template's name live, is unaffected.
     *
     * **Never automatic.** A rename or a note tweak would otherwise become a "version", writing rows
     * nobody asked for. **Never editable and never restorable**: the version opens as a read-only plan,
     * because the request is future reference, and a restore would turn a record into an editing path
     * whose overwritten state then needed keeping in turn. The copy is the same shape
     * [createTemplateFromSession] writes, read from the plan tables instead of a session's.
     */
    suspend fun keepVersion(templateId: String): DataResult<Unit>

    /**
     * One template's name, **including a soft-deleted one**, or null when no such row exists (N58, B74).
     *
     * The name alone, and unfiltered, because that is what a past workout needs: [observeTemplate] hides a
     * deleted plan — the editor cannot open one — while history must go on saying which plan the workout was,
     * and a soft delete keeps the row for the export (P1.12). Reading the whole template here was rejected:
     * it would put the deleted filter in the reader's hands and invite the editor's rules into history.
     */
    fun observeTemplateName(templateId: String): Flow<String?>

    /**
     * The template's exercises in their stored order, each carrying its planned sets
     * (ROADMAP N14).
     */
    fun observeExercises(templateId: String): Flow<List<TemplateExercise>>

    /**
     * The template's planned sets on their own, ordered by exercise position then set
     * index — what a workout started from this plan prefills its targets from.
     */
    fun observeSets(templateId: String): Flow<List<TemplateSet>>

    /**
     * Stores a new template named [name] and returns its id.
     *
     * Creation asks for the name only, exactly as creating an exercise does
     * (ROADMAP N2): the exercises are added afterwards from the editor.
     */
    suspend fun createTemplate(name: String): DataResult<String>

    /**
     * Saves a finished workout as a new plan (ROADMAP N31), returning the new plan's id.
     *
     * **One transaction, not a client-side loop.** [addExercise] returns `Unit` rather than the new id,
     * so a loop would have to re-read the plan to find what it had just written — and could leave a
     * half-built plan behind if anything failed in between. This is the same shape as seeding a session
     * from a plan, in the other direction.
     *
     * Copies the exercises in order and **their performed sets as targets** — role, weight, assistance,
     * reps and RPE — so a copied warm-up ramp is a ramp and a copied superset stays paired. **A hold
     * copies no rep target** (N220): its reps are zero by definition and a plan stating zero reps is one
     * the validator refuses (B244). The
     * exercise's own target RPE is the last set that named one, which is what the plan builds to
     * (N59). It copies neither the readiness note, the ratings nor the workout comment: those describe
     * that day rather than the plan. The source workout is untouched and stays independent of the copy.
     *
     * A workout with nothing to copy is refused rather than turned into an empty plan.
     */
    suspend fun createTemplateFromSession(sessionId: String, name: String): DataResult<String>

    suspend fun renameTemplate(templateId: String, name: String): DataResult<Unit>

    /**
     * Hides the template from the choices, or brings it back (ROADMAP N214).
     *
     * **Not a delete**: the row, its exercises, its sets and every program slot that names it are
     * untouched. **It does not write [updatedAt]**, because that is the clock a program document's
     * staleness check reads (N171) and hiding changes no plan — were it written, every program
     * carrying the workout would refuse an edit the lifter had every right to make.
     *
     * Creation never hides: a new workout is visible, and a lifter who wants a brand-new plan gone
     * deletes it. This is the only way in.
     */
    suspend fun setTemplateHidden(templateId: String, hidden: Boolean): DataResult<Unit>

    /** Soft-deletes the template; its rows stay for an export to carry. */
    suspend fun deleteTemplate(templateId: String): DataResult<Unit>

    /**
     * Brings a deleted workout back, with its plan (ROADMAP N216).
     *
     * **One write.** The delete flagged the template row alone, so its planned exercises and sets were
     * never hidden: clearing `deletedAt` returns them with it, and a program slot that still names the
     * workout reappears on its day because the day list joins live workouts. `hiddenAt` is left as it
     * was, so a workout deleted while hidden comes back hidden, and **`updatedAt` moves**, because the
     * delete moved it — a program document exported before the restore is refused as stale (N171)
     * rather than merged into a definition that changed.
     *
     * Not automatic, and not an undo: undoing a delete the lifter meant stays the backup's job, which
     * is the rule [deleteTemplate]'s soft delete exists for.
     */
    suspend fun restoreTemplate(templateId: String): DataResult<Unit>

    /**
     * Plans this exercise into a superset with another, or leaves one (ROADMAP B16).
     *
     * The same ordinal a session carries, so a workout started from this plan arrives grouped. All of
     * [templateExerciseIds] move together, so a plan cannot end up half-paired (ROADMAP B27).
     */
    suspend fun setSupersetGroup(
        templateExerciseIds: List<String>,
        group: Int?,
    ): DataResult<Unit>

    /** Appends [exerciseId] to the end of the template. */
    suspend fun addExercise(templateId: String, exerciseId: String): DataResult<Unit>

    suspend fun removeExercise(templateExerciseId: String): DataResult<Unit>

    /**
     * Writes [edits] **in front of** what the plan already prescribes, in the order given (ROADMAP B34).
     *
     * `addSet` appends, which is right for authoring a plan top to bottom and wrong for a warm-up ramp
     * derived from a working set already there: appending it put the warm-ups *after* the work they
     * exist to prepare for, which its own KDoc said it did not.
     */
    suspend fun prependSets(templateExerciseId: String, edits: List<TemplateSetEdit>): DataResult<Unit>

/** Appends a planned set to a template exercise (ROADMAP N14). */
    suspend fun addSet(templateExerciseId: String, edit: TemplateSetEdit): DataResult<Unit>

    /** Overwrites a planned set's targets. */
    suspend fun updateSet(templateSetId: String, edit: TemplateSetEdit): DataResult<Unit>

    suspend fun removeSet(templateSetId: String): DataResult<Unit>

    /**
     * Takes a planned set's removal back, **at the position it held** (ROADMAP N90).
     *
     * [templateSetId] is any row of what the removal took: the set the delete was asked for, or a rung of
     * the run that went with it. Everything soft-deleted by that one write comes back together, because a
     * run whose anchor is gone reads as nothing, and the survivors shift down to make room — so the plan's
     * order is what it was rather than what the removal happened to leave behind. Appending instead would
     * silently rewrite a drop run, which is read by position (`runAt`), and a plan's order *is* the plan.
     */
    suspend fun restoreSet(templateSetId: String): DataResult<Unit>

    /**
     * Writes what a plan prescribes for one exercise: a rest, a cue and the target RPE it builds to,
     * any of which may be null to fall back to the library's (N5) or to name no effort (N59).
     */
    suspend fun setExercisePlan(
        templateExerciseId: String,
        restSeconds: Int?,
        techniqueNote: String?,
        targetRpeHalves: Int?,
    ): DataResult<Unit>

    /**
     * Moves an exercise one slot: [delta] -1 for up, +1 for down.
     *
     * Moving past either end is a no-op rather than a failure — the button at the
     * edge should do nothing, not report an error.
     */
    suspend fun moveExercise(templateExerciseId: String, delta: Int): DataResult<Unit>
}

/**
 * The editable targets of a planned set (ROADMAP N14).
 *
 * A parameter object rather than six arguments: everything here is optional except
 * the role, and a call site that lists them positionally would be unreadable.
 */
data class TemplateSetEdit(
    val role: SetType = SetType.NORMAL,
    val targetWeightGrams: Long? = null,
    /** The assistance the plan prescribes, as a magnitude (ROADMAP N15). */
    val targetAssistanceGrams: Long? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    /**
     * Where in the range the lifter has climbed, or null (ROADMAP N74).
     *
     * The two ends above are the range the plan was authored with; this is the number the session is
     * asked for, and the one progression moves. A weight step on a real range writes it back to
     * [targetRepsMin]. Null means "the floor where the plan wrote one, its ceiling otherwise".
     */
    val targetRepsCurrent: Int? = null,
    /**
     * The value this run takes off the set above it, in grams, or null (ROADMAP N79).
     *
     * Carried by a **drop** rung and refused anywhere else, and only meaningful on the run's first
     * rung: the rest inherit it, so a later rung's own value is not what the ladder reads.
     */
    val dropValueGrams: Long? = null,
    /** The hold this set prescribes, in seconds, or null for a counted set (ROADMAP N242). */
    val targetDurationSeconds: Int? = null,
    val note: String? = null,
)
