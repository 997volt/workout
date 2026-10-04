package com.example.androidapp.ui.workout

import com.example.androidapp.domain.model.ProgressionReason
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.ui.components.ExerciseRatingDialog
import com.example.androidapp.ui.components.ExerciseRatingSection
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.SetEditorDialog
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.restLabel
import com.example.androidapp.ui.components.rpeMarker



/**
 * One exercise's block inside the active workout, and the list that stacks them.
 *
 * Split out of `ActiveWorkoutScreen.kt` when that file reached the function ceiling:
 * the screen is the scaffold, the clock and the workout-level actions, while this is
 * everything that belongs to a single exercise — its name and small print, its sets,
 * and the Done/Reopen actions (ROADMAP N7, N8).
 */

/** How far a done exercise's sets are faded (ROADMAP N7). */
private const val DIMMED = 0.45f

/**
 * True once this exercise has written every set its plan asked for (ROADMAP N52).
 *
 * Every set rather than more than every set, because the moment the last planned one lands is the
 * moment the notice is worth reading: the next set is the extra one. A null `plannedSetCount` is
 * "no plan", which is never past it — an empty workout, or an exercise added by hand, has nothing
 * to be done with and must not be told its work is finished.
 */
internal val SessionExerciseRow.isPastPlan: Boolean
    get() = plannedSetCount != null && sets.size >= plannedSetCount

@Composable
internal fun ExerciseList(
    rows: List<SessionExerciseRow>,
    onLogSet: (String, SetEdit) -> Unit,
    onRemoveExercise: (String) -> Unit,
    /** Moves one exercise one place in the session's own order (ROADMAP N54). */
    onMoveExercise: (String, Int) -> Unit,
    onEditSet: (SetRow) -> Unit,
    onDeleteSet: (String) -> Unit,
    onFinishExercise: (String, Int?, Int?, String?) -> Unit,
    onRateExercise: (String, Int?, Int?, String?) -> Unit,
    onReopenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (String) -> Unit = {},
    onAcceptOffer: (String) -> Unit = {},
    /** Whether a rest is counted down, and the fallback its static label uses (ROADMAP N44). */
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.EXERCISE_LIST),
        // Leaves room for the extended FAB so it cannot cover the last row.
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        itemsIndexed(items = rows, key = { _, row -> row.id }) { index, row ->
            ExerciseSection(
                row = row,
                onLogSet = { edit -> onLogSet(row.id, edit) },
                onRemoveExercise = { onRemoveExercise(row.id) },
                onMoveExercise = { delta -> onMoveExercise(row.id, delta) },
                onEditSet = onEditSet,
                onDeleteSet = onDeleteSet,
                onFinishExercise = onFinishExercise,
                onRateExercise = onRateExercise,
                onReopenExercise = { onReopenExercise(row.id) },
                // Row 0 has nothing above it to pair with: with no previous exercise the group
                // comes out null and the write would rewrite every ungrouped row, churning
                // `updatedAt` for no change (ROADMAP B28).
                onToggleSuperset = if (index == 0) null else { { onToggleSuperset(row.id) } },
                onAcceptOffer = { onAcceptOffer(row.id) },
                restTimerEnabled = restTimerEnabled,
                defaultRestSeconds = defaultRestSeconds,
            )
            HorizontalDivider()
        }
    }
}

/**
 * One exercise's whole block: its name and small print, its sets, and the actions
 * that belong to it.
 *
 * ROADMAP N7 adds the third state this renders — open, or done. A done exercise
 * keeps its sets on screen, dimmed and non-editable, offers **Reopen** instead of
 * **Done**, and loses its Log set button; the wording avoids *Finish*, which is the
 * workout-level action.
 */
