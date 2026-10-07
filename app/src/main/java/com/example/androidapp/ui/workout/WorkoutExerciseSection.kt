package com.example.androidapp.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.JointPain
import com.example.androidapp.domain.model.Rpe
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.ui.components.DEFAULT_RPE_HALVES
import com.example.androidapp.ui.components.ExerciseActionsMenu
import com.example.androidapp.ui.components.ExerciseMenuTags
import com.example.androidapp.ui.components.ExerciseRatingSection
import com.example.androidapp.ui.components.SetEdit
import com.example.androidapp.ui.components.SetEntryDraft
import com.example.androidapp.ui.components.SetEntryNumbers
import com.example.androidapp.ui.components.SetRoleSelector
import com.example.androidapp.ui.components.SetRpeField
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.restLabel
import com.example.androidapp.ui.components.rpeMarker
import com.example.androidapp.ui.components.label
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

/**
 * How many of the plan's own sets this exercise has still to write, or null when there is no plan
 * (ROADMAP N70).
 *
 * Counted exactly as [isPastPlan] counts — every logged set against every planned one — so the line
 * and the label can never disagree: at zero the plan's work is done, which is the sentence that
 * replaces the count. A null plan stays null rather than reading zero, because "nothing planned" and
 * "all of it done" are different things to say to somebody.
 */
internal val SessionExerciseRow.plannedSetsLeft: Int?
    get() = plannedSetCount?.let { (it - sets.size).coerceAtLeast(0) }

/**
 * The workout's exercises, and the things that belong in the same scroll as them (ROADMAP N82).
 *
 * The body used to be a fixed column over this list — the clock, the readiness row and the rest bar —
 * and everything outside the list was therefore on screen for the whole session. The list owns what
 * scrolls now: the readiness row arrives as [header], the action that adds a movement as [footer], and
 * the caller keeps only what must stay put. The slots are how that boundary is stated rather than
 * implied, and [empty] is the third state because a session with no exercises is drawn *instead of* the
 * rows while still sitting between the same two.
 */
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
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onReopenExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (String) -> Unit = {},
    /** Whether a rest is counted down, and the fallback its static label uses (ROADMAP N44). */
    restTimerEnabled: Boolean = true,
    defaultRestSeconds: Int = RestTimer.DEFAULT_SECONDS,
    /** Above the rows, inside the scroll: the session's readiness (N82). */
    header: (@Composable () -> Unit)? = null,
    /** Where the rows would be, when there are none (N82). */
    empty: (@Composable () -> Unit)? = null,
    /** Below the rows, inside the scroll: the workout's one action (N82). */
    footer: (@Composable () -> Unit)? = null,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.EXERCISE_LIST),
        // Room at the foot of the scroll, where the extended FAB used to need 96 dp of clearance.
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        header?.let { above -> item(key = "header") { above() } }
        if (rows.isEmpty()) {
            empty?.let { message -> item(key = "empty") { message() } }
        } else {
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
                    onRateExercise = onRateExercise,
                    onReopenExercise = { onReopenExercise(row.id) },
                    // Row 0 has nothing above it to pair with: with no previous exercise the group
                    // comes out null and the write would rewrite every ungrouped row, churning
                    // `updatedAt` for no change (ROADMAP B28).
                    onToggleSuperset = if (index == 0) null else { { onToggleSuperset(row.id) } },
                    restTimerEnabled = restTimerEnabled,
                    defaultRestSeconds = defaultRestSeconds,
                )
                HorizontalDivider()
            }
        }
        footer?.let { below -> item(key = "footer") { below() } }
    }
}

