package com.example.androidapp.ui.programs

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.domain.repository.ProgramImportSummary
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.platform.DocumentIo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
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
            _imported.value = if (text == null) {
                DataResult.Failure(DataError.Storage(IllegalStateException("the file could not be read")))
            } else {
                importDocument(text)
            }
        }
    }

    /** The merge has been reported, so it is not reported twice. */
    fun onImportShown() {
        _imported.value = null
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
