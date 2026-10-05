package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
 * One dialog, opened where the lifter reaches for it: the exercise's own rating row on the workout
 * screen, and the same row on the workout detail. *Done* does not open it, which is why there is no
 * prompt wording left to vary — the title and the secondary button read the same wherever it is
 * reached from.
 *
 * Both halves are steppers on the same 1–10 scale, because neither can be anything else: muscle feel is
 * one number about the whole exercise (N8), and the joint half is a picked list — a joint and, for the
 * paired ones, which side, each with its own score (N63). Muscle feel starts at [DEFAULT_MUSCLE_FEEL]
 * when nothing was recorded: a stepper always shows a number, so the point it starts on is also what
 * *Save* records if the lifter never touches it. The single joint-pain field and its note box are gone;
 * a session rated before them keeps the number and the text, which history still reads.
 */
@Composable
fun ExerciseRatingDialog(
    initialMuscleFeel: Int?,
    initialJoints: List<JointPain>,
    onDismiss: () -> Unit,
    onSave: (muscleFeel: Int?, joints: List<JointPain>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var muscleFeel by rememberSaveable { mutableStateOf(initialMuscleFeel ?: DEFAULT_MUSCLE_FEEL) }
    // `rememberSaveable` for the same reason the stepper is: a rotation mid-edit must not throw away
    // the joints already picked.
    var joints by rememberSaveable(stateSaver = JointsSaver) { mutableStateOf(initialJoints) }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.rating_edit_title))
        },
        text = {
            RatingFields(
                muscleFeel = muscleFeel,
                onMuscleChange = { muscleFeel = it },
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
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** The muscle-feel stepper and the joint list under it, split out so the dialog reads as a dialog. */
@Composable
private fun RatingFields(
    muscleFeel: Int,
    onMuscleChange: (Int) -> Unit,
    joints: List<JointPain>,
    onAddJoint: (JointSite) -> Unit,
    onJointScore: (String, Int) -> Unit,
    onRemoveJoint: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuscleFeelRow(feel = muscleFeel, onFeelChange = onMuscleChange)
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

/**
 * Muscle feel as a −/+ stepper (ROADMAP N8), the shape the scored picks already use (N62, N63).
 *
 * The ends carry their meaning underneath (N12): the number is chosen here, so "1 = barely worked,
 * 10 = fully worked" is what keeps a 7 this month comparable to a 7 next month. The buttons stop at
 * the ends rather than wrapping, so the scale cannot be left by holding one down.
 */
@Composable
private fun MuscleFeelRow(
    feel: Int,
    onFeelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.rating_muscle_label),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onFeelChange(feel - 1) },
                enabled = feel > TenPointScale.MIN,
                modifier = Modifier.testTag(TestTags.RATING_MUSCLE_DECREASE),
            ) {
                Icon(
                    imageVector = Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.rating_muscle_decrease),
                )
            }
            Text(
                text = stringResource(R.string.scored_pick_score, feel),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.testTag(TestTags.RATING_MUSCLE_FIELD),
            )
            IconButton(
                onClick = { onFeelChange(feel + 1) },
                enabled = feel < TenPointScale.MAX,
                modifier = Modifier.testTag(TestTags.RATING_MUSCLE_INCREASE),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.rating_muscle_increase),
                )
            }
        }
        Text(
            text = stringResource(R.string.rating_muscle_anchors),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/**
 * The muscle feel a rating starts on when nothing was recorded (ROADMAP N8).
 *
 * A stepper always shows a number, so this is also what *Save* records when the lifter never touches
 * it — which is why it is not the middle of the scale: 7 is the working end of it, a set worked hard
 * without being taken to failure.
 */
internal const val DEFAULT_MUSCLE_FEEL = 7
