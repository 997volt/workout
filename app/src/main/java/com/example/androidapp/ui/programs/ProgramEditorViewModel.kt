package com.example.androidapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.ui.navigation.ProgramEditor
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
     * The template one slot points at, being read rather than edited, or null (ROADMAP N72).
     *
     * A preview and not an editor: the template is edited in the template editor, and a program is a
     * schedule over it. Tapping the name is how a lifter checks what a day actually does without
     * leaving the program.
     */
    val preview: SlotPreview? = null,
    val error: DataError? = null,
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
    )

    private val extras: Flow<EditorExtras> =
        combine(preview, repository.observeProgramRun(programId)) { open, run ->
            EditorExtras(preview = open, runSlotId = run?.slot?.id)
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
            preview = open.preview,
            error = currentError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProgramEditorUiState(),
    )

    fun onRename(name: String) = write { repository.renameProgram(programId, name) }

    /**
     * This program as a document, for the file picker to write (ROADMAP N47).
     *
     * Suspending and returned rather than pushed into state: the file write belongs to the
     * composable, which holds the `Uri`, and the screen words the result.
     */
    suspend fun exportDocument(): DataResult<String> = repository.exportProgramDocument(programId)

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
