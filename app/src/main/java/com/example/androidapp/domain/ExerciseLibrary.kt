package com.example.androidapp.domain

import com.example.androidapp.domain.ExerciseSearch.matches
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind
import com.example.androidapp.domain.model.taxonomySubtitle
import java.time.Instant

/**
 * The library as a list of rows: category heads with the movements filed under them (ROADMAP N95, N110).
 *
 * Pure and free of Android types, like [ExerciseSearch], so the grouping and the search that reaches
 * through it are covered by fast JVM tests instead of by driving a screen.
 *
 * **One rule and no deeper**: a category holds movements, a movement in no category is a top-level row, and
 * nothing holds a movement — so a row is at depth 0 or 1. The third level the variation feature drew is gone
 * rather than merely unused (N110).
 */

/**
 * The rows to draw, folded and searched.
 *
 * [foldedCategories] holds the ids a lifter has closed. It is a **caller-held set rather than state in
 * here**, because [com.example.androidapp.ui.components.rememberRowFold] is what knows how to survive a
 * rotation (N84's rule) and this stays a pure function of its inputs.
 *
 * **A query is asked about the family before it is asked about the row.** Typing "bench" shows every
 * movement filed under *Bench Press*, including one whose own name never repeats the word — the inheritance
 * N95 describes, reaching the one surface where a lifter notices it. A category is therefore searchable at
 * all, which it would not be if matching stopped at the row.
 */
fun libraryRows(
    exercises: List<Exercise>,
    query: String,
    foldedCategories: Set<String> = emptySet(),
    /**
     * Every row by id, removed ones included, for resolving what a category passes down (ROADMAP N95).
     *
     * Defaults to [exercises], so a caller with nothing removed needs to say nothing. The library passes the
     * fuller list, because a category that was removed still names its children (N58's rule) — and the row's
     * own muscle then comes from a head the list is not drawing.
     */
    headResolver: List<Exercise> = exercises,
    /** How the list is ordered, which is the lifter's choice and defaults to the library as it was (N230). */
    sort: LibrarySort = LibrarySort.NAME,
    /**
     * When each movement was last trained (ROADMAP N230), keyed by id and empty when nothing is known.
     *
     * Passed in rather than read here for the same reason the folds are: this is a pure function of its
     * inputs, and the read is the repository's.
     */
    lastTrained: Map<String, Instant> = emptyMap(),
): List<LibraryRow> {
    // The query is trimmed **once**, here, so every caller searches the same way. The trim used to live in a
    // filter this list no longer has, so a query of only spaces — blank to v1.16 — matched nothing (B87).
    val trimmed = query.trim()
    val byId = exercises.associateBy { it.id }
    val matched = exercises.filter { it.matchesWithItsFamily(trimmed, byId) }.mapTo(mutableSetOf()) { it.id }

    // What both grouping passes read, in one value rather than six parameters: they are the same question
    // asked twice — what is filed under this, and what is left over — so they read the same facts.
    val shape = LibraryShape(
        exercises = exercises,
        matched = matched,
        // **A query opens every family it draws.** The fold is how a lifter *browses* the library, and a
        // search is not browsing: a family the query only reaches inside would otherwise answer with a head
        // that has to be opened before the match can be seen, which is the wrong answer to "where is Back
        // Squat". Both screens that group want this, so it lives here rather than as the same conditional in
        // two of them — and a *blank* query is not a search, so the lifter's folds stand.
        folded = if (trimmed.isEmpty()) foldedCategories else emptySet(),
        library = headResolver,
    )
    val drawn = mutableSetOf<String>()
    val rows = mutableListOf<LibraryRow>()

    val families = exercises.filter { it.rowKind == RowKind.CATEGORY }
    sort.families(families, shape, lastTrained).forEach { category ->
        rows += familyRows(category, shape, drawn, sort, lastTrained)
    }

    rows += looseRows(shape, drawn, sort, lastTrained)

    return rows
}

