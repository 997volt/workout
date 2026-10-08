package com.example.androidapp.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidapp.R
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.components.AppCard
import com.example.androidapp.ui.components.CenteredMessage
import com.example.androidapp.ui.components.MessageSnackbar
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.dataErrorMessage
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.theme.AndroidAppTheme

/**
 * The program list (ROADMAP P3.3).
 *
 * A program is a set of templates in the order they are trained, so this list is where the
 * schedule is chosen; the editor behind a row is where it is written. Reached from the home
 * screen's action row, beside Templates (N42), one tap from the plan it changes.
 */
@Composable
fun ProgramsRoute(
    onOpenProgram: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    /** The sibling list (ROADMAP N93): the two are one tap apart, from either one's own bar. */
    onOpenTemplates: () -> Unit = {},
    viewModel: ProgramsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val created by viewModel.createdProgramId.collectAsStateWithLifecycle()
    val currentOnOpenProgram by rememberUpdatedState(onOpenProgram)

    // Loading a program document reports through the screen's own host (ROADMAP N47). The file
    // read is the composable's, because it holds the `Uri`; the sentence is resolved here.
    var transferMessage by remember { mutableStateOf<String?>(null) }
    val loadProgram = rememberProgramImport(viewModel) { transferMessage = it }

    // A newly created program opens straight into its editor: it has no slots yet and its
    // name is still the default, so there is nothing to see in a row.
    LaunchedEffect(created) {
        created?.let {
            currentOnOpenProgram(it)
            viewModel.onCreatedHandled()
        }
    }

    ProgramsScreen(
        state = state,
        onCreateProgram = viewModel::onCreateProgram,
        onOpenProgram = onOpenProgram,
        onSetActive = viewModel::onSetActive,
        onMoveProgram = viewModel::onMoveProgram,
        onLoadProgram = loadProgram,
        onOpenTemplates = onOpenTemplates,
        transferMessage = transferMessage,
        onDismissTransferMessage = { transferMessage = null },
        onDismissMessage = viewModel::onErrorShown,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    state: ProgramsUiState,
    onCreateProgram: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    onBack: () -> Unit,
    /** Required rather than defaulted, so a route that forgets it fails the build (B49, B52). */
    onLoadProgram: () -> Unit,
    modifier: Modifier = Modifier,
    /** The way across to Templates (ROADMAP N93), in this screen's own bar. */
    onOpenTemplates: () -> Unit = {},
    onSetActive: (String) -> Unit = {},
    onMoveProgram: (String, Int) -> Unit = { _, _ -> },
    transferMessage: String? = null,
    onDismissTransferMessage: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val defaultName = stringResource(R.string.program_default_name)
    val currentOnDismissMessage by rememberUpdatedState(onDismissMessage)
    MessageSnackbar(transferMessage, snackbarHostState, onDismissTransferMessage)

    // The message is resolved during composition and shown from the effect, because a
    // string resource cannot be read inside LaunchedEffect.
    state.error?.let { failure ->
        val message = dataErrorMessage(failure)
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            currentOnDismissMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProgramsTopBar(
                onBack = onBack,
                onLoadProgram = onLoadProgram,
                onOpenTemplates = onOpenTemplates,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onCreateProgram(defaultName) },
                text = { Text(stringResource(R.string.program_new)) },
                icon = { Icon(imageVector = Icons.Filled.Add, contentDescription = null) },
                modifier = Modifier.testTag(TestTags.Programs.NEW),
            )
        },
        // The active program rides the screen's own bottom bar (N92): with one program it is the one
        // thing there is to open, and the far end of a modern phone is the wrong place for it. The
        // scaffold places a floating action **above** a bottom bar, so the *New program* button ends up
        // higher than this card rather than over it — neither is handed a width that has to keep
        // matching the other, which is what drawing them side by side would have cost.
        bottomBar = {
            state.programs.firstOrNull { it.isActive }?.let { active ->
                ActiveProgramCard(program = active, onOpen = { onOpenProgram(active.id) })
            }
        },
    ) { innerPadding ->
        ProgramsContent(
            state = state,
            onOpenProgram = onOpenProgram,
            onSetActive = onSetActive,
            onMoveProgram = onMoveProgram,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The list's bar: the way in from the file, and the way across to Templates (ROADMAP N47, N93).
 *
 * Split out because the screen is at the length this project allows, and because the two entries are one
 * kind of thing — the actions this screen's bar carries — beside the title and the back arrow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramsTopBar(
    onBack: () -> Unit,
    onLoadProgram: () -> Unit,
    onOpenTemplates: () -> Unit,
) {
    TopAppBar(
        title = { Text(stringResource(R.string.programs_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.nav_back),
                )
            }
        },
        actions = {
            // Loading a document is the file's way in (ROADMAP N47). Export is per program,
            // so it lives in the editor rather than here.
            AppTextButton(
                onClick = onLoadProgram,
                modifier = Modifier.testTag(TestTags.Programs.LOAD),
            ) {
                Text(stringResource(R.string.program_load))
            }
            // The sibling list, one tap away (ROADMAP N93): the two screens answer the same question from
            // two sides, and back-home-and-in was the only path between them. A way across rather than a
            // third way to start a workout, so it asks nothing about a session being open.
            AppTextButton(
                onClick = onOpenTemplates,
                modifier = Modifier.testTag(TestTags.Programs.TEMPLATES),
            ) {
                Text(stringResource(R.string.home_templates))
            }
        },
    )
}

