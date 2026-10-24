package com.example.androidapp.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

/**
 * Which gesture a drag that has moved [total] from its down is: the figure's (`true`), the screen's (`false`),
 * or undecided while it is still inside the touch slop (B211, B217).
 *
 * A scrub only ever reads the horizontal place, so a drag that is more vertical than horizontal belongs to the
 * enclosing scrollable. Deciding this before consuming anything is what lets that drag through: a scrollable
 * cancels its own gesture the moment a change it sees has been consumed.
 */
internal fun scrubDirection(total: Offset, slop: Float): Boolean? {
    val horizontal = abs(total.x)
    val vertical = abs(total.y)
    if (horizontal <= slop && vertical <= slop) return null
    return horizontal >= vertical
}

/**
 * Reports where a finger is across the chart, and null when it lifts (N200).
 *
 * A tap and a drag are the same gesture here — the finger lands and the nearest reading is named — so this
 * awaits the pointer's own stream rather than composing two detectors that would disagree about a zero-distance
 * drag. Nothing is consumed until the drag is known to be the figure's (B211).
 *
 * Lives beside [TrendChart] rather than in a feature package (B217): the component is the one that draws the
 * points, so it is also the one that knows where a finger is on them.
 */
internal fun Modifier.scrubbable(onScrub: (Float?) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        onScrub(down.fraction(size.width))
        // Null while the gesture could still become either one.
        var direction: Boolean? = null
        var pressed = true
        while (pressed) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            pressed = change.pressed
            if (!pressed) break
            if (direction == null) {
                direction = scrubDirection(
                    total = change.position - down.position,
                    slop = viewConfiguration.touchSlop,
                )
                if (direction == false) onScrub(null)
            }
            if (direction != false) {
                onScrub(change.fraction(size.width))
                if (direction == true) change.consume()
            }
        }
        onScrub(null)
    }
}

/** How far across the chart the pointer is, as the fraction the projection uses. */
private fun PointerInputChange.fraction(width: Int): Float =
    (position.x / width.toFloat()).coerceIn(0f, 1f)
