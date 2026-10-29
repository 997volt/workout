package com.example.androidapp.data.local

import com.example.androidapp.domain.model.ProgressionDirection

/**
 * The stored form of a declined step's directions (ROADMAP N236).
 *
 * A comma-joined list of **names**, the shape `secondaryMuscles` already uses: a column rather than a second
 * table, because the record is a handful of values hanging off one session exercise and reading it is one
 * column read rather than a join. A name this build does not know is skipped rather than guessed at, and an
 * empty or absent value is "no step was declined" — which is every row written before the column existed.
 */
internal fun encodedDirections(directions: Set<ProgressionDirection>): String? =
    directions.takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR) { it.name }

/** What a stored value means, in the order the enum declares rather than the order it was written. */
internal fun declaredDirections(stored: String?): Set<ProgressionDirection> {
    if (stored.isNullOrEmpty()) return emptySet()
    val named = stored.split(SEPARATOR)
        .mapNotNull(ProgressionDirection::fromName)
        .toSet()
    return ProgressionDirection.entries.filterTo(LinkedHashSet()) { it in named }
}

private const val SEPARATOR = ","