/**
 * What both grouping passes need (ROADMAP N95, N110).
 *
 * [library] is the fuller list a row is named and read from, [matched] is which rows the query's answer
 * includes, and [folded] is the categories the lifter has closed — **already cleared by the caller when a
 * query is present**, because a search opens what it reaches into.
 */
private class LibraryShape(
    val exercises: List<Exercise>,
    val matched: Set<String>,
    val folded: Set<String>,
    val library: List<Exercise>,
)

/**
 * One family's rows: the head, then the movements filed under it (ROADMAP N110).
 *
 * Empty when the query has nothing to say about the family — neither the head nor any movement under it
 * matches, and a family the query does not touch is not part of the answer. A folded head returns itself
 * alone, keeping its count: the count is what a folded family is *for*.
 */
private fun familyRows(
    category: Exercise,
    shape: LibraryShape,
    drawn: MutableSet<String>,
    sort: LibrarySort,
    lastTrained: Map<String, Instant>,
): List<LibraryRow> {
    val isFolded = category.id in shape.folded
    // What the family holds, before the query is asked (N225): the head states its own size, and the matched
    // count is what a search is showing of it.
    val filed = shape.exercises.filter { it.parentId == category.id && it.rowKind == RowKind.MOVEMENT }
    val children = sort.movements(filed.filter { it.id in shape.matched }, lastTrained)
    if (children.isEmpty() && category.id !in shape.matched) return emptyList()

    drawn += category.id
    val head = LibraryRow(
        id = category.id,
        name = category.name,
        subtitle = null,
        depth = 0,
        isCategory = true,
        childCount = filed.size,
        matchedChildCount = children.size,
        isExpanded = !isFolded,
    )
    val rows = mutableListOf(head)
    // A folded head is the one row: its count is what a folded family is *for*.
    if (!isFolded) {
        children.forEach { movement ->
            drawn += movement.id
            rows += movement.toLibraryRow(depth = 1, library = shape.library)
        }
    }
    return rows
}

/**
 * The families in the order this sort draws them (ROADMAP N230).
 *
 * **A family's place is its most recently trained member's**: the grouping is what the library is, and a week
 * of rows under *Squat* is one answer to "what did I train last". A family whose movements have never been
 * trained sorts after the ones that have — which is the point of the order, since the bottom of the list is
 * then the work you have not done.
 */
private fun LibrarySort.families(
    families: List<Exercise>,
    shape: LibraryShape,
    lastTrained: Map<String, Instant>,
): List<Exercise> = when (this) {
    LibrarySort.NAME -> families.sortedBy { it.name }
    LibrarySort.LAST_TRAINED -> families.sortedWith(
        // `compareBy` orders a null before a value and `reversed` turns "newest first" into the whole order:
        // a never-trained family therefore lands after the trained ones, which is the list read from the
        // bottom (N230).
        compareBy<Exercise> { newestOf(shape, it, lastTrained) }.reversed().thenBy { it.name },
    )
}

/**
 * The movements in the order this sort draws them (ROADMAP N230).
 *
 * A tie falls back to the name, so the list is stable without depending on the order the database returned.
 */
private fun LibrarySort.movements(
    movements: List<Exercise>,
    lastTrained: Map<String, Instant>,
): List<Exercise> = when (this) {
    LibrarySort.NAME -> movements.sortedBy { it.name }
    LibrarySort.LAST_TRAINED -> movements.sortedWith(
        compareBy<Exercise> { lastTrained[it.id] }.reversed().thenBy { it.name },
    )
}

/** When the newest of a family's movements was trained, or null when none of them was (N230). */
private fun newestOf(
    shape: LibraryShape,
    family: Exercise,
    lastTrained: Map<String, Instant>,
): Instant? = shape.exercises
    .filter { it.parentId == family.id && it.rowKind == RowKind.MOVEMENT }
    .mapNotNull { lastTrained[it.id] }
    .maxOrNull()