/**
 * The active program, at the foot of the list (ROADMAP N92).
 *
 * A **card** rather than a second list row, because that is the screen's own bottom bar: it is not part of
 * the reference above it — the list keeps every program in the authored order (P3.12), and the card is the
 * one being acted on. With several active programs one of them is shown (the list's own order decides
 * which); with none there is no card at all, so neither case is rearranged to suit the single-program one.
 *
 * It says the same thing the row does — the name, the slot count, and *In use* rather than a *Use* button,
 * because a control that activates the program it is already showing would do nothing.
 */
@Composable
private fun ActiveProgramCard(
    program: WorkoutProgram,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(TestTags.Programs.ACTIVE_CARD)
            .clickable(onClickLabel = program.name, onClick = onOpen),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = program.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = pluralStringResource(
                        R.plurals.program_slots,
                        program.slotCount,
                        program.slotCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.program_in_use),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProgramsContent(
    state: ProgramsUiState,
    onOpenProgram: (String) -> Unit,
    onSetActive: (String) -> Unit,
    onMoveProgram: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> CenteredMessage(
            text = stringResource(R.string.programs_loading),
            modifier = modifier,
        )

        state.programs.isEmpty() -> CenteredMessage(
            text = stringResource(R.string.programs_empty),
            hint = stringResource(R.string.programs_empty_hint),
            modifier = modifier.testTag(TestTags.Programs.EMPTY),
        )

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            // Leaves room for the floating button, which is one bottom bar higher than it used to be
            // (ROADMAP N92): the active program's card rides the scaffold's own bottom bar, so the last
            // row clears both it and the button floating above it.
            contentPadding = PaddingValues(bottom = FLOATING_BUTTON_CLEARANCE + ACTIVE_CARD_CLEARANCE),
        ) {
            itemsIndexed(items = state.programs, key = { _, program -> program.id }) { index, program ->
                ProgramRow(
                    program = program,
                    isFirst = index == 0,
                    isLast = index == state.programs.lastIndex,
                    onOpen = { onOpenProgram(program.id) },
                    onUse = { onSetActive(program.id) },
                    onMoveUp = { onMoveProgram(program.id, -1) },
                    onMoveDown = { onMoveProgram(program.id, 1) },
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * Space the list keeps clear of the extended *New program* button: its 56 dp, plus the margin it floats
 * with (ROADMAP N92).
 */
private val FLOATING_BUTTON_CLEARANCE = 96.dp

/**
 * Space the list keeps for the active program's bottom bar above that (ROADMAP N92).
 *
 * The card's own height plus the 8 dp it is inset by on each side — enough that a last row is readable
 * rather than tucked under the bar, which is a clearance rather than a measurement.
 */
private val ACTIVE_CARD_CLEARANCE = 96.dp

@Composable
private fun ProgramRow(
    program: WorkoutProgram,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: () -> Unit,
    onUse: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(program.name) },
        supportingContent = {
            Text(
                pluralStringResource(
                    R.plurals.program_slots,
                    program.slotCount,
                    program.slotCount,
                ),
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // The authored order (P3.12): which active program comes first is moved here,
                // rather than left to a name or a creation time.
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.testTag(TestTags.Programs.moveProgram(program.id, up = true)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.program_move_program_up, program.name),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.testTag(TestTags.Programs.moveProgram(program.id, up = false)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.program_move_program_down, program.name),
                    )
                }
                if (program.isActive) {
                    // A label rather than a button: following is a state to read, and a second
                    // program may carry it too (P3.12). It is turned off in the editor.
                    Text(
                        text = stringResource(R.string.program_in_use),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    AppTextButton(
                        onClick = onUse,
                        modifier = Modifier.testTag(TestTags.Programs.use(program.id)),
                    ) {
                        Text(stringResource(R.string.program_use))
                    }
                }
            }
        },
        modifier = modifier
            .testTag(TestTags.Programs.row(program.id))
            .clickable(onClick = onOpen),
    )
}

@Preview(showBackground = true)
@Composable
private fun ProgramsScreenPreview() {
    AndroidAppTheme {
        ProgramsScreen(
            state = ProgramsUiState(
                isLoading = false,
                programs = listOf(
                    WorkoutProgram(id = "a", name = "Upper/Lower", slotCount = 4, isActive = true),
                    WorkoutProgram(id = "b", name = "PPL", slotCount = 6),
                ),
            ),
            onCreateProgram = {},
            onOpenProgram = {},
            onBack = {},
            onLoadProgram = {},
        )
    }
}
