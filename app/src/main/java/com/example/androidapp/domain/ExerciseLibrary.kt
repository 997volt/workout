package com.example.androidapp.domain

import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.taxonomySubtitle
import com.example.androidapp.domain.ExerciseSearch.matches

/**
 * The library as a list of rows: category heads with their children under them (ROADMAP N95).
 *
 * Pure and free of Android types, like [ExerciseSearch], so the grouping and the search that reaches
 * through it are covered by fast JVM tests instead of by driving a screen.
 *
 * **Every category's children are its own rows**, and a movement in no category is a top-level row too.
 * That is the two-rule shape seen from the list's side: a category holds exercises, an exercise holds its
 * variations, and nothing is deeper than that — so [LibraryRow.depth] is 0 or 1 and no walk is needed.
 */

/**
 * The rows to draw, folded and searched.
 *
 * [foldedCategories] holds the ids a lifter has closed. It is a **caller-held set rather than state in
 * here**, because [com.example.androidapp.ui.components.rememberRowFold] is what knows how to survive a
 * rotation (N84's rule) and this stays a pure function of its inputs.
 *
 * **A query is asked about the family before it is asked about the row.** Typing "bench" shows every bench
 * variation, including the ones whose own names never repeat the word — the inheritance N95 describes,
 * reaching the one surface where a lifter notices it. A category is therefore searchable at all, which it
 * would not be if matching stopped at the row.
 */
fun libraryRows(
    exercises: List<Exercise>,
    query: String,
    foldedCategories: Set<String> = emptySet(),
    /**
     * False to list the movements flat, with no heads and no folding.
     *
     * The pickers are why this exists: they share this list and this search, and a picker that offered
     * families would be offering a row that cannot be logged (N95's "never offered"). Flat is also the
     * honest shape there — a lifter choosing what they just did is looking for one name, not a tree.
     */
    grouped: Boolean = true,
): List<LibraryRow> {
    if (!grouped) {
        return exercises
            .filter { it.rowKind.isLoggable }
            .sortedBy { it.name }
            .map { it.toLibraryRow(depth = 0) }
    }

    val byId = exercises.associateBy { it.id }
    val categories = exercises.filter { it.rowKind == RowKind.CATEGORY }.sortedBy { it.name }
    val categoryIds = categories.mapTo(mutableSetOf()) { it.id }
    val matched = exercises.filter { it.matchesWithItsFamily(query, byId) }.mapTo(mutableSetOf()) { it.id }

    val rows = mutableListOf<LibraryRow>()

    categories.forEach { category ->
        val children = exercises
            .filter { it.parentId == category.id && it.id in matched }
            .sortedBy { it.name }

        // A head with nothing left under it, and no match of its own, is not a row: a family the query
        // does not touch is not part of the answer.
        if (children.isEmpty() && category.id !in matched) return@forEach

        rows += LibraryRow(
            id = category.id,
            name = category.name,
            subtitle = null,
            depth = 0,
            isCategory = true,
            childCount = children.size,
            isExpanded = category.id !in foldedCategories,
        )
        if (category.id !in foldedCategories) {
            children.forEach { rows += it.toLibraryRow(depth = 1) }
        }
    }

    // Everything the loop above did not draw, as top-level rows: a movement in no category, and one whose
    // parent the library no longer has — possible because a head is soft-deleted and still names its
    // children (N58's rule), and an orphaned row has to stay reachable rather than vanish with its head.
    exercises
        // `rowKind` as well as the parent, or a category is drawn twice: its own loop above emits it, and
        // its `parentId` is null — which is "not under a category" — so this pass would take it too.
        .filter { it.rowKind == RowKind.MOVEMENT && it.id in matched && it.parentId !in categoryIds }
        .sortedBy { it.name }
        .forEach { rows += it.toLibraryRow(depth = 0) }

    return rows
}

/**
 * True when [query] finds this row, **or the head it hangs under**.
 *
 * The second half is what makes a family searchable: a variation called *Speed Day* is found by "bench"
 * because that is what it is filed under, and the row a lifter is looking for is the one they would have
 * found by the name they know. A row whose own name matches is found whatever its parent says, so filing
 * never hides anything.
 */
private fun Exercise.matchesWithItsFamily(
    query: String,
    byId: Map<String, Exercise>,
): Boolean {
    if (this.matches(query)) return true
    // One level up, and one only: a row's parent is either the category it is filed under or the exercise
    // it is a variation of, and the shape is two rules deep, so there is no third name to consult.
    return byId[parentId]?.matches(query) == true
}

private fun Exercise.toLibraryRow(depth: Int) = LibraryRow(
    id = id,
    name = name,
    subtitle = taxonomySubtitle(primaryMuscle, equipment),
    depth = depth,
    isCategory = false,
)