/**
 * The families whose membership changed since [previous], given the movements now (ROADMAP N228).
 *
 * The library folds every family on load and remembers the folds, so filing a movement from its own editor
 * changed a count on a folded row and nothing else — the one place the change happened was the one place it
 * could not be seen. This names the families a lifter would want opened, and the screen opens each once.
 *
 * [previous] is the parent every movement had on the previous composition, or **empty when there is none**:
 * the first composition is the library arriving rather than changing, so it opens nothing and seeds the map
 * instead. A movement that appeared is a change to the family it arrived in; one that was re-filed is a change
 * to both families, because one lost a movement and the other gained it.
 */
fun changedFamilies(
    previous: Map<String, String?>,
    exercises: List<Exercise>,
): Set<String> {
    if (previous.isEmpty()) return emptySet()
    val now = exercises
        .filter { it.rowKind == RowKind.MOVEMENT }
        .associate { it.id to it.parentId }
    val changed = mutableSetOf<String>()
    now.forEach { (id, parent) ->
        val before = previous[id]
        when {
            // Arrived, and filed somewhere: that family gained a movement.
            id !in previous -> parent?.let { changed += it }
            before != parent -> {
                before?.let { changed += it }
                parent?.let { changed += it }
            }
        }
    }
    // Gone entirely: the family it was in lost a movement.
    previous.forEach { (id, before) ->
        if (id !in now) before?.let { changed += it }
    }
    return changed
}

/**
 * The movements drawn at the top level: one in no category, and one whose category the library no longer
 * has.
 *
 * An orphan is possible because a category is soft-deleted and still names its children (N58's rule), and it
 * has to stay reachable rather than vanish with its head. The movement exists, so hiding it because a row
 * above it is gone would hide a lift.
 */
private fun looseRows(
    shape: LibraryShape,
    drawn: Set<String>,
    sort: LibrarySort,
    lastTrained: Map<String, Instant>,
): List<LibraryRow> {
    val known = shape.exercises.mapTo(mutableSetOf()) { it.id }
    return sort.movements(shape.exercises, lastTrained)
        // `rowKind` as well as the parent, or a category is drawn twice: its own loop emits it, and its
        // `parentId` is null — which is "not under a family" — so this pass would take it too.
        .filter { it.rowKind == RowKind.MOVEMENT && it.id in shape.matched && it.id !in drawn }
        // A row whose parent this library **has** belongs under it, and the family pass either drew it there
        // or left it out because the head is folded. Only a row with no parent, or a parent that is gone, is
        // top level.
        .filter { it.parentId == null || it.parentId !in known }
        .map { it.toLibraryRow(depth = 0, library = shape.library) }
}

/**
 * True when [query] finds this row, **or the category it is filed under**.
 *
 * The second half is what makes a family searchable: a movement called *Speed Day* is found by "bench"
 * because that is what it is filed under, and the row a lifter is looking for is the one they would have
 * found by the name they know. A row whose own name matches is found whatever its parent says, so filing
 * never hides anything.
 */
private fun Exercise.matchesWithItsFamily(
    query: String,
    byId: Map<String, Exercise>,
): Boolean {
    if (this.matches(query)) return true
    // One level up, and one only: a movement's parent is the category it is filed under, and the shape is a
    // category over movements and no deeper, so there is no third name to consult (N110).
    return byId[parentId]?.matches(query) == true
}

private fun Exercise.toLibraryRow(
    depth: Int,
    library: List<Exercise>,
) = LibraryRow(
    id = id,
    name = name,
    // The **effective** muscle rather than the row's own (ROADMAP N95): a movement reads the one its category
    // states, so a family lists its members that way without a copy being written to any row.
    subtitle = taxonomySubtitle(effectivePrimaryMuscle(library), equipment),
    depth = depth,
    isCategory = false,
)

