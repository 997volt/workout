package com.example.androidapp.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.example.androidapp.domain.model.Joint
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * One picker over every series, grouped the way the registry groups them (ROADMAP N35), and searchable
 * (N198, B210).
 *
 * The list is every series the registry holds, which is a long menu however it is headed — so the picker opens
 * on **one group** and lists only that group's series (N199), and a filter narrows what is left (N198). A query
 * is **not** scoped to the group on screen: it searches every group and each hit says which one it is in, because
 * a search that answered "nothing" for a series in another group is a search that lies about the registry. The
 * group's own name is matched too, so "body" finds weight without knowing the word "weight".
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
                selected = selected,
                onSelect = { key ->
                    open = false
                    onSelectMetric(key)
                },
            )
        }
    }
}

/**
 * What the menu holds: the group, a filter, then the series the filter leaves (N198, N199).
 *
 * **A group first**, because the registry's four groups are the only split that is always the same size — the
 * headings this replaces made a flat list readable, not short. **The joint sites are one row**: twenty-four
 * *Left knee*-shaped entries are one question ("which site?") behind *Joint pain*, so the workout and exercise
 * lists are their own series rather than a grid of sites.
 */
@Composable
private fun MetricMenuContents(selected: MetricKey, onSelect: (MetricKey) -> Unit) {
    // Fresh each time the menu is composed, which is each time it opens: a filter that outlived its menu would
    // hide the list the next time it was opened.
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf(MetricRegistry.entryFor(selected).group) }
    // The group whose sites are on show, or null while the group's own series are listed.
    var siteGroup by remember { mutableStateOf<MetricGroup?>(null) }

    val groupLabels = MetricGroup.entries.associateWith { stringResource(it.labelRes) }
    val needle = query.trim()
    val searching = needle.isNotEmpty()
    // Every series, and — while a query is typed — every group's matches. The group chips only narrow the
    // browse list; a filter that hid a literal match in another group was the bug B210 records.
    val all = MetricRegistry.entries.map { entry -> entry to entry.label() }
    val matches = if (searching) {
        all.filter { (entry, label) ->
            label.contains(needle, ignoreCase = true) ||
                groupLabels.getValue(entry.group).contains(needle, ignoreCase = true)
        }
    } else {
        all.filter { (entry, _) -> entry.group == group }
    }
    // A site is one row behind *Joint pain* while the group is being read, and its own row once it is being
    // searched for: a filter that hid the thing you typed is not a filter.
    val sites = if (searching) emptyList() else matches.filter { (entry, _) -> entry.isJointSite() }
    val series = if (searching) matches else matches.filter { (entry, _) -> !entry.isJointSite() }
    val showingSites = siteGroup == group && !searching

    Column {
        // The chips choose a browse list, so they wait while a query is the thing narrowing. Leaving them on
        // screen would say a group is selected over a list that is showing all of them.
        if (!searching) {
            GroupChoices(
                labels = groupLabels,
                selected = group,
                onSelect = {
                    group = it
                    siteGroup = null
                },
            )
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

        if (showingSites) {
            SiteRows(sites = sites, onBack = { siteGroup = null }, onSelect = onSelect)
        } else {
            SeriesRows(
                series = series,
                hasSites = sites.isNotEmpty() && !searching,
                nothingMatches = matches.isEmpty(),
                searching = searching,
                onOpenSites = { siteGroup = group },
                onSelect = onSelect,
            )
        }
    }
}

/** One group's own series, its sites' one row, or the sentence that nothing matched (N198, N199). */
@Composable
private fun SeriesRows(
    series: List<Pair<MetricEntry, String>>,
    hasSites: Boolean,
    nothingMatches: Boolean,
    searching: Boolean,
    onOpenSites: () -> Unit,
    onSelect: (MetricKey) -> Unit,
) {
    Column {
        if (nothingMatches) {
            Text(
                text = stringResource(R.string.statistics_metric_no_matches),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag(TestTags.Statistics.METRIC_NO_MATCHES),
            )
        }
        series.forEach { (entry, label) ->
            MetricMenuItem(
                label = label,
                key = entry.key,
                // A search reaches every group, so a hit has to say which one it came from — otherwise the two
                // *Left knee* entries are the same two words again (N130, B210).
                subtitle = if (searching) stringResource(entry.group.labelRes) else null,
                onSelect = onSelect,
            )
        }
        // The sites' one row, offered only where the group has sites and nothing is being searched.
        if (hasSites) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.statistics_metric_joint)) },
                onClick = onOpenSites,
                modifier = Modifier.testTag(TestTags.Statistics.METRIC_JOINT),
            )
        }
    }
}

/** The group's joint sites, behind the one row that names them, with the way back (N199). */
@Composable
private fun SiteRows(
    sites: List<Pair<MetricEntry, String>>,
    onBack: () -> Unit,
    onSelect: (MetricKey) -> Unit,
) {
    Column {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.statistics_metric_joint_back)) },
            onClick = onBack,
            modifier = Modifier.testTag(TestTags.Statistics.METRIC_JOINT_BACK),
        )
        // A heading per joint, three sides under it (N205): one list of twenty-four sites is a scroll rather than
        // a choice, and the joint is the question the reader already has when they open this.
        sites.groupBy { (entry, _) -> entry.jointOf() }.forEach { (joint, sides) ->
            Text(
                text = joint.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag(TestTags.Statistics.metricJointHeading(joint.name)),
            )
            sides.forEach { (entry, label) ->
                MetricMenuItem(label = label, key = entry.key, onSelect = onSelect)
            }
        }
    }
}

/** The four groups, as chips: which list is on show, chosen rather than scrolled past (N199). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChoices(
    labels: Map<MetricGroup, String>,
    selected: MetricGroup,
    onSelect: (MetricGroup) -> Unit,
) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricGroup.entries.forEach { group ->
            AppFilterChip(
                selected = group == selected,
                onClick = { onSelect(group) },
                label = labels.getValue(group),
                testTag = TestTags.Statistics.metricGroupChoice(group.name),
            )
        }
    }
}

/** One series in the list: a word, the group it came from when that is not the list being browsed, and its row. */
@Composable
private fun MetricMenuItem(
    label: String,
    key: MetricKey,
    onSelect: (MetricKey) -> Unit,
    subtitle: String? = null,
) {
    DropdownMenuItem(
        text = {
            if (subtitle == null) {
                Text(label)
            } else {
                Column {
                    Text(label)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onClick = { onSelect(key) },
        modifier = Modifier.testTag(TestTags.Statistics.metric(key.id)),
    )
}

/** The joint a site row names, at either level (N130, N205). */
private fun MetricEntry.jointOf(): Joint = when (val site = key) {
    is MetricKey.JointSite -> site.joint
    is MetricKey.ExerciseJointSite -> site.joint
    else -> error("not a joint site: $key")
}

/** True for the two keys that name a joint site, at either level (N130). */
private fun MetricEntry.isJointSite(): Boolean = when (key) {
    is MetricKey.JointSite, is MetricKey.ExerciseJointSite -> true
    else -> false
}
