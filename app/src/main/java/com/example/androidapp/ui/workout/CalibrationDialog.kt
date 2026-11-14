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
 * and the screen then carries the stall notice (N305) or closes.
 */
@Composable
internal fun CalibrationDialog(
    notices: List<CalibrationNotice>,
    onUse: (CalibrationNotice) -> Unit,
    onSkip: (CalibrationNotice) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        modifier = Modifier.testTag(TestTags.CALIBRATION_DIALOG),
        title = { Text(stringResource(R.string.calibration_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notices.forEach { notice ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.calibration_question, notice.exerciseName),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = pluralStringResource(
                                R.plurals.calibration_detail,
                                notice.setCount,
                                notice.setCount,
                                Weight.display(
                                    notice.topWeightGrams,
                                    notice.topAssistanceGrams,
                                    notice.weightUnit,
                                ) + " " + stringResource(
                                    if (notice.weightUnit == WeightUnit.KILOGRAMS) {
                                        R.string.suggestion_unit_kg
                                    } else {
                                        R.string.suggestion_unit_lb
                                    },
                                ),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppTextButton(
                                onClick = { onSkip(notice) },
                                modifier = Modifier.testTag(TestTags.CALIBRATION_SKIP),
                            ) {
                                Text(stringResource(R.string.calibration_skip))
                            }
                            AppTextButton(
                                onClick = { onUse(notice) },
                                modifier = Modifier.testTag(TestTags.CALIBRATION_USE),
                            ) {
                                Text(stringResource(R.string.calibration_use))
                            }
                        }
                    }
                }
            }
        },
        // Every line answers itself; there is no global dismissal that would leave a marker behind.
        confirmButton = {},
        dismissButton = {},
    )
}
