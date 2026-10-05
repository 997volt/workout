package com.example.androidapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.RestTimer
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.ui.components.label
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.AppRow
import com.example.androidapp.ui.components.ClearEverythingDialog
import com.example.androidapp.ui.components.FailureMessage
import com.example.androidapp.ui.components.IconTile
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.SectionHeader
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.TopBarTitle
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.theme.TileAccent
import com.example.androidapp.ui.transfer.ClearOutcome
import com.example.androidapp.ui.transfer.DataTransferViewModel
import com.example.androidapp.ui.transfer.rememberDataTransferActions
import kotlinx.coroutines.launch

/** Five chips fit a phone's width; a sixth runs off it. */
private const val CHOICES_PER_ROW = 5

/**
 * The app's settings (ROADMAP N21), and the app's data (N43).
 *
 * One setting was the reason the screen existed: the default rest was a hardcoded 90 seconds that
 * nothing could change. The rest sound and keep-screen-on followed (N27), and export, import and
 * delete-everything moved here from the home overflow (N43) — they act on the whole database, which
 * is what this screen is about.
 */
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    transferViewModel: DataTransferViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The data actions report into a snackbar and a held failure, the shape the home screen used
    // before they moved (F7, F8). The clear is suspending and its sentence is a resource, so it
    // needs a scope and a string resolved during composition.
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val transferActions = rememberDataTransferActions(transferViewModel) { message = it }
    val clearedText = stringResource(R.string.clear_done)
    var failure by remember { mutableStateOf<DataError?>(null) }
    FailureMessage(failure = failure, onMessage = { message = it }, onClear = { failure = null })

    SettingsScreen(
        state = state,
        onSetDefaultRest = viewModel::onSetDefaultRest,
        onSetRestCue = viewModel::onSetRestCue,
        onSetKeepScreenOn = viewModel::onSetKeepScreenOn,
        onSetRestTimer = viewModel::onSetRestTimer,
        onSetProgressionPrompt = viewModel::onSetProgressionPrompt,
        onSetWeightUnit = viewModel::onSetWeightUnit,
        onExportData = transferActions.export,
        onImportData = transferActions.import,
        onClearData = {
            scope.launch {
                when (val outcome = transferViewModel.clearEverything()) {
                    is ClearOutcome.Cleared -> message = clearedText
                    is ClearOutcome.Failed -> failure = outcome.error
                }
            }
        },
        message = message,
        onDismissMessage = { message = null },
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onSetDefaultRest: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSetRestCue: (Boolean) -> Unit = {},
    onSetKeepScreenOn: (Boolean) -> Unit = {},
    onSetRestTimer: (Boolean) -> Unit = {},
    onSetProgressionPrompt: (Boolean) -> Unit = {},
    onSetWeightUnit: (WeightUnit) -> Unit = {},
    onExportData: () -> Unit = {},
    onImportData: () -> Unit = {},
    onClearData: () -> Unit = {},
    message: String? = null,
    onDismissMessage: () -> Unit = {},
) {
    var confirmingClear by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    MessageSnackbar(message, snackbarHostState, onDismissMessage)

    ClearConfirmation(
        visible = confirmingClear,
        onExport = onExportData,
        onConfirm = {
            confirmingClear = false
            onClearData()
        },
        onDismiss = { confirmingClear = false },
    )

    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.SETTINGS_SCREEN),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { TopBarTitle(text = stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        SettingsBody(
            state = state,
            onSetDefaultRest = onSetDefaultRest,
            onSetRestCue = onSetRestCue,
            onSetKeepScreenOn = onSetKeepScreenOn,
            onSetRestTimer = onSetRestTimer,
            onSetProgressionPrompt = onSetProgressionPrompt,
            onSetWeightUnit = onSetWeightUnit,
            onExportData = onExportData,
            onImportData = onImportData,
            onClearData = { confirmingClear = true },
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The scrollable settings content, split out so the screen stays a scaffold (ROADMAP N43).
 *
 * The Data section is last, so the rest choices stay where the screen's first user left them and
 * the irreversible action sits at the bottom.
 */
@Composable
private fun SettingsBody(
    state: SettingsUiState,
    onSetDefaultRest: (Int) -> Unit,
    onSetRestCue: (Boolean) -> Unit,
    onSetKeepScreenOn: (Boolean) -> Unit,
    onSetRestTimer: (Boolean) -> Unit,
    onSetProgressionPrompt: (Boolean) -> Unit,
    onSetWeightUnit: (WeightUnit) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    onClearData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // The unit comes first: it is the one preference that changes what every other screen
        // says, rather than how this one behaves (ROADMAP N64).
        WeightUnitSection(
            state = state,
            onSetWeightUnit = onSetWeightUnit,
        )
        RestDefaultSection(
            state = state,
            onSetDefaultRest = onSetDefaultRest,
        )
        WorkoutSwitches(
            state = state,
            onSetRestCue = onSetRestCue,
            onSetKeepScreenOn = onSetKeepScreenOn,
            onSetRestTimer = onSetRestTimer,
            onSetProgressionPrompt = onSetProgressionPrompt,
        )
        DataSection(
            onExport = onExportData,
            onImport = onImportData,
            onClear = onClearData,
        )
        // Outside the cards: a write that failed is not part of the setting it failed on,
        // and inside a card it would read as the value.
        state.error?.let { error ->
            Text(
                text = dataErrorMessage(error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * The typed confirmation behind the clear, split out so the screen stays a scaffold (N43).
 *
 * Split rather than inlined because the screen around it is at the length this project allows, and
 * because "is it open" is one boolean and one dialog wherever it is shown.
 */
@Composable
private fun ClearConfirmation(
    visible: Boolean,
    onExport: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    ClearEverythingDialog(
        onExport = onExport,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * The app's data: export, import and the one irreversible action (ROADMAP N43).
 *
 * Delete-everything is last and coloured, because it is still the one entry that can cost the user
 * something; the typed confirmation behind it is the guard, and the export inside that dialog is
 * the only thing platform backup being off leaves to survive it.
 */
@Composable
private fun DataSection(
    onExport: () -> Unit,
    onImport: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(text = stringResource(R.string.settings_data_title))
        AppCard {
            AppRow(
                headline = stringResource(R.string.transfer_export),
                supporting = stringResource(R.string.settings_data_export_hint),
                leading = { IconTile(icon = Icons.Filled.Upload, accent = TileAccent.Sky) },
                onClick = onExport,
                onClickLabel = stringResource(R.string.transfer_export),
                testTag = TestTags.DATA_EXPORT,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AppRow(
                headline = stringResource(R.string.transfer_import),
                supporting = stringResource(R.string.settings_data_import_hint),
                leading = { IconTile(icon = Icons.Filled.Download, accent = TileAccent.Indigo) },
                onClick = onImport,
                onClickLabel = stringResource(R.string.transfer_import),
                testTag = TestTags.DATA_IMPORT,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AppRow(
                headline = stringResource(R.string.clear_menu),
                supporting = stringResource(R.string.settings_data_clear_hint),
                leading = { IconTile(icon = Icons.Filled.Delete, accent = TileAccent.Coral) },
                onClick = onClear,
                onClickLabel = stringResource(R.string.clear_menu),
                testTag = TestTags.SETTINGS_CLEAR_DATA,
                headlineColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** One row of rest choices, each one tap. */
@Composable
private fun RestChoiceRow(
    choices: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { seconds ->
            AppFilterChip(
                selected = seconds == selected,
                onClick = { onSelect(seconds) },
                label = RestTimer.format(seconds),
                testTag = TestTags.settingRest(seconds),
            )
        }
    }
}

/** A labelled switch, with the sentence that says what turning it on does. */
@Composable
private fun SettingSwitch(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag),
            )
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The switches that change how a workout behaves (ROADMAP N27, N44, N66). */
@Composable
private fun WorkoutSwitches(
    state: SettingsUiState,
    onSetRestCue: (Boolean) -> Unit,
    onSetKeepScreenOn: (Boolean) -> Unit,
    onSetRestTimer: (Boolean) -> Unit,
    onSetProgressionPrompt: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        SettingSwitch(
            label = stringResource(R.string.settings_rest_timer),
            hint = stringResource(R.string.settings_rest_timer_hint),
            checked = state.restTimerEnabled,
            onCheckedChange = onSetRestTimer,
            testTag = TestTags.SETTINGS_REST_TIMER,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingSwitch(
            label = stringResource(R.string.settings_progression),
            hint = stringResource(R.string.settings_progression_hint),
            checked = state.progressionPromptEnabled,
            onCheckedChange = onSetProgressionPrompt,
            testTag = TestTags.SETTINGS_PROGRESSION,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingSwitch(
            label = stringResource(R.string.settings_rest_cue),
            hint = stringResource(R.string.settings_rest_cue_hint),
            checked = state.restCueEnabled,
            onCheckedChange = onSetRestCue,
            testTag = TestTags.SETTINGS_REST_CUE,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingSwitch(
            label = stringResource(R.string.settings_keep_screen_on),
            hint = stringResource(R.string.settings_keep_screen_on_hint),
            checked = state.keepScreenOn,
            onCheckedChange = onSetKeepScreenOn,
            testTag = TestTags.SETTINGS_KEEP_SCREEN_ON,
        )
    }
}

/**
 * The unit every weight is shown and typed in (ROADMAP N64).
 *
 * Two chips rather than a switch: a switch has an on and an off, and these are two names for the same
 * setting. An exercise may override it for itself, which is said here rather than left to be found.
 */
@Composable
private fun WeightUnitSection(
    state: SettingsUiState,
    onSetWeightUnit: (WeightUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_weight_unit),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.settings_weight_unit_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WeightUnit.entries.forEach { unit ->
                AppFilterChip(
                    selected = unit == state.weightUnit,
                    onClick = { onSetWeightUnit(unit) },
                    label = unit.label(),
                    testTag = unit.chipTag(),
                )
            }
        }
    }
}

/** The tag the chip for one unit answers to (ROADMAP N64). */
private fun WeightUnit.chipTag(): String = when (this) {
    WeightUnit.KILOGRAMS -> TestTags.SETTINGS_WEIGHT_UNIT_KG
    WeightUnit.POUNDS -> TestTags.SETTINGS_WEIGHT_UNIT_LB
}

/** The default rest: what is in force, what it means, and the choices (ROADMAP N21). */
@Composable
private fun RestDefaultSection(
    state: SettingsUiState,
    onSetDefaultRest: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_rest_title),
            style = MaterialTheme.typography.titleMedium,
        )
        // The current value is the loudest thing in the card: it is the answer to the question
        // the section's title asks, and the chips below are only how you change it.
        Text(
            text = stringResource(
                R.string.settings_rest_current,
                RestTimer.format(state.defaultRestSeconds),
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .testTag(TestTags.SETTINGS_REST_CURRENT),
        )
        Text(
            text = stringResource(R.string.settings_rest_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.choices.chunked(CHOICES_PER_ROW).forEach { row ->
                RestChoiceRow(
                    choices = row,
                    selected = state.defaultRestSeconds,
                    onSelect = onSetDefaultRest,
                )
            }
        }
    }
}
