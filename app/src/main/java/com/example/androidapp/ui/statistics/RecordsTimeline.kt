package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.RecordEvent
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.history.HistoryFormat
import java.time.ZoneId

/**
 * The records timeline (ROADMAP N290).
 *
 * The app already celebrated a record at the moment it happened and can say a lift's best; this is the
 * story between those two: what was first, what was beaten, and what was held longer. The rows are the
 * records the DAO judged — this section does not decide what one is — grouped by the month they were
 * performed in and newest first.
 *
 * **Bounded, and titled as such**: the read takes the newest [RECORDS_SHOWN] records rather than the whole
 * history, so the heading says *recent* rather than promising a complete ledger. An empty list is a real
 * answer, not a failed read, and says what would make one appear — the same rule the history list's empty
 * state follows.
 */
@Composable
internal fun RecordsTimeline(records: List<RecordEvent>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().testTag(RecordTimelineTags.TIMELINE)) {
        SectionHeader(text = stringResource(R.string.statistics_records_title))
        if (records.isEmpty()) {
            EmptyRecords()
        } else {
            // The fallback for a row written before the session recorded its zone (ROADMAP N25). Read when
            // the list is drawn rather than frozen in a companion, because the device may have moved.
            val zone = ZoneId.systemDefault()
            RecordTimelineFormat.months(records, zone).forEachIndexed { monthIndex, group ->
                SectionHeader(
                    text = HistoryFormat.month(group.month),
                    modifier = Modifier.testTag(RecordTimelineTags.month(monthIndex)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    group.records.forEachIndexed { index, record ->
                        RecordRow(
                            record = record,
                            zone = zone,
                            testTag = RecordTimelineTags.row(monthIndex, index),
                        )
                    }
                }
            }
        }
    }
}

/**
 * What makes a record, in the empty state's own words.
 *
 * The section is not hidden when it is empty: a reader who has just set a first record needs to know the
 * feature exists, and a heading over nothing is worse than a heading that explains itself.
 */
@Composable
private fun EmptyRecords() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .testTag(RecordTimelineTags.EMPTY),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_records_empty),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.statistics_records_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One record: the lift it belongs to, what it did, and the day it did it. */
@Composable
private fun RecordRow(record: RecordEvent, zone: ZoneId, testTag: String) {
    val date = HistoryFormat.date(record.performedAt, zone = RecordTimelineFormat.zoneOf(record, zone))
    // The value is the sentence's subject, so it comes before the date; the date is what the month above
    // only narrows to a month. A hold says "Held", because "0:45" alone does not say what kind of number it
    // is.
    val value = when (record) {
        is RecordEvent.Load -> {
            val unit = exerciseWeightUnit(record.weightUnit)
            RecordTimelineFormat.loadText(record, unit, unit.label())
        }

        is RecordEvent.Hold -> stringResource(
            R.string.statistics_records_hold,
            RecordTimelineFormat.holdText(record),
        )
    }
    AppRow(
        headline = record.exerciseName,
        supporting = "$value · $date",
        testTag = testTag,
    )
}

/**
 * How many records the timeline holds (ROADMAP N290).
 *
 * Fifty is a year of weekly records and several years of a quiet lifter's; the point is that the screen
 * never loads the whole training history to draw a section. `Recent records` is the honest name for a
 * list with this bound.
 */
internal const val RECORDS_SHOWN = 50
