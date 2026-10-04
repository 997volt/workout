package com.example.androidapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A tile's glyph reads on the accent behind it (ROADMAP B55).
 *
 * The palette made this claim and nothing held it: `IconTile` tinted every glyph white, which is
 * 2.5:1 on `Amber`. A screenshot cannot assert a ratio, so this does — the same reason the ramp's
 * predicate is a test rather than a comment.
 */
class TileAccentTest {

    @Test
    fun everyAccent_givesItsGlyphAtLeastTheGraphicsMinimum() {
        TileAccent.entries.forEach { accent ->
            assertThat(contrast(accent.color, accent.onColor))
                .isAtLeast(GRAPHICS_MINIMUM)
        }
    }

    @Test
    fun theLightAccents_readBetterInInkThanInWhite() {
        // Why the pair exists rather than one blanket colour: on these the old white was the worse
        // half, and the test says which one the palette chose and why.
        val light = TileAccent.entries.filter { it != TileAccent.Indigo }
        light.forEach { accent ->
            assertThat(contrast(accent.color, accent.onColor))
                .isGreaterThan(contrast(accent.color, Color.White))
        }
    }

    /** WCAG's ratio: 3:1 is the bar for a graphic that carries meaning. */
    private fun contrast(first: Color, second: Color): Double {
        val a = first.luminance().toDouble()
        val b = second.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private companion object {
        const val GRAPHICS_MINIMUM = 3.0
    }
}
