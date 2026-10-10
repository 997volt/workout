package com.example.androidapp.ui.home

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
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Rows are cards now, so the list owns the page margins that a plain ListItem used to
        // supply for itself — and the bottom inset is the bar's job, through the scaffold.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        todayPlanItems(state = state, onStartTemplate = onStartTemplate, onSubstitute = onSubstitute)
        wayInItems(
            onStartWorkout = onStartWorkout,
            onOpenPrograms = onOpenPrograms,
            onOpenTemplates = onOpenTemplates,
            onOpenMeasurements = onOpenMeasurements,
        )
    }
}

/** Today's scheduled plans, headed by the weekday (ROADMAP N16, P3.3, P3.11). */
private fun LazyListScope.todayPlanItems(
    state: WorkoutsHomeUiState,
    onStartTemplate: (TodayPlan) -> Unit,
    onSubstitute: (TodayPlan) -> Unit,
) {
    if (state.todaysPlan.isEmpty()) return
    item(key = "today") {
        SectionHeader(text = stringResource(R.string.home_today, state.today.longLabel()))
    }
    items(state.todaysPlan.size, key = { state.todaysPlan[it].id }) { index ->
        val plan = state.todaysPlan[index]
        TodayPlanCard(
            plan = plan,
            onStart = { onStartTemplate(plan) },
            onSubstitute = { onSubstitute(plan) },
        )
    }
}

/**
 * One scheduled workout, as a card.
 *
 * The row's two actions sit *under* its name rather than beside it. Beside it they were
 * competing with the name for the width of the card, and "Substitute" plus "Start" left the
 * thing being started with about ninety dp — enough for one short word. Underneath, the name
 * gets the card and the actions get a line of their own.
 */
@Composable
private fun TodayPlanCard(
    plan: TodayPlan,
    onStart: () -> Unit,
    onSubstitute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val exercises = pluralStringResource(
        R.plurals.home_plan_exercises,
        plan.exerciseCount,
        plan.exerciseCount,
    )
    AppCard(modifier = modifier) {
        PlanHeading(
            name = plan.name,
            supporting = exercises,
            icon = Icons.Filled.FitnessCenter,
            accent = TileAccent.Indigo,
        )
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
                // is the template, and the slot carries its prescription (P3.3, P3.8).
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
 * A card's leading tile and its two lines.
 *
 * Shared by the scheduled and the next-up card, which differ in their glyph, their accent and
 * what sits under them — not in how they are headed. Extracted at its second caller, which is
 * this one.
 */
@Composable
private fun PlanHeading(
    name: String,
    supporting: String,
    icon: ImageVector,
    accent: TileAccent,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon = icon, accent = accent)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
 */
private fun LazyListScope.wayInItems(
    onStartWorkout: () -> Unit,
    onOpenPrograms: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenMeasurements: () -> Unit,
) {
    item(key = "way-in-programs") {
        WayInRow(
            headline = stringResource(R.string.home_programs),
            supporting = stringResource(R.string.home_programs_supporting),
            onClickLabel = stringResource(R.string.home_open, stringResource(R.string.home_programs)),
            icon = Icons.Filled.EventAvailable,
            accent = TileAccent.Teal,
            testTag = TestTags.HOME_PROGRAMS,
            onClick = onOpenPrograms,
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
            onClick = onOpenTemplates,
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
            onClick = onOpenMeasurements,
        )
    }
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
