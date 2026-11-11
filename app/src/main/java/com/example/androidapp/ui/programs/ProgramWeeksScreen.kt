package com.example.androidapp.ui.programs

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.example.androidapp.domain.model.DeloadSuggestion
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.history.HistoryFormat
import java.time.LocalDate
import java.time.ZoneId

/** The recovery-week choices for one program (ROADMAP N300). */
@Composable
fun ProgramWeeksRoute(
    onBack: () -> Unit,
    onBuild: () -> Unit,
    viewModel: ProgramWeeksViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The pair of weeks follows the device rather than this composition (B415, N286).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
    ProgramWeeksScreen(
        state = state,
        onBack = onBack,
        onBuild = onBuild,
        onSetExempt = viewModel::onSetExempt,
        onDismissError = viewModel::onErrorShown,
    )
}

/**
 * *Exempt this/next week* beside *Build a lighter week* (ROADMAP N300).
 *
 * One destination for both because they are one decision: the mark is the exemption from scoring, the
 * built plan is the lighter training. A week that has not started can be exempted here, which the
 * calendar cannot do; the builder already targets next week and reuses the mark.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramWeeksScreen(
    state: ProgramWeeksUiState,
    onBack: () -> Unit,
    onBuild: () -> Unit,
    onSetExempt: (LocalDate, Boolean) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val currentOnError by rememberUpdatedState(onDismissError)
    state.error?.let { failure ->
        val message = dataErrorMessage(failure)
        LaunchedEffect(message) {
            snackbar.showSnackbar(message)
            currentOnError()
        }
    }
    var unmarking by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.Programs.WEEKS_SCREEN),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { TopBarTitle(text = stringResource(R.string.program_weeks_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.nav_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        ProgramWeeksContent(
            state = state,
            onBuild = onBuild,
            onSetExempt = onSetExempt,
            onUnmark = { unmarking = it },
            modifier = Modifier.padding(innerPadding),
        )
    }

    unmarking?.let { week ->
        UnmarkExemptionDialog(
            onDismiss = { unmarking = null },
            onConfirm = {
                unmarking = null
                onSetExempt(week, false)
            },
        )
    }
}

@Composable
private fun ProgramWeeksContent(
    state: ProgramWeeksUiState,
    onBuild: () -> Unit,
    onSetExempt: (LocalDate, Boolean) -> Unit,
    onUnmark: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredMessage(
            text = stringResource(R.string.programs_loading),
            modifier = modifier,
        )

        state.notFound -> CenteredMessage(
            text = stringResource(R.string.program_weeks_not_found),
            modifier = modifier,
        )

        else -> RecoveryWeekChoices(
            state = state,
            onBuild = onBuild,
            onSetExempt = onSetExempt,
            onUnmark = onUnmark,
            modifier = modifier,
        )
    }
}

@Composable
private fun RecoveryWeekChoices(
    state: ProgramWeeksUiState,
    onBuild: () -> Unit,
    onSetExempt: (LocalDate, Boolean) -> Unit,
    onUnmark: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.programName,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.program_weeks_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        state.suggestion?.takeIf { it.due }?.let { due ->
            DeloadSuggestionCard(suggestion = due, onBuild = onBuild)
        }
        state.thisWeek?.let { week ->
            WeekCard(
                title = stringResource(R.string.program_weeks_this_week),
                week = week,
                exempt = state.thisWeekExempt,
                hasPlan = state.thisWeekHasPlan,
                writing = state.writing,
                tag = TestTags.Programs.WEEKS_THIS,
                text = THIS_WEEK_TEXT,
                onToggle = { exempt, plan ->
                    if (exempt && plan) onUnmark(week) else onSetExempt(week, !exempt)
                },
            )
        }
        state.nextWeek?.let { week ->
            WeekCard(
                title = stringResource(R.string.program_weeks_next_week),
                week = week,
                exempt = state.nextWeekExempt,
                hasPlan = state.nextWeekHasPlan,
                writing = state.writing,
                tag = TestTags.Programs.WEEKS_NEXT,
                text = NEXT_WEEK_TEXT,
                onToggle = { exempt, plan ->
                    if (exempt && plan) onUnmark(week) else onSetExempt(week, !exempt)
                },
            )
        }
        BuildDeloadCard(onBuild = onBuild)
    }
}

/** The cadence's sentence: a suggestion, with the creator one tap away (ROADMAP N301). */
@Composable
private fun DeloadSuggestionCard(suggestion: DeloadSuggestion, onBuild: () -> Unit) {
    AppCard(modifier = Modifier.testTag(TestTags.Programs.WEEKS_SUGGESTION)) {
        Text(
            text = stringResource(R.string.program_weeks_suggestion_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = if (suggestion.sinceLastDeload) {
                pluralStringResource(
                    R.plurals.program_weeks_suggestion_since_last,
                    suggestion.weeksSince,
                    suggestion.weeksSince,
                )
            } else {
                pluralStringResource(
                    R.plurals.program_weeks_suggestion_since_start,
                    suggestion.weeksSince,
                    suggestion.weeksSince,
                )
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AppTextButton(
            onClick = onBuild,
            modifier = Modifier.testTag(TestTags.Programs.WEEKS_SUGGESTION_BUILD),
        ) {
            Text(stringResource(R.string.program_weeks_build))
        }
    }
}

/** The other action: the creator, which writes a lighter plan rather than marking a week. */
@Composable
private fun BuildDeloadCard(onBuild: () -> Unit) {
    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBuild)
            .testTag(TestTags.Programs.WEEKS_BUILD),
    ) {
        Text(
            text = stringResource(R.string.program_weeks_build),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.program_weeks_build_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UnmarkExemptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.Programs.WEEKS_UNMARK_DIALOG),
        title = { Text(stringResource(R.string.program_weeks_unmark_title)) },
        text = { Text(stringResource(R.string.program_weeks_unmark_text)) },
        confirmButton = {
            AppTextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.Programs.WEEKS_UNMARK_CONFIRM),
            ) {
                Text(stringResource(R.string.program_weeks_unmark_confirm))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/**
 * The three sentences one week's card states (ROADMAP N300, B399).
 *
 * A card's control acts on one week, so the words cannot be shared between the two cards: the next-week
 * card saying "Exempt this week" names the wrong week to the lifter and to a screen reader, and there is
 * no state a reader can use to tell the two controls apart.
 */
private data class WeekCardText(
    @StringRes val exempt: Int,
    @StringRes val exempted: Int,
    @StringRes val hasPlan: Int,
)

private val THIS_WEEK_TEXT = WeekCardText(
    exempt = R.string.program_weeks_exempt,
    exempted = R.string.program_weeks_exempted,
    hasPlan = R.string.program_weeks_has_plan,
)

private val NEXT_WEEK_TEXT = WeekCardText(
    exempt = R.string.program_weeks_exempt_next,
    exempted = R.string.program_weeks_exempted_next,
    hasPlan = R.string.program_weeks_has_plan_next,
)

@Composable
private fun WeekCard(
    title: String,
    week: LocalDate,
    exempt: Boolean,
    hasPlan: Boolean,
    writing: Boolean,
    tag: String,
    text: WeekCardText,
    onToggle: (exempt: Boolean, hasPlan: Boolean) -> Unit,
) {
    AppCard {
        val zone = ZoneId.systemDefault()
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Text(
            // The one history formatter, so this date reads like every other (B373, B408).
            text = HistoryFormat.date(week.atStartOfDay(zone).toInstant(), zone),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (exempt) {
                stringResource(text.exempted)
            } else {
                stringResource(R.string.program_weeks_exempt_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (exempt && hasPlan) {
            Text(
                text = stringResource(text.hasPlan),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AppTextButton(
            onClick = { onToggle(exempt, hasPlan) },
            enabled = !writing,
            modifier = Modifier.testTag(tag),
        ) {
            Text(
                if (exempt) {
                    stringResource(R.string.program_weeks_remove)
                } else {
                    stringResource(text.exempt)
                },
            )
        }
    }
}
