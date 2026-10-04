package com.example.androidapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Every text pair the palette hands out clears WCAG AA (ROADMAP N49, N52).
 *
 * The two roles this asserts are the ones the active workout screen made the argument for. *Log
 * set* is a filled tonal button, so its label is `onSecondaryContainer` on `secondaryContainer` —
 * white on the category Teal it used to be, 2.9:1. The links beside it are a `TextButton`, so
 * their label is `primary` — Indigo at 4.07:1 against the page and 3.77:1 on a raised card, both
 * under what body-size text needs. Both were replaced in the palette rather than worked around at
 * the call sites, and this is the test that holds the replacement: a ratio in a comment is a
 * claim nobody can check.
 *
 * The page and the raised card are the two surfaces a text action is drawn on, so both are
 * asserted for each role. 4.5:1 is the AA bar for body-size text; the graphics bar is 3:1, which
 * `TileAccentTest` already holds for the tiles.
 */
class PaletteContrastTest {

    @Test
    fun theLinks_clearBodyTextOnBothSurfacesTheyAreDrawnOn() {
        assertThat(contrast(IndigoLink, Ink)).isAtLeast(BODY_TEXT_MINIMUM)
        assertThat(contrast(IndigoLink, InkRaised)).isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun aFilledTonalButtonsLabel_clearsBodyTextOnItsContainer() {
        // What the Log set button and the rest bar draw: white on the container the palette fills.
        assertThat(contrast(WorkoutColors.onSecondaryContainer, WorkoutColors.secondaryContainer))
            .isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun theFilledTonalContainer_isStillVisibleAgainstThePage() {
        // A quieter container is the point of N49, but a control nobody can see is not quiet —
        // it is gone. 3:1 is the bar for a graphic that carries meaning, which a button's own
        // extent is.
        assertThat(contrast(WorkoutColors.secondaryContainer, Ink)).isAtLeast(GRAPHICS_MINIMUM)
        assertThat(contrast(WorkoutColors.secondaryContainer, InkRaised)).isAtLeast(GRAPHICS_MINIMUM)
    }

    @Test
    fun theAppColour_stillCarriesWhiteWhereItFills() {
        // N49 leaves primary filling the Start pill, the selected tab and the chips, so the pair
        // it fills with is asserted here rather than assumed from the links' change.
        assertThat(contrast(Indigo, Color.White)).isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun errorText_stillReadsOnBothSurfaces() {
        // The workout screen draws a read failure in `error` on the page, and a dialog draws it
        // on a high surface; neither was this change's to move, and neither may have drifted.
        assertThat(contrast(WorkoutColors.error, Ink)).isAtLeast(BODY_TEXT_MINIMUM)
        assertThat(contrast(WorkoutColors.error, InkHigh)).isAtLeast(BODY_TEXT_MINIMUM)
    }

    /** WCAG's ratio: 4.5:1 for body-size text. */
    private fun contrast(first: Color, second: Color): Double {
        val a = first.luminance().toDouble()
        val b = second.luminance().toDouble()
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private companion object {
        const val BODY_TEXT_MINIMUM = 4.5
        const val GRAPHICS_MINIMUM = 3.0
    }
}
