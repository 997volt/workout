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
 * **Every category's children are its own rows**, and a movement in no category is a top-level row with its
 * variations under it. That is the two-rule shape seen from the list's side: a category holds exercises, an
 * exercise holds its variations, and a movement in no category holds its variations itself — so a row is at
 * depth 0, 1 or 2 and no deeper.
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
     * False to list the movements flat, with no heads and no folding, but still filtered by [query].
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
    // The query is trimmed **once**, here, so every caller searches the same way. The trim used to live in
    // `ExerciseSearch.filter`, which the flat branch never went through, so a query of only spaces — blank to
    // v1.16 — matched nothing (B87).
    val trimmed = query.trim()
    val byId = exercises.associateBy { it.id }
    val matched = exercises.filter { it.matchesWithItsFamily(trimmed, byId) }.mapTo(mutableSetOf()) { it.id }
    // The variations, grouped once: N95's shape puts them one level below their exercise, so both passes
    // below need to ask "what hangs under this movement" rather than walking the list again.
    val variationsOf = exercises
        .filter { it.rowKind == RowKind.MOVEMENT }
        .groupBy { it.parentId }

    if (!grouped) {
        // Movements only, and **filtered by the query** (B79): the picker passes its search text here, and
        // the flat branch used to ignore it, so typing did nothing at all. A row matches through its family
        // for the same reason it does in the library, so "bench" finds a variation that never says bench.
        return exercises
            .filter { it.rowKind.isLoggable && it.id in matched }
            .sortedBy { it.name }
            .map { it.toLibraryRow(depth = 0, library = headResolver) }
    }

    // The movements the answer includes: one that matched, and one that did not but has a variation that did
    // — a variation is never drawn without its exercise above it, and a query naming only *Speed Day* used to
    // emit nothing at all (B81).
    val drawnMovements = exercises
        .filter { it.rowKind == RowKind.MOVEMENT }
        .filter { movement ->
            movement.id in matched || variationsOf[movement.id].orEmpty().any { it.id in matched }
        }
        .mapTo(mutableSetOf()) { it.id }

    val shape = LibraryShape(
        exercises = exercises,
        variationsOf = variationsOf,
        library = headResolver,
        drawnMovements = drawnMovements,
    )
    val drawn = mutableSetOf<String>()
    val rows = mutableListOf<LibraryRow>()

    exercises
        .filter { it.rowKind == RowKind.CATEGORY }
        .sortedBy { it.name }
        .forEach { category -> rows += familyRows(category, shape, matched, foldedCategories, drawn) }

    rows += looseRows(shape, matched, drawn)

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
    drawn: MutableSet<String>,
): List<LibraryRow> {
    val isFolded = category.id in folded
    val children = shape.exercises
        .filter { it.parentId == category.id && it.rowKind == RowKind.MOVEMENT && it.id in shape.drawnMovements }
        .sortedBy { it.name }
    if (children.isEmpty() && category.id !in matched) return emptyList()

    drawn += category.id
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
            drawn += movement.id
            val variations = shape.variationsOf[movement.id].orEmpty()
                .filter { it.id in matched }
                .sortedBy { it.name }
            rows += movement.toLibraryRow(depth = 1, childCount = variations.size, library = shape.library)
            variations.forEach { variation ->
                drawn += variation.id
                rows += variation.toLibraryRow(depth = 2, library = shape.library)
            }
        }
    }
    return rows
}

/**
 * The movements drawn at the top level, each with its own variations under it: one in no family, and one
 * whose family the library no longer has.
 *
 * An orphan is possible because a head is soft-deleted and still names its children (N58's rule), and it has
 * to stay reachable rather than vanish with its head. A variation whose *exercise* is missing is here for the
 * same reason: the movement exists, so hiding it because a row above it is gone would hide a lift.
 *
 * **A variation of an unfiled movement hangs under that movement here** (B82). This pass used to draw the
 * movement alone, so a variation of a custom exercise in no category existed, was offered by the flat picker,
 * and could not be found in the library at all.
 */
