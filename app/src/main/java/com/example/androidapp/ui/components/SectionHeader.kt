package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * The heading over a group of rows.
 *
 * White and sentence-cased rather than the tinted small caps this app used to use for the same
 * job. On the reference, a header over content is white and the *eyebrow* — the tracked-out
 * capitalised line above a screen or a card — is the quiet one; making the group heading the
 * tinted element competes with the primary action for the same colour.
 *
 * Carries no horizontal padding of its own: it is placed inside a list that already has it, and
 * a component that pads itself cannot be aligned with the rows it heads.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    testTag: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SECTION_TOP, bottom = SECTION_BOTTOM)
            .let { if (testTag == null) it else it.testTag(testTag) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        trailing?.invoke()
    }
}

private val SECTION_TOP = 20.dp
private val SECTION_BOTTOM = 8.dp
