package com.example.androidapp.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue

/**
 * The tab the bar highlights, which **two** things change (N223, B264).
 *
 * The route the stack settles on names the tab for anything pushed *inside* one, but not for a tab **restored at a
 * detail**: switching tabs saves each tab's stack, and coming back to a tab left on an exercise restores that
 * exercise, whose route is not a tab at all — [AppTab.forRoute] answers null and the highlight would keep whichever
 * tab the user came from. The tap therefore writes it too, and that write is load-bearing rather than cosmetic: it
 * is what makes the next tap on that tab read as a *re-tap* rather than a switch, which is the whole of N223.
 *
 * A class rather than two `remember`ed values in the host so the rule can be asserted without standing up the
 * application graph: rendering `AppNavHost` needs Hilt and every screen, which is why the app's screens are tested
 * stateless (see `ExerciseLibraryScreenTest`'s note).
 */
internal class TabHighlight(initial: AppTab = AppTab.WORKOUTS) {

    /** Compose state, because the bar reads it: a plain `var` would never recompose. */
    var selected: AppTab by mutableStateOf(initial)
        private set

    /** The stack settled on [route]. A tab adopts it; anything pushed inside one leaves the highlight alone. */
    fun onRoute(route: String?) {
        AppTab.forRoute(route)?.let { selected = it }
    }

    /**
     * A tap on [tapped], applied *before* the navigation so the highlight is already right where the route cannot
     * say it. Returns the tab that was showing, which is what the switch must read as already selected — by the time
     * it is asked, [selected] has moved.
     */
    fun onTap(tapped: AppTab): AppTab {
        val showing = selected
        selected = tapped
        return showing
    }

    companion object {
        /**
         * **By name rather than ordinal**, the rule every enum this app persists keeps — and the highlight the
         * user left on Statistics should survive the process being killed, which `rememberSaveable` gave the
         * single value this replaces.
         */
        val Saver: Saver<TabHighlight, String> = Saver(
            save = { it.selected.name },
            restore = { TabHighlight(AppTab.valueOf(it)) },
        )
    }
}
