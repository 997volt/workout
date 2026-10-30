package com.example.androidapp.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.data.Housekeeping
import com.example.androidapp.platform.CrashLog
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import java.time.Instant

/**
 * The way a crash reaches someone who can fix it (ROADMAP N244).
 *
 * The app records crashes and failed housekeeping passes to disk but had no screen to read them, so a report
 * cost a backup and a reproduction. This hands over the last of each, with **Copy details** for the person on
 * the other end and a retry for a failed pass.
 */
@Composable
fun DiagnosticsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DiagnosticsScreen(
        state = state,
        onBack = onBack,
        onCopy = { text ->
            context.getSystemService(ClipboardManager::class.java)
                ?.setPrimaryClip(ClipData.newPlainText("Workout diagnostics", text))
        },
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DiagnosticsScreen(
    state: DiagnosticsUiState,
    onBack: () -> Unit,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.DIAGNOSTICS_SCREEN),
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopBarTitle(text = stringResource(R.string.diagnostics_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.diagnostics_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        DiagnosticsBody(
            state = state,
            onCopy = onCopy,
            onRetry = onRetry,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun DiagnosticsBody(
    state: DiagnosticsUiState,
    onCopy: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.diagnostics_version, state.appVersion, state.versionCode),
            style = MaterialTheme.typography.titleMedium,
        )
        SectionHeader(text = stringResource(R.string.diagnostics_crash_title))
        CrashCard(crash = state.crash)
        SectionHeader(text = stringResource(R.string.diagnostics_repair_title))
        RepairCard(repair = state.repair, retrying = state.retrying, onRetry = onRetry)
        Button(
            onClick = { onCopy(diagnosticsReport(state)) },
            modifier = Modifier.testTag(TestTags.DIAGNOSTICS_COPY),
        ) {
            Text(stringResource(R.string.diagnostics_copy))
        }
    }
}

@Composable
private fun CrashCard(crash: CrashLog?) {
    AppCard(modifier = Modifier.testTag(TestTags.DIAGNOSTICS_CRASH)) {
        if (crash == null) {
            Text(stringResource(R.string.diagnostics_crash_none), style = MaterialTheme.typography.bodyMedium)
            return@AppCard
        }
        Text(
            text = stringResource(
                R.string.diagnostics_repair_when,
                Instant.ofEpochMilli(crash.timestamp).toString(),
                crash.exceptionClass,
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        SelectionContainer {
            Text(
                text = crash.stackTrace,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun RepairCard(
    repair: Housekeeping.Failure?,
    retrying: Boolean,
    onRetry: () -> Unit,
) {
    AppCard(modifier = Modifier.testTag(TestTags.DIAGNOSTICS_REPAIR)) {
        if (repair == null) {
            Text(stringResource(R.string.diagnostics_repair_none), style = MaterialTheme.typography.bodyMedium)
            return@AppCard
        }
        Text(
            text = stringResource(
                R.string.diagnostics_repair_when,
                Instant.ofEpochMilli(repair.at).toString(),
                repair.exceptionClass,
            ),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.diagnostics_repair_reason),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onRetry,
            enabled = !retrying,
            modifier = Modifier.testTag(TestTags.DIAGNOSTICS_RETRY),
        ) {
            Text(stringResource(if (retrying) R.string.diagnostics_retrying else R.string.diagnostics_retry))
        }
    }
}

/** The facts **Copy details** hands over: the build, the last crash and the last failed pass. */
internal fun diagnosticsReport(state: DiagnosticsUiState): String = buildString {
    appendLine("Workout ${state.appVersion} (versionCode ${state.versionCode})")
    val crash = state.crash
    if (crash == null) {
        appendLine("No crash recorded.")
    } else {
        appendLine("Crash at ${Instant.ofEpochMilli(crash.timestamp)}: ${crash.exceptionClass}")
        appendLine("Android ${crash.androidVersion}, ${crash.deviceModel}")
        appendLine(crash.stackTrace)
    }
    val repair = state.repair
    if (repair == null) {
        appendLine("Housekeeping: finished.")
    } else {
        appendLine("Housekeeping failed at ${Instant.ofEpochMilli(repair.at)}: ${repair.exceptionClass}")
        appendLine(repair.frames)
    }
}
