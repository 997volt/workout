package com.example.androidapp.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.longLabel
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.TileAccent

/**
 * The home list and the rows it is made of.
 *
 * Its own file because the screen beside it — route, scaffold, dialogs, menu and the start bar
 * — is already at the length this project allows a file to be, and these five composables are a
 * coherent other half: everything here is about drawing one workout, scheduled or done.
 */

/**
 * Today's plans and the ways in, in one list (ROADMAP N16, N102).
 *
 * Split out of the body because the two halves together are long enough to be their own composable — and
 * because "what is scheduled" and "where else can I go" are different questions that happen to share a
 * scroll.
 */
@Composable
internal fun HomeBody(
    state: WorkoutsHomeUiState,
    onStartWorkout: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenMeasurements: () -> Unit,
    onMeasureToday: () -> Unit,
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
    onOpenPlannedWorkout: (TodayPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.HOME_LIST),
        // Rows are cards now, so the list owns the page margins that a plain ListItem used to
        // supply for itself — and the bottom inset is the bar's job, through the scaffold.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        wayInItems(
            // Starting is offered only while nothing is running (B101): the bar's Resume pill is the same
            // action, and a second row for it would be the pill under the wrong label.
            showStart = state.activeWorkout == null,
            // **Recording is not starting**, so B101's withholding does not reach this row: it is drawn
            // whenever today has no daily entry, a workout running or not (N114).
            showMeasureToday = !state.todayIsMeasured,
            actions = WayInActions(
                onStartWorkout = onStartWorkout,
                onOpenPrograms = onOpenPrograms,
                onOpenTemplates = onOpenTemplates,
                onOpenMeasurements = onOpenMeasurements,
                onMeasureToday = onMeasureToday,
            ),
        )
        // Today's plan comes **after** the ways in (N115, N124): the destinations are what this screen is
        // for on a day with nothing scheduled, and the day's own work belongs at the bottom edge the thumb
        // is already at.
        planItems(
            state = state,
            onStartTemplate = onStartTemplate,
            onSubstitute = onSubstitute,
            onOpenPlannedWorkout = onOpenPlannedWorkout,
        )
    }
}

/**
 * Today's plan, headed by the weekday (ROADMAP N115, P3.3, N125).
 *
 * **The one answer home gives to "what am I doing today".** Until N125 there was a second — a next-up row
 * per active program with nothing scheduled today — and the two were shown on opposite conditions. The row
 * is gone: a day's workout is the question this screen is for, and a program's position on some other day
 * is an answer nothing on the screen can act on. What that costs is named rather than discovered: a program
 * with no weekday, and every rest day of one that has them, is reached through the Programs screen now
 * (P3.9's home surface is superseded).
 *
 * It sits at the **foot** of the list, where N115 put it, and is drawn only when the day has work; the
 * state of a day that has none is N126's.
 */
private fun LazyListScope.planItems(
    state: WorkoutsHomeUiState,
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
    onOpenPlannedWorkout: (TodayPlan) -> Unit,
) {
    if (state.todaysPlan.isEmpty()) return
    item(key = "today") {
        SectionHeader(text = stringResource(R.string.home_today, state.today.longLabel()))
    }
    items(state.todaysPlan.size, key = { state.todaysPlan[it].id }) { index ->
        val plan = state.todaysPlan[index]
        TodayPlanCard(
            plan = plan,
            onOpen = { onOpenPlannedWorkout(plan) },
            onStart = { onStartTemplate(plan) },
            onSubstitute = { onSubstitute(plan) },
        )
    }
}

/**
 * One scheduled workout, as a card.
 *
 * The name is the way into the plan and the button underneath is the way to start it, which is the two
 * gestures **looking and starting** have always been here (N55): the card states what the workout is, and
 * tapping it answers "what is in it" without starting anything (N127). The subtitle names the program when
 * there is one, because two programs can schedule the same workout on the same day (P3.12).
 *
 * The row's two actions sit *under* its name rather than beside it. Beside it they were
 * competing with the name for the width of the card, and "Substitute" plus "Start" left the
 * thing being started with about ninety dp — enough for one short word. Underneath, the name
 * gets the card and the actions get a line of their own.
 */
