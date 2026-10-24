package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.androidapp.R
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * One picker over every series, grouped the way the registry groups them (ROADMAP N35), and searchable (N198).
 *
 * The list is every series the registry holds, which is a long menu however it is headed: the headings say which
 * *kind* of series a word belongs to (N130 — two *Left knee* rows need it), and the search says which series it
 * is. Filtering matches the group's own name too, so "body" finds weight without knowing the word "weight".
 */
@Composable
internal fun MetricPicker(selected: MetricKey, onSelectMetric: (MetricKey) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selectedEntry = MetricRegistry.entryFor(selected)

    Column {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.Statistics.METRIC),
        ) {
            Text(
                text = stringResource(R.string.statistics_metric) + ": " + selectedEntry.label(),
            )
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            // The menu holds a text field, so it has to take focus: a menu that cannot is a search box that
            // cannot be typed into.
            properties = PopupProperties(focusable = true),
        ) {
            MetricMenuContents(
                onSelect = { key ->
                    open = false
                    onSelectMetric(key)
                },
            )
        }
    }
}

/**
 * What the menu holds: a filter, then every series the filter leaves, under its group (N198).
 *
 * Its own composable because the picker around it is the trigger-and-popup shape every menu in the app has, and
 * the contents are a list rather than a control — the two have different reasons to change.
 */
@Composable
private fun MetricMenuContents(onSelect: (MetricKey) -> Unit) {
    Column {
        // Fresh each time the menu is composed, which is each time it opens: a filter that outlived its menu
        // would hide the list the next time it was opened.
        var query by remember { mutableStateOf("") }
        val groupLabels = MetricGroup.entries.associateWith { stringResource(it.labelRes) }
        val needle = query.trim()
        val matches = MetricRegistry.entries
            .map { entry -> entry to entry.label() }
            .filter { (entry, label) ->
                needle.isEmpty() ||
                    label.contains(needle, ignoreCase = true) ||
                    groupLabels[entry.group].orEmpty().contains(needle, ignoreCase = true)
            }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            label = { Text(stringResource(R.string.statistics_metric_search)) },
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag(TestTags.Statistics.METRIC_SEARCH),
        )
        if (matches.isEmpty()) {
            Text(
                text = stringResource(R.string.statistics_metric_no_matches),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag(TestTags.Statistics.METRIC_NO_MATCHES),
            )
        }
        MetricGroup.entries.forEach { group ->
            val inGroup = matches.filter { (entry, _) -> entry.group == group }
            if (inGroup.isEmpty()) return@forEach
            // A heading per group, because two series can share a label (N130): *Left knee* is a workout
            // series and a per-lift one, and without this the list offered the same words twice with nothing
            // to tell them apart.
            Text(
                text = groupLabels.getValue(group),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag(TestTags.Statistics.metricGroup(group.name)),
            )
            inGroup.forEach { (entry, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onSelect(entry.key) },
                    modifier = Modifier.testTag(TestTags.Statistics.metric(entry.key.id)),
                )
            }
        }
    }
}
