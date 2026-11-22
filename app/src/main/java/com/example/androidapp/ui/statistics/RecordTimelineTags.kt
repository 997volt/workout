package com.example.androidapp.ui.statistics

/** The records timeline's own test tags (ROADMAP N290); the N285 split left them with the feature. */
internal object RecordTimelineTags {
    const val TIMELINE = "statistics_records_timeline"
    const val EMPTY = "statistics_records_empty"

    /** One month heading, by its place in the list — newest month first. */
    fun month(index: Int) = "statistics_records_month_$index"

    /** One record, by its month's place and its own place inside that month. */
    fun row(month: Int, index: Int) = "statistics_records_row_${month}_$index"
}
