package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.repository.ProgramDocumentPreview
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.platform.DocumentIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProgramsUiState(
    val isLoading: Boolean = true,
    val programs: List<WorkoutProgram> = emptyList(),
    /** Set when a write failed, so the screen can say so instead of lying. */
    val error: DataError? = null,
)

/** An edit waiting for its confirmation, with the file it came from (ROADMAP N171). */
data class PendingProgramEdit(val text: String, val preview: ProgramDocumentPreview.Edit)

/**
 * The document whose name another plan holds, waiting for the lifter to give it its own (ROADMAP N243).
 *
 * The text rather than the decoded document, for the edit's reason (N171): the repository decodes and merges
 * inside its transaction, so a device that changed between the question and the answer is a refusal rather
 * than a merge into something nobody read.
 */
data class PendingProgramRename(val text: String, val preview: ProgramDocumentPreview.NameTaken)

/**
 * The program list (ROADMAP P3.3).
 *
 * Creating a program takes a name from the screen — a ViewModel cannot read a string
 * resource — and then reports the new id so the list can open its editor, which is where
 * its slots are added.
 */
@HiltViewModel
class ProgramsViewModel @Inject constructor(
    private val repository: ProgramRepository,
    private val documentIo: DocumentIo,
) : ViewModel() {

    private val error = MutableStateFlow<DataError?>(null)

    private val _imported = MutableStateFlow<DataResult<ProgramImportSummary>?>(null)

    /**
     * The last document merge, or null while there is nothing to report (B159).
     *
     * The transfer runs in [viewModelScope] rather than the composition's, so a rotation cannot cancel it
     * between the picker creating the document and the read finishing — the failure the backup's own transfer
     * had, and this is the same shape.
     */
    val imported: StateFlow<DataResult<ProgramImportSummary>?> = _imported

    /** Reads the picked document and merges it, reporting through [imported]. */
    fun readDocument(context: Context, uri: Uri) {
        viewModelScope.launch {
            val text = documentIo.read(context, uri)
            if (text == null) {
                reportUnreadable()
                return@launch
            }
            openDocument(text)
        }
    }

    /**
     * What the read document means — the half that is about the document rather than about Android.
     *
     * An edit is a proposal to change a program this device holds, so it stops here for a
     * confirmation (N171); a document to add still goes straight through, as it always has. Split
     * from [readDocument] so this decision is testable without a content resolver (N180).
     */
    internal suspend fun openDocument(text: String) {
        when (val preview = repository.previewProgramDocument(text)) {
            is DataResult.Failure -> {
                _edited.value = false
                _imported.value = preview
            }

            is DataResult.Success -> when (val value = preview.data) {
                ProgramDocumentPreview.Add -> {
                    _edited.value = false
                    _imported.value = importDocument(text)
                }

                is ProgramDocumentPreview.NameTaken -> {
                    _edited.value = false
                    _pendingRename.value = PendingProgramRename(text, value)
                }

                is ProgramDocumentPreview.Edit -> {
                    _edited.value = false
                    _pendingEdit.value = PendingProgramEdit(text, value)
                }
            }
        }
    }

    /** A file that could not be read: the same sentence whether it was unreadable or empty (B159). */
    private fun reportUnreadable() {
        _edited.value = false
        _imported.value = DataResult.Failure(
            DataError.Storage(IllegalStateException("the file could not be read")),
        )
    }

    private val _pendingEdit = MutableStateFlow<PendingProgramEdit?>(null)

    /** The edit awaiting confirmation, or null. */
    val pendingEdit: StateFlow<PendingProgramEdit?> = _pendingEdit

    /** True while the load being reported was an edit, so the screen says "updated", not "loaded". */
    private val _edited = MutableStateFlow(false)
    val edited: StateFlow<Boolean> = _edited

    /**
     * Applies the pending edit the lifter confirmed (ROADMAP N171).
     *
     * The text is kept rather than the decoded document: the repository decodes and plans again
     * inside its transaction, so a change made on the phone between the preview and this tap is a
     * refusal rather than a silently lost edit.
     */
    fun confirmEdit() {
        val pending = _pendingEdit.value ?: return
        _pendingEdit.value = null
        viewModelScope.launch {
            _edited.value = true
            _imported.value = importDocument(pending.text)
        }
    }

    /** Leaves the program as it is; the preview wrote nothing. */
    fun dismissEdit() {
        _pendingEdit.value = null
    }

    private val _pendingRename = MutableStateFlow<PendingProgramRename?>(null)

    /** The load waiting for a name of its own, or null (N243). */
    val pendingRename: StateFlow<PendingProgramRename?> = _pendingRename

    /**
     * Loads the held document as a second program under [name] (ROADMAP N243).
     *
     * The copy is an add, so nothing the device already holds changes — the plan that took the name keeps
     * it, and the arriving one gets an id of its own.
     */
    fun confirmRename(name: String) {
        val pending = _pendingRename.value ?: return
        _pendingRename.value = null
        viewModelScope.launch {
            _edited.value = false
            _imported.value = repository.importProgramDocumentCopy(pending.text, name)
        }
    }

    /** Leaves the document unloaded rather than arriving as a second program. */
    fun dismissRename() {
        _pendingRename.value = null
    }

    /** The merge has been reported, so it is not reported twice. */
    fun onImportShown() {
        _imported.value = null
        _edited.value = false
    }

    /**
     * Inserts one of the shipped examples (P3.19).
     *
     * An example is a program document in assets, so it travels the same road as a picked file: the
     * app's codec, the insert-only merge, and the *already there* summary all apply, and importing the
     * same one twice adds nothing.
     */
    fun insertExample(context: Context, assetName: String) {
        viewModelScope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(assetName).bufferedReader().use { reader -> reader.readText() }
                }.getOrNull()
            }
            _edited.value = false
            _imported.value = if (text == null) {
                DataResult.Failure(
                    DataError.Storage(IllegalStateException("the example could not be read")),
                )
            } else {
                importDocument(text)
            }
        }
    }

    /** Emits the id of a just-created program, so the list can open its editor. */
    private val _created = MutableStateFlow<String?>(null)
    val createdProgramId: StateFlow<String?> = _created

    val uiState: StateFlow<ProgramsUiState> = combine(
        repository.observePrograms(),
        error,
    ) { programs, currentError ->
        ProgramsUiState(isLoading = false, programs = programs, error = currentError)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ProgramsUiState(),
    )

    fun onCreateProgram(name: String) {
        viewModelScope.launch {
            when (val result = repository.createProgram(name)) {
                is DataResult.Success -> {
                    error.value = null
                    _created.value = result.data
                }

                is DataResult.Failure -> error.value = result.error
            }
        }
    }

    /** Starts following this program, without stopping any other (P3.12). */
    fun onSetActive(programId: String) {
        viewModelScope.launch {
            error.value = (repository.activateProgram(programId) as? DataResult.Failure)?.error
        }
    }

    /** [delta] -1 moves the program up, +1 down (P3.12). */
    fun onMoveProgram(programId: String, delta: Int) {
        viewModelScope.launch {
            error.value = (repository.moveProgram(programId, delta) as? DataResult.Failure)?.error
        }
    }

    /** Called once the screen has navigated, so a recomposition cannot re-open it. */
    fun onCreatedHandled() {
        _created.value = null
    }

    /**
     * Loads one program document (ROADMAP N47).
     *
     * Suspending and returned rather than pushed into state: the file read belongs to the
     * composable, which holds the `Uri`, and the screen words the summary.
     */
    suspend fun importDocument(text: String): DataResult<ProgramImportSummary> =
        repository.importProgramDocument(text)

    fun onErrorShown() {
        error.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
