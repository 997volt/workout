package com.example.androidapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The finish's find-the-weight prompt (ROADMAP N309).
 *
 * One line per marked plan: the question, how many sets and the top load, and the two answers. **Use**
 * adopts every logged work set's load and clears the marker; **Not now** leaves the plan weightless and
 * clears it too, so the prompt is not repeated. The dialog has no dismiss of its own: each line is answered,
 * and the screen then carries the stall notice (N305) or closes. A failed write states the failure on its
 * line and offers a plain *Close* that moves on without writing (B422), so the dialog cannot strand a
 * stored workout.
 */
@Composable
internal fun CalibrationDialog(
    notices: List<CalibrationNotice>,
    onUse: (CalibrationNotice) -> Unit,
    onSkip: (CalibrationNotice) -> Unit,
    /** The plan whose answer last failed to write, with a non-writing way out (B422). */
    errorNotice: CalibrationNotice? = null,
    onClose: (CalibrationNotice) -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.testTag(TestTags.CALIBRATION_DIALOG),
        title = { Text(stringResource(R.string.calibration_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notices.forEach { notice ->
                    CalibrationNoticeLine(
                        notice = notice,
                        failed = errorNotice?.templateExerciseId == notice.templateExerciseId,
                        onUse = onUse,
                        onSkip = onSkip,
                        onClose = onClose,
                    )
                }
            }
        },

        // Every line answers itself; there is no global dismissal that would leave a marker behind.
        confirmButton = {},
        dismissButton = {},
    )
}

/** One marked plan's question, its numbers, the two answers and — after a failed write — the way out. */
@Composable
private fun CalibrationNoticeLine(
    notice: CalibrationNotice,
    failed: Boolean,
    onUse: (CalibrationNotice) -> Unit,
    onSkip: (CalibrationNotice) -> Unit,
    onClose: (CalibrationNotice) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.calibration_question, notice.exerciseName),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = calibrationDetail(notice),
            style = MaterialTheme.typography.bodySmall,
        )
        if (failed) {
            Text(
                text = stringResource(R.string.calibration_write_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        CalibrationAnswers(notice, failed, onUse, onSkip, onClose)
    }
}

/** The count and the top load, or only the count when there is no load to state (B450). */
@Composable
private fun calibrationDetail(notice: CalibrationNotice): String {
    val bodyweight = notice.topWeightGrams == 0L && notice.topAssistanceGrams == 0L
    if (bodyweight) {
        return pluralStringResource(
            R.plurals.calibration_detail_no_load,
            notice.setCount,
            notice.setCount,
        )
    }
    val unitLabel = stringResource(
        if (notice.weightUnit == WeightUnit.KILOGRAMS) R.string.suggestion_unit_kg else R.string.suggestion_unit_lb,
    )
    return pluralStringResource(
        R.plurals.calibration_detail,
        notice.setCount,
        notice.setCount,
        Weight.display(notice.topWeightGrams, notice.topAssistanceGrams, notice.weightUnit) + " " + unitLabel,
    )
}

/** One lift's buttons, each tagged and named for that lift (B447). */
@Composable
private fun CalibrationAnswers(
    notice: CalibrationNotice,
    failed: Boolean,
    onUse: (CalibrationNotice) -> Unit,
    onSkip: (CalibrationNotice) -> Unit,
    onClose: (CalibrationNotice) -> Unit,
) {
    val skipDescription = stringResource(R.string.calibration_skip_for, notice.exerciseName)
    val useDescription = stringResource(R.string.calibration_use_for, notice.exerciseName)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextButton(
                onClick = { onSkip(notice) },
                modifier = Modifier
                    .testTag(TestTags.calibrationSkip(notice.templateExerciseId))
                    .semantics { contentDescription = skipDescription },
            ) {
                Text(stringResource(R.string.calibration_skip))
            }
            AppTextButton(
                onClick = { onUse(notice) },
                modifier = Modifier
                    .testTag(TestTags.calibrationUse(notice.templateExerciseId))
                    .semantics { contentDescription = useDescription },
            ) {
                Text(stringResource(R.string.calibration_use))
            }
        }
        if (failed) {
            val closeDescription = stringResource(R.string.calibration_close_for, notice.exerciseName)
            AppTextButton(
                onClick = { onClose(notice) },
                modifier = Modifier
                    .testTag(TestTags.calibrationClose(notice.templateExerciseId))
                    .semantics { contentDescription = closeDescription },
            ) {
                Text(stringResource(R.string.calibration_close))
            }
        }
    }
}
