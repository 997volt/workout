package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A raised block on the page.
 *
 * The app's lists used to be rows separated by hairlines, which on a black page leaves the
 * dividers doing all the work and the content looking like a table. Cards group instead: a
 * thing that is one thing is drawn as one thing, and the gap between cards is what says where
 * one ends.
 *
 * No shadow and no elevation. On this background a shadow is invisible, and reaching for
 * `tonalElevation` would lighten the card towards the *primary* colour, tinting every surface
 * purple — depth here is the tone ladder in the theme, which is why `surfaceContainer` is
 * already the right grey.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(modifier = Modifier.padding(CARD_PADDING), content = content)
    }
}

/** Wide enough that a two-line row does not feel cramped, tight enough to keep text off the edge. */
private val CARD_PADDING = 16.dp
