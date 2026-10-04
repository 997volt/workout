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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.WorkoutSummary
import com.example.androidapp.domain.model.zoneIdOrNull
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.longLabel
import com.example.androidapp.ui.history.HistoryFormat
import com.example.androidapp.ui.theme.TileAccent
import com.example.androidapp.ui.workout.WorkoutFormat
import java.time.ZoneId

/**
 * The home list and the rows it is made of.
 *
 * Its own file because the screen beside it — route, scaffold, dialogs, menu and the start bar
 * — is already at the length this project allows a file to be, and these five composables are a
 * coherent other half: everything here is about drawing one workout, scheduled or done.
 */

/**
 * Today's plans and the recent workouts, in one list (ROADMAP N16).
 *
 * Split out of the body because the two lists together are long enough to be their own
 * composable — and because "today" and "recent" are different questions that happen to
 * share a scroll.
 */
@Composable
internal fun TodayAndRecent(
    state: WorkoutsHomeUiState,
    onOpenWorkout: (String) -> Unit,
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
        recentItems(state = state, onOpenWorkout = onOpenWorkout)
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
                TextButton(
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

/** The recent workouts, newest first (ROADMAP N1). */
private fun LazyListScope.recentItems(
    state: WorkoutsHomeUiState,
    onOpenWorkout: (String) -> Unit,
) {
    if (state.recent.isEmpty()) return
    item(key = "recent") {
        SectionHeader(text = stringResource(R.string.home_recent))
    }
    items(state.recent.size, key = { state.recent[it].id }) { index ->
        RecentWorkoutRow(
            workout = state.recent[index],
            onClick = { onOpenWorkout(state.recent[index].id) },
        )
    }
}

@Composable
internal fun RecentWorkoutRow(
    workout: WorkoutSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val setCount = pluralStringResource(R.plurals.history_sets, workout.setCount, workout.setCount)
    val duration = workout.duration?.let { WorkoutFormat.elapsed(it) }.orEmpty()
    val volume = stringResource(R.string.history_volume, Weight.kilograms(workout.volumeGrams))

    AppRow(
        // The session's own zone, like history and the workout detail (ROADMAP B33). Omitting it
        // here was a dropped argument rather than missing data, and it made one workout read as
        // two different dates on two screens.
        headline = HistoryFormat.historyHeadline(
            workout.startedAt,
            zone = workout.zoneIdOrNull() ?: ZoneId.systemDefault(),
        ),
        // The template's name comes first, because it is the half of the row that says what the
        // session *was* (ROADMAP N58); a workout with no plan behind it contributes nothing.
        supporting = listOf(workout.templateName, duration, setCount, volume)
            .filter { !it.isNullOrEmpty() }
            .joinToString(" · "),
        leading = { IconTile(icon = Icons.Filled.History, accent = TileAccent.Sky) },
        trailing = { DiscloseChevron() },
        onClick = onClick,
        // The headline is a date, which names the workout but not the tap: without this the
        // row announces "Monday 28 September, button".
        onClickLabel = stringResource(R.string.action_open_workout),
        testTag = TestTags.HOME_RECENT_ROW,
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
