package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.PrescriptionHistory
import com.example.androidapp.domain.model.ProgramKind
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.STALL_SESSIONS
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.domain.model.stall
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TrendsRepository
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
     * The first prescription in this program that has stopped moving, or null (ROADMAP N237).
     *
     * One sentence, not a list: the app is suggesting a week off, and a screen full of stalls would be a
     * diagnosis where a suggestion was asked for. The button beside it opens the deload creator the program
     * already has.
     */
    val stall: PlanStall? = null,
) {
    /** The program was deleted, or never existed — either way there is no editor. */
    val notFound: Boolean get() = !isLoading && program == null
}

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
    private val trends: TrendsRepository,
    private val documentIo: DocumentIo,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val programId: String = savedStateHandle.toRoute<ProgramEditor>().programId

    private val error = MutableStateFlow<DataError?>(null)

    /** True once the program is gone, so the screen can leave the editor. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

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
        // The stall is read from history rather than from a stored flag (N237): it is a pattern in the last few
        // sessions, so it has to be recomputed after every one of them.
        .combine(stallForProgram()) { state, stall -> state.copy(stall = stall) }
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
    private fun stallForProgram(): Flow<PlanStall?> =
        repository.observeSlots(programId).mapLatest { slots ->
            slots.mapNotNull { slot -> slot.templateId }
                .distinct()
                .firstNotNullOfOrNull { templateId -> stallForTemplate(templateId) }
        }

    /** One template's first stalled prescription, or null when none of its plans has stopped moving. */
    private suspend fun stallForTemplate(templateId: String): PlanStall? {
        val planned = templateRepository.observeExercises(templateId).first()
        val sessions = (trends.templateSessions(templateId) as? DataResult.Success)?.data.orEmpty()
        // Three sessions is the rule's own threshold (N237), and a plan with fewer cannot have one yet.
        return planned.takeIf { it.isNotEmpty() && sessions.size >= STALL_SESSIONS }
            ?.firstNotNullOfOrNull { exercise ->
                // One entry per session that **trained** the movement: absence is not a failure (N237).
                val performed = sessions.mapNotNull { session ->
                    session.performed.firstOrNull { it.exerciseId == exercise.exerciseId }?.sets
                }
                PrescriptionHistory(
                    exerciseName = exercise.exerciseName,
                    planned = exercise.progressionSets(),
                    performedBySession = performed,
                ).stall()
            }
    }

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

    fun onAddSlot(templateId: String) = write { repository.addSlot(programId, templateId) }

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
    fun onAddNewSlot(name: String) {
        viewModelScope.launch {
            when (val created = templateRepository.createTemplate(name)) {
                is DataResult.Success -> when (val slot = repository.addSlot(programId, created.data)) {
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

    fun onErrorShown() {
        error.value = null
    }

    private fun write(action: suspend () -> DataResult<Unit>) {
        viewModelScope.launch {
            error.value = (action() as? DataResult.Failure)?.error
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
