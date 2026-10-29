package com.example.androidapp.ui.exercises

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.androidapp.R
import com.example.androidapp.domain.LibrarySort
import com.example.androidapp.ui.components.AppTextButton
import com.example.androidapp.ui.components.TestTags

/**
 * How the list is ordered (ROADMAP N230).
 *
 * A text button naming the current order rather than a second row of chips: the order is one choice out of
 * two, and the chips below it are the filters — two rows of chips would read as one control each and mean
 * neither. The label is the order it *is*, so the state is on screen and the menu is the only thing that opens.
 */
@Composable
internal fun LibrarySortControl(
    sort: LibrarySort,
    onSortChange: (LibrarySort) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.library_sort_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box {
            AppTextButton(
                onClick = { open = true },
                modifier = Modifier.testTag(TestTags.LIBRARY_SORT),
            ) {
                Text(stringResource(sort.label()))
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
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
}

/** What one order is called where the lifter picks it (ROADMAP N230). */
@StringRes
private fun LibrarySort.label(): Int = when (this) {
    LibrarySort.NAME -> R.string.library_sort_name
    LibrarySort.LAST_TRAINED -> R.string.library_sort_last_trained
}
