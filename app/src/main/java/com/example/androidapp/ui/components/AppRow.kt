package com.example.androidapp.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

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
    /**
     * The state the row is in, for a screen reader — "Open", "Folded" (ROADMAP N95).
     *
     * The companion to [onClickLabel] for a row whose tap *toggles* rather than navigates: the action names
     * what the tap will do and this says what it will do it to, because a control whose behaviour depends on
     * state the reader cannot see is exactly what a screen reader cannot report for itself (B21's rule).
     */
    stateDescription: String? = null,
    testTag: String? = null,
    /**
     * The headline's colour, for the one row that has to read as destructive (ROADMAP N43).
     *
     * Unspecified by default, which is what makes the headline follow the list item's own
     * content colour rather than a colour a caller guessed.
     */
    headlineColor: Color = Color.Unspecified,
    /**
     * A second action on the same row, reached by holding it (ROADMAP N117).
     *
     * Optional, unlike [onClick]: the first long press in this app is a library family's details, and a row
     * that has no second action keeps the plain tap it had.
     */
    onLongClick: (() -> Unit)? = null,
    /**
     * What the hold does, for a screen reader — required whenever [onLongClick] is given.
     *
     * Not a nicety: a long press has no control anyone can see, so this label is the *only* thing that tells a
     * reader the action exists. A visible affordance was rejected instead (N117) — the trailing slot on this
     * row already means fold.
     */
    onLongClickLabel: String? = null,
) {
    // The surface's own shape, and the shape the tap is clipped to: one value, so a ripple can never be a
    // rectangle over a rounded row.
    val shape = MaterialTheme.shapes.medium
    val tappable = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .rowTaps(
                shape = shape,
                onClick = onClick,
                onClickLabel = onClickLabel,
                onLongClick = onLongClick,
                onLongClickLabel = onLongClickLabel,
            )
            .then(
                if (stateDescription == null) {
                    Modifier
                } else {
                    Modifier.semantics { this.stateDescription = stateDescription }
                },
            )
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
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = headlineColor,
                )
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

/**
 * The row's tap and its hold (ROADMAP N117).
 *
 * **Clipped *before* the click, not after**: the surface clips its own content to the rounded shape, but a
 * clickable applied to the outer modifier ripples as a rectangle over it.
 *
 * Extracted from [AppRow] because that function is at the length this project allows; the state the row
 * announces is applied by the caller, which is where the conditions for it already live.
 */
private fun Modifier.rowTaps(
    shape: Shape,
    onClick: () -> Unit,
    onClickLabel: String?,
    onLongClick: (() -> Unit)?,
    onLongClickLabel: String?,
): Modifier {
    // `combinedClickable` rather than `clickable` plus a gesture detector, because it carries both labels and
    // the hold's own semantics. Its `onClick` is **not** nullable, which is why this branch exists: passing it a
    // default would make every row without an action clickable.
    val tap = if (onLongClick == null) {
        Modifier.clickable(onClickLabel = onClickLabel, onClick = onClick)
    } else {
        Modifier.combinedClickable(
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }

    return clip(shape).then(tap)
}
