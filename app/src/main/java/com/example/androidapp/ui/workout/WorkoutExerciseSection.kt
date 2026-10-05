package com.example.androidapp.ui.workout

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
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.ProgressionDirection
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.ui.components.DEFAULT_RPE_HALVES
import com.example.androidapp.ui.components.ExerciseRatingSection
import com.example.androidapp.ui.components.ProgressionDialog
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.SetEntryDraft
import com.example.androidapp.ui.components.SetEntryNumbers
import com.example.androidapp.ui.components.SetRoleSelector
import com.example.androidapp.ui.components.SetRpeField
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.restLabel
import com.example.androidapp.ui.components.rpeMarker
import com.example.androidapp.ui.components.toEdit
import com.example.androidapp.ui.components.values
import com.example.androidapp.ui.components.AppTextButton



/**
 * One exercise's block inside the active workout, and the list that stacks them.
 *
 * Split out of `ActiveWorkoutScreen.kt` when that file reached the function ceiling:
 * the screen is the scaffold, the clock and the workout-level actions, while this is
 * everything that belongs to a single exercise — its name and small print, its sets,
 * and the Done/Reopen actions, with the progression prompt behind Done (ROADMAP N7, N8, N50).
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
    onFinishExercise: (String) -> Unit,
    /** Writes the step a lifter accepted at *Done*, and finishes the exercise (ROADMAP N50). */
    onAcceptProgression: (String, ProgressionDirection) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onReopenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (String) -> Unit = {},
    /** Whether a rest is counted down, and the fallback its static label uses (ROADMAP N44). */
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
    /** Whether *Done* asks about the next step a plan earned (ROADMAP N66). */
    progressionPromptEnabled: Boolean = true,
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
                // A row's neighbours are the list's own knowledge, so the entries that would write
                // nothing are simply not offered (ROADMAP N54, the shape B28's row-0 exclusion uses).
                canMoveUp = index > 0,
                canMoveDown = index < rows.lastIndex,
                onEditSet = onEditSet,
                onDeleteSet = onDeleteSet,
                onFinishExercise = onFinishExercise,
                onAcceptProgression = onAcceptProgression,
                onRateExercise = onRateExercise,
                onReopenExercise = { onReopenExercise(row.id) },
                // Row 0 has nothing above it to pair with: with no previous exercise the group
                // comes out null and the write would rewrite every ungrouped row, churning
                // `updatedAt` for no change (ROADMAP B28).
                onToggleSuperset = if (index == 0) null else { { onToggleSuperset(row.id) } },
                restTimerEnabled = restTimerEnabled,
                defaultRestSeconds = defaultRestSeconds,
                progressionPromptEnabled = progressionPromptEnabled,
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
 * **Done**, and loses its next-set fields; the wording avoids *Finish*, which is the
 * workout-level action.
 */
@Composable
private fun ExerciseSection(
    row: SessionExerciseRow,
    onLogSet: (SetEdit) -> Unit,
    onRemoveExercise: () -> Unit,
    onMoveExercise: (Int) -> Unit,
    /** Whether the exercise above/below exists to swap with (ROADMAP N54). */
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEditSet: (SetRow) -> Unit,
    onDeleteSet: (String) -> Unit,
    onFinishExercise: (String) -> Unit,
    onAcceptProgression: (String, ProgressionDirection) -> Unit,
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onReopenExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
    /** Whether *Done* asks about the next step a plan earned (ROADMAP N66). */
    progressionPromptEnabled: Boolean = true,
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
            ExerciseStateAction(
                row = row,
                onReopenExercise = onReopenExercise,
                onFinishExercise = onFinishExercise,
                onAcceptProgression = onAcceptProgression,
                progressionPromptEnabled = progressionPromptEnabled,
            )
            // The rare actions moved in here rather than sitting on the header (ROADMAP N53): the
            // header is read constantly mid-session, and a text link in every one of them cost more
            // attention than the action earned.
            ExerciseOverflow(
                row = row,
                onToggleSuperset = onToggleSuperset,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
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
        )

        // N10: the ratings can be given while the exercise is still in front of you. This row is the
        // only way in: *Done* no longer opens the rating (N8) — it is reached when the lifter reaches
        // for it, rather than handed to them on the way out of the exercise.
        ExerciseRatingSection(
            muscleFeel = row.muscleFeel,
            joints = row.joints,
            legacyJointPain = row.jointPain,
            legacyJointPainNote = row.jointPainNote,
            onRate = { feel, joints -> onRateExercise(row.id, feel, joints) },
        )
    }
}

