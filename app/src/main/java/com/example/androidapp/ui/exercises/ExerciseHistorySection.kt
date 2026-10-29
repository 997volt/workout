package com.example.androidapp.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DurationFormat
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.PersonalRecords
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.setSummaryText
import com.example.androidapp.ui.history.HistoryFormat
import java.time.ZoneId

/**
 * What this movement's record says: when it was last trained, what that session held, and the best it has
 * been (ROADMAP N233).
 *
 * The section the screen used to apologise for. Two blocks rather than one list, because they answer two
 * questions a lifter asks in this order — "when did I last do this, and what did I do" then "what is my
 * best" — and merging them would make the second read as another session.
 *
 * **A hold is a record in time, not in kilos** (N220, N233): a timed movement's best is the longest one, and
 * it is drawn beside the rep list rather than instead of it, because a carry can hold both a load and a
 * duration.
 */
@Composable
internal fun ExerciseHistorySection(
    history: ExerciseHistoryUi,
    /**
     * The unit the weights read in: the exercise's own, else the app's (ROADMAP N64).
     *
     * Resolved by the screen rather than carried in [history], because it is a display fact the rest of the
     * screen already resolves from the same place — a second copy in the view model would be a second answer.
     */
    unit: WeightUnit,
    error: DataError?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().testTag(TestTags.EXERCISE_HISTORY)) {
        Text(
            text = stringResource(R.string.exercise_history_title),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
        )
        when {
            // A read that did not happen says so rather than drawing an empty section, which would read as
            // "never trained" — a claim about the training rather than about the read.
            error != null -> Text(
                text = dataErrorMessage(error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(TestTags.EXERCISE_HISTORY_ERROR),
            )

            history.lastTrainedAt == null -> Text(
                text = stringResource(R.string.exercise_history_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.EXERCISE_HISTORY_EMPTY),
            )

            else -> {
                LastTrainedBlock(history = history, unit = unit)
                if (!history.records.isEmpty) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    BestBlock(records = history.records, unit = unit)
                }
            }
        }
    }
}

/**
 * The newest session: its date, then the working sets it holds.
 *
 * The date is read in the **device's own zone**, because this screen has no session of its own to take an
 * offset from — the same rule history follows for a workout that predates stored offsets (ROADMAP B38).
 */
@Composable
private fun LastTrainedBlock(history: ExerciseHistoryUi, unit: WeightUnit) {
    val at = history.lastTrainedAt ?: return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(
                R.string.exercise_history_last_trained,
                HistoryFormat.date(at, zone = ZoneId.systemDefault()),
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag(TestTags.EXERCISE_HISTORY_LAST_TRAINED),
        )
        history.lastSets.forEach { set ->
            Text(
                text = setSummaryText(
                    weightGrams = set.weightGrams,
                    assistanceGrams = set.assistanceGrams,
                    bodyweightGrams = set.bodyweightGrams,
                    durationSeconds = set.durationSeconds,
                    reps = set.reps,
                    unit = unit,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The best at each rep count, then the longest hold where there is one.
 *
 * A counted movement reads as `5 reps · 100 kg`, in the unit it is measured in; a hold reads as a length of
 * time, because "a 45-second carry" is the sentence and a rep count would be a number nobody logged.
 */
@Composable
private fun BestBlock(records: PersonalRecords, unit: WeightUnit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = stringResource(R.string.exercise_history_best),
            style = MaterialTheme.typography.bodyMedium,
        )
        // Ascending by rep count, which is the order a record list has always been read in.
        records.bestByReps.toSortedMap().forEach { (reps, grams) ->
            Text(
                text = pluralStringResource(
                    R.plurals.exercise_history_best_row,
                    reps,
                    reps,
                    Weight.display(grams, 0L, unit),
                    unit.label(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        records.longestHoldSeconds?.let { seconds ->
            Text(
                text = stringResource(
                    R.string.exercise_history_best_hold,
                    DurationFormat.ofSeconds(seconds.toLong()),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.EXERCISE_HISTORY_BEST_HOLD),
            )
        }
    }
}