/**
 * The **one winner rule** (ROADMAP N103): the row's own value when it states one, otherwise the row above it
 * that does, otherwise null.
 *
 * N95 gave each of these facts its own rule — a muscle was read from the nearest *stating ancestor* while
 * the row's own value was only a fallback, and a pattern was read off the head and only if the head was a
 * category. Those agree only while no row states what its head also states, which the seed arranged but a
 * lifter's own edit did not have to: a movement's own muscle was written, exported and read by nothing.
 * One rule for all of them ends that, and it is what makes a **null** column meaningful — `null` is
 * *unstated*, and the walk answers it, while a stated value (including `OTHER`) is the row's own answer.
 *
 * **One step is all the shape has** (N110): a movement's parent is a category, and a category has no parent.
 * The loop stays because a file is a place a cycle can arrive and `seen` is what ends one (B92) — not because
 * there is a second row to read.
 */
private fun <T : Any> Exercise.statedOrInherited(library: List<Exercise>, stated: (Exercise) -> T?): T? {
    val byId = library.associateBy { it.id }
    val seen = mutableSetOf(id)
    var found = stated(this)
    var ancestor = byId[parentId]
    while (found == null && ancestor != null && seen.add(ancestor.id)) {
        found = stated(ancestor)
        ancestor = byId[ancestor.parentId]
    }
    return found
}

/**
 * This exercise's **effective** primary muscle: its own if it states one, else the category's, else null
 * (ROADMAP N103, N110).
 *
 * *Live*, not copied, which is what "a movement inherits its category's primary muscle" means in practice
 * and why nothing is written to the child when it is filed: one fact with one home. Change the category's
 * muscle and every movement under it that states none reads the new one, so a family cannot disagree with
 * itself. **A category that states nothing is silent rather than authoritative**, and a row whose category
 * is not in [library] keeps its own answer for the same reason: nothing to inherit is not the same as
 * inheriting nothing.
 */
fun Exercise.effectivePrimaryMuscle(library: List<Exercise>): MuscleGroup? =
    statedOrInherited(library) { it.primaryMuscle }

/**
 * This exercise's **effective** secondary muscles, which are the one place a *list* is the value: its own
 * when it names any, else the category's, else none (N95, N110).
 *
 * "Secondary muscles default from the category and are the exercise's to change" — the default applies
 * exactly while the exercise is silent, and the moment a lifter names one the exercise's own list is the
 * answer.
 */
fun Exercise.effectiveSecondaryMuscles(library: List<Exercise>): List<MuscleGroup> =
    statedOrInherited(library) { it.secondaryMuscles.ifEmpty { null } }.orEmpty()

/**
 * The name of the category this row is filed under, or null — read live, and readable after it is removed
 * (N95).
 */
fun Exercise.headName(library: List<Exercise>): String? =
    library.firstOrNull { it.id == parentId }?.name

/**
 * The library's shape, enforced on any list of rows about to be written (ROADMAP N95, N110, B92).
 *
 * A file is the one place a link this app would never write can arrive: a cycle, a category filed under
 * something, a movement filed under a movement. The screen's own write refuses those at the repository, and
 * the importers write raw rows, so this is shared by both DTO and domain rows.
 *
 * **A movement under a movement is flattened, not cleared** (N110). It is what every variation in a file
 * written before this change is, and dropping the link would trade one wrong shape for a lost category: the
 * walk folds what the row used to read from the movement above it — [inherit] — because a variation has
 * become an ordinary movement and a movement states its own facts, then files it under the first category it
 * reaches. A **cycle** is the one case that is cleared rather than folded, because there is no top to reach.
 *
 * A parent that is not in the list is left alone: the library already draws such a row top-level, and a
 * program document may legitimately name a movement whose category it does not carry.
 *
 * Generic over the row type because the transfer layer never builds a domain [Exercise] — it maps its own
 * DTO — and the rule must not exist twice.
 */
