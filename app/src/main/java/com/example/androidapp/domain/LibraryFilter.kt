package com.example.androidapp.domain

import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.RowKind
import java.time.Instant

/**
 * How a lifter narrows the library, on top of the search rather than instead of it (ROADMAP N231).
 *
 * The chips are a filter, not a mode: the search applies **inside** the chosen group, because "bench" inside
 * *Bodyweight* is the question a lifter actually has. That is the difference from the templates list, where a
 * mode draws its group *instead of* the live rows (N217) — there, the group is a different list of things;
 * here, both controls narrow one list.
 */
enum class LibraryFilter {
    /** Everything the search reaches. */
    ALL,

    /** Movements whose newest finished session is older than the window, or that were never trained. */
    NOT_TRAINED,

    /** Movements loaded by the lifter's own body (N219). */
    BODYWEIGHT,

    /** Movements held rather than counted (N220). */
    TIMED,

    /** Movements filed under no family — the lifter's own, and the one the seed leaves loose (N229). */
    UNFILED,
}

/**
 * The window [LibraryFilter.NOT_TRAINED] means, in days (ROADMAP N231).
 *
 * One constant, because the chip's label states it and the rule measures it: a label saying thirty days over a
 * filter measuring something else is the kind of drift this project counts as a defect.
 */
const val LIBRARY_NOT_TRAINED_DAYS: Long = 30

/**
 * True when [filter] includes this row (ROADMAP N231).
 *
 * **Only a movement is in a group**: a family is not bodyweight, is not held and is never trained on its own,
 * so a head is drawn by the movements under it that matched rather than by the chip. That is also why the
 * counts never count heads.
 */
fun Exercise.matchesFilter(
    filter: LibraryFilter,
    lastTrained: Map<String, Instant>,
    notTrainedSince: Instant,
): Boolean {
    if (rowKind != RowKind.MOVEMENT) return filter == LibraryFilter.ALL
    return when (filter) {
        LibraryFilter.ALL -> true
        LibraryFilter.BODYWEIGHT -> isBodyweight
        LibraryFilter.TIMED -> isTimed
        LibraryFilter.UNFILED -> parentId == null
        // A movement never trained is not recently trained: the chip is about what is overdue, and the one
        // thing more overdue than a month ago is never.
        LibraryFilter.NOT_TRAINED -> lastTrained[id]?.isBefore(notTrainedSince) ?: true
    }
}

/**
 * How many movements each filter holds, from the **whole** library (ROADMAP N231).
 *
 * The chip's count is the group's size rather than the search's result, so the number does not move while a
 * query is typed — and a group that is empty makes the chip impossible to select, which is what the screen's
 * fallback to *All* reads.
 */
fun libraryFilterCounts(
    exercises: List<Exercise>,
    lastTrained: Map<String, Instant>,
    notTrainedSince: Instant,
): Map<LibraryFilter, Int> = LibraryFilter.entries.associateWith { filter ->
    // **Movements only**, which is what every group is about: a family is not bodyweight and is never
    // trained, so counting heads would make *All* disagree with the others by the number of families.
    exercises.count {
        it.rowKind == RowKind.MOVEMENT && it.matchesFilter(filter, lastTrained, notTrainedSince)
    }
}
