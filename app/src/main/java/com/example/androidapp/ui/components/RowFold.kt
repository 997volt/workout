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
 * **What a row answers before the lifter touches it is [newRowsOpen], and the two callers want opposite
 * things.** The editor wants N91's rule — *only the blocks that were already there start folded* — so a
 * planned exercise's first set needs no second tap: its seed is the id list of the **first** composition
 * that has content, and a row absent from it is the one just added, which opens. The library wants the
 * mirror, and passes false: **a family is folded until the lifter opens it**, whether it was there on the
 * first composition or arrived after it. One boolean with one meaning rather than a mode, because
 * "everything opens" is nobody's rule.
 *
 * That is also why the library cannot use the editor's seed. The library loads asynchronously, so its first
 * composition has no ids at all — every row would then be judged "arrived afterwards" and opened, which is
 * the opposite of what it asks for. **Taken once, with no key, is the point** (B89): keyed on
 * `ids.isEmpty()`, the seed was rebuilt when an empty editor gained its first exercise, so the one row that
 * must open was judged already-there and folded. A first composition while a plan is still loading seeds
 * nothing, and the seed cannot tell that apart from the added case.
 *
 * `rememberSaveable` is N84's rule — this activity declares no `configChanges`, so a rotation would
 * otherwise fold what the lifter opened.
 */
@Composable
fun rememberRowFold(ids: List<String>, newRowsOpen: Boolean = true): RowFold {
    // No key: the first content the screen shows is the whole seed. A key that flips with emptiness rebuilt
    // it to include the first row added, folding exactly the row N91 says opens (B89).
    val alreadyThere = remember { ids.toSet() }
    val state = rememberSaveable { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    if (ids.isNotEmpty()) {
        val missing = ids.filterNot { it in state.value }
        if (missing.isNotEmpty()) {
            state.value = state.value + missing.associateWith { id ->
                // N91 folds what the screen opened on and opens what arrives; the library folds both, which
                // is the whole of the flag's job.
                newRowsOpen && id !in alreadyThere
            }
        }
    }
    return RowFold(
        open = state.value,
        newRowsOpen = newRowsOpen,
        onToggle = { id -> state.value = state.value + (id to !(state.value[id] ?: newRowsOpen)) },
    )
}

/** One row's answer: whether it is open, and the tap that changes that (ROADMAP N91). */
class RowFold(
    private val open: Map<String, Boolean>,
    private val newRowsOpen: Boolean,
    private val onToggle: (String) -> Unit,
) {
    /** A row with no entry yet is one that just arrived — open in the editor (N91), folded in the library. */
    fun isExpanded(id: String): Boolean = open[id] ?: newRowsOpen

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
