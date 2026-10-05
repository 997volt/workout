package com.example.androidapp.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Leaves a screen the first time [closed] turns true — once, not twice (ROADMAP B44).
 *
 * This was two `LaunchedEffect(closed)` blocks in the route above, each calling the leave
 * callback. Both ran in the same frame, so closing a session popped the back stack twice: the
 * workout left, and so did the screen beneath it, leaving the navigation host with nothing to
 * render — the white screen that followed a discard. The duplication was invisible while both
 * effects were anonymous; one effect, named, is the fix.
 *
 * It reads the latest [onLeave] through a remembered state because the effect restarts on
 * `closed`: reading the lambda directly would capture whichever one was current when the effect
 * last started.
 *
 * Its own file since N66: a screen that has to be split to stay under the length AND the function
 * count this project allows does not get to keep a piece that has nothing to do with it.
 */
@Composable
fun LeaveWhenClosed(closed: Boolean, onLeave: () -> Unit) {
    val leave by rememberUpdatedState(onLeave)
    LaunchedEffect(closed) { if (closed) leave() }
}
