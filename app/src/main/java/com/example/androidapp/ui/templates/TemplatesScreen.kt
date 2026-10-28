package com.example.androidapp.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.programs.programStartGate
import com.example.androidapp.ui.programs.StartIntent
import com.example.androidapp.ui.theme.AndroidAppTheme

/**
 * The template list (ROADMAP N3).
 *
 * A template is started from here rather than from home: the home start action
 * offers the *choice* (empty, or from a template), and this is where the choice is
 * made concrete. The same screen is also the way in to editing one.
 */
@Composable
fun TemplatesRoute(
    onOpenTemplate: (String) -> Unit,
    onStartTemplate: (String) -> Unit,
    onBack: () -> Unit,
    /** The sibling list (ROADMAP N93): the two are one tap apart, from either one's own bar. */
    onOpenPrograms: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TemplatesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val created by viewModel.createdTemplateId.collectAsStateWithLifecycle()
    val currentOnOpenTemplate by rememberUpdatedState(onOpenTemplate)

    // Starting a plan is a start like any other, so the program's missed-day question is
    // asked here too (ROADMAP P3.3). The gate is the home screen's, shared rather than
    // copied: "which day did I miss" is one rule, not two.
    var gateMessage by remember { mutableStateOf<String?>(null) }
    val requestStart = programStartGate(
        onStart = { intent -> intent.templateId?.let(onStartTemplate) },
        onError = { gateMessage = it },
    )

    // A newly created template opens straight into its editor: it has no exercises
    // yet and its name is still the default, so there is nothing to see in a row.
    LaunchedEffect(created) {
        created?.let {
            currentOnOpenTemplate(it)
            viewModel.onCreatedHandled()
        }
    }

    TemplatesScreen(
        state = state,
        onCreateTemplate = viewModel::onCreateTemplate,
        onOpenTemplate = onOpenTemplate,
        onStartTemplate = { templateId ->
            requestStart(
                StartIntent(
                    templateId = templateId,
                    label = state.templates.firstOrNull { it.id == templateId }?.name,
                ),
            )
        },
        onDismissMessage = viewModel::onErrorShown,
        onFilterChange = viewModel::onFilterChange,
        onUnhide = viewModel::onUnhide,
        onRestore = viewModel::onRestore,
        onBack = onBack,
        onOpenPrograms = onOpenPrograms,
        modifier = modifier,
        message = gateMessage,
        onDismissGateMessage = { gateMessage = null },
    )
}

/** The list's own bar: the title, back, and the way across to Programs (ROADMAP N93, N214). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesTopBar(onBack: () -> Unit, onOpenPrograms: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.templates_title),
                modifier = Modifier.testTag(TestTags.TEMPLATES_TITLE),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // The sibling list, one tap away (ROADMAP N93): Programs was back-home-and-in from
            // here, and the two screens answer the same question from two sides. It is a way
            // across rather than a start, so it says nothing about a session being open — the
            // rows below carry that withholding themselves (N78, B43).
            AppTextButton(
                onClick = onOpenPrograms,
                modifier = Modifier.testTag(TestTags.TEMPLATES_PROGRAMS),
            ) {
                Text(stringResource(R.string.home_programs))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    state: TemplatesUiState,
    onCreateTemplate: (String) -> Unit,
    onOpenTemplate: (String) -> Unit,
    onStartTemplate: (String) -> Unit,
    onBack: () -> Unit,
    /** The way across to Programs (ROADMAP N93), in this screen's own bar. */
    onOpenPrograms: () -> Unit,
    onDismissMessage: () -> Unit,
    onDismissGateMessage: () -> Unit,
    /** The list's own filter (ROADMAP N217): which group of the library this list draws. */
    onFilterChange: (TemplatesFilter) -> Unit,
    /** Puts a workout back in the choices from its row, without opening the editor (N214). */
    onUnhide: (String) -> Unit,
    /** Brings a deleted workout back, with its plan (ROADMAP N216). */
    onRestore: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** A sentence from the point-of-start question, shown on the same host (ROADMAP P3.3). */
    message: String? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultName = stringResource(R.string.template_default_name)
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)
    MessageSnackbar(message, snackbarHostState, onDismissGateMessage)

    // The message is resolved during composition and shown from the effect, because
    // a string resource cannot be read inside LaunchedEffect.
    state.error?.let { failure ->
        val failureMessage = dataErrorMessage(failure)
        LaunchedEffect(failureMessage) {
            snackbarHostState.showSnackbar(failureMessage)
            currentOnDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TemplatesTopBar(onBack = onBack, onOpenPrograms = onOpenPrograms) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreateTemplate(defaultName) },
                text = { Text(stringResource(R.string.template_new)) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.testTag(TestTags.TEMPLATES_NEW),
            )
        },
    ) { innerPadding ->
        TemplatesContent(
            state = state,
            onOpenTemplate = onOpenTemplate,
            onStartTemplate = onStartTemplate,
            onUnhide = onUnhide,
            onRestore = onRestore,
            onFilterChange = onFilterChange,
            modifier = Modifier.padding(innerPadding),
            startEnabled = !state.hasActiveWorkout,
        )
    }
}

