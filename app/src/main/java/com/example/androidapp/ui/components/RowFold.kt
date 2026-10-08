package com.example.androidapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Which rows of a list are open, keyed by id (ROADMAP N91, reused by N95's library).
 *
 * **A map written once per row, and an immutable value on every change.** A `mutableSetOf()` held inside a
 * state object is mutated in place, and Compose compares state by `equals`, so the set's own contents
 * changing never reads as a change: the tap did nothing and the row stayed open. Assigning a new map is
 * what makes a fold a change, at the cost of copying a handful of pairs per tap.
 *
 * **A row the list has already drawn is folded; one that arrives afterwards opens.** That is N91's "only the
 * blocks that were already there start folded": the map is seeded from what the first content composition
 * can see, so a row absent from it is the one just added — which opens, so its first set (or its first child
 * in the library) needs no second tap. `rememberSaveable` is N84's rule — this activity declares no
 * `configChanges`, so a rotation would otherwise fold what the lifter opened.
 *
 * A first composition while the plan is still loading seeds nothing, so those rows open once it arrives,
 * which the seed cannot tell apart from the added case.
 *
 * [initiallyOpen] is for a caller whose rows have nothing to hide on arrival — the library, where a family
 * that started folded would be a list of names with the movements behind them: both halves of the rule
 * above invert with it, so what the screen opened on is open and what arrives is open too.
 */
@Composable
fun rememberRowFold(ids: List<String>, initiallyOpen: Boolean = false): RowFold {
    val alreadyThere = remember(ids.isEmpty()) { ids.toSet() }
    val state = rememberSaveable { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    if (ids.isNotEmpty()) {
        val missing = ids.filterNot { it in state.value }
        if (missing.isNotEmpty()) {
            state.value = state.value + missing.associateWith { id ->
                // N91's shape folds what the screen opened on and opens what arrives; the library wants
                // every family open on arrival, so both halves invert together with this flag rather than
                // one of them being special-cased.
                if (initiallyOpen) true else id !in alreadyThere
            }
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

    /**
     * The ids the lifter has closed, for a caller that derives something from the folds (ROADMAP N95).
     *
     * N91's own caller asks row by row and needs no such list; the library's grouping has to know the whole
     * set before it can decide which rows exist at all.
     */
    val foldedIds: Set<String> get() = open.filterValues { !it }.keys
}

