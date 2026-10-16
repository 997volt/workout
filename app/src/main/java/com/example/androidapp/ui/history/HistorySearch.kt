package com.example.androidapp.ui.history

import com.example.androidapp.domain.model.WorkoutSummary
import java.time.ZoneId
import java.util.Locale

/**
 * The rows one query and one filter leave, grouped by month, newest first (ROADMAP N146).
 *
 * **Searched on what the row says**, which is deliberate: the list is a projection of totals and a template
 * name, so a query cannot reach an exercise name without loading every set of every session — the cost the
 * projection exists to avoid. What it can reach is the month the row is grouped under, the plan it was started
 * from, what kind of day it was, and its set count — and those are what a lifter scanning a log has in front of
 * them. "cycling" and "push" both land.
 *
 * Pure and file-level, like the grouping it wraps, so the matching rules are unit-tested rather than driven
 * through a screen.
 */
internal fun historyGroups(
    workouts: List<WorkoutSummary>,
    query: String,
    filter: HistoryFilter,
    zone: ZoneId,
    locale: Locale = Locale.getDefault(),
): List<HistoryGroup> {
    // Both sides are compared with the spaces taken out (N146). A stored name is `ROCK_CLIMBING` and nobody
    // types an underscore, while the row draws "Rock climbing" — so `rockclimbing`, `rock climbing` and
    // `Rock_Climbing` are one query, and neither the caller nor the stored form has to be guessed at.
    val wanted = query.searchKey(locale)

    return groupByMonth(workouts.filter(filter::admits), zone)
        .mapNotNull { group ->
            if (wanted.isEmpty()) {
                group
            } else {
                val month = HistoryFormat.month(group.month, locale)
                val rows = group.workouts.filter { row -> row.searchWords(month).contains(wanted) }
                // A month with nothing left in it is not a heading over an empty list: the heading exists to
                // group rows, and a query that removed them all has nothing to group.
                rows.takeIf { it.isNotEmpty() }?.let { HistoryGroup(month = group.month, workouts = it) }
            }
        }
}

/** What a row is searched by: the month above it, what it was, and its set count. */
private fun WorkoutSummary.searchWords(month: String): String = buildString {
    append(month)
    append(' ')
    // The stored name rather than the row's label: the label is a string resource, and a ViewModel may not read
    // one. `ROCK_CLIMBING` is what a lifter calls rock climbing, which is what [searchKey] makes of both.
    append(activity?.name.orEmpty())
    append(' ')
    append(templateName.orEmpty())
    append(' ')
    append(setCount)
}.searchKey(Locale.getDefault())

/**
 * The form both a query and a row are compared in: lowercase, and without the spaces.
 *
 * Exposed for the test that pins the rule, because "why did `rock climbing` not find `ROCK_CLIMBING`" is the
 * question this answers and the only place that can be asked.
 */
internal fun String.searchKey(locale: Locale = Locale.getDefault()): String =
    lowercase(locale).filterNot { it.isWhitespace() || it == '_' }
