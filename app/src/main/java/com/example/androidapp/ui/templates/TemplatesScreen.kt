package com.example.androidapp.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
    modifier: Modifier = Modifier,
    /** The sibling list (ROADMAP N93): the two are one tap apart, from either one's own bar. */
    onOpenPrograms: () -> Unit = {},
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
        onBack = onBack,
        onOpenPrograms = onOpenPrograms,
        modifier = modifier,
        message = gateMessage,
        onDismissGateMessage = { gateMessage = null },
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
    modifier: Modifier = Modifier,
    /** The way across to Programs (ROADMAP N93), in this screen's own bar. */
    onOpenPrograms: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
    /** A sentence from the point-of-start question, shown on the same host (ROADMAP P3.3). */
    message: String? = null,
    onDismissGateMessage: () -> Unit = {},
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
        topBar = {
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
        },
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
    modifier: Modifier = Modifier,
    /** False while a session is open, which one template list cannot start (ROADMAP N78). */
    startEnabled: Boolean = true,
) {
    when {
        state.isLoading -> CenteredMessage(
            text = stringResource(R.string.templates_loading),
            modifier = modifier,
        )

        state.templates.isEmpty() -> CenteredMessage(
            text = stringResource(R.string.templates_empty),
            hint = stringResource(R.string.templates_empty_hint),
            modifier = modifier.testTag(TestTags.TEMPLATES_EMPTY),
        )

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            // Leaves room for the extended FAB so it cannot cover the last row.
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(items = state.templates, key = { it.id }) { template ->
                TemplateRow(
                    template = template,
                    onOpen = { onOpenTemplate(template.id) },
                    onStart = { onStartTemplate(template.id) },
                    startEnabled = startEnabled,
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: WorkoutTemplate,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
    startEnabled: Boolean = true,
) {
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
                // Greying alone would leave "why" to be guessed, and a Start that resumes a workout
                // from another template is exactly what needs saying (N78).
                if (!startEnabled) {
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
            AppTextButton(
                onClick = onStart,
                enabled = startEnabled,
                // Starting is the point of a template, so it gets the row's own
                // button; tapping the row itself edits it instead.
                modifier = Modifier.testTag(TestTags.templateStart(template.id)),
            ) {
                Text(stringResource(R.string.template_start))
            }
        },
        modifier = modifier
            .testTag(TestTags.templateRow(template.id))
            .clickable(onClick = onOpen),
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
        )
    }
}