internal fun <T> List<T>.validLibraryShape(
    id: (T) -> String,
    parentId: (T) -> String?,
    isCategory: (T) -> Boolean,
    withParent: (T, String?) -> T,
    inherit: (T, T) -> T,
): List<T> {
    val byId = associateBy(id)

    return map { row ->
        when {
            // A category sits at the top level and may not be filed under anything.
            isCategory(row) -> withParent(row, null)
            parentId(row) == null -> row
            // A parent this list does not carry cannot be judged, and clearing it would lose the link.
            byId[parentId(row)] == null -> row
            else -> {
                var current = row
                var parent = byId[parentId(current)]
                val seen = mutableSetOf(id(row))
                var usable = true
                while (parent != null && !isCategory(parent)) {
                    if (!seen.add(id(parent))) {
                        usable = false
                        break
                    }
                    current = inherit(current, parent)
                    parent = byId[parentId(parent)]
                }
                if (usable) withParent(current, parent?.let { id(it) }) else withParent(row, null)
            }
        }
    }
}

/** [validLibraryShape] for a domain row, folding what a flattened row used to read from the movement above. */
fun List<Exercise>.withValidLibraryShape(): List<Exercise> =
    validLibraryShape(
        id = { it.id },
        parentId = { it.parentId },
        isCategory = { it.rowKind == RowKind.CATEGORY },
        withParent = { row, parent -> row.copy(parentId = parent) },
        // **Materialised rather than inherited**: the row is becoming a movement in its own right, and a
        // movement states its own facts. Equipment travels with them even though it is never inherited
        // *live* (a family spans equipment) — this is the one moment a formerly inherited value has to be
        // written down, or a paused bench would arrive with no bar.
        inherit = { child, parent ->
            child.copy(
                primaryMuscle = child.primaryMuscle ?: parent.primaryMuscle,
                secondaryMuscles = child.secondaryMuscles.ifEmpty { parent.secondaryMuscles },
                equipment = child.equipment ?: parent.equipment,
                movementPattern = child.movementPattern ?: parent.movementPattern,
            )
        },
    )

/**
 * The ids one lift's series reads: itself, and the movements filed under it (ROADMAP N97, N110).
 *
 * A *head* is a row that holds others, and only a **category** does: the movements under it are what a
 * family is. A movement holds nothing, so it reads itself. The ids are distinct because a movement has one
 * category, so reading the family cannot double-count a set.
 *
 * **The head's own row is included**: a category never names a set, so including it costs the family
 * nothing, and the same expression is then right for a movement too — a category is the only head there is,
 * but the shape of the answer does not have to care.
 *
 * One level and no more, because that is the shape (N110); `seen` is what keeps a file that carries a cycle
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

/** The library's shape is one rule deep (N110), so a family is at most one step down. */
private const val SERIES_DEPTH = 1

/**
 * True when choosing this row reads a family rather than one lift (ROADMAP N97, N110).
 *
 * **Only a category heads others**, because the movements filed under it are the whole of a family — a
 * movement holds nothing, so it reads as the single lift it is. This is what the statistics lift list gates
 * its family label on: a movement is offered under every metric, and a category only where the metric reads
 * one (B98).
 */
val Exercise.headsOthers: Boolean
    get() = rowKind == RowKind.CATEGORY

/**
 * This row's movement pattern: its own if it states one, else the category's, else null (ROADMAP N96,
 * amended by N103 and N110).
 *
 * N96 read this off the **category** — a row in no category had none, and a movement's own value was never
 * consulted. N103 made it the same rule as the muscle: **own-if-stated**, so a category is the pattern's home
 * because nothing under it states one rather than because it outranks them. The seed keeps that true of the
 * data (a filed movement states no pattern, and an unfiled one is left to the lifter), which is what
 * preserves everything N96 bought: a family's pattern decided once, and a loose movement with none unless
 * somebody says otherwise.
 */
fun Exercise.effectiveMovementPattern(library: List<Exercise>): MovementPattern? =
    statedOrInherited(library) { it.movementPattern }

/**
 * The head an exercise is counted under: the category it is filed under, or itself when it is in none
 * (ROADMAP N97, N110).
 *
 * A category for a movement filed under one, and the row itself for anything unfiled — which is what makes
 * the per-lift adherence breakdown count a family as one row while a loose movement still stands alone.
 *
 * A cycle is not a shape this app writes, but the walk must not hang on one, so `seen` ends it — the guard
 * every resolver here carries (B92). One level is all the shape has, so the loop is bounded anyway.
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
