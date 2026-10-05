package com.example.androidapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.label

/**
 * How an exercise felt, and the editor behind it (ROADMAP N8, N10, N63).
 *
 * Extracted at its second caller: the workout detail had this row, and N10 gives the
 * *active* workout the same one, because a rating that can only be given at the
 * moment an exercise is marked done is a rating given from memory. The Done prompt
 * stays as the last chance rather than the only one.
 *
 * [joints] is the picked list a new rating writes (N63); [legacyJointPain] and [legacyJointPainNote]
 * are the single number and free text a session rated before that change still carries. The legacy
 * pair is **read and shown, never rewritten and never parsed** — the summary prefers the picked list
 * when there is one, and falls back to what was recorded when there is not.
 *
 * [onRate] is called with the values the dialog collects; a caller that also
 * needs to finish the exercise (the Done prompt) keeps its own dialog for that, since
 * "save these" and "save these and close the exercise" are different acts.
 */
@Composable
fun ExerciseRatingSection(
    muscleFeel: Int?,
    joints: List<JointPain>,
    legacyJointPain: Int?,
    legacyJointPainNote: String?,
    onRate: (muscleFeel: Int?, joints: List<JointPain>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editing by remember { mutableStateOf(false) }
    val editLabel = stringResource(R.string.rating_edit_title)
    val rated = muscleFeel != null || joints.isNotEmpty() ||
        legacyJointPain != null || legacyJointPainNote != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TestTags.EXERCISE_RATING_ROW)
            .clickable(onClickLabel = editLabel) { editing = true }
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.rating_row_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = ratingSummary(muscleFeel, joints, legacyJointPain, legacyJointPainNote)
                ?: stringResource(R.string.rating_row_add),
            style = MaterialTheme.typography.bodyMedium,
            color = if (rated) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }

    if (editing) {
        ExerciseRatingDialog(
            initialMuscleFeel = muscleFeel,
            initialJoints = joints,
            isPrompt = false,
            onDismiss = { editing = false },
            onSave = { feel, picked ->
                editing = false
                onRate(feel, picked)
            },
        )
    }
}

/**
 * `Muscle feel 8 · Left knee 6/10 · Neck 3/10`, or the legacy `Joint pain 2` and where it hurt
 * (N9, N63), or null when nothing was recorded.
 *
 * The picked joints ride with the summary rather than being lines of their own: each is an aside to
 * the rating, and "left knee" means nothing on its own. The legacy text is shown as written — a
 * resource that only echoes its argument would give a translator nothing to decide — and a session
 * that has both shows the picked list, because that is the rating that was actually given.
 */
@Composable
private fun ratingSummary(
    muscleFeel: Int?,
    joints: List<JointPain>,
    legacyJointPain: Int?,
    legacyJointPainNote: String?,
): String? {
    val parts = buildList {
        muscleFeel?.let { add(stringResource(R.string.rating_muscle_value, it)) }
        if (joints.isNotEmpty()) {
            joints.forEach { add(stringResource(R.string.rating_joint_site, it.label, it.score)) }
        } else {
            legacyJointPain?.let { add(stringResource(R.string.rating_joint_value, it)) }
            legacyJointPainNote?.let { add(it) }
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
