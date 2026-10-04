package com.example.androidapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.TileAccent
import com.example.androidapp.ui.theme.color
import com.example.androidapp.ui.theme.onColor

/**
 * The coloured square that leads a row.
 *
 * The one piece of the reference's language that carries across every screen it has: a solid
 * accent square with a glyph on it, which lets a list be recognised at a glance before any of it is
 * read. Solid rather than a tinted wash, because a wash on a near-black card reads as a slightly
 * different grey until the screen is bright — the point is that it is unmistakably *coloured*.
 *
 * The glyph is drawn in the accent's own [onColor] rather than a blanket white (B55): white is
 * 2.5:1 on `Amber` and 2.9:1 on `Teal`, under the contrast a graphic needs.
 *
 * It carries no content description, and no parameter for one (B54). The tile sits beside a headline
 * that already says what the row is, so a description would make a screen reader announce the
 * category twice — and a parameter nothing passes is the dead API this project deletes. A caller
 * whose tile is ever the row's only content should make the tile's own `Icon` say so.
 *
 * 48dp because that is the smallest square that still reads as a deliberate shape beside two
 * lines of text, and because it is a comfortable minimum target if a caller ever makes the tile
 * itself the thing you tap.
 */
@Composable
fun IconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: TileAccent = TileAccent.Indigo,
) {
    Box(
        modifier = modifier
            .size(TILE_SIZE)
            .clip(RoundedCornerShape(TILE_CORNER))
            .background(accent.color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent.onColor,
            modifier = Modifier.size(TILE_ICON),
        )
    }
}

private val TILE_SIZE = 48.dp
private val TILE_CORNER = 14.dp
private val TILE_ICON = 24.dp
