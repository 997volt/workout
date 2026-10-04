package com.example.androidapp.ui.components

import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * A chip that means "this is one of the choices, and this one is on".
 *
 * Explicitly coloured rather than left to the component's defaults. Material's stock selected
 * chip is `secondaryContainer` — the filled tonal slot the palette fills with a muted teal
 * (ROADMAP N49), which is a *surface* rather than the accent a category of row wears. Selecting
 * is the app's own colour, so it is the app's own colour here too, and a screen with five of them
 * does not read as five categories.
 *
 * Its own component because two screens pick from a fixed set of options — the default rest and
 * the statistics range — and a chip that looked different in one of them would be the kind of
 * drift nobody notices until they are seen side by side.
 */
@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        modifier = if (testTag == null) modifier else modifier.testTag(testTag),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            // Unselected chips are a surface, not an outline: the page already has enough
            // hairlines, and an outlined chip beside a filled card is a third grey.
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}