/**
 * The header's state action: **Reopen** on a done exercise, **Done** on an open one (ROADMAP N7).
 *
 * Split out of [ExerciseSection] because the choice belongs to the row's *state* rather than to its
 * content, and because the section around it is at the length this project allows.
 */
@Composable
private fun ExerciseStateAction(
    row: SessionExerciseRow,
    onReopenExercise: () -> Unit,
    onFinishExercise: (String) -> Unit,
    onAcceptProgression: (String, ProgressionDirection) -> Unit,
    progressionPromptEnabled: Boolean,
) {
    if (row.isFinished) {
        AppTextButton(
            onClick = onReopenExercise,
            modifier = Modifier.testTag(TestTags.EXERCISE_REOPEN),
        ) {
            Text(stringResource(R.string.active_workout_reopen))
        }
    } else {
        FinishExerciseAction(
            row = row,
            onFinish = onFinishExercise,
            onAcceptProgression = onAcceptProgression,
            progressionPromptEnabled = progressionPromptEnabled,
        )
    }
}

/**
 * The **Done** button for one exercise, and the prompt behind it (ROADMAP N7, N50, N66).
 *
 * Where a plan can answer it, Done opens the **progression prompt** — what the plan asked, what was
 * done, and the next step when the session earned one. **Where there is no plan there is no next step
 * to decide**, so Done only finishes the exercise; and the switch at Settings can withdraw the
 * question entirely (N66), because a lifter who does not want the app editing the plan is not asked
 * about it.
 *
 * The rating is deliberately not on this path (N8, N10): *How did that feel?* is opened where the
 * lifter reaches for it — the exercise's own row — and never handed to them on the way out of it.
 * Owning the prompt here keeps the transient "form is open" state next to the button that opens it,
 * the shape [ReadinessSection] already uses.
 */