private fun looseRows(
    shape: LibraryShape,
    matched: Set<String>,
    drawn: Set<String>,
): List<LibraryRow> {
    val known = shape.exercises.mapTo(mutableSetOf()) { it.id }
    val rows = mutableListOf<LibraryRow>()
    shape.exercises
        // `rowKind` as well as the parent, or a category is drawn twice: its own loop emits it, and its
        // `parentId` is null — which is "not under a family" — so this pass would take it too.
        .filter { it.rowKind == RowKind.MOVEMENT && it.id in shape.drawnMovements && it.id !in drawn }
        // A row whose parent this library **has** belongs under it, and the family pass either drew it there
        // or left it out because the head is folded. Only a row with no parent, or a parent that is gone, is
        // top level — which is what keeps a movement's variation from surfacing beside its exercise.
        .filter { it.parentId == null || it.parentId !in known }
        .sortedBy { it.name }
        .forEach { movement ->
            val variations = shape.variationsOf[movement.id].orEmpty()
                .filter { it.id in matched }
                .sortedBy { it.name }
            rows += movement.toLibraryRow(depth = 0, childCount = variations.size, library = shape.library)
            variations.forEach { rows += it.toLibraryRow(depth = 1, library = shape.library) }
        }
    return rows
}

/**
 * What both grouping passes need, in one value rather than five parameters (ROADMAP N95).
 *
 * The two passes are the same question asked of two row kinds — what hangs under this, and what is left over
 * — so they read the same facts: the rows to draw, the variations indexed by their exercise, the fuller
 * library a head is named and inherited from, and **which movements the query's answer includes**, which is
 * not the same as which movements matched (a movement is drawn to carry a matched variation).
 */