@Composable
private fun TodayPlanCard(
    plan: TodayPlan,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onSubstitute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = pluralStringResource(
        R.plurals.home_plan_exercises,
        plan.exerciseCount,
        plan.exerciseCount,
    )
    // The whole heading is the field, so the target is the row rather than a caption inside it, and
    // `clickable` carries an `onClickLabel` because the headline names the workout and never the action
    // (the rule every row here follows).
    val openLabel = stringResource(R.string.home_plan_open, plan.name)
    AppCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.Home.planPreview(plan.id))
                .clickable(onClickLabel = openLabel, onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(icon = Icons.Filled.FitnessCenter, accent = TileAccent.Indigo)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = plan.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = plan.programName?.let { "$exercises · $it" } ?: exercises,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Only a program slot can be substituted: the event is keyed by slot and week
            // (P3.11), and a pinned plan has no slot to key it by.
            if (plan.slotId != null) {
                AppTextButton(
                    onClick = onSubstitute,
                    modifier = Modifier.testTag(TestTags.homeSubstitute(plan.id)),
                ) {
                    Text(stringResource(R.string.home_substitute))
                }
            }
            Button(
                // The row's identity is the slot's, and the whole row travels: what starts
                // is the template, and the slot id travels for the prefill history (P3.3, N73).
                onClick = onStart,
                // Today's planned start and the next-up pill are one action on one plan, so they are one
                // colour (N83): the tonal container *Log set* draws, rather than the accent.
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .testTag(TestTags.Home.startPlan(plan.id)),
            ) {
                Text(stringResource(R.string.home_plan_start))
            }
        }
    }
}

/**
 * The ways in: three destinations and the screen's own primary action (ROADMAP N102).
 *
 * These replace the *Recent* list, and they are the row the recent workouts were: a tile, a name, a line
 * under it and a chevron. A chevron promises a screen, so the three destinations carry one and *Start empty
 * workout* does not — it is the action this screen exists for, and it wears the empty start's own accent
 * (N61) rather than reading as a fourth peer.
 *
 * No section header over them: they are the body rather than a category of content, and a heading over four
 * different destinations would be a label for "miscellaneous".
 *
 * **[showStart] is false while a workout is open** (ROADMAP B101), and that is B43's withholding rather than a
 * gap: the bar already carries *Resume* for the session, and an empty start is find-or-create, so a second row
 * reading *Start empty workout* would perform the pill's own act under the wrong label — the same action twice
 * on one screen, which is what N102's own rule cites as what N42 removed. The three destinations stay, because
 * looking is not starting (N78).
 */
private fun LazyListScope.wayInItems(
    showStart: Boolean,
    showMeasureToday: Boolean,
    actions: WayInActions,
) {
    item(key = "way-in-programs") {
        WayInRow(
            headline = stringResource(R.string.home_programs),
            supporting = stringResource(R.string.home_programs_supporting),
            onClickLabel = stringResource(R.string.home_open, stringResource(R.string.home_programs)),
            icon = Icons.Filled.EventAvailable,
            accent = TileAccent.Teal,
            testTag = TestTags.HOME_PROGRAMS,
            onClick = actions.onOpenPrograms,
        )
    }
    item(key = "way-in-templates") {
        WayInRow(
            headline = stringResource(R.string.home_templates),
            supporting = stringResource(R.string.home_templates_supporting),
            onClickLabel = stringResource(R.string.home_open, stringResource(R.string.home_templates)),
            icon = Icons.Filled.FitnessCenter,
            accent = TileAccent.Sky,
            testTag = TestTags.HOME_TEMPLATES,
            onClick = actions.onOpenTemplates,
        )
    }
    item(key = "way-in-measurements") {
        WayInRow(
            headline = stringResource(R.string.measurements_title),
            supporting = stringResource(R.string.home_measurements_supporting),
            onClickLabel = stringResource(R.string.home_open, stringResource(R.string.measurements_title)),
            icon = Icons.Filled.Straighten,
            accent = TileAccent.Coral,
            testTag = TestTags.HOME_MEASUREMENTS,
            onClick = actions.onOpenMeasurements,
        )
    }
    if (showStart) startEmptyRow(onStartWorkout = actions.onStartWorkout)
    if (showMeasureToday) measureTodayRow(onMeasureToday = actions.onMeasureToday)
}

