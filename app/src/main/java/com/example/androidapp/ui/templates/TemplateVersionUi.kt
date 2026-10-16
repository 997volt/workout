package com.example.androidapp.ui.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.androidapp.R
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.WorkoutTemplate
import com.example.androidapp.domain.model.runAt
import com.example.androidapp.domain.model.rungWeightAt
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import com.example.androidapp.ui.components.exerciseWeightUnit
import com.example.androidapp.ui.components.setNumbers
import com.example.androidapp.ui.components.summary
import com.example.androidapp.ui.history.HistoryFormat
import java.time.ZoneId

/**
 * The versions kept of one plan: the list, the one being read, and what may be done with them
 * (ROADMAP N134).
 *
 * Bundled because six more parameters pushed the editor screen past the length this project allows,
 * and they belong together anyway: every one of them is about the versions of this plan.
 */
class TemplateVersionUi(
    val versions: List<WorkoutTemplate> = emptyList(),
    val openVersion: TemplateVersionPlan? = null,
    val versionKept: Boolean = false,
    val onKeepVersion: () -> Unit = {},
    val onDismissVersionNotice: () -> Unit = {},
    val onOpenVersion: (WorkoutTemplate) -> Unit = {},
    val onDismissVersion: () -> Unit = {},
)

/**
 * The confirmation that a version was kept, said once and then dismissed (ROADMAP N134).
 *
 * Nothing on the screen changed, because the live plan did not, so the sentence is the whole of what
 * happened. The callback goes through `rememberUpdatedState` because a composable lambda read directly
 * inside a restarting effect is the shape the Compose rules refuse.
 */
@Composable
internal fun ShowVersionKept(
    versionKept: Boolean,
    hostState: SnackbarHostState,
    onDismiss: () -> Unit,
) {
    val message = stringResource(R.string.template_version_kept)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(versionKept) {
        if (versionKept) {
            hostState.showSnackbar(message)
            currentOnDismiss()
        }
    }
}

/**
 * The versions kept of this plan: the list, and the one being read (ROADMAP N134).
 *
 * Both are dialogs on the editor, so the two are named together here rather than in the screen's body.
 */
@Composable
internal fun TemplateVersionOverlays(
    versionUi: TemplateVersionUi,
    showingVersions: Boolean,
    onDismissVersions: () -> Unit,
) {
    if (showingVersions) {
        TemplateVersionsDialog(
            versions = versionUi.versions,
            // The list closes as the plan opens, so the two are never stacked on each other.
            onOpen = { version ->
                onDismissVersions()
                versionUi.onOpenVersion(version)
            },
            onDismiss = onDismissVersions,
        )
    }
    versionUi.openVersion?.let { plan ->
        TemplateVersionPlanDialog(plan = plan, onDismiss = versionUi.onDismissVersion)
    }
}

/**
 * The versions kept of one template, newest first (ROADMAP N134).
 *
 * A version is read, never edited and never restored, so this surface offers exactly one thing to do
 * with one: open it and look. Where the *copies* live is the decision worth stating — inside the
 * template they belong to rather than loose in the template list — which is why this is a dialog on the
 * editor rather than a fourth thing the list can show.
 */
@Composable
internal fun TemplateVersionsDialog(
    versions: List<WorkoutTemplate>,
    onOpen: (WorkoutTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.TEMPLATE_VERSIONS_DIALOG),
        title = { Text(stringResource(R.string.template_versions)) },
        text = {
            if (versions.isEmpty()) {
                Text(stringResource(R.string.template_versions_empty))
            } else {
                LazyColumn {
                    items(items = versions, key = { it.id }) { version ->
                        VersionRow(version = version, onOpen = { onOpen(version) })
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/** One kept version: its own name, what it holds, and when it was taken (ROADMAP N134). */
@Composable
private fun VersionRow(version: WorkoutTemplate, onOpen: () -> Unit) {
    // The name is the field and the tap says what it does, the rule every row in this app follows.
    val openLabel = stringResource(R.string.template_open_version, version.name)
    val kept = version.keptAt?.let { at ->
        stringResource(
            R.string.template_version_kept_at,
            HistoryFormat.date(at, zone = ZoneId.systemDefault()),
        )
    }
    val count = pluralStringResource(
        R.plurals.template_exercises,
        version.exerciseCount,
        version.exerciseCount,
    )
    ListItem(
        headlineContent = { Text(version.name) },
        supportingContent = { Text(listOfNotNull(count, kept).joinToString(" · ")) },
        modifier = Modifier
            .testTag(TestTags.templateVersion(version.id))
            .clickable(onClickLabel = openLabel, onClick = onOpen),
    )
}

/**
 * One kept version opened as a **read-only plan** (ROADMAP N134).
 *
 * What a lifter asked for when they kept it: what the plan was. Nothing here writes, and there is no
 * control that could — the version cannot be changed into something that looks live. The rendering is
 * the planned-workout preview's, one exercise per row with its planned sets beneath it, so reading a
 * version reads like reading any other plan.
 */
@Composable
internal fun TemplateVersionPlanDialog(
    plan: TemplateVersionPlan,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(TestTags.TEMPLATE_VERSION_DIALOG),
        title = {
            Column {
                Text(plan.version.name, modifier = Modifier.testTag(TestTags.TEMPLATE_VERSION_TITLE))
                plan.version.keptAt?.let { at ->
                    Text(
                        text = stringResource(
                            R.string.template_version_kept_at,
                            HistoryFormat.date(at, zone = ZoneId.systemDefault()),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        text = {
            when {
                // A read that has not answered yet is not an empty plan: the spinner is the difference.
                plan.isLoading -> Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                plan.exercises.isEmpty() -> Text(
                    text = stringResource(R.string.template_version_empty),
                    modifier = Modifier.testTag(TestTags.TEMPLATE_VERSION_EMPTY),
                )

                else -> LazyColumn {
                    itemsIndexed(plan.exercises) { index, exercise ->
                        ListItem(
                            headlineContent = { Text(exercise.exerciseName) },
                            leadingContent = { Text("${index + 1}") },
                            supportingContent = { PlannedSetLines(exercise) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag(TestTags.TEMPLATE_VERSION_CLOSE),
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
    )
}

/**
 * One exercise's planned sets, read-only.
 *
 * The same lines the editor draws, minus every control, and derived the same way: a rung states what it
 * takes off its anchor (N79) and the sets are numbered by the rule every list of sets numbers by (N112),
 * because a version that read differently from the plan it was kept from would be a second rendering of
 * one thing.
 */
@Composable
private fun PlannedSetLines(exercise: TemplateExercise) {
    if (exercise.sets.isEmpty()) {
        // The **read-only** sentence, not the editor's imperative: there is no control here that could add a
        // set, and telling the reader to add one is an instruction this dialog cannot honour (B147).
        Text(stringResource(R.string.template_plan_none))
        return
    }
    val unit = exerciseWeightUnit(exercise.weightUnit)
    val numbers = exercise.sets.setNumbers { it.role }
    Text(
        text = exercise.sets.mapIndexed { index, set ->
            val number = numbers[index]?.let { "$it. " }.orEmpty()
            number + set.summary(
                unit = unit,
                rungWeightGrams = exercise.sets.rungWeightAt(index),
                holdsTheRunValue = exercise.sets.runAt(index)?.rung == 1,
            )
        }.joinToString("\n"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
