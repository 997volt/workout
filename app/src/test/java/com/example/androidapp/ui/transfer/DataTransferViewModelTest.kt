package com.example.androidapp.ui.transfer

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.repository.BackupPreview
import com.example.androidapp.domain.repository.BackupProblem
import com.example.androidapp.domain.repository.BackupRepository
import com.example.androidapp.domain.repository.ImportSummary
import com.example.androidapp.domain.repository.PreviewCounts
import com.example.androidapp.platform.DocumentIo
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The transfer ViewModel's orchestration (N273).
 *
 * Its collaborators were covered and the orchestration between them was not: which outcome the screen gets,
 * what happens to the picked file while the confirmation is open, and that a clear's failure is not a success.
 * Both ports are fakes; the Context and Uri are Robolectric's, because the interface still speaks Android at
 * its boundary (the implementation is where the ContentResolver lives).
 */
@RunWith(AndroidJUnit4::class)
class DataTransferViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val uri: Uri = Uri.parse("content://test/backup.json")
    private val repository = FakeBackupRepository()
    private val io = FakeDocumentIo()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = DataTransferViewModel(repository, io)

    @Test
    fun anExport_ready_isTheDocumentsText() = runTest(dispatcher) {
        repository.exportResult = DataResult.Success("{\"schemaVersion\":8}")

        assertThat(viewModel().export()).isEqualTo(ExportOutcome.Ready("{\"schemaVersion\":8}"))
    }

    @Test
    fun aFailedExport_isRefused_andNothingIsWritten() = runTest(dispatcher) {
        repository.exportResult = DataResult.Failure(DataError.Storage(IllegalStateException("db")))
        val viewModel = viewModel()

        viewModel.writeBackup(context, uri)
        advanceUntilIdle()

        assertThat(viewModel.outcome.value).isInstanceOf(TransferOutcome.Refused::class.java)
        assertThat(io.written).isNull()
    }

    @Test
    fun aWrittenExport_isExported_andAStorageFailureIsSaid() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.writeBackup(context, uri)
        advanceUntilIdle()
        assertThat(viewModel.outcome.value).isEqualTo(TransferOutcome.Exported)
        assertThat(io.written).isEqualTo("{}")

        io.writeResult = false
        viewModel.onOutcomeShown()
        viewModel.writeBackup(context, uri)
        advanceUntilIdle()
        assertThat(viewModel.outcome.value).isEqualTo(TransferOutcome.StorageFailure)
    }

    @Test
    fun aReadBackup_waitsForConfirmation_andThenMerges() = runTest(dispatcher) {
        io.readText = "{\"schemaVersion\":8}"
        repository.previewResult = DataResult.Success(preview())
        repository.importResult = DataResult.Success(ImportSummary(added = 2, restored = 1))
        val viewModel = viewModel()

        viewModel.readBackup(context, uri)
        advanceUntilIdle()
        assertThat(viewModel.pendingImport.value?.text).isEqualTo(io.readText)
        assertThat(repository.imported).isEmpty()

        viewModel.confirmImport()
        advanceUntilIdle()

        assertThat(viewModel.pendingImport.value).isNull()
        assertThat(repository.imported).containsExactly(io.readText)
        assertThat(viewModel.outcome.value)
            .isEqualTo(TransferOutcome.Merged(ImportSummary(added = 2, restored = 1)))
    }

    @Test
    fun cancellingTheQuestion_forgetsTheFile_andNeverMergesIt() = runTest(dispatcher) {
        repository.previewResult = DataResult.Success(preview())
        val viewModel = viewModel()
        viewModel.readBackup(context, uri)
        advanceUntilIdle()

        viewModel.cancelImport()

        assertThat(viewModel.pendingImport.value).isNull()
        assertThat(repository.imported).isEmpty()
    }

    @Test
    fun aKnownBadFile_isRefused_withoutMerging_it() = runTest(dispatcher) {
        repository.previewResult = DataResult.Success(
            preview(problems = listOf(BackupProblem("A set", "is impossible"))),
        )
        val viewModel = viewModel()
        viewModel.readBackup(context, uri)
        advanceUntilIdle()

        viewModel.confirmImport()
        advanceUntilIdle()

        assertThat(viewModel.pendingImport.value).isNull()
        assertThat(repository.imported).isEmpty()
        assertThat(viewModel.outcome.value).isInstanceOf(TransferOutcome.Refused::class.java)
    }

    @Test
    fun aRefusedPreview_isReported_andNothingIsPending() = runTest(dispatcher) {
        repository.previewResult = DataResult.Failure(DataError.Invalid("not a backup"))
        val viewModel = viewModel()

        viewModel.readBackup(context, uri)
        advanceUntilIdle()

        assertThat(viewModel.pendingImport.value).isNull()
        assertThat(viewModel.outcome.value).isEqualTo(TransferOutcome.Refused(DataError.Invalid("not a backup")))
    }

    @Test
    fun anUnreadableDocument_isAStorageFailure() = runTest(dispatcher) {
        io.readText = null
        val viewModel = viewModel()

        viewModel.readBackup(context, uri)
        advanceUntilIdle()

        assertThat(viewModel.outcome.value).isEqualTo(TransferOutcome.StorageFailure)
        assertThat(repository.previewed).isEmpty()
    }

    @Test
    fun clearingEverything_answersCleared_orTheFailure() = runTest(dispatcher) {
        val viewModel = viewModel()
        assertThat(viewModel.clearEverything()).isEqualTo(ClearOutcome.Cleared)

        repository.clearResult = DataResult.Failure(DataError.Storage(IllegalStateException("db")))
        assertThat(viewModel.clearEverything()).isInstanceOf(ClearOutcome.Failed::class.java)
    }

    @Test
    fun showingTheOutcome_clearsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.writeBackup(context, uri)
        advanceUntilIdle()
        assertThat(viewModel.outcome.value).isNotNull()

        viewModel.onOutcomeShown()

        assertThat(viewModel.outcome.value).isNull()
    }

    private fun preview(problems: List<BackupProblem> = emptyList()): BackupPreview {
        val none = PreviewCounts(inFile = 0, alreadyHere = 0, restored = 0, added = 0)
        return BackupPreview(
            sessions = none,
            sets = none,
            measurements = none,
            exercises = none,
            problems = problems,
        )
    }
}

