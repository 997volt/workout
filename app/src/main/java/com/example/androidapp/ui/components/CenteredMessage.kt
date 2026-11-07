package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A screen's whole-body message — "Loading…", "No workouts yet", "No exercises match".
 *
 * Three screens had each grown their own copy of this layout: the exercise library,
 * the workout history list, and the history detail. They differed only in whether
 * they showed a second line, which is exactly the kind of duplication that drifts —
 * one gets a padding or alignment fix and the others quietly do not.
 *
 * Screens keep their own thin, named wrapper (`EmptyState`, `HistoryMessage`,
 * `DetailMessage`) so call sites read in the screen's vocabulary; the *layout* lives
 * here and only here.
 *
 * [textStyle] is a parameter rather than a decision made here, because collapsing
 * three styles into one would silently restyle an existing screen. A refactor should
 * not change how anything looks.
 */
@Composable
fun CenteredMessage(
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showSpinner: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    CenteredMessageLayout(
        text = text,
        modifier = modifier,
        hint = hint,
        textStyle = textStyle,
        showSpinner = showSpinner,
        action = action,
    )
}

/**
 * A message that fills one lazy item's viewport (B384).
 *
 * A plain [CenteredMessage] inside a `LazyColumn` item sees an infinite height and cannot know how tall the
 * viewport is. This overload applies `fillParentMaxSize`, which is the constraint only a lazy item can give,
 * so the message centres in the visible page rather than in its own text.
 */
@Composable
fun LazyItemScope.CenteredMessage(
    text: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    textStyle: TextStyle = MaterialTheme.typography.titleMedium,
    showSpinner: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    CenteredMessageLayout(
        text = text,
        modifier = modifier.fillParentMaxSize(),
        hint = hint,
        textStyle = textStyle,
        showSpinner = showSpinner,
        action = action,
    )
}

@Composable
private fun CenteredMessageLayout(
    text: String,
    hint: String?,
    textStyle: TextStyle,
    showSpinner: Boolean,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)?,
) {
    // B371: inside a scrolling parent the height constraint is infinite, and `fillMaxSize` then does not
    // centre — it resolves to the child's own height at the top. A bounded parent still fills; an unbounded
    // one gets a readable minimum box that the column can centre within.
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val contentHeight = if (maxHeight == Dp.Infinity) Modifier.heightIn(min = 200.dp) else Modifier.fillMaxSize()
        Column(
            modifier = contentHeight.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (showSpinner) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            }
            Text(text = text, style = textStyle)
            hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            action?.let {
                Box(modifier = Modifier.padding(top = 8.dp)) { it() }
            }
        }
    }
}
