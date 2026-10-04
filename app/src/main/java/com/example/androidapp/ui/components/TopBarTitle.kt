package com.example.androidapp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import com.example.androidapp.ui.theme.ScreenTitleStyle

/**
 * A screen's name, in the top bar, set the way the reference sets its own.
 *
 * Uppercased and tracked out, and small with it: on the reference the top bar is an eyebrow
 * rather than a headline, because the screen's content is what should be read first. Half of
 * that effect is the case, which is why the transform lives here rather than in the string
 * resources — a translator should not have to know that a heading is displayed capitalised, and
 * a screen reader should not have to spell it out.
 *
 * Capitalised with the active locale rather than `Locale.ROOT`: in Turkish "i" uppercases to
 * "İ", and a title that rendered as `SETTİNGS` would be a bug, not a style. It is read from
 * [LocalConfiguration] and not from `Locale.getDefault()`, which is not observable — a device
 * that changed language while the app was open would keep the old capitals until the next
 * recomposition for some other reason.
 *
 * [maxLines] is 1 with ellipsis because the bar does not scroll and a long translated title
 * must not push the navigation icon off the screen.
 */
@Composable
fun TopBarTitle(
    text: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    val locale = LocalConfiguration.current.locales[0]
    Text(
        text = text.uppercase(locale),
        style = ScreenTitleStyle,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = if (testTag == null) modifier else modifier.testTag(testTag),
    )
}