/**
 * One exercise's whole block: its name and small print, its sets, and the actions
 * that belong to it.
 *
 * ROADMAP N7 adds the third state this renders — open, or done. A done exercise
 * keeps its sets on screen, dimmed and non-editable, offers **Reopen** instead of
 * **Done**, and loses its next-set fields; the wording avoids *Finish*, which is the
 * workout-level action. N69 moved that action to the foot of the block and withdrew
 * *Done* entirely until the exercise has a set to finish.
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
    onRateExercise: (String, Int?, List<JointPain>) -> Unit,
    onReopenExercise: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleSuperset: (() -> Unit)? = null,
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
            // The rare actions moved in here rather than sitting on the header (ROADMAP N53): the
            // header is read constantly mid-session, and a text link in every one of them cost more
            // attention than the action earned. The menu itself is shared with the template editor
            // since N71; only its tags and the removal's sentence belong to this screen.
            WorkoutExerciseActions(
                row = row,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
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
        )

        // N10: the ratings can be given while the exercise is still in front of you, and this row is
        // the only way in — *Done* no longer opens the rating (N8). N76 puts the exercise's own action
        // at the end of that same row: "the exercise is over" and "how did that feel" are one closing
        // decision, and a button eight points under the field read as a separate one.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseRatingSection(
                muscleFeel = row.muscleFeel,
                joints = row.joints,
                legacyJointPain = row.jointPain,
                legacyJointPainNote = row.jointPainNote,
                onRate = { feel, joints -> onRateExercise(row.id, feel, joints) },
                // The rating gives up the width the action needs (N76): its click target covered the
                // whole row, and a full-width one would swallow the button beside it.
                modifier = Modifier.weight(1f),
            )

            // The action stays at the exercise's foot (N69) — beside the last thing said about the
            // exercise rather than under it.
            ExerciseStateAction(
                row = row,
                onReopenExercise = onReopenExercise,
                onFinishExercise = onFinishExercise,
            )
        }
    }
}

/**
 * This screen's use of the shared exercise menu: its tags, its wording, and its two exclusions
 * (ROADMAP N71, N53).
 *
 * Split out so [ExerciseSection] stays under the length this project allows, and because what belongs
 * to *this* screen is exactly these seven arguments: the rest of the menu is the component's.
 *
 * The exclusions are the workout's own. A row with nothing above it is passed a null
 * [onToggleSuperset] (B28), and a done exercise is out of the round as well (N7) — both make
 * [ExerciseActionsMenu] leave the pairing entry out rather than write something that does nothing. A
 * direction with nowhere to go is left to the list, which is what [canMoveUp]/[canMoveDown] carry.
 */
@Composable
private fun WorkoutExerciseActions(
    row: SessionExerciseRow,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleSuperset: (() -> Unit)?,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExerciseActionsMenu(
        contentDescription = stringResource(R.string.exercise_menu_more, row.name),
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
        supersetGrouped = if (onToggleSuperset != null && !row.isFinished) {
            row.supersetGroup != null
        } else {
            null
        },
        removeTitle = stringResource(R.string.active_workout_remove_confirm_title),
        removeText = stringResource(R.string.active_workout_remove_confirm_text, row.name),
        onToggleSuperset = { onToggleSuperset?.invoke() },
        onMove = onMove,
        onRemove = onRemove,
        tags = ExerciseMenuTags(
            menu = TestTags.exerciseMenu(row.id),
            moveUp = TestTags.exerciseMove(row.id, up = true),
            moveDown = TestTags.exerciseMove(row.id, up = false),
            superset = TestTags.supersetToggle(row.id),
            remove = TestTags.EXERCISE_REMOVE,
        ),
        modifier = modifier,
    )
}

/**
 * The exercise's state action, rendered at its foot: **Reopen** on a done exercise, **Done** on one
 * that has something to finish (ROADMAP N7, N69).
 *
 * Split out of [ExerciseSection] because the choice belongs to the row's *state* rather than to its
 * content, and because the section around it is at the length this project allows.
 *
 * **An exercise with nothing logged has no action at all** (N69). *Done* says the work is over, and
 * there is no work to be over before a set exists; the way past an exercise you did not do is the
 * overflow's *Remove*, which asks before it takes anything. Reopen keeps the same place as Done, so
 * the foot of the block is where its state is decided either way.
 */