@Composable
private fun FinishExerciseAction(
    row: SessionExerciseRow,
    onFinish: (String) -> Unit,
    onAcceptProgression: (String, ProgressionDirection) -> Unit,
    progressionPromptEnabled: Boolean,
) {
    var prompting by remember { mutableStateOf(false) }
    // A plan is what the progression prompt reads (N50), and the setting is what asks for it (N66).
    val prompts = progressionPromptEnabled && row.progression.planned != null

    AppTextButton(
        onClick = { if (prompts) prompting = true else onFinish(row.id) },
        modifier = Modifier.testTag(TestTags.EXERCISE_DONE),
    ) {
        Text(stringResource(R.string.active_workout_done_exercise))
    }

    if (prompting) {
        ProgressionDialog(
            exerciseName = row.name,
            prompt = row.progression,
            onAccept = { direction ->
                prompting = false
                onAcceptProgression(row.id, direction)
            },
            onNotNow = {
                prompting = false
                onFinish(row.id)
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
 * - Order (N54) offers only the direction that exists, for the same reason: the list knows which row
 *   it is drawing, so the first exercise has no *Move up* rather than one that writes nothing.
 */
@Composable
private fun ExerciseOverflow(
    row: SessionExerciseRow,
    onToggleSuperset: (() -> Unit)?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
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
        ExerciseMenuItems(
            row = row,
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            onToggleSuperset = onToggleSuperset,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onMove = onMove,
            onRemove = { confirmingRemoval = true },
        )
    }

    if (confirmingRemoval) {
        ConfirmRemovalDialog(
            name = row.name,
            onDismiss = { confirmingRemoval = false },
            onConfirm = {
                confirmingRemoval = false
                onRemove()
            },
        )
    }
}

/**
 * A done exercise's sets stay on screen, dimmed and non-editable, and it loses its
 * next-set fields entirely (ROADMAP N7). Split from [ExerciseSection] so the section
 * stays a header plus its two blocks rather than one long function.
 */
@Composable
private fun ExerciseSets(
    row: SessionExerciseRow,
    onLogSet: (SetEdit) -> Unit,
    onEditSet: (SetRow) -> Unit,
    onDeleteSet: (String) -> Unit,
    modifier: Modifier = Modifier,
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

        // No next-set fields once the exercise is done: that is the accident N7 exists to prevent.
        if (!row.isFinished) {
            NextSetEditor(row = row, onLogSet = onLogSet)
        }
    }
}

/**
 * The next set, stated on the screen and committed by the *Log set* beside it (ROADMAP N59).
 *
 * This replaces N51's shape — *Log set* opened the editor, and logging *was* the dialog — with the
 * one visible fields make possible: the values the plan and history prefill are already on screen,
 * so they can be read and changed before anything is written, and the button writes exactly what is
 * under it. B7's rule is back on this path, because the button carries the values it commits.
 *
 * **The role** is the plan's own next unlogged one (B48), carried the way reps and weight are, so a
 * template that opens with a ramp is logged as warm-ups rather than as working sets; it stays a
 * picker so one set can still be overridden (N19). **The plan's RPE** now *fills* the RPE stepper —
 * N59's original "shown beside the field, never written into it" is deliberately reversed: the lifter
 * reads the plan's own number and changes it when the set felt different, and a plan that names none
 * starts at 9.0 so a logged set always carries one. **A warm-up carries none** (N67), so the field is
 * not offered at all while the role picker says warm-up, and the plan's number is not put into it.
 *
 * The draft is keyed on `row.sets.size`, so writing a set re-reads the plan's next one and the fields
 * re-arm — N19's "clears itself", with the plan as the resting value. Re-arming rather than surviving
 * is the point: a value typed for a set that was then written would otherwise become the next set's.
 */
@Composable
private fun NextSetEditor(
    row: SessionExerciseRow,
    onLogSet: (SetEdit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggestion = row.suggestion
    var draft by remember(row.id, row.sets.size) {
        mutableStateOf(
            SetEntryDraft(
                repsText = suggestion.reps.toString(),
                // Shown as one signed number: -20 is 20 kg of assistance (N15).
                weightText = Weight.display(suggestion.weightGrams, suggestion.assistanceGrams),
                // The plan's own target, or 9.0 where it names none: the stepper always shows a
                // number, so a logged set always carries one (N59). A planned warm-up opens it
                // empty instead, because a warm-up carries no effort to show (N67).
                rpeText = if (suggestion.setType.recordsEffort) {
                    Rpe.format(suggestion.targetRpeHalves ?: DEFAULT_RPE_HALVES)
                } else {
                    ""
                },
                setType = suggestion.setType,
            ),
        )
    }
    val values = draft.values()

    Column(modifier = modifier) {
        PlanDoneNotice(row = row)
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SetRoleSelector(
                role = draft.setType,
                onSelect = { draft = draft.copy(setType = it) },
                // Tagged for this row: a test reaches the picker through the flow it actually takes.
                testTag = TestTags.exercisePendingRole(row.id),
                optionTag = { role -> TestTags.exercisePendingRole(row.id, role) },
            )
            SetEntryNumbers(draft = draft, onDraftChange = { draft = it })
            // A warm-up has no effort to state, so the field is not offered at all rather than
            // shown disabled: a control that cannot write is worse than no control (N67). The
            // role picker above is what brings it back.
            if (draft.setType.recordsEffort) {
                // Full width on its own row (N68). It used to share one with *Log set*, which left
                // the ± pair squeezed into whatever the button did not take and put the control that
                // commits the set a thumb-width from the one that states the effort. Now the button
                // is the row below, and every value it writes is still above it.
                SetRpeField(
                    draft = draft,
                    onDraftChange = { draft = it },
                    targetRpeHalves = suggestion.targetRpeHalves,
                )
            }
            FilledTonalButton(
                onClick = { onLogSet(draft.toEdit()) },
                enabled = values.isComplete,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SET_LOG),
            ) {
                // The plan's work is done, so the label stops promising a target the plan no
                // longer names (ROADMAP N52). Logging an extra set is what the control still
                // does — nothing closes, and the way to end the exercise is Done.
                Text(
                    stringResource(
                        if (row.isPastPlan) R.string.set_log_extra else R.string.set_log,
                    ),
                )
            }
        }
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
 * It is per exercise and it is a notice rather than a dialog, deliberately. Nothing closes, because
 * accepting the notice is the header's **Done** (N7) — logging an extra set is what the control
 * still does, which is why the label changes rather than the action. N59 removed the dialog this
 * once had to avoid; the notice stays a notice because a modal over the next set is still the wrong
 * shape for something the lifter may simply read and walk past.
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
 * (ROADMAP N5), and the rest prescription when the timer is off (N44).
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
            // What a set was is reps, load *and* effort, so the RPE reads on the line rather than as a
            // marker (N6): the stepper records one per set (N59), and the row has to say it back.
            // A warm-up recorded none, so it has none to say (N67).
            set.rpeHalves?.takeIf { set.setType.recordsEffort }?.let { halves ->
                Text(
                    text = rpeMarker(halves),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
 * The small marker a set carries when it has a role or a comment (ROADMAP N6).
 *
 * A marker, not the text: the workout row has to stay scannable mid-set, and the
 * comment itself belongs on the workout detail. The RPE is no longer one of these — it reads on the
 * set's own line, because the stepper records one per set (N59) and a marker could only hint at the
 * number — so nothing is emitted when the set carries neither a role nor a note, which is the state
 * the one-tap log path leaves it in.
 */
@Composable
private fun SetExtrasMarker(set: SetRow, modifier: Modifier = Modifier) {
    // A working set is the default and needs no label; anything else is worth
    // showing, because the role is the thing the user chose (ROADMAP N14).
    val roleMarker = set.setType
        .takeIf { it != SetType.NORMAL }
        ?.let { stringResource(R.string.set_role_marker, it.label) }
    val noteMarker = if (set.note != null) stringResource(R.string.set_note_marker) else null
    val marker = listOfNotNull(roleMarker, noteMarker).joinToString(" · ")

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
 * The entries one exercise's overflow offers (ROADMAP N53, N54).
 *
 * Its own composable for the reason the project keeps splitting them: the button, the dialog and the
 * menu were one function at ninety-odd lines, and each of the three is a thing that can be read on its
 * own. Order comes first — it is the entry most likely to be reached for mid-session, and the one that
 * changes what everything below it means — and each direction is offered only where it exists, because
 * the list this menu is drawn from knows a row's neighbours (N54, B28's shape).
 */
@Composable
private fun ExerciseMenuItems(
    row: SessionExerciseRow,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onToggleSuperset: (() -> Unit)?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (canMoveUp) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.active_workout_move_up)) },
                onClick = {
                    onDismiss()
                    onMove(-1)
                },
                modifier = Modifier.testTag(TestTags.exerciseMove(row.id, up = true)),
            )
        }
        if (canMoveDown) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.active_workout_move_down)) },
                onClick = {
                    onDismiss()
                    onMove(1)
                },
                modifier = Modifier.testTag(TestTags.exerciseMove(row.id, up = false)),
            )
        }
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
                    onDismiss()
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
                onDismiss()
                onRemove()
            },
            modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE),
        )
    }
}

/**
 * The question a removal asks before it takes anything (ROADMAP B2, moved by N53).
 *
 * Its own composable because the overflow around it is at the length this project allows, and because
 * the guard is the part worth reading on its own: removing an exercise takes its sets with it and has
 * no undo to reach for.
 */
@Composable
private fun ConfirmRemovalDialog(
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.active_workout_remove_confirm_title)) },
        text = { Text(stringResource(R.string.active_workout_remove_confirm_text, name)) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE_CONFIRM),
            ) {
                Text(stringResource(R.string.active_workout_remove_confirm))
            }
        },
        dismissButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.EXERCISE_REMOVE_CANCEL),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

