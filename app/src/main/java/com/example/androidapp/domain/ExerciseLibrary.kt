package com.example.androidapp.domain

import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MuscleGroup
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
    /**
     * Every row by id, removed ones included, for resolving what a head passes down (ROADMAP N95).
     *
     * Defaults to [exercises], so a caller with nothing removed needs to say nothing. The library passes the
     * fuller map, because a head that was removed still names its children (N58's rule) — and the row's own
     * muscle then comes from a head the list is not drawing.
     */
    headResolver: List<Exercise> = exercises,
): List<LibraryRow> {
    if (!grouped) {
        return exercises
            .filter { it.rowKind.isLoggable }
            .sortedBy { it.name }
            .map { it.toLibraryRow(depth = 0, library = headResolver) }
    }

    val byId = exercises.associateBy { it.id }
    val matched = exercises.filter { it.matchesWithItsFamily(query, byId) }.mapTo(mutableSetOf()) { it.id }
    // The variations, grouped once: N95's shape puts them one level below their exercise, so both passes
    // below need to ask "what hangs under this movement" rather than walking the list again.
    val variationsOf = exercises
        .filter { it.rowKind == RowKind.MOVEMENT }
        .groupBy { it.parentId }

    val rows = mutableListOf<LibraryRow>()

    val shape = LibraryShape(exercises = exercises, variationsOf = variationsOf, library = headResolver)
    exercises
        .filter { it.rowKind == RowKind.CATEGORY }
        .sortedBy { it.name }
        .forEach { category -> rows += familyRows(category, shape, matched, foldedCategories) }

    rows += looseRows(shape, matched, rows.mapTo(mutableSetOf()) { it.id })

    return rows
}

/**
 * One family's rows: the head, then its movements, then each movement's own variations.
 *
 * Empty when the query has nothing to say about the family — neither the head nor any movement under it
 * matches, and a family the query does not touch is not part of the answer. A folded head returns itself
 * alone, keeping its count: the count is what a folded family is *for*.
 */
private fun familyRows(
    category: Exercise,
    shape: LibraryShape,
    matched: Set<String>,
    folded: Set<String>,
): List<LibraryRow> {
    val isFolded = category.id in folded
    val children = shape.exercises
        .filter { it.parentId == category.id && it.id in matched }
        .sortedBy { it.name }
    if (children.isEmpty() && category.id !in matched) return emptyList()

    val head = LibraryRow(
        id = category.id,
        name = category.name,
        subtitle = null,
        depth = 0,
        isCategory = true,
        childCount = children.size,
        isExpanded = !isFolded,
    )
    val rows = mutableListOf(head)
    // A folded head is the one row: its count is what a folded family is *for*.
    if (!isFolded) {
        children.forEach { movement ->
            val variations = shape.variationsOf[movement.id].orEmpty()
                .filter { it.id in matched }
                .sortedBy { it.name }
            rows += movement.toLibraryRow(depth = 1, childCount = variations.size, library = shape.library)
            variations.forEach { rows += it.toLibraryRow(depth = 2, library = shape.library) }
        }
    }
    return rows
}

/**
 * The movements drawn at the top level: one in no family, and one whose family the library no longer has.
 *
 * An orphan is possible because a head is soft-deleted and still names its children (N58's rule), and it has
 * to stay reachable rather than vanish with its head. A variation whose *exercise* is missing is here for the
 * same reason: the movement exists, so hiding it because a row above it is gone would hide a lift.
 */
private fun looseRows(
    shape: LibraryShape,
    matched: Set<String>,
    drawn: Set<String>,
): List<LibraryRow> {
    val known = shape.exercises.mapTo(mutableSetOf()) { it.id }
    return shape.exercises
        // `rowKind` as well as the parent, or a category is drawn twice: its own loop emits it, and its
        // `parentId` is null — which is "not under a family" — so this pass would take it too.
        .filter { it.rowKind == RowKind.MOVEMENT && it.id in matched && it.id !in drawn }
        // A row whose parent this library **has** belongs under it, and the family pass either drew it there
        // or left it out because the head is folded. Only a row with no parent, or a parent that is gone, is
        // top level — which is what keeps a movement's variation from surfacing beside its exercise.
        .filter { it.parentId == null || it.parentId !in known }
        .sortedBy { it.name }
        .map { it.toLibraryRow(depth = 0, library = shape.library) }
}

/**
 * What both grouping passes need, in one value rather than four parameters (ROADMAP N95).
 *
 * The two passes are the same question asked of two row kinds — what hangs under this, and what is left over
 * — so they read the same three facts: the rows to draw, the variations indexed by their exercise, and the
 * fuller library a head is named and inherited from.
 */
private class LibraryShape(
    val exercises: List<Exercise>,
    val variationsOf: Map<String?, List<Exercise>>,
    val library: List<Exercise>,
)

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

private fun Exercise.toLibraryRow(
    depth: Int,
    library: List<Exercise>,
    childCount: Int = 0,
) = LibraryRow(
    id = id,
    name = name,
    // The **effective** muscle rather than the row's own (ROADMAP N95): an exercise inherits its head's, so
    // a family whose head says Shoulders lists its members that way without a copy being written to any row.
    subtitle = taxonomySubtitle(effectivePrimaryMuscle(library), equipment),
    depth = depth,
    isCategory = false,
    childCount = childCount,
)

/**
 * This exercise's **effective** primary muscle: its head's, or its own (ROADMAP N95).
 *
 * *Live*, not copied, which is what "an exercise inherits its category's primary muscle" means in practice
 * and why nothing is written to the child when it is filed: one fact with one home. Change the head's muscle
 * and every movement under it reads the new one, so a family cannot disagree with itself — the same reason a
 * variation inherits its exercise's muscles rather than restating them.
 *
 * **A head that says nothing is silent rather than authoritative**, so the row keeps its own value and starts
 * inheriting the moment the head is filled in. `OTHER` is how this app spells "not specified yet" (N2), and it
 * is every *new* category's value — so reading it literally would blank the muscle of every movement filed
 * under one on the day it was created, while the row still held a real answer nobody could see.
 *
 * A row whose head is not in [library] keeps its own value for the same reason: nothing to inherit is not the
 * same as inheriting nothing.
 */
fun Exercise.effectivePrimaryMuscle(library: List<Exercise>): MuscleGroup =
    library.firstOrNull { it.id == parentId }
        ?.primaryMuscle
        ?.takeIf { it != MuscleGroup.OTHER }
        ?: primaryMuscle

/**
 * This exercise's **effective** secondary muscles: its head's while it has named none of its own (N95).
 *
 * "Secondary muscles default from the category and are the exercise's to change" — so the default applies
 * exactly while the exercise is silent, and the moment a lifter names one the exercise's own list is the
 * answer. That is the one place in this shape where the child's own value wins over its head's, and it is
 * deliberate: the default is a starting point, not a claim about every movement under the head.
 */
fun Exercise.effectiveSecondaryMuscles(library: List<Exercise>): List<MuscleGroup> =
    secondaryMuscles.ifEmpty {
        library.firstOrNull { it.id == parentId }?.secondaryMuscles.orEmpty()
    }

/** The name of the head this row hangs under, or null — read live, and readable after it is removed (N95). */
fun Exercise.headName(library: List<Exercise>): String? =
    library.firstOrNull { it.id == parentId }?.name
