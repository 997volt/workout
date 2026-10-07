package com.example.androidapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.androidapp.ui.theme.IndigoLink

/**
 * The app's text action — a link, in the vocabulary the palette uses (ROADMAP N49).
 *
 * `TextButton`'s own label colour is `primary`, and in this palette `primary` is the colour that
 * **fills** things: the selected tab, a chip that is on, and home's *Resume*. Indigo is too dark to be read
 * as text on the page (4.07:1) or on a raised card (3.77:1), which is what N49 measured, so every link
 * goes through here instead of each call site reaching for `TextButton` and inheriting the fill colour.
 * The call site that forgets is the one nobody can read, and a bare `IndigoLink` constant sitting
 * beside the scheme is exactly how the first attempt at this shipped a role nothing used.
 *
 * [contentColor] is open for the one surface that is itself a filled container: the rest bar draws its
 * links on `secondaryContainer`, where the role that reads is `onSecondaryContainer` and not the link
 * colour — IndigoLink on that teal measures 2.66:1, worse than the white it replaces.
 */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = IndigoLink,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        content = content,
    )
}
