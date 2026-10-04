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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.androidapp.ui.theme.TileAccent
import com.example.androidapp.ui.theme.color

/**
 * The coloured square that leads a row.
 *
 * The one piece of the reference's language that carries across every screen it has: a solid
 * accent square with a white glyph, which lets a list be recognised at a glance before any of
 * it is read. Solid rather than a tinted wash, because a wash on a near-black card reads as a
 * slightly different grey until the screen is bright — the point is that it is unmistakably
 * *coloured*.
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
    /**
     * Null by default, and that is the usual answer: the tile sits beside a headline that
     * already says what the row is, so a description here would make TalkBack announce the
     * category twice. Set it only when the tile is the row's only content.
     */
    contentDescription: String? = null,
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
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(TILE_ICON),
        )
    }
}

private val TILE_SIZE = 48.dp
private val TILE_CORNER = 14.dp
private val TILE_ICON = 24.dp
