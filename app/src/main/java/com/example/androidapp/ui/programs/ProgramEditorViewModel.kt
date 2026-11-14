package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.PlanStallFinder
import com.example.androidapp.domain.PlanVolumeFinder
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.DeloadedSet
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.ReplaceOutcome
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.model.VolumeSuggestion
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.model.deloadOf
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.platform.DocumentIo
import com.example.androidapp.ui.navigation.ProgramEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

/**
 * What one slot's template prescribes, as the preview shows it (ROADMAP N72).
 *
 * The exercises in the template's own order, each with the sets the plan wrote. Nothing here is
 * editable, so there is no draft beside it.
 */
data class SlotPreview(
    val templateName: String,
    val exercises: List<TemplateExercise>,
)

data class ProgramEditorUiState(
    val isLoading: Boolean = true,
    val program: WorkoutProgram? = null,
    val slots: List<ProgramSlot> = emptyList(),
    /** The templates a slot can point at, in the list the picker shows. */
    val templates: List<WorkoutTemplate> = emptyList(),
    /**
     * The slot the program's run is at, or null (ROADMAP P3.9).
     *
     * The order was real in this editor and nowhere else; this is what marks the place the rotation
     * is up to, derived from what was done rather than stored.
     */
    val runSlotId: String? = null,
    /**
     * True when a block has no session left (B197).
     *
     * The run carries it, and the editor is where "Day 9 of 9" used to read as upcoming: a finished
     * block says it is done rather than marking its last day as the place to be.
     */
    val blockComplete: Boolean = false,
    /**
     * The template one slot points at, being read rather than edited, or null (ROADMAP N72).
     *
     * A preview and not an editor: the template is edited in the template editor, and a program is a
     * schedule over it. Tapping the name is how a lifter checks what a day actually does without
     * leaving the program.
     */
    val preview: SlotPreview? = null,
    val error: DataError? = null,
    /**
     * The prescriptions in this program that have stopped moving, in the program's order (ROADMAP N310).
     *
     * Every one, because each is now a decision the card can offer (amending N237's one-stall rule). Empty
     * is the normal state.
     */
    val stalls: List<PlanStall> = emptyList(),
    /** The plan's earned sets, one per exercise the card offers (N311). */
    val volumes: List<VolumeSuggestion> = emptyList(),
) {
    /** The program was deleted, or never existed — either way there is no editor. */
    val notFound: Boolean get() = !isLoading && program == null
}

/** One kept work set's before and after, as the per-lift deload's confirm shows it (N307). */
data class DeloadPreviewSet(val before: TemplateSet, val after: DeloadedSet)

/**
 * A per-lift deload waiting for the lifter's yes (N307).
 *
 * It is a **plan edit**, so the confirm shows the numbers it will write and names the other programs
 * only when the plan is actually shared. Nothing is written until [onConfirmDeload].
 */
data class DeloadPrompt(
    val templateExerciseId: String,
    val exerciseName: String,
    val kept: List<DeloadPreviewSet>,
    val removedCount: Int,
    /** The other programs that name this plan, empty when it is not shared (decision 5a). */
    val sharedProgramNames: List<String>,
    val stepGrams: Long,
    /** The plan's own unit, or null to follow the app setting (N64). */
    val weightUnit: WeightUnit?,
)

/**
 * The deload the snackbar can still take back (N307).
 *
 * It carries both halves of the edit: [keptBefore] to rewrite the kept targets and [removedSetIds] so one
 * restore takes the removal back. That is decision 4a — the undo is the whole edit, not only the targets.
 */
data class DeloadUndo(
    val templateExerciseId: String,
    val exerciseName: String,
    val keptBefore: List<TemplateSet>,
    val removedSetIds: List<String>,
)

/** A replace hand-off waiting for the route to open its sheet (N308). */
data class ReplaceTarget(val templateId: String, val templateExerciseId: String)

/** The replace the plan screen can take back (N308, decision 4a). */
data class ReplaceUndo(val outcome: ReplaceOutcome)

