package com.example.androidapp.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.LIBRARY_NOT_TRAINED_DAYS
import com.example.androidapp.domain.LibraryFilter
import com.example.androidapp.domain.LibrarySort
import com.example.androidapp.ui.components.AppFilterChip
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * The two controls that decide what the library shows: its order and its group (ROADMAP N230, N231).
 *
 * **Two rows, and the order is the short one**: the chips are sentences ("Not trained in 30 days") and a row
 * shared with a pinned button cuts them off mid-word, which is what the first version did. The order is set
 * once and read at a glance, so it names itself on a line above the groups.
 *
 * **A chip whose group is empty is not drawn** (N217's rule from the templates list): a control that can only
 * answer "nothing" is not a control. The search stays above both, and it narrows what the chosen group holds
 * rather than replacing it.
 */
@Composable
internal fun LibraryControls(
    sort: LibrarySort,
    filter: LibraryFilter,
    counts: Map<LibraryFilter, Int>,
    onSortChange: (LibrarySort) -> Unit,
    onFilterChange: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_sort_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LibrarySortControl(sort = sort, onSortChange = onSortChange)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LibraryFilter.entries.forEach { option ->
                val count = counts[option] ?: 0
                // All is always offered: it is the way back out of a group, so an empty library still has it.
                if (count > 0 || option == LibraryFilter.ALL) {
                    AppFilterChip(
                        selected = filter == option,
                        onClick = { onFilterChange(option) },
                        label = stringResource(R.string.library_filter_chip, filterLabel(option), count),
                        testTag = TestTags.libraryFilter(option.name),
                    )
                }
            }
        }
    }
}

/** The order control: the current order as a button, and both orders behind it (N230). */
@Composable
private fun LibrarySortControl(
    sort: LibrarySort,
    onSortChange: (LibrarySort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        AppTextButton(
            onClick = { open = true },
            modifier = Modifier.testTag(TestTags.LIBRARY_SORT),
        ) {
            Text(stringResource(sort.label()))
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = stringResource(R.string.library_sort_menu),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LibrarySort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label())) },
                    onClick = {
                        open = false
                        onSortChange(option)
                    },
                    modifier = Modifier.testTag(TestTags.librarySortOption(option.name)),
                )
            }
        }
    }
}

/** What one order is called where the lifter picks it (ROADMAP N230). */
@StringRes
private fun LibrarySort.label(): Int = when (this) {
    LibrarySort.NAME -> R.string.library_sort_name
    LibrarySort.LAST_TRAINED -> R.string.library_sort_last_trained
}

/**
 * What one group is called on its chip (ROADMAP N231).
 *
 * The window is in the *label* rather than only in the rule, and it is the same constant: a chip called "Stale"
 * would make the lifter guess what it measured.
 */
@Composable
private fun filterLabel(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.ALL -> stringResource(R.string.library_filter_all)
    // A plural rather than a string with a number in it: the window is a count of days, and a translation
    // has to be able to say one of them (N231).
    LibraryFilter.NOT_TRAINED -> pluralStringResource(
        R.plurals.library_filter_not_trained,
        LIBRARY_NOT_TRAINED_DAYS.toInt(),
        LIBRARY_NOT_TRAINED_DAYS,
    )
    LibraryFilter.BODYWEIGHT -> stringResource(R.string.library_filter_bodyweight)
    LibraryFilter.TIMED -> stringResource(R.string.library_filter_timed)
    LibraryFilter.UNFILED -> stringResource(R.string.library_filter_unfiled)
}
