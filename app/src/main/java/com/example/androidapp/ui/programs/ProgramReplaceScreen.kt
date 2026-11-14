package com.example.androidapp.ui.programs

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.ReplaceOutcome
import com.example.androidapp.domain.model.ReplacementSeed
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.LocalWeightUnit
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage

/** Replacing one plan's movement (ROADMAP N308): the new row leaves via [onReplaced], the plan shows undo. */
@Composable
fun ProgramReplaceRoute(
    onBack: () -> Unit,
    onBrowse: () -> Unit,
    onReplace: (ReplaceOutcome) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgramReplaceViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnReplace by rememberUpdatedState(onReplace)
    LaunchedEffect(state.replaced) {
        state.replaced?.let(currentOnReplace)
    }
    ProgramReplaceScreen(
        state = state,
        onSelect = viewModel::select,
        onSetEmpty = viewModel::setEmpty,
        onBrowse = onBrowse,
        onConfirm = viewModel::confirm,
        onBack = onBack,
        onDismissMessage = viewModel::onErrorShown,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProgramReplaceScreen(
    state: ProgramReplaceUiState,
    onSelect: (String) -> Unit,
    onSetEmpty: (Boolean) -> Unit,
    onBrowse: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.error?.let { dataErrorMessage(it) }
    val currentDismiss by rememberUpdatedState(onDismissMessage)
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            currentDismiss()
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize().testTag(TestTags.Programs.REPLACE_SCREEN),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.program_replace_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.program_replace_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Column(
                modifier = Modifier.padding(innerPadding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.program_replace_loading))
            }

            else -> ProgramReplaceBody(
                state = state,
                onSelect = onSelect,
                onSetEmpty = onSetEmpty,
                onBrowse = onBrowse,
                onConfirm = onConfirm,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun ProgramReplaceBody(
    state: ProgramReplaceUiState,
    onSelect: (String) -> Unit,
    onSetEmpty: (Boolean) -> Unit,
    onBrowse: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.program_replace_old, state.oldName),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.program_replace_sets, state.setCountBefore, state.setCountAfter),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(stringResource(R.string.program_replace_with), style = MaterialTheme.typography.titleSmall)
        state.suggestions.forEach { exercise ->
            ReplaceOption(
                exercise = exercise,
                selected = state.selected?.id == exercise.id,
                onClick = { onSelect(exercise.id) },
            )
        }
        AppTextButton(onClick = onBrowse) {
            Text(stringResource(R.string.program_replace_browse))
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onSetEmpty(!state.empty) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = state.empty,
                onCheckedChange = onSetEmpty,
                modifier = Modifier.testTag(TestTags.Programs.REPLACE_EMPTY),
            )
            Column {
                Text(stringResource(R.string.program_replace_empty))
                Text(
                    text = stringResource(R.string.program_replace_empty_hint),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        ReplaceConfirmSection(state, onConfirm)
    }
}

/** What the chosen movement does to the plan: the seed or Empty, sharing, and the write. */
@Composable
private fun ReplaceConfirmSection(state: ProgramReplaceUiState, onConfirm: () -> Unit) {
    val selected = state.selected
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (selected != null) {
            Text(selected.name, style = MaterialTheme.typography.titleSmall)
            if (state.empty) {
                Text(
                    text = stringResource(R.string.program_replace_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val seeded = state.seeds.filterNotNull()
                Text(
                    text = if (seeded.isEmpty()) {
                        stringResource(R.string.program_replace_no_seed)
                    } else {
                        stringResource(R.string.program_replace_seeded, seedSummary(seeded.first()))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.sharedProgramNames.isNotEmpty()) {
                Text(
                    text = stringResource(
                        R.string.program_replace_shared,
                        state.sharedProgramNames.joinToString(", "),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Button(
            onClick = onConfirm,
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().testTag(TestTags.Programs.REPLACE_CONFIRM),
        ) {
            Text(stringResource(R.string.program_replace_confirm))
        }
    }
}

/** One suggestion: a movement filed under the old one's family (N95/N110). */
@Composable
private fun ReplaceOption(exercise: Exercise, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null)
        }
    }
}

/** The first seed, as the confirm reads it: the load and the reps or the hold. */
@Composable
private fun seedSummary(seed: ReplacementSeed): String {
    val unit = LocalWeightUnit.current
    val load = Weight.display(seed.weightGrams, seed.assistanceGrams, unit)
    return seed.durationSeconds?.let { "$load · $it s" } ?: "$load × ${seed.reps}"
}
