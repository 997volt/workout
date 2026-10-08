package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Which rows of a list are open, keyed by id (ROADMAP N91).
 *
 * **A map written once per exercise, and an immutable value on every change.** A `mutableSetOf()` held
 * inside a state object is mutated in place, and Compose compares state by `equals`, so the set's own
 * contents changing never reads as a change: the tap did nothing and the block stayed open. Assigning a
 * new map is what makes a fold a change, at the cost of copying a handful of pairs per tap.
 *
 * **The first exercise the list draws is recorded as open — or folded, if the plan already held it when
 * the screen arrived.** That is the whole of N91's "only the blocks that were already there start
 * folded": a block's absence from the map means it is the exercise just added, which opens so its first
 * set can be added without a second tap. `rememberSaveable` is N84's rule — this activity declares no
 * `configChanges`, so a rotation would otherwise fold what the lifter opened — and it is why the map
 * holds a plain `Boolean` per id rather than deriving the state from a list's membership.
 *
 * A first composition while the plan is still loading records nothing, so those blocks open once it
 * arrives, which is the case the second half above cannot tell apart from an added exercise.
 */
@Composable
fun rememberRowFold(ids: List<String>): RowFold {
    val alreadyThere = remember(ids.isEmpty()) { ids.toSet() }
    val state = rememberSaveable { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    if (ids.isNotEmpty()) {
        val missing = ids.filterNot { it in state.value }
        if (missing.isNotEmpty()) {
            state.value = state.value + missing.associateWith { it !in alreadyThere }
        }
    }
    return RowFold(
        open = state.value,
        onToggle = { id -> state.value = state.value + (id to !(state.value[id] ?: true)) },
    )
}

/** One row's answer: whether it is open, and the tap that changes that (ROADMAP N91). */
class RowFold(
    private val open: Map<String, Boolean>,
    private val onToggle: (String) -> Unit,
) {
    /** A row with no entry yet is the one that just arrived, so it opens (ROADMAP N91). */
    fun isExpanded(id: String): Boolean = open[id] ?: true

    /** The tap on a row's name: fold it, or open it again. */
    fun toggle(id: String) = onToggle(id)
}

