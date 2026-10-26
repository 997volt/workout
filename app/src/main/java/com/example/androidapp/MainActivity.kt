package com.example.androidapp

import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.example.androidapp.data.AutomaticBackup
import com.example.androidapp.ui.navigation.AppNavHost
import com.example.androidapp.ui.theme.AndroidAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The app's single activity.
 *
 * It owns no UI state of its own: the navigation graph (F2) decides which
 * screen is showing and each screen's ViewModel (F3) owns its state. Keeping
 * the activity this thin is what lets every screen be tested without it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * The backup that writes itself when the app is next opened (ROADMAP N218).
     *
     * Injected here rather than started from `Application`, because **onStart is what "opened" means**: a
     * process can sit in the background for days, so a check in `onCreate` would run once and then miss every
     * period that elapsed while it was alive. Running it on each foregrounding is cheap — the first thing it
     * reads is the stored settings, and a backup that is not due returns before touching the database.
     */
    @Inject
    lateinit var automaticBackup: AutomaticBackup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dark bars, unconditionally. `enableEdgeToEdge()` with no arguments picks the status
        // bar's icon tint from the *system* light/dark setting, which on a light-mode device
        // draws dark icons onto this app's near-black bars — the clock and the battery simply
        // vanish. The app has one theme, so the bars have one setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Color.Transparent.toArgb()),
        )
        setContent {
            AndroidAppTheme {
                // Test tags as resource ids, so a device-side tool can address a control by *identity* rather
                // than by coordinates: a tap then either lands on the control or fails, instead of silently
                // hitting its neighbour. It exposes the tags to accessibility tooling, which is the point —
                // without it a Compose app is an opaque tree of anonymous Views to everything outside the
                // process, and the last two rounds were spent guessing offsets because of it.
                Box(modifier = Modifier.semantics { testTagsAsResourceId = true }) {
                    AppNavHost()
                }
            }
        }
    }

    /**
     * Checks whether a backup is due each time the app comes to the front (ROADMAP N218).
     *
     * Fire-and-forget in the activity's own scope, which is the right lifetime for it: a rotation cancels and
     * restarts the check, and a cancelled check either has not written yet or has written and recorded nothing
     * — the second case simply runs again, and the folder gains a file it would have gained anyway.
     */
    override fun onStart() {
        super.onStart()
        lifecycleScope.launch { automaticBackup.runIfDue() }
    }
}