private class LibraryShape(
    val exercises: List<Exercise>,
    val variationsOf: Map<String?, List<Exercise>>,
    val library: List<Exercise>,
    val drawnMovements: Set<String>,
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
 * This exercise's **effective** primary muscle: the nearest row above it that states one, or its own.
 *
 * *Live*, not copied, which is what "an exercise inherits its category's primary muscle" means in practice
 * and why nothing is written to the child when it is filed: one fact with one home. Change the head's muscle
 * and every movement under it reads the new one, so a family cannot disagree with itself.
 *
 * **The walk goes all the way up** (B86): the resolver used to read the parent's stored field, so a category's
 * Chest reached its movements but not *their* variations, which then read `OTHER` and disagreed with the
 * exercise above them. The nearest ancestor that states a muscle wins.
 *
 * **A head that says nothing is silent rather than authoritative**, so a row with no stating ancestor keeps
 * its own value and starts inheriting the moment a head is filled in. `OTHER` is how this app spells "not
 * specified yet" (N2), and it is every *new* category's value — so reading it literally would blank the
 * muscle of every movement filed under one on the day it was created, while the row still held a real answer
 * nobody could see. A row whose head is not in [library] keeps its own value for the same reason: nothing to
 * inherit is not the same as inheriting nothing.
 */
fun Exercise.effectivePrimaryMuscle(library: List<Exercise>): MuscleGroup {
    val byId = library.associateBy { it.id }
    // A cycle is not a shape this app writes, but a resolver must not hang on one (B92); `seen` ends the walk.
    val seen = mutableSetOf(id)
    var ancestor = byId[parentId]
    while (ancestor != null && seen.add(ancestor.id)) {
        if (ancestor.primaryMuscle != MuscleGroup.OTHER) return ancestor.primaryMuscle
        ancestor = byId[ancestor.parentId]
    }
    return primaryMuscle
}

/**
 * This exercise's **effective** secondary muscles: the nearest row above it that names one, or its own.
 *
 * "Secondary muscles default from the category and are the exercise's to change" — so the default applies
 * exactly while the exercise is silent, and the moment a lifter names one the exercise's own list is the
 * answer. That is the one place in this shape where the child's own value wins over its head's, and it is
 * deliberate: the default is a starting point, not a claim about every movement under the head. The walk goes
 * all the way up for [effectivePrimaryMuscle]'s reason (B86).
 */
fun Exercise.effectiveSecondaryMuscles(library: List<Exercise>): List<MuscleGroup> {
    val byId = library.associateBy { it.id }
    // A cycle is not a shape this app writes, but a resolver must not hang on one (B92); `seen` ends the walk.
    val seen = mutableSetOf(id)
    var ancestor = byId[parentId]
    var found: List<MuscleGroup>? = secondaryMuscles.ifEmpty { null }
    while (found == null && ancestor != null && seen.add(ancestor.id)) {
        found = ancestor.secondaryMuscles.ifEmpty { null }
        ancestor = byId[ancestor.parentId]
    }
    return found.orEmpty()
}

/** The name of the head this row hangs under, or null — read live, and readable after it is removed (N95). */
fun Exercise.headName(library: List<Exercise>): String? =
    library.firstOrNull { it.id == parentId }?.name

/**
 * The library's shape, enforced on any list of rows about to be written (ROADMAP N95, B92).
 *
 * A file is the one place a parent link this app would never write can arrive: a cycle, a category under a
 * category, a variation of a variation. The screen's own write refuses those at the repository, and the
 * importers write raw rows, so this is shared by both DTO and domain rows and clears the illegal link on the
 * way in rather than letting the row vanish from every grouped list. A parent that is not in the list is left
 * alone: the library already draws such a row top-level, and a program document may legitimately name a
 * movement whose head it does not carry.
 *
 * Generic over the row type because the transfer layer never builds a domain [Exercise] — it maps its own
 * DTO — and the rule must not exist twice.
 */
internal fun <T> List<T>.validLibraryShape(
    id: (T) -> String,
    parentId: (T) -> String?,
    isCategory: (T) -> Boolean,
    withParent: (T, String?) -> T,
): List<T> {
    val byId = associateBy(id)

    // True when this row's parent chain is at most two links and reaches a category or nothing. A cycle is
    // what `seen` ends; "too deep" is a third link, whatever that link points at.
    fun parentIsUsable(row: T): Boolean {
        var current = row
        val seen = mutableSetOf(id(row))
        var depth = 0
        var usable = true
        var atTop = false
        while (!atTop && usable) {
            val parent = byId[parentId(current)]
            when {
                parent == null -> atTop = true
                isCategory(parent) -> {
                    depth++
                    if (depth > 2) usable = false else atTop = true
                }

                else -> {
                    depth++
                    if (depth > 2 || !seen.add(id(parent))) usable = false else current = parent
                }
            }
        }
        return usable
    }

    return map { row ->
        when {
            // A category sits at the top level and may not be filed under anything.
            isCategory(row) -> withParent(row, null)
            parentId(row) == null -> row
            parentIsUsable(row) -> row
            else -> withParent(row, null)
        }
    }
}

/** [validLibraryShape] for a domain row. */
fun List<Exercise>.withValidLibraryShape(): List<Exercise> =
    validLibraryShape(
        id = { it.id },
        parentId = { it.parentId },
        isCategory = { it.rowKind == RowKind.CATEGORY },
        withParent = { row, parent -> row.copy(parentId = parent) },
    )

/**
 * The ids one lift's series reads: itself, and every row beneath it (ROADMAP N97).
 *
 * A *head* is any row that holds others — a category over its movements and their variations, or an
 * exercise over its own variations — and a variation holds nothing, so a variation reads itself. The ids are
 * distinct because the shape is a tree two rules deep, so reading the family cannot double-count a set.
 *
 * **The head's own row is included**, and the same expression is right for both kinds: an exercise with
 * variations can still be logged directly, while a category never names a set, so including it costs a
 * category nothing and keeps an exercise's own work in its own series.
 *
 * Two levels and no more, because that is the shape (N95); `seen` is what keeps a file that carries a cycle
 * from looping, which is the guard every resolver here already has (B92).
 */
fun Exercise.seriesSubjectIds(library: List<Exercise>): List<String> {
    val byParent = library.groupBy { it.parentId }
    val ids = mutableListOf(id)
    val seen = mutableSetOf(id)
    // A frontier rather than two nested loops: same answer, one shape, and it reads as "walk down as far as
    // the shape goes" instead of as an accidental depth.
    var frontier: List<Exercise> = byParent[id].orEmpty()
    repeat(SERIES_DEPTH) {
        val fresh = frontier.filter { seen.add(it.id) }
        ids += fresh.map { it.id }
        frontier = fresh.flatMap { byParent[it.id].orEmpty() }
    }
    return ids
}

/** The library's shape is two rules deep and no deeper (N95), so a family is at most two steps down. */
private const val SERIES_DEPTH = 2

/**
 * True when choosing this row reads a family rather than one lift (ROADMAP N97).
 *
 * A category always heads others — that is what it is for — and a movement heads its own variations when it
 * has any. A variation heads nothing, so it reads as the single lift it is.
 */
fun Exercise.headsOthers(library: List<Exercise>): Boolean =
    rowKind == RowKind.CATEGORY || library.any { it.parentId == id }

/**
 * The head an exercise is counted under: the top of its chain, or itself when it heads nothing
 * (ROADMAP N97).
 *
 * A category for a movement filed under one, the movement for its own variations, and the row itself for
 * anything unfiled — which is what makes the per-lift adherence breakdown count a family as one row while a
 * loose movement still stands alone.
 *
 * A cycle is not a shape this app writes, but the walk must not hang on one, so `seen` ends it — the guard
 * every resolver here carries (B92). Two levels are all the shape has, so the loop is bounded anyway.
 */
fun headOfExercise(id: String, parents: Map<String, String?>): String {
    val seen = mutableSetOf(id)
    var current = id
    while (true) {
        val parent = parents[current] ?: return current
        if (!seen.add(parent)) return current
        current = parent
    }
}
