package com.example.androidapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.model.WorkoutProgram
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags
import java.time.DayOfWeek
import java.time.format.TextStyle

/** The dialogs around saving a plan (ROADMAP N31) and scheduling it (P3.18). */
/** Asks for the plan's name, since a copy called "Untitled" is a copy nobody finds again (N31). */
@Composable
private fun SaveAsPlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_save_as_plan)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.detail_plan_name)) },
                modifier = Modifier.testTag(TestTags.DETAIL_PLAN_NAME),
            )
        },
        confirmButton = {
            AppTextButton(
                onClick = { onConfirm(name) },
                // A blank name is refused by the repository too; disabling the button says so before
                // the tap rather than after it.
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag(TestTags.DETAIL_PLAN_CONFIRM),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Confirms the copy and offers to go to it (ROADMAP N31), or to schedule it (P3.18).
 *
 * Two actions rather than one: opening the plan is about its contents, scheduling is about when it is
 * trained, and the lifter usually wants the second next.
 */
@Composable
private fun SavedAsPlanDialog(
    canSchedule: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onSchedule: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_saved_as_plan)) },
        confirmButton = {
            Row {
                if (canSchedule) {
                    AppTextButton(
                        onClick = onSchedule,
                        modifier = Modifier.testTag(TestTags.DETAIL_SCHEDULE),
                    ) {
                        Text(stringResource(R.string.detail_schedule))
                    }
                }
                AppTextButton(
                    onClick = onOpen,
                    modifier = Modifier.testTag(TestTags.DETAIL_OPEN_NEW_PLAN),
                ) {
                    Text(stringResource(R.string.detail_open_plan))
                }
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/**
 * Adds the just-saved plan to a program (P3.18), on a weekday or order-only.
 *
 * Choosing a day here puts the plan on the week without a second trip to the editor; choosing none leaves
 * it order-only, which P3.3 allows and the editor can change.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SchedulePlanDialog(
    programs: List<WorkoutProgram>,
    onDismiss: () -> Unit,
    onConfirm: (String, DayOfWeek?) -> Unit,
) {
    var programId by rememberSaveable { mutableStateOf(programs.firstOrNull()?.id) }
    var weekdayName by rememberSaveable { mutableStateOf<String?>(null) }
    val weekday = weekdayName?.let { DayOfWeek.valueOf(it) }
    // The configuration is the observable way to read the locale (lint's NonObservableLocale).
    val locale = LocalConfiguration.current.locales[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_schedule_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                programs.forEach { program ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = program.id == programId,
                                role = Role.RadioButton,
                                onClick = { programId = program.id },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = program.id == programId, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = program.name)
                    }
                }
                Text(
                    text = stringResource(R.string.detail_schedule_day),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = weekday == day,
                            onClick = { weekdayName = if (weekday == day) null else day.name },
                            label = {
                                Text(day.getDisplayName(TextStyle.SHORT, locale))
                            },
                            modifier = Modifier.testTag(TestTags.detailScheduleDay(day.name)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppTextButton(
                onClick = { programId?.let { onConfirm(it, weekday) } },
                enabled = programId != null,
                modifier = Modifier.testTag(TestTags.DETAIL_SCHEDULE_CONFIRM),
            ) {
                Text(stringResource(R.string.detail_schedule_confirm))
            }
        },
        dismissButton = {
            AppTextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/**
 * Holds the schedule dialog's own state, so the screen does not.
 *
 * Extracted at the point [WorkoutDetailScreen] reached the length this project allows; the two flags are
 * consequences of one offer, and keeping them together is also what makes *dismiss the schedule* return to
 * the offer rather than closing both.
 */
@Composable
internal fun PlanDialogsHost(
    savingAsPlan: Boolean,
    savedTemplate: String?,
    programs: List<WorkoutProgram>,
    onSaveAsTemplate: (String) -> Unit,
    onDismissName: () -> Unit,
    onDismissSavedTemplate: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onScheduleTemplate: (String, DayOfWeek?) -> Unit,
) {
    var schedulingPlan by rememberSaveable { mutableStateOf(false) }
    PlanDialogs(
        savingAsPlan = savingAsPlan,
        savedTemplate = savedTemplate,
        programs = programs,
        scheduling = schedulingPlan,
        onConfirmName = onSaveAsTemplate,
        onDismissName = onDismissName,
        onDismissOffer = {
            schedulingPlan = false
            onDismissSavedTemplate()
        },
        onOpenTemplate = onOpenTemplate,
        onScheduleOpen = { schedulingPlan = true },
        onSchedule = { programId, weekday ->
            schedulingPlan = false
            onScheduleTemplate(programId, weekday)
        },
        onDismissSchedule = { schedulingPlan = false },
    )
}

/** The dialogs this screen can show around saving a plan, together (ROADMAP N31, P3.18). */
@Composable
private fun PlanDialogs(
    savingAsPlan: Boolean,
    savedTemplate: String?,
    programs: List<WorkoutProgram>,
    scheduling: Boolean,
    onConfirmName: (String) -> Unit,
    onDismissName: () -> Unit,
    onDismissOffer: () -> Unit,
    onOpenTemplate: (String) -> Unit,
    onScheduleOpen: () -> Unit,
    onSchedule: (String, DayOfWeek?) -> Unit,
    onDismissSchedule: () -> Unit,
) {
    if (savingAsPlan) {
        SaveAsPlanDialog(onDismiss = onDismissName, onConfirm = onConfirmName)
    }
    savedTemplate?.let { templateId ->
        if (scheduling && programs.isNotEmpty()) {
            SchedulePlanDialog(
                programs = programs,
                onDismiss = onDismissSchedule,
                onConfirm = onSchedule,
            )
        } else {
            SavedAsPlanDialog(
                canSchedule = programs.isNotEmpty(),
                onDismiss = onDismissOffer,
                onOpen = {
                    onDismissOffer()
                    onOpenTemplate(templateId)
                },
                onSchedule = onScheduleOpen,
            )
        }
    }
}
