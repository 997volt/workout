package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.BodyMeasurement
import kotlinx.coroutines.flow.Flow

/**
 * Body measurements (ROADMAP N32).
 *
 * Its own port rather than more methods on an existing one: measurements are not training, and the two
 * never read each other.
 */
interface MeasurementRepository {

    /** Every live entry, newest first. */
    fun observeAll(): Flow<List<BodyMeasurement>>

    /**
     * Writes an entry, keyed by its day.
     *
     * **Saving onto a day that already has an entry edits it** rather than adding a second: the second
     * reading of a day is nearly always a correction of the first, and "what did I weigh today" should
     * not have two answers. An entry carrying an id updates that row; one without is placed by its day.
     */
    suspend fun save(measurement: BodyMeasurement): DataResult<Unit>

    /**
     * The newest weight on record, or null when the scale was never stepped on (ROADMAP N219).
     *
     * What a bodyweight set adds to its load. Null is an answer rather than a failure, and it is the one the
     * screen turns into a prompt: a set whose load cannot be stated is not one this app writes quietly as
     * zero.
     */
    suspend fun latestWeightGrams(): DataResult<Long?>

    suspend fun delete(id: String): DataResult<Unit>

    /**
     * Puts a deleted entry back (ROADMAP N133).
     *
     * The delete is a soft one — [delete] sets `deletedAt` rather than removing the row — so the way back
     * is clearing that field, the mechanism N90's restore uses: nothing is re-inserted, and the entry
     * keeps its id and the sore rows that went with it.
     *
     * **Keyed to the write that hid it.** Only the row hidden by *this* removal comes back, so an undo
     * cannot resurrect an entry the lifter deleted deliberately a moment earlier.
     *
     * **Refused when the day has been re-entered since.** A measurement's identity is its **day and its
     * kind** (N32, N113), so restoring one onto a day and kind that now holds a newer reading would
     * silently replace that reading — the report is a [DataError.Invalid] carrying the sentence, not a
     * write.
     */
    suspend fun restore(id: String): DataResult<Unit>
}