/**
 * Starting a workout with nothing planned (ROADMAP N102).
 *
 * Split out of the ways in with the row below it: together they were past the length this project allows a
 * function, and each carries reasoning of its own.
 */
private fun LazyListScope.startEmptyRow(onStartWorkout: () -> Unit) {
    item(key = "way-in-start") {
        WayInRow(
            // The row names its own action, so its label is that name rather than a second "Open".
            headline = stringResource(R.string.home_start_empty_workout),
            supporting = stringResource(R.string.home_start_supporting),
            onClickLabel = stringResource(R.string.home_start_empty_workout),
            icon = Icons.Filled.Add,
            accent = TileAccent.Indigo,
            testTag = TestTags.HOME_START,
            onClick = onStartWorkout,
            discloses = false,
        )
    }
}

/**
 * Writing today down, while today has no entry (ROADMAP N114).
 *
 * Last of the ways in, and the only one that is not always drawn: the row is the prompt, so an entry for
 * today takes it away rather than leaving a row whose only news is that the day is already answered.
 */
private fun LazyListScope.measureTodayRow(onMeasureToday: () -> Unit) {
    item(key = "way-in-measure-today") {
        WayInRow(
            headline = stringResource(R.string.home_measure_today),
            supporting = stringResource(R.string.home_measure_today_supporting),
            onClickLabel = stringResource(R.string.home_measure_today),
            // The measurements destination's own icon with the one accent no other row uses: the same place,
            // today's half of it, rather than a fifth destination.
            icon = Icons.Filled.Straighten,
            accent = TileAccent.Amber,
            testTag = TestTags.HOME_MEASURE_TODAY,
            onClick = onMeasureToday,
        )
    }
}

/**
 * What the ways in do, bundled (ROADMAP N114).
 *
 * Five callbacks passed one by one pushed the list builder past the parameter ceiling this project sets, and
 * they belong together anyway: every one of them is "where a row goes".
 */
internal class WayInActions(
    val onStartWorkout: () -> Unit,
    val onOpenPrograms: () -> Unit,
    val onOpenTemplates: () -> Unit,
    val onOpenMeasurements: () -> Unit,
    val onMeasureToday: () -> Unit,
)

/**
 * One way in: a tile, a name, a line about it, and a chevron where the tap opens a screen.
 *
 * [discloses] is the whole of the difference between the three destinations and the start — a chevron
 * promises somewhere to go, and a row that begins something has nowhere to promise.
 */
@Composable
private fun WayInRow(
    headline: String,
    supporting: String,
    onClickLabel: String,
    icon: ImageVector,
    accent: TileAccent,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    discloses: Boolean = true,
) {
    AppRow(
        headline = headline,
        supporting = supporting,
        leading = { IconTile(icon = icon, accent = accent) },
        trailing = if (discloses) {
            { DiscloseChevron() }
        } else {
            null
        },
        onClick = onClick,
        // A row's headline names the thing and never the action, so the tap says what it does.
        onClickLabel = onClickLabel,
        testTag = testTag,
        modifier = modifier,
    )
}

/**
 * The arrow that says a row opens something.
 *
 * Decorative, so it carries no description: the row's own label already says what the tap does,
 * and a described chevron would be a second, identical announcement.
 */
@Composable
private fun DiscloseChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}
