package com.example.androidapp.ui.statistics

import com.example.androidapp.domain.DurationFormat
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.RecordEvent
import com.example.androidapp.domain.model.storedZoneOrFallback
import java.time.YearMonth
import java.time.ZoneId

/** One month of records, newest month first and each month's rows newest first (ROADMAP N290). */
internal data class RecordMonth(
    val month: YearMonth,
    val records: List<RecordEvent>,
)

/**
 * The records timeline's pure half: which month a record belongs to, how the months are ordered, and what
 * its value reads as.
 *
 * Pure and explicit about [ZoneId], because a record is grouped by the month it was *performed* in — a
 * record set in Tokyo on the 1st belongs to that month even when it is read a day earlier in London
 * (ROADMAP N25) — and because a formatter that quietly read the machine's defaults could not be tested.
 */
internal object RecordTimelineFormat {

    /** The zone the record was performed in, or [fallback] for a row that predates the column (N25). */
    fun zoneOf(record: RecordEvent, fallback: ZoneId): ZoneId =
        storedZoneOrFallback(record.zoneOffsetMinutes, fallback)

    /**
     * The records as months, newest first.
     *
     * The list is sorted rather than assumed sorted: the query already orders by performance, and a pure
     * function that repeated that promise without keeping it would be a second place for the order to be
     * wrong. Sorting is stable, so records performed in the same session keep the query's within-session
     * order (the later set first), which is the order the record rule calls "later".
     */
    fun months(records: List<RecordEvent>, fallbackZone: ZoneId): List<RecordMonth> =
        records
            .sortedByDescending { it.performedAt }
            .groupBy { YearMonth.from(it.performedAt.atZone(zoneOf(it, fallbackZone))) }
            .toSortedMap(compareByDescending { it })
            .map { (month, inMonth) -> RecordMonth(month = month, records = inMonth) }

    /**
     * A weighted record as the list states it: `100 kg × 5`.
     *
     * The unit label travels as an argument rather than being resolved here, because a translation may put
     * it somewhere else in the sentence and a pure function may not read a string resource.
     */
    fun loadText(record: RecordEvent.Load, unit: WeightUnit, unitLabel: String): String =
        "${Weight.format(record.weightGrams, unit)} $unitLabel × ${record.reps}"

    /** A hold as the list states it: `0:45`, the app's one elapsed-time format. */
    fun holdText(record: RecordEvent.Hold): String =
        DurationFormat.ofSeconds(record.seconds.toLong())
}