/**
 * One program: its name, its ordered slots, and whether it is the active one (ROADMAP P3.3).
 *
 * Every write goes through [ProgramRepository] and surfaces a failure rather than
 * swallowing it (F7): a reorder that silently did not happen would leave the user training
 * the wrong day.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgramEditorViewModel @Inject constructor(
    private val repository: ProgramRepository,
    private val templateRepository: TemplateRepository,
    private val stallFinder: PlanStallFinder,
    private val planVolumeFinder: PlanVolumeFinder,
    private val documentIo: DocumentIo,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val programId: String = savedStateHandle.toRoute<ProgramEditor>().programId

    private val deloadTarget = savedStateHandle.toRoute<ProgramEditor>().let {
        it.deloadTemplateId to it.deloadTemplateExerciseId
    }

    private val replaceTargetRoute = savedStateHandle.toRoute<ProgramEditor>().let {
        it.replaceTemplateId to it.replaceTemplateExerciseId
    }

    /** A replace asked for by a weekly-review hand-off, one-shot for the route to open (N308). */
    private val _replaceTarget = MutableStateFlow<ReplaceTarget?>(null)
    val replaceTarget: StateFlow<ReplaceTarget?> = _replaceTarget.asStateFlow()

    /** The replace the snackbar can still undo (N308). */
    private val _replaceUndo = MutableStateFlow<ReplaceUndo?>(null)
    val replaceUndo: StateFlow<ReplaceUndo?> = _replaceUndo.asStateFlow()

    private val replaceResult = savedStateHandle.getStateFlow<String?>("replaceOldId", null)

    init {
        val (templateId, exerciseId) = deloadTarget
        if (templateId != null && exerciseId != null) {
            // The hand-off is a one-shot (N310): clear it before a recreation can open it again.
            savedStateHandle.remove<String>("deloadTemplateId")
            savedStateHandle.remove<String>("deloadTemplateExerciseId")
            openDeload(templateId, exerciseId)
        }
        val (replaceTemplate, replaceExercise) = replaceTargetRoute
        if (replaceTemplate != null && replaceExercise != null) {
            savedStateHandle.remove<String>("replaceTemplateId")
            savedStateHandle.remove<String>("replaceTemplateExerciseId")
            _replaceTarget.value = ReplaceTarget(replaceTemplate, replaceExercise)
        }
        viewModelScope.launch {
            replaceResult.collect { old ->
                if (old == null) return@collect
                val newId = savedStateHandle.get<String>("replaceNewId") ?: return@collect
                val ids = savedStateHandle.get<ArrayList<String>>("replaceSetIds").orEmpty()
                savedStateHandle.remove<String>("replaceOldId")
                savedStateHandle.remove<String>("replaceNewId")
                savedStateHandle.remove<ArrayList<String>>("replaceSetIds")
                _replaceUndo.value = ReplaceUndo(ReplaceOutcome(old, newId, ids))
            }
        }
    }

    private val error = MutableStateFlow<DataError?>(null)

    /** True once the program is gone, so the screen can leave the editor. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    /** The deload confirm, or null: a plan edit is a decision and waits for it (N307). */
    private val _deloadPrompt = MutableStateFlow<DeloadPrompt?>(null)
    val deloadPrompt: StateFlow<DeloadPrompt?> = _deloadPrompt.asStateFlow()

    /** The deload the snackbar can still undo, or null (N307). */
    private val _deloadUndo = MutableStateFlow<DeloadUndo?>(null)
    val deloadUndo: StateFlow<DeloadUndo?> = _deloadUndo.asStateFlow()

    /** Bumped after a plan write, so the stall reads the changed plan rather than its old answer. */
    private val stallRefresh = MutableStateFlow(0)

    /**
     * The slot the preview is showing, or null (ROADMAP N72).
     *
     * The name travels with the id: the slot already holds it, and reading the templates list again
     * to find it would be a second subscription for a string the caller has.
     */
    private data class PreviewRequest(val templateId: String, val templateName: String)

    private val previewRequest = MutableStateFlow<PreviewRequest?>(null)

    /**
     * What the preview shows, read only while a slot is open.
     *
     * The exercise flow is what the template editor draws, so a plan edited there is what this
     * shows the next time it is opened — which is the point of a preview over a copy.
     */
    private val preview: Flow<SlotPreview?> = previewRequest.flatMapLatest { request ->
        if (request == null) {
            flowOf(null)
        } else {
            templateRepository.observeExercises(request.templateId).map { exercises ->
                SlotPreview(templateName = request.templateName, exercises = exercises)
            }
        }
    }

    /**
     * The two things this editor overlays on the program: the open preview and the run's place
     * (ROADMAP N72, P3.9).
     *
     * Kept together so the state combine stays at the arity the rest of the screen uses — which is
     * the shape N73 deleted when there was only one of them.
     */
    private data class EditorExtras(
        val preview: SlotPreview?,
        val runSlotId: String?,
        val blockComplete: Boolean,
    )

    private val extras: Flow<EditorExtras> =
        combine(preview, repository.observeProgramRun(programId)) { open, run ->
            val complete = run?.isComplete == true
            EditorExtras(
                preview = open,
                // A finished block has no next day: the repository answers with its last slot so the
                // screen has something to name, and naming it as the place would be the bug (B197).
                runSlotId = run?.slot?.id?.takeUnless { complete },
                blockComplete = complete,
            )
        }

    /** The program, its slots, the templates to add, and where the run is (ROADMAP P3.3, P3.9). */
    val uiState: StateFlow<ProgramEditorUiState> = combine(
        repository.observeProgram(programId),
        repository.observeSlots(programId),
        templateRepository.observeTemplates(),
        error,
        extras,
    ) { program, slots, templates, currentError, open ->
        ProgramEditorUiState(
            isLoading = false,
            program = program,
            slots = slots,
            templates = templates,
            runSlotId = open.runSlotId,
            blockComplete = open.blockComplete,
            preview = open.preview,
            error = currentError,
        )
    }
        // The stalls are read from history rather than from a stored flag (N237): a pattern in the last few
        // sessions, so they are recomputed after every one of them.
        .combine(stallsForProgram()) { state, stalls ->
            when (stalls) {
                is DataResult.Success -> state.copy(stalls = stalls.data)
                is DataResult.Failure -> state.copy(stalls = emptyList(), error = stalls.error)
            }
        }
        .combine(volumeSuggestions()) { state, volumes ->
            when (volumes) {
                is DataResult.Success -> state.copy(volumes = volumes.data)
                is DataResult.Failure -> state.copy(volumes = emptyList(), error = volumes.error)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ProgramEditorUiState(),
        )

    /**
     * The first stall among this program's plans, or null (ROADMAP N237).
     *
     * Read per slot's template rather than per movement, because the scope of the rule is the plan: the same
     * movement on another day of the same program prescribes different work, and a session started from no
     * template is not one of these.
     */
    private fun stallsForProgram(): Flow<DataResult<List<PlanStall>>> =
        combine(repository.observeSlots(programId), stallRefresh) { _, _ -> Unit }
            .mapLatest { stallFinder.stallsForProgram(programId) }

    private fun volumeSuggestions(): Flow<DataResult<List<VolumeSuggestion>>> =
        combine(repository.observeSlots(programId), stallRefresh) { _, _ -> Unit }
            .mapLatest { planVolumeFinder.suggestionsForProgram(programId) }

    fun onRename(name: String) = write { repository.renameProgram(programId, name) }

    /** A week that repeats, or an order that ends (ROADMAP N174, N178). */
    fun onSetKind(kind: ProgramKind) = write { repository.setProgramKind(programId, kind) }

    /** The day a block begins, or null for the day it was created (ROADMAP N178). */
    fun onSetStartDate(startDate: LocalDate?) =
        write { repository.setProgramStartDate(programId, startDate) }

    /**
     * This program as a document, for the file picker to write (ROADMAP N47).
     *
     * Suspending and returned rather than pushed into state: the file write belongs to the
     * composable, which holds the `Uri`, and the screen words the result.
     */
    suspend fun exportDocument(): DataResult<String> = repository.exportProgramDocument(programId)

    private val _exported = MutableStateFlow<DataResult<Unit>?>(null)

    /**
     * The last document write, or null while there is nothing to report (B159).
     *
     * In [viewModelScope], not the composition's: the picker has already created the file by the time this
     * runs, so a cancelled scope leaves an empty document and no message — which is exactly what a rotation
     * did before.
     */
    val exported: StateFlow<DataResult<Unit>?> = _exported

    /** Writes this program to the picked document, reporting through [exported]. */
    fun writeDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            _exported.value = when (val produced = exportDocument()) {
                is DataResult.Success -> if (documentIo.write(context, uri, produced.data)) {
                    DataResult.Success(Unit)
                } else {
                    DataResult.Failure(DataError.Storage(IllegalStateException("the file could not be written")))
                }

                is DataResult.Failure -> produced
            }
        }
    }

    /** The write has been reported, so it is not reported twice. */
    fun onExportShown() {
        _exported.value = null
    }

    /**
     * Starts or stops following this program (P3.3, amended by P3.12).
     *
     * Off stops following **this** one and leaves the others alone; more than one may be
     * active, so there is no longer a "follow none" side effect to turning one off.
     */
    fun onSetActive(active: Boolean) = write {
        if (active) repository.activateProgram(programId) else repository.deactivateProgram(programId)
    }

    /**
     * Adds a day to the program, in [phaseWeek] or in the single repeating week when it is null
     * (ROADMAP N292).
     *
     * The week travels with the tap: the editor shows one week at a time, so the day belongs to the
     * week the lifter is looking at rather than to whichever week the schedule happens to be in.
     */
    fun onAddSlot(templateId: String, phaseWeek: Int?) =
        write { repository.addSlot(programId, templateId, phaseWeek = phaseWeek) }

    /** Removes one authored week's days; the workouts they named stay in the library (N292). */
    fun onRemovePhaseWeek(week: Int) = write { repository.removePhaseWeek(programId, week) }

    private val _templateToEdit = MutableStateFlow<String?>(null)

    /**
     * The workout just created inline, for the screen to open (P3.18).
     *
     * A new workout has no exercises, so the slot exists and the plan is empty: the editor it opens is
     * where it becomes a plan.
     */
    val templateToEdit: StateFlow<String?> = _templateToEdit.asStateFlow()

    /**
     * Creates a workout and appends it to this program in one step (P3.18).
     *
     * The two writes stay separate repository calls by design — a template is not a program's row — and a
     * failure of either surfaces. The slot is added only after the template exists, so a slot never points
     * at nothing.
     */
    fun onAddNewSlot(name: String, phaseWeek: Int?) {
        viewModelScope.launch {
            when (val created = templateRepository.createTemplate(name)) {
                is DataResult.Success -> when (
                    val slot = repository.addSlot(programId, created.data, phaseWeek = phaseWeek)
                ) {
                    is DataResult.Success -> {
                        error.value = null
                        _templateToEdit.value = created.data
                    }

                    is DataResult.Failure -> error.value = slot.error
                }

                is DataResult.Failure -> error.value = created.error
            }
        }
    }

    /** Called once the screen has opened the new workout, so it is opened once. */
    fun onTemplateEditHandled() {
        _templateToEdit.value = null
    }

    fun onSetSlotWeekday(slotId: String, weekday: DayOfWeek?) = write {
        repository.setSlotWeekday(slotId, weekday)
    }

    /** [delta] -1 moves the slot up, +1 down. */
    fun onMoveSlot(slotId: String, delta: Int) = write { repository.moveSlot(slotId, delta) }

    fun onRemoveSlot(slotId: String) = write { repository.removeSlot(slotId) }

    /** Opens the read-only view of what one slot's template trains (ROADMAP N72). */
    fun onPreviewSlot(templateId: String, templateName: String) {
        previewRequest.value = PreviewRequest(templateId, templateName)
    }

    fun onClosePreview() {
        previewRequest.value = null
    }

    fun onDeleteProgram() {
        viewModelScope.launch {
            when (val result = repository.deleteProgram(programId)) {
                is DataResult.Success -> {
                    error.value = null
                    _deleted.value = true
                }

                is DataResult.Failure -> error.value = result.error
            }
        }
    }

    fun onReplaceTargetHandled() {
        _replaceTarget.value = null
    }

    fun onUndoReplace() {
        val undo = _replaceUndo.value ?: return
        _replaceUndo.value = null
        viewModelScope.launch {
            when (val read = templateRepository.undoReplaceExercise(undo.outcome)) {
                is DataResult.Success -> stallRefresh.update { it + 1 }
                is DataResult.Failure -> error.value = read.error
            }
        }
    }

    fun onReplaceUndoShown() {
        _replaceUndo.value = null
    }

    /** The card's "Deload this lift" (N307): read the plan, then ask. */
    fun onDeloadLift(stall: PlanStall) = openDeload(stall.templateId, stall.templateExerciseId)

    private fun openDeload(templateId: String, templateExerciseId: String) {
        viewModelScope.launch {
            val read = dataResultOf {
                val exercise = templateRepository.observeExercises(templateId).first()
                    .firstOrNull { it.id == templateExerciseId }
                    ?: throw NoSuchElementException("the plan's exercise is gone")
                val sets = templateRepository.observeSets(templateId).first()
                    .filter { it.templateExerciseId == templateExerciseId }
                val step = Weight.stepGramsFor(
                    exercise.stepGrams,
                    exercise.weightUnit ?: WeightUnit.KILOGRAMS,
                )
                val plan = deloadOf(sets, step)
                val byId = sets.associateBy { it.id }
                val kept = plan.kept.mapNotNull { after ->
                    byId[after.setId]?.let { before -> DeloadPreviewSet(before, after) }
                }
                val shared = when (
                    val names = repository.otherProgramNamesUsingTemplates(listOf(templateId), programId)
                ) {
                    is DataResult.Success -> names.data
                    is DataResult.Failure -> {
                        // A failed read is not "nothing else uses it" (B351); say it, and still ask.
                        error.value = names.error
                        emptyList()
                    }
                }
                DeloadPrompt(
                    templateExerciseId = templateExerciseId,
                    exerciseName = exercise.exerciseName,
                    kept = kept,
                    removedCount = plan.removedSetIds.size,
                    sharedProgramNames = shared,
                    stepGrams = step,
                    weightUnit = exercise.weightUnit,
                )
            }
            when (read) {
                is DataResult.Success -> _deloadPrompt.value = read.data
                is DataResult.Failure -> error.value = read.error
            }
        }
    }

    /** Writes the edit the confirm showed; the undo is the whole edit (N307, decision 4a). */
    fun onConfirmDeload() {
        val prompt = _deloadPrompt.value ?: return
        viewModelScope.launch {
            when (val read = templateRepository.deloadExercise(prompt.templateExerciseId, prompt.stepGrams)) {
                is DataResult.Success -> {
                    _deloadPrompt.value = null
                    _deloadUndo.value = DeloadUndo(
                        templateExerciseId = prompt.templateExerciseId,
                        exerciseName = prompt.exerciseName,
                        keptBefore = prompt.kept.map { it.before },
                        removedSetIds = read.data.removedSetIds,
                    )
                    stallRefresh.update { it + 1 }
                }
                is DataResult.Failure -> error.value = read.error
            }
        }
    }

    fun onCancelDeload() {
        _deloadPrompt.value = null
    }

    fun onDeloadUndoShown() {
        _deloadUndo.value = null
    }

    fun onUndoDeload() {
        val undo = _deloadUndo.value ?: return
        _deloadUndo.value = null
        viewModelScope.launch {
            var failure: DataError? = null
            // One removed id restores the whole removal: the deload's rows share one deletion timestamp.
            undo.removedSetIds.firstOrNull()?.let { id ->
                (templateRepository.restoreSet(id) as? DataResult.Failure)?.let { failure = it.error }
            }
            if (failure == null) {
                for (before in undo.keptBefore) {
                    when (val result = templateRepository.updateSet(before.id, before.asEdit())) {
                        is DataResult.Success -> Unit
                        is DataResult.Failure -> {
                            failure = result.error
                            break
                        }
                    }
                }
            }
            if (failure != null) error.value = failure else stallRefresh.update { it + 1 }
        }
    }

    private fun TemplateSet.asEdit() = TemplateSetEdit(
        role = role,
        targetWeightGrams = targetWeightGrams,
        targetAssistanceGrams = targetAssistanceGrams,
        targetRepsMin = targetRepsMin,
        targetRepsMax = targetRepsMax,
        targetRepsCurrent = targetRepsCurrent,
        dropValueGrams = dropValueGrams,
        targetDurationSeconds = targetDurationSeconds,
        note = note,
    )

    fun onErrorShown() {
        error.value = null
    }

    private val _volumeUndo = MutableStateFlow<String?>(null)
    val volumeUndo: StateFlow<String?> = _volumeUndo.asStateFlow()

    /** Adds the card's set to the plan, after the reader chose it (N311). */
    fun onAddVolume(suggestion: VolumeSuggestion) {
        viewModelScope.launch {
            when (val write = templateRepository.addSet(suggestion.templateExerciseId, suggestion.edit)) {
                is DataResult.Success -> {
                    error.value = null
                    stallRefresh.update { it + 1 }
                    _volumeUndo.value = addedSetId(suggestion)
                }

                is DataResult.Failure -> error.value = write.error
            }
        }
    }

    fun onUndoVolume() {
        val id = _volumeUndo.value ?: return
        viewModelScope.launch {
            when (val write = templateRepository.removeSet(id)) {
                is DataResult.Success -> {
                    _volumeUndo.value = null
                    stallRefresh.update { it + 1 }
                }

                is DataResult.Failure -> error.value = write.error
            }
        }
    }

    fun onDismissVolumeUndo() {
        _volumeUndo.value = null
    }

    private suspend fun addedSetId(suggestion: VolumeSuggestion): String? =
        templateRepository.observeExercises(suggestion.templateId)
            .first()
            .firstOrNull { it.id == suggestion.templateExerciseId }
            ?.sets
            ?.maxByOrNull { it.setIndex }
            ?.id

    private fun write(action: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            error.value = (action() as? DataResult.Failure)?.error
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
