package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.JOINT_SITES
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.Side
import com.example.androidapp.domain.model.TenPointScale
import com.example.androidapp.domain.model.jointSiteKey
import com.example.androidapp.domain.model.label
import com.example.androidapp.domain.model.jointSiteLabel

/**
 * How an exercise felt — muscle feel and joint pain (ROADMAP N8, N63).
 *
 * One dialog serves the prompt shown when an exercise is marked done and the edit
 * reached from the workout detail; [isPrompt] only changes the wording and whether
 * the secondary button reads *Skip* or *Cancel*.
 *
 * The muscle-feel number is unchanged: it is one number about the whole exercise, which is what
 * "how well was the target muscle worked" is. The joint half is a picked list — a joint and, for
 * the paired ones, which side, each with its own 1–10 score (N63) — because a bare number with a
 * free-text location cannot be read back one joint at a time later. The single joint-pain field and
 * its note box are gone; a session rated before them keeps the number and the text, which history
 * still reads.
 *
 * The muscle field is optional — the whole capture is skippable, which is what the Skip button says
 * — and a value outside 1–10 keeps Save disabled rather than being clamped, since a silent 11 → 10
 * would misstate the session. A picked joint's score can only be stepped between 1 and 10.
 */
@Composable
fun ExerciseRatingDialog(
    initialMuscleFeel: Int?,
    initialJoints: List<JointPain>,
    isPrompt: Boolean,
    onDismiss: () -> Unit,
    onSave: (muscleFeel: Int?, joints: List<JointPain>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var muscleText by rememberSaveable { mutableStateOf(initialMuscleFeel?.toString().orEmpty()) }
    // `rememberSaveable` for the same reason the field is: a rotation mid-edit must not throw away
    // the joints already picked.
    var joints by rememberSaveable(stateSaver = JointsSaver) { mutableStateOf(initialJoints) }

    val muscleFeel = muscleText.trim().ifEmpty { null }?.toIntOrNull()
    // Blank is valid; anything typed has to parse *and* sit on the scale.
    val muscleIsValid = muscleText.isBlank() || (muscleFeel != null && TenPointScale.isValid(muscleFeel))

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isPrompt) R.string.rating_prompt_title else R.string.rating_edit_title,
                ),
            )
        },
        text = {
            RatingFields(
                muscleText = muscleText,
                onMuscleChange = { muscleText = it },
                muscleIsValid = muscleIsValid,
                joints = joints,
                onAddJoint = { site ->
                    joints = joints + JointPain(site.joint, site.side, DEFAULT_SCORED_PICK)
                },
                onJointScore = { key, score ->
                    joints = joints.map {
                        if (jointSiteKey(it.joint, it.side) == key) it.copy(score = score) else it
                    }
                },
                onRemoveJoint = { key ->
                    joints = joints.filterNot { jointSiteKey(it.joint, it.side) == key }
                },
            )
        },
        confirmButton = {
            AppTextButton(
                modifier = Modifier.testTag(TestTags.RATING_SAVE),
                enabled = muscleIsValid,
                onClick = { onSave(muscleFeel, joints) },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(
                modifier = Modifier.testTag(TestTags.RATING_DISMISS),
                onClick = onDismiss,
            ) {
                Text(
                    stringResource(
                        if (isPrompt) R.string.rating_skip else R.string.action_cancel,
                    ),
                )
            }
        },
    )
}

/** The muscle-feel field and the joint list under it, split out so the dialog reads as a dialog. */
@Composable
private fun RatingFields(
    muscleText: String,
    onMuscleChange: (String) -> Unit,
    muscleIsValid: Boolean,
    joints: List<JointPain>,
    onAddJoint: (JointSite) -> Unit,
    onJointScore: (String, Int) -> Unit,
    onRemoveJoint: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = muscleText,
            onValueChange = onMuscleChange,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.RATING_MUSCLE_FIELD),
            singleLine = true,
            label = { Text(stringResource(R.string.rating_muscle_label)) },
            // The anchors, not "Optional": the Skip button already says the rating
            // can be left alone, and this line is the only place the scale can be
            // explained while the number is being picked (ROADMAP N12).
            supportingText = { Text(stringResource(R.string.rating_muscle_anchors)) },
            isError = muscleText.isNotBlank() && !muscleIsValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        // N9's location box became N63's picked list: every joint a lift may name, left and right
        // apart, each with its own score. The shared editor (N62's second caller) owns the
        // interaction, so a stepped score and a removal behave the same here as on the readiness note.
        ScoredPicksEditor(
            picked = joints.map {
                ScoredPick(jointSiteKey(it.joint, it.side), it.label, it.score)
            },
            options = JOINT_SITES.map { (joint, side) ->
                ScoredPick(jointSiteKey(joint, side), jointSiteLabel(joint, side), DEFAULT_SCORED_PICK)
            },
            tags = ScoredPickTags(RATING_JOINT_TAG_PREFIX),
            strings = ScoredPickStrings(
                title = stringResource(R.string.rating_joint_list_title),
                add = stringResource(R.string.rating_joint_add),
                decrease = { stringResource(R.string.rating_joint_decrease, it) },
                increase = { stringResource(R.string.rating_joint_increase, it) },
                remove = { stringResource(R.string.rating_joint_remove, it) },
            ),
            onAdd = { key -> onAddJoint(JointSite.byKey(key)) },
            onScore = onJointScore,
            onRemove = onRemoveJoint,
        )
    }
}

/** One joint a rating may name, with the identity its test tag and callbacks use (ROADMAP N63). */
private data class JointSite(val joint: Joint, val side: Side) {
    companion object {
        /** Resolves a [jointSiteKey] back to its pair; the picker only ever offers keys built above. */
        fun byKey(key: String): JointSite {
            val (joint, side) = JOINT_SITES.first { jointSiteKey(it.first, it.second) == key }
            return JointSite(joint, side)
        }
    }
}

/**
 * Saves the picked list as `JOINT:SIDE:score` strings — the shape saved instance state can hold.
 *
 * A name that no longer resolves and a score that no longer parses are both dropped rather than
 * guessed at: this is a restoration, and inventing a joint would write something nobody picked.
 */
private val JointsSaver = listSaver<List<JointPain>, String>(
    save = { joints -> joints.map { "${it.joint.name}:${it.side.name}:${it.score}" } },
    restore = { saved ->
        saved.mapNotNull { entry ->
            val parts = entry.split(":")
            val joint = Joint.entries.firstOrNull { it.name == parts.getOrNull(0) }
            val side = Side.entries.firstOrNull { it.name == parts.getOrNull(1) }
            val score = parts.getOrNull(2)?.toIntOrNull()
            if (joint == null || side == null || score == null) null else JointPain(joint, side, score)
        }
    },
)