@Composable
private fun ExerciseStateAction(
    row: SessionExerciseRow,
    onReopenExercise: () -> Unit,
    onFinishExercise: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        row.isFinished -> AppTextButton(
            onClick = onReopenExercise,
            modifier = modifier.testTag(TestTags.EXERCISE_REOPEN),
        ) {
            Text(stringResource(R.string.active_workout_reopen))
        }

        row.sets.isEmpty() -> Unit

        else -> AppTextButton(
            onClick = { onFinishExercise(row.id) },
            modifier = modifier.testTag(TestTags.EXERCISE_DONE),
        ) {
            Text(stringResource(R.string.active_workout_done_exercise))
        }
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
 * The draft after the role picker says something else (ROADMAP N59, N67).
 *
 * A planned warm-up opens with **no** effort (N67), so returning the role to one that records an
 * effort has to state the plan's number again rather than leave *Not recorded* under a caption that
 * names it: the picker is what brings the field back, and the field it brings back should be the
 * plan's. Effort the lifter already stated stands, so going to *Warm-up* and back does not discard a
 * number they typed.
 *
 * File-level and pure, the shape [planEntriesFor] uses, so the rule can be tested without composing
 * the screen.
 */
internal fun SetEntryDraft.withRole(role: SetType, planRpeHalves: Int?): SetEntryDraft {
    val effort = if (role.recordsEffort && rpeText.isBlank()) {
        Rpe.format(planRpeHalves ?: DEFAULT_RPE_HALVES)
    } else {
        rpeText
    }
    return copy(setType = role, rpeText = effort)
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
    // The exercise's own unit, resolved once for this block (ROADMAP N64). Read *before* the draft
    // because the field is seeded in it: a prefill shown in kilograms and parsed in pounds wrote the
    // wrong weight the moment *Log set* was pressed without touching it.
    val unit = row.weightUnit
    var draft by remember(row.id, row.sets.size) {
        mutableStateOf(
            SetEntryDraft(
                repsText = suggestion.reps.toString(),
                // Shown as one signed number, in this exercise's unit: -20 is 20 kg of assistance
                // (N15), or the pound equivalent where the exercise reads in pounds (N64).
                weightText = Weight.display(
                    suggestion.weightGrams,
                    suggestion.assistanceGrams,
                    unit,
                ),
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
    val values = draft.values(unit)

    Column(modifier = modifier) {
        PlanProgressNotice(row = row)
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SetRoleSelector(
                role = draft.setType,
                // A planned warm-up opens with no effort at all (N67), so the picker is what states
                // the plan's number again when the role comes back to one that records an effort.
                onSelect = { role -> draft = draft.withRole(role, suggestion.targetRpeHalves) },
                // Tagged for this row: a test reaches the picker through the flow it actually takes.
                testTag = TestTags.exercisePendingRole(row.id),
                optionTag = { role -> TestTags.exercisePendingRole(row.id, role) },
                // A rung hangs off the set above it and the pending set is appended, so the picker
                // offers one only where the boundary would accept it: a first set, or one after a
                // ramp, cannot be armed with a role that is going to be refused (ROADMAP N79, B63, B64).
                offers = { role -> row.sets.canLogAs(role) },
            )
            SetEntryNumbers(
                draft = draft, onDraftChange = { draft = it }, unit = unit, stepGrams = row.stepGrams,
            )
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
                onClick = { onLogSet(draft.toEdit(unit)) },
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
 * Where this exercise is against the plan: the sets still to write, or that the plan's work is done
 * (ROADMAP N52, N70).
 *
 * N52's notice could only speak once the last planned set had landed, so the part of the session
 * where the count would have been useful — the sets still to write — said nothing at all. It now
 * states the remainder while there is one (*"2 planned sets left"*) and falls back to the same
 * sentence N52 shipped once there is none, because "none left" is the moment that sentence is for.
 *
 * It is per exercise and it is a notice rather than a dialog, deliberately. Nothing closes, because
 * accepting it is the exercise's own **Done** (N7, N69) — logging an extra set is what the control
 * still does, which is why the label changes rather than the action. N59 removed the dialog this once
 * had to avoid; the notice stays a notice because a modal over the next set is still the wrong shape
 * for something the lifter may simply read and walk past.
 */
@Composable
private fun PlanProgressNotice(row: SessionExerciseRow, modifier: Modifier = Modifier) {
    val left = row.plannedSetsLeft ?: return

    Text(
        text = if (left == 0) {
            stringResource(R.string.set_plan_done)
        } else {
            pluralStringResource(R.plurals.set_plan_left, left, left)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = 8.dp)
            // The two states answer to two tags (N70): one tag cannot mean "the plan is done" and
            // "the plan is not done yet" at once, which is what a test would have to read it as.
            .testTag(
                if (left == 0) TestTags.EXERCISE_PLAN_DONE else TestTags.EXERCISE_PLAN_LEFT,
            ),
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
                    Weight.display(set.weightGrams, set.assistanceGrams, set.weightUnit),
                    set.weightUnit.label(),
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