@Composable
private fun ExerciseSection(
    row: SessionExerciseRow,
    onLogSet: (SetEdit) -> Unit,
    onRemoveExercise: () -> Unit,
    onMoveExercise: (Int) -> Unit,
    onEditSet: (SetRow) -> Unit,
    onDeleteSet: (String) -> Unit,
    onFinishExercise: (String, Int?, Int?, String?) -> Unit,
    onRateExercise: (String, Int?, Int?, String?) -> Unit,
    onReopenExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
    onAcceptOffer: (() -> Unit)? = null,
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            ExerciseNames(
                row = row,
                restTimerEnabled = restTimerEnabled,
                defaultRestSeconds = defaultRestSeconds,
                modifier = Modifier.weight(1f),
            )
            if (row.isFinished) {
                TextButton(
                    onClick = onReopenExercise,
                    modifier = Modifier.testTag(TestTags.EXERCISE_REOPEN),
                ) {
                    Text(stringResource(R.string.active_workout_reopen))
                }
            } else {
                FinishExerciseAction(
                    exerciseId = row.id,
                    muscleFeel = row.muscleFeel,
                    jointPain = row.jointPain,
                    jointPainNote = row.jointPainNote,
                    onFinish = onFinishExercise,
                )
            }
            // The rare actions moved in here rather than sitting on the header (ROADMAP N53): the
            // header is read constantly mid-session, and a text link in every one of them cost more
            // attention than the action earned.
            ExerciseOverflow(
                row = row,
                onToggleSuperset = onToggleSuperset,
                onMove = onMoveExercise,
                onRemove = onRemoveExercise,
            )
        }

        if (row.isFinished) {
            Text(
                text = stringResource(R.string.active_workout_exercise_is_done),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.EXERCISE_FINISHED_LABEL),
            )
        }

        ExerciseSets(
            row = row,
            onLogSet = onLogSet,
            onEditSet = onEditSet,
            onDeleteSet = onDeleteSet,
            onAcceptOffer = onAcceptOffer,
        )

        // N10: the ratings can be given while the exercise is still in front of you,
        // rather than only from memory when it is marked done. The Done prompt stays
        // as the last chance rather than the only one.
        ExerciseRatingSection(
            muscleFeel = row.muscleFeel,
            jointPain = row.jointPain,
            jointPainNote = row.jointPainNote,
            onRate = { feel, pain, note -> onRateExercise(row.id, feel, pain, note) },
        )
    }
}

/**
 * The **Done** button for one exercise, and the rating prompt behind it (ROADMAP
 * N7, N8).
 *
 * Owning the dialog here keeps the transient "form is open" state next to the
 * button that opens it, the same shape as [ReadinessSection]. Saving writes the
 * ratings and then finishes; skipping finishes without them, because the whole
 * capture is optional.
 */
@Composable
private fun FinishExerciseAction(
    exerciseId: String,
    muscleFeel: Int?,
    jointPain: Int?,
    jointPainNote: String?,
    onFinish: (String, Int?, Int?, String?) -> Unit,
) {
    var rating by remember { mutableStateOf(false) }

    TextButton(
        onClick = { rating = true },
        modifier = Modifier.testTag(TestTags.EXERCISE_DONE),
    ) {
        Text(stringResource(R.string.active_workout_done_exercise))
    }

    if (rating) {
        ExerciseRatingDialog(
            initialMuscleFeel = muscleFeel,
            initialJointPain = jointPain,
            initialJointPainNote = jointPainNote.orEmpty(),
            isPrompt = true,
            onDismiss = {
                rating = false
                onFinish(exerciseId, null, null, null)
            },
            onSave = { feel, pain, note ->
                rating = false
                onFinish(exerciseId, feel, pain, note)
            },
        )
    }
}

/**
 * One exercise's rare actions, behind its own ⋮ menu (ROADMAP N53).
 *
 * *Superset with above* was a text link in every exercise header and *Delete* an icon beside *Done*.
 * Both are rarely used and the header is read constantly mid-session, so the two of them cost more
 * attention than they earned. They moved into a per-exercise overflow — the shape the workout-level
 * actions used until N42 removed the one that no longer had a reason to exist — because the action
 * moved rather than changed:
 *
 * - Delete keeps its confirmation (B2). Removing an exercise soft-deletes it *and* takes its sets out
 *   of the session, and unlike deleting a set or marking one done there is no undo to reach for, so
 *   the guard is a question rather than a way back.
 * - Pairing keeps its row-0 exclusion (B28): the caller passes a null [onToggleSuperset] for the first
 *   exercise, which has nothing above it, so the entry is simply not offered rather than writing a
 *   group that would rewrite every ungrouped row. A done exercise is out of the round as well (N7),
 *   which is why the item goes with its Log set button.
 */
