package com.example.androidapp.ui.programs

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.AutoProgressionCheck
import com.example.androidapp.domain.model.ProgressionMode
import com.example.androidapp.domain.model.ProgressionRule
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.theme.IndigoLink
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Auto progression's one switch, its preflight and its baseline (ROADMAP N299).
 *
 * Reached from the program editor's overflow rather than a sixth surface: it is about the program
 * that is open, and it says what will change before the switch is turned on. The preflight is the
 * same pure rule the app reads at *Done*, so the sheet cannot promise a step the plan will not take.
 */
@Composable
fun ProgramProgressionRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramProgressionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgramProgressionScreen(
        state = state,
        onBack = onBack,
        onSetAuto = viewModel::onSetAuto,
        onConfirmEnable = viewModel::onConfirmEnable,
        onDismissEnable = viewModel::onDismissEnable,
        onReset = viewModel::onResetToBaseline,
        onConfirmReset = viewModel::onConfirmReset,
        onDismissReset = viewModel::onDismissReset,
        onDismissError = viewModel::onErrorShown,
        onDismissRestoreMessage = viewModel::onRestoredShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
fun ProgramProgressionScreen(
    state: ProgramProgressionUiState,
    onBack: () -> Unit,
    onSetAuto: (Boolean) -> Unit,
    onConfirmEnable: () -> Unit,
    onDismissEnable: () -> Unit,
    onReset: () -> Unit,
    onConfirmReset: () -> Unit,
    onDismissReset: () -> Unit,
    onDismissError: () -> Unit,
    onDismissRestoreMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    // A failed *read* is the content; a failed *write* is a message over content the screen still has.
    val readFailed = state.programName.isEmpty() && state.error != null && !state.notFound
    val writeError = state.error?.takeIf { !readFailed && !state.notFound }
    val writeErrorText = writeError?.let { dataErrorMessage(it) }
    val restoredText = state.restored?.let {
        pluralStringResource(R.plurals.program_progression_restored, it, it)
    }
    // Read through `rememberUpdatedState` so an effect cannot fire a stale callback after a
    // recomposition while its message is on screen.
    val currentOnDismissError by rememberUpdatedState(onDismissError)
    val currentOnDismissRestoreMessage by rememberUpdatedState(onDismissRestoreMessage)

    LaunchedEffect(writeErrorText) {
        if (writeErrorText != null) {
            snackbar.showSnackbar(writeErrorText)
            currentOnDismissError()
        }
    }
    LaunchedEffect(restoredText) {
        if (restoredText != null) {
            snackbar.showSnackbar(restoredText)
            currentOnDismissRestoreMessage()
        }
    }

    if (state.confirmEnable) {
        AlertDialog(
            onDismissRequest = onDismissEnable,
            title = { Text(stringResource(R.string.program_progression_enable_title)) },
            text = { Text(stringResource(R.string.program_progression_enable_text)) },
            confirmButton = {
                Button(
                    onClick = onConfirmEnable,
                    modifier = Modifier.testTag(TestTags.Programs.AUTO_ENABLE),
                ) {
                    Text(stringResource(R.string.program_progression_enable_confirm))
                }
            },
            dismissButton = {
                AppTextButton(onClick = onDismissEnable) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
    if (state.confirmReset) {
        AlertDialog(
            onDismissRequest = onDismissReset,
            title = { Text(stringResource(R.string.program_progression_reset_title)) },
            text = { Text(stringResource(R.string.program_progression_reset_text)) },
            confirmButton = {
                Button(
                    onClick = onConfirmReset,
                    modifier = Modifier.testTag(TestTags.Programs.AUTO_RESET_CONFIRM),
                ) {
                    Text(stringResource(R.string.program_progression_reset_confirm))
                }
            },
            dismissButton = {
                AppTextButton(onClick = onDismissReset) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.program_progression_title)) },
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
        when {
            state.isLoading -> CenteredMessage(
                text = stringResource(R.string.active_workout_loading),
                showSpinner = true,
                modifier = Modifier.padding(innerPadding),
            )

            state.notFound -> CenteredMessage(
                text = stringResource(R.string.program_progression_missing),
                showSpinner = false,
                modifier = Modifier.padding(innerPadding),
            )

            readFailed -> CenteredMessage(
                text = dataErrorMessage(state.error),
                showSpinner = false,
                modifier = Modifier.padding(innerPadding),
            )

            else -> ProgramProgressionContent(
                state = state,
                onSetAuto = onSetAuto,
                onReset = onReset,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProgramProgressionContent(
    state: ProgramProgressionUiState,
    onSetAuto: (Boolean) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(TestTags.Programs.AUTO_PROGRESSION_SCREEN),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = state.programName, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.program_progression_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            AutoProgressionSwitchCard(
                auto = state.autoProgression,
                enabled = !state.writing,
                onSetAuto = onSetAuto,
            )
        }
        if (state.autoProgression && state.baselineAt != null) {
            item { AutoProgressionBaseline(baselineAt = state.baselineAt, onReset = onReset) }
        }
        if (state.preflight.checks.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.program_progression_empty),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            if (state.preflight.willProgress.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.program_progression_none_will),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            preflightSection(R.string.program_progression_will, state.preflight.willProgress) {
                stringResource(ruleLabel(it.rule))
            }
            preflightSection(R.string.program_progression_wont, state.preflight.blocked) {
                stringResource(ruleLabel(it.rule))
            }
            preflightSection(R.string.program_progression_overridden, state.preflight.overridden) {
                it.mode?.let { mode -> stringResource(modeLabel(mode)) }.orEmpty()
            }
        }
    }
}

/** The one switch, with the sentence that says what it will do. */
@Composable
private fun AutoProgressionSwitchCard(
    auto: Boolean,
    enabled: Boolean,
    onSetAuto: (Boolean) -> Unit,
) {
    val label = stringResource(R.string.program_progression_toggle)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(R.string.program_progression_toggle_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = auto,
                onCheckedChange = onSetAuto,
                enabled = enabled,
                modifier = Modifier
                    .testTag(TestTags.Programs.AUTO_SWITCH)
                    .semantics { contentDescription = label },
            )
        }
    }
}

/** Where the baseline started, and the one action that puts it back. */
@Composable
private fun AutoProgressionBaseline(baselineAt: Instant, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(
                R.string.program_progression_baseline,
                BASELINE_DATE.format(baselineAt.atZone(ZoneId.systemDefault()).toLocalDate()),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        AppTextButton(
            onClick = onReset,
            modifier = Modifier.testTag(TestTags.Programs.AUTO_RESET),
        ) {
            Text(stringResource(R.string.program_progression_reset))
        }
    }
}

/** One named group of preflight rows, kept out of the list body. */
private fun androidx.compose.foundation.lazy.LazyListScope.preflightSection(
    @StringRes title: Int,
    checks: List<AutoProgressionCheck>,
    label: @Composable (AutoProgressionCheck) -> String,
) {
    if (checks.isEmpty()) return
    item(key = "section-$title") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = IndigoLink,
        )
    }
    items(items = checks) { check ->
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = check.exerciseName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${check.templateName} · ${label(check)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            blockedHint(check.rule)?.let { hint ->
                Text(
                    text = stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The sentence beside a plan that can move. */
@StringRes
private fun ruleLabel(rule: ProgressionRule): Int = when (rule) {
    ProgressionRule.REPS_THEN_LOAD -> R.string.program_progression_rule_reps_then_load
    ProgressionRule.REPS_ONLY -> R.string.program_progression_rule_reps_only
    ProgressionRule.LOAD_ONLY -> R.string.program_progression_rule_load_only
    ProgressionRule.HOLD -> R.string.program_progression_rule_hold
    ProgressionRule.NO_TARGET_RPE -> R.string.program_progression_rule_no_target_rpe
    ProgressionRule.NO_REPS -> R.string.program_progression_rule_no_reps
    ProgressionRule.NOTHING_TO_MOVE -> R.string.program_progression_rule_nothing
}

/** The sentence beside a movement that answers for itself. */
@StringRes
private fun modeLabel(mode: ProgressionMode): Int = when (mode) {
    ProgressionMode.SUGGEST -> R.string.settings_progression_suggest
    ProgressionMode.APPLY_AT_DONE -> R.string.settings_progression_apply
    ProgressionMode.OFF -> R.string.settings_progression_off
}

/** What to change so a blocked plan can move, or null where the label already says it. */
@StringRes
private fun blockedHint(rule: ProgressionRule): Int? = when (rule) {
    ProgressionRule.NO_TARGET_RPE -> R.string.program_progression_blocked_no_rpe
    ProgressionRule.NO_REPS -> R.string.program_progression_blocked_no_reps
    ProgressionRule.NOTHING_TO_MOVE -> R.string.program_progression_blocked_nothing
    else -> null
}

private val BASELINE_DATE: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
