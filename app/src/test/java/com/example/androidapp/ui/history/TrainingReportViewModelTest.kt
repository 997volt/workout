package com.example.androidapp.ui.history

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TrainingExercise
import com.example.androidapp.domain.model.TrainingReport
import com.example.androidapp.domain.model.TrainingSession
import com.example.androidapp.domain.model.TrainingSet
import com.example.androidapp.domain.repository.TrainingBlockChoice
import com.example.androidapp.domain.repository.TrainingReportRepository
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
import java.time.Instant
import java.time.LocalDate

/** The block sheet's state: one report, one CSV, one download (N331). */
@RunWith(AndroidJUnit4::class)
class TrainingReportViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun report() = TrainingReport(
        blockName = "Block",
        blockStart = LocalDate.of(2026, 9, 4),
        blockEnd = LocalDate.of(2026, 10, 16),
        blockComplete = true,
        exportedAt = Instant.parse("2026-10-17T10:00:00Z"),
        unit = WeightUnit.KILOGRAMS,
        sessions = listOf(
            TrainingSession(
                id = "s1",
                workout = "Push",
                startedAt = Instant.parse("2026-10-01T08:00:00Z"),
                finishedAt = Instant.parse("2026-10-01T09:00:00Z"),
                exercises = listOf(
                    TrainingExercise(
                        name = "Bench Press",
                        sets = listOf(
                            TrainingSet(
                                setIndex = 0,
                                performed = true,
                                role = SetType.NORMAL,
                                unit = WeightUnit.KILOGRAMS,
                                reps = 5,
                                weightGrams = 100_000L,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    private class FakeRepository(
        var result: DataResult<TrainingReport>,
        private val blocks: DataResult<List<TrainingBlockChoice>> = DataResult.Success(emptyList()),
    ) : TrainingReportRepository {
        override suspend fun lastBlock() = result
        override suspend fun block(programId: String) = result
        override suspend fun range(start: LocalDate, end: LocalDate) = result
        override suspend fun recentBlocks() = blocks
    }

    private class FakeDocumentIo(private val writes: Boolean = true) : DocumentIo {
        var written: String? = null
        override suspend fun write(context: Context, uri: Uri, text: String): Boolean {
            written = text
            return writes
        }

        override suspend fun read(context: Context, uri: Uri): String? = null
    }

    @Test
    fun opening_loadsTheBlock_andShareStagesTheOneCsv() = runTest(dispatcher) {
        val repository = FakeRepository(
            result = DataResult.Success(report()),
            blocks = DataResult.Success(
                listOf(TrainingBlockChoice("p1", "Block", LocalDate.of(2026, 9, 4))),
            ),
        )
        val viewModel = TrainingReportViewModel(
            repository = repository,
            documentIo = FakeDocumentIo(),
            timeSource = TimeSource { Instant.parse("2026-10-17T10:00:00Z") },
        )

        viewModel.open()
        advanceUntilIdle()

        assertThat(viewModel.state.value.report?.blockName).isEqualTo("Block")
        assertThat(viewModel.state.value.blocks).hasSize(1)

        viewModel.share()
        val file = viewModel.state.value.shareFile
        assertThat(file).isNotNull()
        assertThat(file!!.name).startsWith("workout-block-")
        assertThat(file.csv).startsWith("record_type,")
        assertThat(file.csv).contains("block,Block")

        viewModel.onShareFinished(true)
        assertThat(viewModel.state.value.shareFile).isNull()
    }

    @Test
    fun downloading_writesTheCsv_andSaysItDid() = runTest(dispatcher) {
        val io = FakeDocumentIo()
        val viewModel = TrainingReportViewModel(
            repository = FakeRepository(DataResult.Success(report())),
            documentIo = io,
            timeSource = TimeSource { Instant.parse("2026-10-17T10:00:00Z") },
        )
        viewModel.open()
        advanceUntilIdle()

        viewModel.download(context, Uri.parse("content://test/block.csv"))
        advanceUntilIdle()

        assertThat(io.written).startsWith("record_type,")
        assertThat(viewModel.state.value.outcome).isEqualTo(TrainingReportOutcome.WRITTEN)
    }

    @Test
    fun aFailedChange_keepsTheOldReportOutOfReach() = runTest(dispatcher) {
        // N338: a failed Change leaves the previous report in state so the sheet can retry, but the
        // error line describes the block that was asked for — the old one must not be downloadable.
        val repository = FakeRepository(DataResult.Success(report()))
        val io = FakeDocumentIo()
        val viewModel = TrainingReportViewModel(
            repository = repository,
            documentIo = io,
            timeSource = TimeSource { Instant.parse("2026-10-17T10:00:00Z") },
        )
        viewModel.open()
        advanceUntilIdle()
        assertThat(viewModel.state.value.canWrite).isTrue()

        repository.result = DataResult.Failure(DataError.Storage(IllegalStateException("boom")))
        viewModel.choose("p-other")
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.report).isNotNull()
        assertThat(viewModel.state.value.canWrite).isFalse()

        viewModel.share()
        assertThat(viewModel.state.value.shareFile).isNull()
        viewModel.download(context, Uri.parse("content://test/block.csv"))
        advanceUntilIdle()
        assertThat(io.written).isNull()
    }

    @Test
    fun aFailedLoad_saysSo_andOffersNoFile() = runTest(dispatcher) {
        val viewModel = TrainingReportViewModel(
            repository = FakeRepository(DataResult.Failure(DataError.Storage(IllegalStateException("boom")))),
            documentIo = FakeDocumentIo(),
            timeSource = TimeSource { Instant.parse("2026-10-17T10:00:00Z") },
        )

        viewModel.open()
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.canWrite).isFalse()
    }
}
