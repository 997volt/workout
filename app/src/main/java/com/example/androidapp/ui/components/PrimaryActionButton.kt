package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Comfortably over the 48dp minimum target, and the height the reference's pill reads at. */
private val PRIMARY_ACTION_HEIGHT = 52.dp

/**
 * The app's primary action: a full-width filled pill with a leading glyph (ROADMAP P1.16, N1).
 *
 * Extracted from home's start button at its second caller — the planned-workout pill under a
 * next-up row — so the two full-width actions the screen offers cannot drift apart in height or
 * shape.
 *
 * A filled pill rather than the extended floating button this used to be, and full width in its
 * bar: the reference's own call to action is a wide violet pill resting on the bottom of the
 * screen, and it is the one shape a user reads as "this is the thing to do here". The glyph is
 * decorative — the caption already names the action — so it carries no description.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier.heightIn(min = PRIMARY_ACTION_HEIGHT),
        onClick = onClick,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = text,
            // A resumed session's caption carries an elapsed time and an exercise count, so it is
            // the one that can outgrow the bar; the label must not push the icon out.
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