private class FakeDocumentIo : DocumentIo {
    var readText: String? = "{}"
    var writeResult = true
    var written: String? = null

    override suspend fun write(context: Context, uri: Uri, text: String): Boolean {
        if (writeResult) written = text
        return writeResult
    }

    override suspend fun read(context: Context, uri: Uri): String? = readText
}

private class FakeBackupRepository : BackupRepository {
    var exportResult: DataResult<String> = DataResult.Success("{}")
    var previewResult: DataResult<BackupPreview> = DataResult.Success(
        BackupPreview(
            sessions = PreviewCounts(inFile = 0, alreadyHere = 0, restored = 0, added = 0),
            sets = PreviewCounts(inFile = 0, alreadyHere = 0, restored = 0, added = 0),
            measurements = PreviewCounts(inFile = 0, alreadyHere = 0, restored = 0, added = 0),
            exercises = PreviewCounts(inFile = 0, alreadyHere = 0, restored = 0, added = 0),
        ),
    )
    var importResult: DataResult<ImportSummary> = DataResult.Success(ImportSummary(added = 0, restored = 0))
    var clearResult: DataResult<Unit> = DataResult.Success(Unit)

    val imported = mutableListOf<String>()
    val previewed = mutableListOf<String>()

    override suspend fun export(): DataResult<String> = exportResult

    override suspend fun preview(text: String): DataResult<BackupPreview> {
        previewed += text
        return previewResult
    }

    override suspend fun import(text: String): DataResult<ImportSummary> {
        imported += text
        return importResult
    }

    override suspend fun clearAllUserData(): DataResult<Unit> = clearResult
}
