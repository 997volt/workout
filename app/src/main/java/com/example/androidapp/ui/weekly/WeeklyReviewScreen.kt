package com.example.androidapp.ui.weekly

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.BlockProgress
import com.example.androidapp.domain.model.MovementGain
import com.example.androidapp.domain.model.OccurrenceState
import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.ReviewDay
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.programs.PlanStallCard
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The week in review (ROADMAP N247): planned against done, this week against last, and the plan that stopped.
 *
 * It is the one screen that answers the *week* rather than the day or the lift, and it deliberately reads the
 * same adherence record the month grid does, so two screens cannot disagree about whether Tuesday happened.
 */
@Composable
fun WeeklyReviewRoute(
    onBack: () -> Unit,
    onOpenDeload: (String) -> Unit,
    /** The per-lift plan edit's hand-off: the program, its template and the exercise row (N307, N310). */
    onDeloadLift: (String, String, String) -> Unit,
    /** The replace sheet's hand-off (N308); the plan editor owns its undo. */
    onReplaceLift: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WeeklyReviewViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    WeeklyReviewScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onOpenDeload = onOpenDeload,
        onDeloadLift = onDeloadLift,
        onReplaceLift = onReplaceLift,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeeklyReviewScreen(
    state: WeeklyReviewUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenDeload: (String) -> Unit,
    onDeloadLift: (String, String, String) -> Unit,
    onReplaceLift: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.WEEKLY_REVIEW_SCREEN),
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopBarTitle(text = stringResource(R.string.weekly_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.weekly_review_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> CenteredMessage(
                text = stringResource(R.string.history_loading),
                showSpinner = true,
                modifier = Modifier.padding(innerPadding),
            )

            state.error != null -> Column(
                modifier = Modifier.padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = dataErrorMessage(state.error),
                    style = MaterialTheme.typography.bodyLarge,
                )
                AppTextButton(onClick = onRetry) {
                    Text(stringResource(R.string.weekly_review_retry))
                }
            }

            else -> WeeklyReviewBody(
                state = state,
                onOpenDeload = onOpenDeload,
                onDeloadLift = onDeloadLift,
                onReplaceLift = onReplaceLift,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun WeeklyReviewBody(
    state: WeeklyReviewUiState,
    onOpenDeload: (String) -> Unit,
    onDeloadLift: (String, String, String) -> Unit,
    onReplaceLift: (String, String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val start = state.weekStart
        val end = state.weekEnd
        if (start != null && end != null) {
            Text(
                text = stringResource(
                    R.string.weekly_review_range,
                    start.format(dateFormat),
                    end.format(dateFormat),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        SectionHeader(text = stringResource(R.string.weekly_review_summary_title))
        WeekSummaryCard(state = state)
        if (state.blocks.isNotEmpty()) {
            SectionHeader(text = stringResource(R.string.weekly_review_block_title))
            BlockLines(blocks = state.blocks)
        }
        SectionHeader(text = stringResource(R.string.weekly_review_days_title))
        PlannedDaysSection(days = state.days, dateFormat = dateFormat)
        SectionHeader(text = stringResource(R.string.weekly_review_moved_title))
        MovedSection(movements = state.movements)
        SectionHeader(text = stringResource(R.string.weekly_review_stall_title))
        StallSection(
            stall = state.stall,
            programId = state.stallProgramId,
            onOpenDeload = onOpenDeload,
            onDeloadLift = onDeloadLift,
            onReplaceLift = onReplaceLift,
        )
    }
}

/** The week's totals beside the plan's ratio, and last week's numbers for comparison (N247). */
@Composable
private fun WeekSummaryCard(state: WeeklyReviewUiState) {
    AppCard(modifier = Modifier.testTag(TestTags.WEEKLY_REVIEW_SUMMARY)) {
        Text(
            text = when {
                state.plan.planned > 0 -> pluralStringResource(
                    R.plurals.weekly_review_planned,
                    state.plan.planned,
                    state.plan.done,
                    state.plan.planned,
                )

                state.blocks.isNotEmpty() -> stringResource(R.string.weekly_review_block_summary)
                state.hasActivePlan -> stringResource(R.string.weekly_review_order_summary)
                else -> stringResource(R.string.weekly_review_no_plan)
            },
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(
                R.string.weekly_review_totals,
                pluralStringResource(
                    R.plurals.weekly_review_session_count,
                    state.thisWeek.sessions,
                    state.thisWeek.sessions,
                ),
                pluralStringResource(
                    R.plurals.weekly_review_set_count,
                    state.thisWeek.sets,
                    state.thisWeek.sets,
                ),
                Weight.format(state.thisWeek.volumeGrams, LocalWeightUnit.current),
                LocalWeightUnit.current.label(),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(
                R.string.weekly_review_last_week,
                pluralStringResource(
                    R.plurals.weekly_review_session_count,
                    state.lastWeek.sessions,
                    state.lastWeek.sessions,
                ),
                pluralStringResource(
                    R.plurals.weekly_review_set_count,
                    state.lastWeek.sets,
                    state.lastWeek.sets,
                ),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The planned days, each with what became of its occurrences (N247). */
@Composable
private fun PlannedDaysSection(days: List<ReviewDay>, dateFormat: DateTimeFormatter) {
    val plannedDays = days.filter { it.occurrences.isNotEmpty() }
    if (plannedDays.isEmpty()) {
        // The card above already says why a plan schedules no weekdays, so this sentence stays generic
        // (N256): two versions of "no weekday schedule" in one screen is one too many.
        Text(
            text = stringResource(R.string.weekly_review_no_days),
            style = MaterialTheme.typography.bodyMedium,
        )
    } else {
        plannedDays.forEach { day ->
            PlannedDay(day = day, dateFormat = dateFormat)
        }
    }
}

/** One line per followed block, so a plan without weekdays is still in the review (B329). */
@Composable
private fun BlockLines(blocks: List<BlockProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        blocks.forEach { block ->
            Text(
                text = if (block.complete) {
                    stringResource(R.string.adherence_block_complete, block.name)
                } else {
                    stringResource(
                        R.string.adherence_block_progress,
                        block.name,
                        block.position,
                        block.length,
                    )
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(TestTags.weeklyReviewBlock(block.name)),
            )
        }
    }
}

/** The lifts that beat themselves this week, or the sentence that says none did (N251). */
@Composable
private fun MovedSection(movements: List<MovementGain>) {
    if (movements.isEmpty()) {
        Text(
            text = stringResource(R.string.weekly_review_no_moved),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    val unit = LocalWeightUnit.current
    val label = unit.label()
    movements.forEach { gain ->
        val top = Weight.format(gain.topGrams, unit)
        val previous = gain.previousGrams
        Text(
            text = when {
                gain.firstTime -> stringResource(R.string.weekly_review_moved_first, gain.name)
                // Nothing to compare the week against: a movement trained before only at bodyweight, assisted
                // or held. The line says the number it has rather than dressing a zero as a "was" (B332).
                previous == null -> stringResource(R.string.weekly_review_moved_top, gain.name, top, label)
                else -> stringResource(
                    R.string.weekly_review_moved_line,
                    gain.name,
                    top,
                    label,
                    Weight.format(previous, unit),
                    label,
                )
            },
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** The first stopped plan, or the sentence that says there is none (N247). */
@Composable
private fun StallSection(
    stall: PlanStall?,
    programId: String?,
    onOpenDeload: (String) -> Unit,
    onDeloadLift: (String, String, String) -> Unit,
    onReplaceLift: (String, String, String) -> Unit,
) {
    if (stall != null && programId != null) {
        Box(modifier = Modifier.testTag(TestTags.WEEKLY_REVIEW_STALL)) {
            PlanStallCard(
                stall = stall,
                onDeloadLift = { onDeloadLift(programId, stall.templateId, stall.templateExerciseId) },
                onReplaceLift = { onReplaceLift(programId, stall.templateId, stall.templateExerciseId) },
                onOpenDeload = { onOpenDeload(programId) },
            )
        }
    } else {
        Text(
            text = stringResource(R.string.weekly_review_no_stall),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** One scheduled day: its date, and each occurrence with what became of it (N247). */
@Composable
private fun PlannedDay(day: ReviewDay, dateFormat: DateTimeFormatter) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = day.date.format(dateFormat),
            style = MaterialTheme.typography.labelLarge,
        )
        day.occurrences.forEach { occurrence ->
            Text(
                text = "${occurrence.templateName} · ${stateLabel(occurrence.state)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun stateLabel(state: OccurrenceState): String = when (state) {
    OccurrenceState.DONE -> stringResource(R.string.weekly_review_done)
    OccurrenceState.MISSED -> stringResource(R.string.weekly_review_missed)
    OccurrenceState.SKIPPED -> stringResource(R.string.weekly_review_skipped)
    OccurrenceState.PENDING -> stringResource(R.string.weekly_review_upcoming)
    OccurrenceState.DELOAD -> stringResource(R.string.weekly_review_deload)
}
