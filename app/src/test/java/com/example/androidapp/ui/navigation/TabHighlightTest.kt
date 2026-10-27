package com.example.androidapp.ui.navigation

import androidx.compose.runtime.saveable.SaverScope
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The bar's selected tab and the two writes that keep it right (N223, B264).
 *
 * The interesting case is the one the route cannot describe: a tab **restored at a detail**. Switching tabs saves
 * each tab's stack, so coming back to a tab left on an exercise restores the exercise — and an exercise is not a
 * tab, so the route writes nothing. Everything below is about the tap's write being what keeps the bar honest
 * there, because otherwise the next tap on that tab reads as a switch rather than a re-tap, which is the whole
 * behaviour N223 added.
 *
 * Plain JUnit: `TabHighlight` is Compose state and a rule, not a screen, so it needs no Robolectric.
 */
class TabHighlightTest {

    private val statistics = Statistics.serializer().descriptor.serialName
    private val library = ExerciseLibrary.serializer().descriptor.serialName
    private val detail = ExerciseDetail.serializer().descriptor.serialName

    @Test
    fun aRouteThatNamesATab_movesTheHighlight() {
        val highlight = TabHighlight()

        highlight.onRoute(statistics)
        assertThat(highlight.selected).isEqualTo(AppTab.STATISTICS)

        highlight.onRoute(library)
        assertThat(highlight.selected).isEqualTo(AppTab.LIBRARY)
    }

    @Test
    fun aTabRestoredAtADetail_keepsTheHighlightItWasGiven() {
        // The route of a restored detail names no tab, so it must not be read as "no tab is showing" and
        // leave the bar on whichever tab the user came from.
        val highlight = TabHighlight(AppTab.STATISTICS)

        highlight.onRoute(detail)

        assertThat(highlight.selected).isEqualTo(AppTab.STATISTICS)
    }

    @Test
    fun aTapMovesTheHighlightBeforeTheRouteCan_andReportsWhatWasShowing() {
        val highlight = TabHighlight(AppTab.STATISTICS)

        // A switch: the caller is told the tab that was showing, which is what it compares against.
        assertThat(highlight.onTap(AppTab.LIBRARY)).isEqualTo(AppTab.STATISTICS)
        // And the highlight has already moved, even though the route that follows will be a detail.
        assertThat(highlight.selected).isEqualTo(AppTab.LIBRARY)

        // A re-tap: same tab in, same tab out, which is the answer N223 needs.
        assertThat(highlight.onTap(AppTab.LIBRARY)).isEqualTo(AppTab.LIBRARY)
        assertThat(highlight.selected).isEqualTo(AppTab.LIBRARY)
    }

    @Test
    fun theWholeRestoredShape_readsTheNextTapAsAReTap() {
        // Start in Statistics, switch to Library, open an exercise there, then switch away and back: the
        // stack comes back on the exercise, so the route names nothing and only the tap's write says Library.
        val highlight = TabHighlight()
        highlight.onRoute(statistics)
        assertThat(highlight.onTap(AppTab.LIBRARY)).isEqualTo(AppTab.STATISTICS)
        highlight.onRoute(detail)

        assertThat(highlight.selected).isEqualTo(AppTab.LIBRARY)
        assertThat(highlight.onTap(AppTab.LIBRARY)).isEqualTo(AppTab.LIBRARY)
    }

    @Test
    fun everyTabSurvivesTheProcessBeingKilled() {
        // `rememberSaveable` held a single AppTab before this class; the highlight is still saved state, and
        // still by name rather than ordinal.
        AppTab.entries.forEach { tab ->
            val saved = save(TabHighlight(tab))

            assertThat(saved).isEqualTo(tab.name)
            assertThat(TabHighlight.Saver.restore(requireNotNull(saved))?.selected).isEqualTo(tab)
        }
    }

    /** `Saver.save` takes a [SaverScope] receiver as well as the saver, so both are supplied here. */
    private fun save(state: TabHighlight): String? {
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
        return with(scope) { with(TabHighlight.Saver) { save(state) } }
    }
}
