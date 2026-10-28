package com.example.androidapp.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
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
 * drag. Nothing is consumed until the drag is known to be the figure's (B211), and a second finger hands the
 * gesture to [onZoom] (N213).
 */
@Composable
internal fun Modifier.scrubbable(
    onScrub: (Float?) -> Unit,
    onZoom: ((scale: Float, focus: Float) -> Unit)? = null,
): Modifier {
    // The pointer loop is started once and keeps the callbacks it captured (B290). It used to key on
    // `onZoom` while reading `onScrub`, which the figure rebuilds on every composition with the current
    // overlay and series — so a scrub either read a stale chart or the loop restarted mid-gesture.
    val scrub = rememberUpdatedState(onScrub)
    val zoom = rememberUpdatedState(onZoom)
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            scrub.value(down.fraction(size.width))
            val gesture = ChartGesture(
                down = down,
                onScrub = { fraction -> scrub.value(fraction) },
                onZoom = { scale, focus -> zoom.value?.invoke(scale, focus) },
                slop = viewConfiguration.touchSlop,
                width = size.width,
            )
            var event = awaitPointerEvent()
            while (event.changes.any { it.pressed }) {
                gesture.apply(event)
                event = awaitPointerEvent()
            }
            scrub.value(null)
        }
    }
}

/**
 * One chart gesture's state and its two shapes: a scrub while one finger is down, a pinch once a second joins.
 *
 * A pinch takes the gesture over for good — the rest of it is not a scrub — and consumes its events so the
 * enclosing scrollable stays out of it. A drag that is more vertical than horizontal is left unconsumed, which
 * is what lets the screen scroll (B211).
 */
private class ChartGesture(
    private val down: PointerInputChange,
    private val onScrub: (Float?) -> Unit,
    private val onZoom: ((scale: Float, focus: Float) -> Unit)?,
    private val slop: Float,
    private val width: Int,
) {
    private var direction: Boolean? = null
    private var pinching = false

    fun apply(event: PointerEvent) {
        val pressed = event.changes.count { it.pressed }
        if (onZoom != null && (pressed >= 2 || pinching)) {
            pinch(event)
        } else if (!pinching) {
            scrub(event)
        }
    }

    private fun pinch(event: PointerEvent) {
        if (!pinching) {
            pinching = true
            onScrub(null)
        }
        val zoom = event.calculateZoom()
        if (zoom.isFinite() && zoom > 0f) {
            onZoom?.invoke(zoom, event.calculateCentroid(useCurrent = true).x / width)
        }
        event.changes.forEach { it.consume() }
    }

    private fun scrub(event: PointerEvent) {
        val change = event.changes.firstOrNull { it.id == down.id }
        if (change != null && change.pressed) {
            if (direction == null) {
                direction = scrubDirection(change.position - down.position, slop)
                if (direction == false) onScrub(null)
            }
            if (direction != false) {
                onScrub(change.fraction(width))
                if (direction == true) change.consume()
            }
        }
    }
}

/** How far across the chart the pointer is, as the fraction the projection uses. */
private fun PointerInputChange.fraction(width: Int): Float =
    (position.x / width.toFloat()).coerceIn(0f, 1f)
