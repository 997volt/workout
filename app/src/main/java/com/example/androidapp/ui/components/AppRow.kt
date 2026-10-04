package com.example.androidapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag

/**
 * One row in a list, drawn as its own card.
 *
 * The app's workhorse row. Every list that used to be `ListItem` + `HorizontalDivider` is this
 * instead, which is the single change that does most of the work of the restyle: cards with
 * gaps between them, a coloured [IconTile] leading, and the supporting line muted.
 *
 * Built on Material's `ListItem` rather than a hand-rolled `Row`. The drawn result would be the
 * same and the semantics would not: `ListItem` is what tells a screen reader that a headline
 * and its supporting line are one item, and re-implementing it would quietly cost that.
 */
@Composable
fun AppRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    /**
     * What the tap does, for a screen reader — "Open Back Squat", not "Back Squat, button".
     *
     * Required whenever [onClick] is given: a row's headline names the thing, never the action,
     * so without this the control announces what it is and not what it will do.
     */
    onClickLabel: String? = null,
    testTag: String? = null,
) {
    // Clipped *before* the click, not after: the surface clips its own content to the rounded
    // shape, but a clickable applied to the outer modifier ripples as a rectangle over it.
    val shape = MaterialTheme.shapes.medium
    val tappable = if (onClick == null) {
        Modifier
    } else {
        Modifier.clip(shape).clickable(onClickLabel = onClickLabel, onClick = onClick)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .then(tappable),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        ListItem(
            headlineContent = {
                Text(text = headline, style = MaterialTheme.typography.titleMedium)
            },
            supportingContent = supporting?.let { line ->
                {
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            leadingContent = leading,
            trailingContent = trailing,
            colors = ListItemDefaults.colors(
                // Transparent so the card's own colour shows through, and so the row does not
                // paint a square-cornered rectangle over the card's rounded ones.
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onSurface,
                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