@Composable
private fun ExerciseOverflow(
    row: SessionExerciseRow,
    onToggleSuperset: (() -> Unit)?,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmingRemoval by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { menuOpen = true },
            modifier = Modifier.testTag(TestTags.exerciseMenu(row.id)),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.active_workout_exercise_more, row.name),
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            // Order first: it is the entry most likely to be reached for mid-session, and it is the
            // one that changes what everything below it means (ROADMAP N54). A move at the top or the
            // bottom is refused by the repository rather than by the menu, because the row does not
            // know how long the list is and a disabled entry that looks the same is worse than one
            // that does nothing.
            DropdownMenuItem(
                text = { Text(stringResource(R.string.active_workout_move_up)) },
                onClick = {
                    menuOpen = false
                    onMove(-1)
                },
                modifier = Modifier.testTag(TestTags.exerciseMove(row.id, up = true)),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.active_workout_move_down)) },
                onClick = {
                    menuOpen = false
                    onMove(1)
                },
                modifier = Modifier.testTag(TestTags.exerciseMove(row.id, up = false)),
            )
            if (onToggleSuperset != null && !row.isFinished) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (row.supersetGroup == null) {
                                    R.string.superset_pair
                                } else {
                                    R.string.superset_unpair
                                },
                            ),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onToggleSuperset()
                    },
                    modifier = Modifier.testTag(TestTags.supersetToggle(row.id)),
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.active_workout_remove_action),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    menuOpen = false
                    confirmingRemoval = true
                },
                modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE),
            )
        }
    }

    if (confirmingRemoval) {
        AlertDialog(
            onDismissRequest = { confirmingRemoval = false },
            title = { Text(stringResource(R.string.active_workout_remove_confirm_title)) },
            text = { Text(stringResource(R.string.active_workout_remove_confirm_text, row.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingRemoval = false
                        onRemove()
                    },
                    modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE_CONFIRM),
                ) {
                    Text(stringResource(R.string.active_workout_remove_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingRemoval = false },
                    modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE_CANCEL),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

/**
 * A done exercise's sets stay on screen, dimmed and non-editable, and it loses its
 * Log set button entirely (ROADMAP N7). Split from [ExerciseSection] so the section
 * stays a header plus its two blocks rather than one long function.
 */
@Composable
private fun ExerciseSets(
    row: SessionExerciseRow,
    onLogSet: (SetEdit) -> Unit,
    onEditSet: (SetRow) -> Unit,
    onDeleteSet: (String) -> Unit,
    modifier: Modifier = Modifier,
    onAcceptOffer: (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        // Dimmed rather than hidden: the sets stay visible as a record of what was
        // done, and `editable` is what actually stops the taps.
        Column(modifier = if (row.isFinished) Modifier.alpha(DIMMED) else Modifier) {
            row.sets.forEach { set ->
                SetLine(
                    set = set,
                    editable = !row.isFinished,
                    onEdit = { onEditSet(set) },
                    onDelete = { onDeleteSet(set.id) },
                )
            }
        }

        // No Log set button once the exercise is done: that is the accident N7 exists to prevent.
        if (!row.isFinished) {
            row.suggestion.offer?.let { offer -> SuggestionOffer(offer = offer, onAccept = onAcceptOffer) }
            LogSetAction(row = row, onLogSet = onLogSet)
        }
    }
}

/**
 * The set the app offers next, as the logging dialog opens on it (ROADMAP N51).
 *
 * The role is the plan's own next unlogged one (B48), carried the way reps and weight already are,
 * so a template that opens with a ramp is logged as warm-ups rather than as working sets. The
 * resting value lives here rather than on the dialog so it re-arms from the plan the moment a set is
 * written: keyed on the logged count, logging a set re-reads the plan's next set, which is the
 * "clears itself" rule (N19) with the plan as the resting value.
 */
private data class LoggingValues(
    val role: SetType,
    val reps: Int,
    val weightGrams: Long,
    val assistanceGrams: Long,
)

/**
 * **Log set**, and the dialog it opens (ROADMAP N51).
 *
 * Logging *is* the dialog the edit path opens, prefilled from the same offer and committed on Save.
 * The one-tap path went: it decided something the user did not mean — a set that differed from the
 * prefill was written first and corrected afterwards — and the decision is that it is not wanted any
 * more rather than a cost to weigh. That inverts B7 for this button: it no longer writes the set its
 * label describes, because the label no longer describes one.
 *
 * Opening the dialog is a local state here, next to the button that opens it, and the values it opens
 * on still come from the row — so a write that lands while the dialog is open cannot desync a draft
 * from the plan it was prefilled from.
 */
@Composable
private fun LogSetAction(
    row: SessionExerciseRow,
    onLogSet: (SetEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var logging by rememberSaveable(row.id, row.sets.size) { mutableStateOf(false) }
    val values = LoggingValues(
        role = row.suggestion.setType,
        reps = row.suggestion.reps,
        weightGrams = row.suggestion.weightGrams,
        assistanceGrams = row.suggestion.assistanceGrams,
    )

    Column(modifier = modifier) {
        PlanDoneNotice(row = row)
        FilledTonalButton(
            onClick = { logging = true },
            modifier = Modifier.padding(top = 8.dp).testTag(TestTags.SET_LOG),
        ) {
            // The plan's work is done, so the label stops describing values the app no longer
            // offers a target for (ROADMAP N52). Logging an extra set is what the control still
            // does — nothing closes, and the way to end the exercise is Done.
            if (row.isPastPlan) {
                Text(stringResource(R.string.set_log_extra))
            } else {
                Text(
                    text = stringResource(
                        R.string.set_log,
                        stringResource(
                            R.string.set_summary,
                            Weight.display(values.weightGrams, values.assistanceGrams),
                            values.reps,
                        ),
                    ),
                )
            }
        }
    }

    if (logging) {
        SetEditorDialog(
            initialReps = values.reps,
            initialWeightGrams = values.weightGrams,
            initialSetType = values.role,
            initialAssistanceGrams = values.assistanceGrams,
            title = stringResource(R.string.set_log_title),
            // Tagged for this row: the flow under test is "the control opens the dialog the edit path
            // opens", and a test has to reach the role selector through the flow it actually takes.
            roleTag = TestTags.exercisePendingRole(row.id),
            roleOptionTag = { role -> TestTags.exercisePendingRole(row.id, role) },
            onDismiss = { logging = false },
            onSave = { edit ->
                logging = false
                onLogSet(edit)
            },
        )
    }
}

/**
 * The plan's work is done for this exercise (ROADMAP N52).
 *
 * Past the last planned set the exercise keeps accepting sets with nothing to say the work the plan
 * asked for is finished — `comparePlanToActual` says so only in the review, after *Finish*. This is
 * the moment it happens: the last planned set has just been written, so the notice appears beside
 * the control that would write one more.
 *
 * It is per exercise and it is a notice rather than a dialog, deliberately. N51 already puts a
 * dialog in front of every set, and a second one would interrupt the next exercise's first set; and
 * nothing closes, because accepting the notice is the header's **Done** (N7) — logging an extra set
 * is what the control still does, which is why the label changes rather than the action.
 */
@Composable
private fun PlanDoneNotice(row: SessionExerciseRow, modifier: Modifier = Modifier) {
    if (!row.isPastPlan) return

    Text(
        text = stringResource(R.string.set_plan_done),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = 8.dp)
            .testTag(TestTags.EXERCISE_PLAN_DONE),
    )
}

/**
 * The exercise's name and the small print under it: taxonomy, the technique cue
 * (ROADMAP N5) and what was lifted last time.
 *
 * Split out of [ExerciseSection] because that section has a set list and a button
 * too, and mixing the two made one function carry the whole row.
 */
@Composable
private fun ExerciseNames(
    row: SessionExerciseRow,
    modifier: Modifier = Modifier,
    /**
     * Whether a rest is counted down, and the fallback its static label uses (ROADMAP N44).
     *
     * Off is not "hide the number": the prescription is still worth reading, so it is drawn here as
     * a fact about the exercise rather than a countdown that never moves.
     */
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
) {
    Column(modifier = modifier) {
        Text(
            text = row.supersetLabel?.let { "$it · ${row.name}" } ?: row.name,
            style = MaterialTheme.typography.titleMedium,
        )
        row.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // The cue is the note you want *while* lifting, so it belongs here with the
        // name and not only on the detail screen.
        row.techniqueNote?.let { cue ->
            Text(
                text = cue,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!restTimerEnabled) {
            Text(
                text = stringResource(
                    R.string.active_workout_rest_prescription,
                    restLabel(row.restSeconds ?: defaultRestSeconds),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TestTags.EXERCISE_REST_PRESCRIPTION),
            )
        }
        row.lastTime?.let { last ->
            Text(
                text = stringResource(
                    R.string.set_last_time,
                    stringResource(
                        R.string.set_summary,
                        Weight.display(last.weightGrams, last.assistanceGrams),
                        last.reps,
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * One logged set. [editable] is false once its exercise is done (ROADMAP N7): the
 * line stops being tappable and loses its delete button, and the accessibility
 * label goes with it so a screen reader does not advertise an edit that cannot
 * happen.
 */
@Composable
private fun SetLine(
    set: SetRow,
    editable: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editLabel = stringResource(R.string.set_edit_action)
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .testTag(TestTags.SET_ROW)
                // Without a label a screen reader announces the row and gives no
                // hint that tapping it edits the set. A done exercise's sets are not
                // editable, so the label and the action both disappear (N7).
                .let { row ->
                    if (editable) {
                        row.clickable(onClickLabel = editLabel, onClick = onEdit)
                    } else {
                        row
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.set_number, set.number),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.set_summary,
                    Weight.display(set.weightGrams, set.assistanceGrams),
                    set.reps,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            SetExtrasMarker(set = set)
        }
        if (editable) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    // Names the action, not the outcome: a screen reader should
                    // announce "Delete set", not the confirmation that follows it.
                    contentDescription = stringResource(R.string.set_delete),
                )
            }
        }
    }
}

/**
 * The small marker a set carries when it has an RPE or a comment (ROADMAP N6).
 *
 * A marker, not the text: the workout row has to stay scannable mid-set, and the
 * comment itself belongs on the workout detail. Nothing is emitted when the set
 * carries neither, which is the state the one-tap log path leaves it in.
 */
@Composable
private fun SetExtrasMarker(set: SetRow, modifier: Modifier = Modifier) {
    // A working set is the default and needs no label; anything else is worth
    // showing, because the role is the thing the user chose (ROADMAP N14).
    val roleMarker = set.setType
        .takeIf { it != SetType.NORMAL }
        ?.let { stringResource(R.string.set_role_marker, it.label) }
    val rpePart = set.rpeHalves?.let { rpeMarker(it) }
    val noteMarker = if (set.note != null) stringResource(R.string.set_note_marker) else null
    val marker = listOfNotNull(roleMarker, rpePart, noteMarker).joinToString(" · ")

    if (marker.isNotEmpty()) {
        Text(
            text = marker,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
    }
}

/**
 * How the rule explains itself (ROADMAP N22).
 *
 * A number the app chose is an instruction unless it says why, and every one of these is a
 * sentence a lifter would say to themselves between sets.
 */
private fun ProgressionReason.explanationRes(): Int = when (this) {
    ProgressionReason.MORE_REPS -> R.string.suggestion_more_reps
    ProgressionReason.MORE_WEIGHT -> R.string.suggestion_more_weight
    ProgressionReason.LESS_ASSISTANCE -> R.string.suggestion_less_assistance
    // Nothing recorded yet: the plan (or nothing) is being echoed, not progressed, so there
    // is no step to explain — the line is only drawn for the three above.
    ProgressionReason.NO_HISTORY -> R.string.suggestion_more_reps
}

/**
 * The app's proposal, and the way to take it (ROADMAP N33).
 *
 * Shown with its reason and applied only when accepted — which is the whole change: while the proposal
 * *was* the prefill, the next tap logged the app's arithmetic whether or not it was wanted, and a lifter
 * who progresses by hand had to undo it on every first set.
 */
@Composable
private fun SuggestionOffer(offer: SetOffer, onAccept: (() -> Unit)? = null) {
    // One source at the top level, which is what the rule asks for and what reads better: the reason and
    // the way to take it belong together.
    Column {
    offer.reason?.let { reason ->
        Text(
            text = stringResource(reason.explanationRes()),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.testTag(TestTags.SUGGESTION_REASON),
        )
    }
    if (onAccept != null) {
        TextButton(
            onClick = onAccept,
            modifier = Modifier.testTag(TestTags.SUGGESTION_ACCEPT),
        ) {
            Text(stringResource(R.string.set_use_suggestion))
        }
    }
    }
}
