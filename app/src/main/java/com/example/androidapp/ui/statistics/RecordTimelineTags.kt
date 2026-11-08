package com.example.androidapp.ui.statistics

/**
 * The records timeline's test tags (ROADMAP N290).
 *
 * **Temporary home**: N285 is splitting `TestTags` into area files while this change is in flight, so these
 * four belong in `TestTagsStatistics`'s `StatisticsTags` and are referenced as `TestTags.Statistics.*` once
 * that split lands. Keeping them in the feature file until then lets the timeline and its tests compile
 * without an edit to a file another change owns.
 */
internal object RecordTimelineTags {
    const val TIMELINE = "statistics_records_timeline"
    const val EMPTY = "statistics_records_empty"

    /** One month heading, by its place in the list — newest month first. */
    fun month(index: Int) = "statistics_records_month_$index"

    /** One record, by its month's place and its own place inside that month. */
    fun row(month: Int, index: Int) = "statistics_records_row_${month}_$index"
}