@Composable
private fun TemplatesContent(
    state: TemplatesUiState,
    onOpenTemplate: (String) -> Unit,
    onStartTemplate: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onRestore: (String) -> Unit,
    onFilterChange: (TemplatesFilter) -> Unit,
    modifier: Modifier = Modifier,
    /** False while a session is open, which one template list cannot start (ROADMAP N78). */
    startEnabled: Boolean = true,
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Only when there is a group to reach: a library with nothing put away gains no control it never
        // needed, and these chips are the one place the put-away rows come back (ROADMAP N214, N217).
        if (state.hiddenCount > 0 || state.deletedCount > 0) {
            TemplatesFilterRow(state = state, onFilterChange = onFilterChange)
        }
        when {
            state.isLoading -> CenteredMessage(
                text = stringResource(R.string.templates_loading),
                modifier = Modifier.fillMaxSize(),
            )

            state.templates.isEmpty() -> TemplatesEmpty(state)

            else -> TemplateRows(
                state = state,
                onOpenTemplate = onOpenTemplate,
                onStartTemplate = onStartTemplate,
                onUnhide = onUnhide,
                onRestore = onRestore,
                startEnabled = startEnabled,
            )
        }
    }
}

/**
 * The three modes, each drawing its own group (ROADMAP N217), with N216's deleted chip beside them.
 *
 * A chip is drawn only when its group holds something: the count is the control's reason to exist, and
 * a filter that can only draw nothing is a question with no answer.
 */
@Composable
private fun TemplatesFilterRow(
    state: TemplatesUiState,
    onFilterChange: (TemplatesFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppFilterChip(
            selected = state.filter == TemplatesFilter.ALL,
            onClick = { onFilterChange(TemplatesFilter.ALL) },
            label = stringResource(R.string.templates_filter_all),
            testTag = TestTags.TEMPLATES_FILTER_ALL,
        )
        if (state.hiddenCount > 0) {
            AppFilterChip(
                selected = state.filter == TemplatesFilter.HIDDEN,
                onClick = { onFilterChange(TemplatesFilter.HIDDEN) },
                label = stringResource(R.string.templates_hidden_filter, state.hiddenCount),
                testTag = TestTags.TEMPLATES_HIDDEN_FILTER,
            )
        }
        if (state.deletedCount > 0) {
            AppFilterChip(
                selected = state.filter == TemplatesFilter.DELETED,
                onClick = { onFilterChange(TemplatesFilter.DELETED) },
                label = stringResource(R.string.templates_deleted_filter, state.deletedCount),
                testTag = TestTags.TEMPLATES_DELETED_FILTER,
            )
        }
    }
}

/** What an empty list says: a library with nothing in it, or one whose workouts are all put away. */
@Composable
private fun TemplatesEmpty(state: TemplatesUiState, modifier: Modifier = Modifier) {
    // "No templates yet" over a library the lifter tidied would read as data loss (ROADMAP N214, N217).
    val putAway = state.hiddenCount > 0 || state.deletedCount > 0
    CenteredMessage(
        text = stringResource(if (putAway) R.string.templates_filter_empty else R.string.templates_empty),
        hint = stringResource(
            if (putAway) R.string.templates_filter_empty_hint else R.string.templates_empty_hint,
        ),
        modifier = modifier.fillMaxSize().testTag(TestTags.TEMPLATES_EMPTY),
    )
}

