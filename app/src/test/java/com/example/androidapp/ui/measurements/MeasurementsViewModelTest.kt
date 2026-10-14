package com.example.androidapp.ui.measurements

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.repository.MeasurementRepository
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.example.androidapp.domain.model.MeasurementKind

/** What the measurements screen's ViewModel owns: passing an entry on, and reporting what came back. */
@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val entries = MutableStateFlow<List<BodyMeasurement>>(emptyList())

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun itShowsWhatTheRepositoryHolds_newestFirst() = runTest(dispatcher) {
        entries.value = listOf(entry("m2", weight = 82_000L), entry("m1", weight = 83_000L))
        val viewModel = MeasurementsViewModel(FakeMeasurementRepository(entries))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(
            listOf("m2", "m1"),
            viewModel.uiState.value.entries.map { it.id },
        )
    }

    @Test
    fun saving_passesTheEntryThrough_andClearsTheError() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(entries)
        repository.failNextSave = true
        val viewModel = MeasurementsViewModel(repository)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("", weight = 82_000L))
        advanceUntilIdle()
        assertTrue(
            "a failure is surfaced",
            viewModel.error.value is DataError.Storage,
        )

        repository.failNextSave = false
        viewModel.onSave(entry("", weight = 82_000L))
        advanceUntilIdle()
        assertNull("and cleared by the next save that works", viewModel.error.value)
    }

    @Test
    fun dismissingTheError_clearsIt() = runTest(dispatcher) {
        val repository = FakeMeasurementRepository(entries)
        repository.failNextSave = true
        val viewModel = MeasurementsViewModel(repository)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onSave(entry("", weight = 82_000L))
        advanceUntilIdle()
        assertTrue(viewModel.error.value is DataError.Storage)

        viewModel.onDismissError()

        assertNull("dismissing clears it, so the snackbar is not left owed", viewModel.error.value)
    }

    @Test
    fun deleting_offersTheEntryBack() = runTest(dispatcher) {
        // N133: what the snackbar's Undo puts back is the entry itself, read before the write — after it
        // the row is not in the list, and the message has to name what went.
        entries.value = listOf(entry("m1", weight = 82_000L))
        val viewModel = MeasurementsViewModel(FakeMeasurementRepository(entries))
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()

        assertEquals("m1", viewModel.pendingUndo.value?.id)
    }

    @Test
    fun undo_restoresTheEntry_itOffered() = runTest(dispatcher) {
        entries.value = listOf(entry("m1", weight = 82_000L))
        val repository = FakeMeasurementRepository(entries)
        val viewModel = MeasurementsViewModel(repository)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()
        viewModel.onUndoDelete()
        advanceUntilIdle()

        assertEquals("the undo reached the row the delete took", "m1", repository.restored)
        assertNull("and the offer is taken rather than left standing", viewModel.pendingUndo.value)
    }

    @Test
    fun aRefusedUndo_saysWhy_ratherThanFailingSilently() = runTest(dispatcher) {
        // The collision N133 names: the day was re-entered, so the restore is refused with a sentence.
        entries.value = listOf(entry("m1", weight = 82_000L))
        val repository = FakeMeasurementRepository(entries)
        repository.failNextRestore = true
        val viewModel = MeasurementsViewModel(repository)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()
        viewModel.onUndoDelete()
        advanceUntilIdle()

        assertEquals(
            "the refusal is the sentence, which the snackbar shows",
            DataError.Invalid("That day has a newer daily measurement."),
            viewModel.error.value,
        )
    }

    @Test
    fun dismissingTheUndo_dropsTheOffer_withoutWritingAnything() = runTest(dispatcher) {
        entries.value = listOf(entry("m1", weight = 82_000L))
        val repository = FakeMeasurementRepository(entries)
        val viewModel = MeasurementsViewModel(repository)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        advanceUntilIdle()

        viewModel.onDelete("m1")
        advanceUntilIdle()
        viewModel.onDismissUndo()

        assertNull(viewModel.pendingUndo.value)
        assertNull("taking the message is not a restore", repository.restored)
    }

    private fun entry(id: String, weight: Long) = BodyMeasurement(
        id = id,
        measuredAt = Instant.parse("2026-10-02T08:00:00Z"),
        kind = MeasurementKind.DAILY,
        weightGrams = weight,
    )
}

private class FakeMeasurementRepository(
    private val entries: MutableStateFlow<List<BodyMeasurement>>,
) : MeasurementRepository {

    var failNextSave = false
    var failNextRestore = false

    /** What the repository was asked to delete, so a test can read the write back. */
    var deleted: String? = null

    /** What the repository was asked to restore (ROADMAP N133). */
    var restored: String? = null

    override fun observeAll(): Flow<List<BodyMeasurement>> = entries

    override suspend fun save(measurement: BodyMeasurement): DataResult<Unit> {
        if (failNextSave) return DataResult.Failure(DataError.Storage(IllegalStateException()))
        entries.value = listOf(measurement)
        return DataResult.Success(Unit)
    }

    override suspend fun delete(id: String): DataResult<Unit> {
        deleted = id
        entries.value = entries.value.filterNot { it.id == id }
        return DataResult.Success(Unit)
    }

    override suspend fun restore(id: String): DataResult<Unit> {
        restored = id
        if (failNextRestore) {
            return DataResult.Failure(DataError.Invalid("That day has a newer daily measurement."))
        }
        return DataResult.Success(Unit)
    }
}
