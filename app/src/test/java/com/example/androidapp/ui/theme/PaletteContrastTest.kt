package com.example.androidapp.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.androidapp.kotlinSources
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/**
 * The palette's text pairs, and the one place a link may be drawn (ROADMAP N49).
 *
 * The two roles this holds are the ones the active workout screen made the argument for. *Log set* is
 * a filled tonal button, so its label is `onSecondaryContainer` on `secondaryContainer` — white on the
 * category Teal it used to be, 2.9:1. The links beside it are `TextButton`s, whose own label is
 * `primary` — Indigo at 4.07:1 against the page and 3.77:1 on a raised card, both under what body-size
 * text needs. Both were replaced in the palette rather than worked around at the call sites: the
 * container is the same hue taken down to a surface ([TealDeep]), links draw [IndigoLink] through
 * `AppTextButton`, and `primary` goes on filling the pill, the selected tab and the chips.
 *
 * These are the pairs the app **draws**, not every pair in the scheme. The scheme still carries
 * Material's `secondary`/`onSecondary` (white on Teal, 2.90:1) and its coral and crimson containers,
 * which nothing reads as a text pair today — and saying so is the honest half of a test whose first
 * version claimed to hold every pair while a role nothing used passed it.
 *
 * Which is why the link half is a **source scan** rather than a ratio: a ratio on a constant nobody
 * reads is a claim nobody can check, and that is exactly how the first attempt shipped IndigoLink
 * without a single control using it. 4.5:1 is the AA bar for body-size text; 3:1 is the bar for a
 * graphic that carries meaning, which `TileAccentTest` already holds for the tiles.
 */
class PaletteContrastTest {

    @Test
    fun theLinks_clearBodyTextOnEverySurfaceTheyAreDrawnOn() {
        assertThat(contrast(IndigoLink, Ink)).isAtLeast(BODY_TEXT_MINIMUM)
        assertThat(contrast(IndigoLink, InkRaised)).isAtLeast(BODY_TEXT_MINIMUM)
        // A dialog is its own surface, and it is where most of the app's text actions live.
        assertThat(contrast(IndigoLink, InkHigh)).isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun aFilledTonalButtonsLabel_clearsBodyTextOnItsContainer() {
        // What the Log set button draws, what a planned-workout start draws since N83, and what the rest
        // bar's own label and its links draw: the container's own content colour, white on the container
        // the palette fills.
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
    fun theDeepIndigoContainer_carriesItsLabel() {
        // The empty start's container (N61), and since N83 the only pill that draws it: Indigo taken down
        // to the theme's `primaryContainer`, one step below the accent, which the planned-workout starts
        // and *Resume* now take or keep. Only the label pair is held here — unlike the teal container,
        // this one is a recessed tone beside the accent rather than a quiet stand-alone control, so it is
        // the white caption, not the extent, that names it.
        assertThat(contrast(WorkoutColors.onPrimaryContainer, WorkoutColors.primaryContainer))
            .isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun theAppColour_stillCarriesWhiteWhereItFills() {
        // N49 leaves primary filling the selected tab, a chip that is on, and the workout's *Resume* —
        // the planned-workout starts moved to the tonal container at N83 — so the pair it fills with is
        // asserted here rather than assumed from the links' change.
        assertThat(contrast(Indigo, Color.White)).isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun errorText_stillReadsOnBothSurfaces() {
        // The workout screen draws a read failure in `error` on the page, and a dialog draws it
        // on a high surface; neither was this change's to move, and neither may have drifted.
        assertThat(contrast(WorkoutColors.error, Ink)).isAtLeast(BODY_TEXT_MINIMUM)
        assertThat(contrast(WorkoutColors.error, InkHigh)).isAtLeast(BODY_TEXT_MINIMUM)
    }

    @Test
    fun everyLink_inProduction_drawsTheRoleRatherThanTheFillColour() {
        // A bare `TextButton` inherits `primary`, which is the colour that fills — so one that reaches
        // for it is a label under 4.5:1, which is the defect N49 exists to remove. `AppTextButton` is
        // the single caller allowed to name it, and this is what stops the role going unused again.
        val stray = kotlinSources("app/src/main")
            .filterNot { it.name == APP_TEXT_BUTTON }
            .filter { STANDALONE_TEXT_BUTTON.containsMatchIn(it.readText()) }
            .map { it.relativeTo(repoRoot).invariantSeparatorsPath }
            .sorted()

        assertThat(stray).isEmpty()
    }

    @Test
    fun thePlannedWorkoutStarts_drawTheTonalContainer_thePairAboveHolds() {
        // N83, and N49's own lesson applied to the change that extended it: a ratio on a colour no control
        // draws is a claim nobody can check, which is why the link half of this file is a scan rather than
        // an assertion. The pair above is worth holding because these two read it — and both of them,
        // because the two planned-workout starts are one action and only one of them carried the accent.
        val plannedStarts = listOf(
            "app/src/main/java/com/example/androidapp/ui/home/HomeRows.kt",
            "app/src/main/java/com/example/androidapp/ui/home/WorkoutsHomeScreen.kt",
        )

        plannedStarts.forEach { path ->
            assertThat(File(repoRoot, path).readText())
                .contains("containerColor = MaterialTheme.colorScheme.secondaryContainer")
        }
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
        const val APP_TEXT_BUTTON = "AppTextButton.kt"

        /**
         * `TextButton(` and not `AppTextButton(`: there is no word boundary between the `p` and the
         * `T`, so the wrapper's own call does not match and does not need excluding by name.
         */
        val STANDALONE_TEXT_BUTTON = Regex("""\bTextButton\(""")
    }
}