/** The rows of whichever group is drawn; a deleted row's one action is *Restore* (ROADMAP N216). */
@Composable
private fun TemplateRows(
    state: TemplatesUiState,
    onOpenTemplate: (String) -> Unit,
    onStartTemplate: (String) -> Unit,
    onUnhide: (String) -> Unit,
    onRestore: (String) -> Unit,
    startEnabled: Boolean,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Leaves room for the extended FAB so it cannot cover the last row.
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(items = state.templates, key = { it.id }) { template ->
            TemplateRow(
                template = template,
                onOpen = { onOpenTemplate(template.id) },
                onStart = { onStartTemplate(template.id) },
                onUnhide = { onUnhide(template.id) },
                // Only the deleted group's rows are deleted rows: one mode is drawn at a time, so the
                // group is the fact the row needs (ROADMAP N216).
                onRestore = if (state.filter == TemplatesFilter.DELETED) {
                    { onRestore(template.id) }
                } else {
                    null
                },
                startEnabled = startEnabled,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun TemplateRow(
    template: WorkoutTemplate,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onUnhide: () -> Unit,
    modifier: Modifier = Modifier,
    /** Non-null only in the deleted group, where the row's one action is bringing the plan back (N216). */
    onRestore: (() -> Unit)? = null,
    startEnabled: Boolean = true,
) {
    val hidden = template.hiddenAt != null
    // A deleted workout resolves by no read, so it can be neither opened nor started: its row carries
    // Restore and nothing else (ROADMAP N216).
    val deleted = onRestore != null
    ListItem(
        headlineContent = { Text(template.name) },
        supportingContent = {
            Column {
                Text(
                    pluralStringResource(
                        R.plurals.template_exercises,
                        template.exerciseCount,
                        template.exerciseCount,
                    ),
                )
                // The chip above the list names the group, so the hidden row needs no marker of its own
                // (ROADMAP N217) — only the button that is the way back without opening the editor.
                if (hidden && !deleted) {
                    AppTextButton(
                        onClick = onUnhide,
                        modifier = Modifier.testTag(TestTags.templateUnhide(template.id)),
                    ) {
                        Text(stringResource(R.string.template_unhide))
                    }
                }
                if (onRestore != null) {
                    AppTextButton(
                        onClick = onRestore,
                        modifier = Modifier.testTag(TestTags.templateRestore(template.id)),
                    ) {
                        Text(stringResource(R.string.template_restore))
                    }
                }
                // Greying alone would leave "why" to be guessed, and a Start that resumes a workout
                // from another template is exactly what needs saying (N78).
                if (!startEnabled && !deleted) {
                    Text(
                        text = stringResource(R.string.templates_start_blocked),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag(TestTags.templateStartReason(template.id)),
                    )
                }
            }
        },
        trailingContent = {
            if (!deleted) {
                AppTextButton(
                    onClick = onStart,
                    enabled = startEnabled,
                    // Starting is the point of a template, so it gets the row's own
                    // button; tapping the row itself edits it instead.
                    modifier = Modifier.testTag(TestTags.templateStart(template.id)),
                ) {
                    Text(stringResource(R.string.template_start))
                }
            }
        },
        modifier = modifier
            .testTag(TestTags.templateRow(template.id))
            .then(if (deleted) Modifier else Modifier.clickable(onClick = onOpen)),
    )
}

@Preview(showBackground = true)
@Composable
private fun TemplatesScreenPreview() {
    AndroidAppTheme {
        TemplatesScreen(
            state = TemplatesUiState(
                isLoading = false,
                templates = listOf(
                    WorkoutTemplate(id = "a", name = "Push day", exerciseCount = 5),
                    WorkoutTemplate(id = "b", name = "Legs", exerciseCount = 3),
                ),
            ),
            onCreateTemplate = {},
            onOpenTemplate = {},
            onStartTemplate = {},
            onBack = {},
            onOpenPrograms = {},
            onDismissMessage = {},
            onDismissGateMessage = {},
            onFilterChange = {},
            onUnhide = {},
            onRestore = {},
        )
    }
}
