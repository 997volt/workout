package com.example.androidapp.ui.components

/**
 * The Adherence screen's own tags (ROADMAP P3.5).
 */
internal object AdherenceTags {
    const val MONTH = "adherence_month"
    const val PREVIOUS_MONTH = "adherence_previous_month"
    const val NEXT_MONTH = "adherence_next_month"
    const val RATIO = "adherence_ratio"
    const val SUMMARY = "adherence_summary"
    const val DONE = "adherence_done"
    const val SKIPPED = "adherence_skipped"
    const val MISSED = "adherence_missed"

    /** Shown in place of the ratio when there is no elapsed schedule to score. */
    const val NO_RATIO = "adherence_no_ratio"

    /** A followed block's own progress line (B193's siblings, N189). */
    const val BLOCK_PROGRESS = "adherence_block_progress"

    /** The weeks that can be marked as a deload (ROADMAP P3.10). */
    const val DELOAD_TITLE = "adherence_deload_title"

    fun dayCell(date: String) = "adherence_day_$date"

    fun deloadWeek(date: String, programId: String) = "adherence_deload_${date}_$programId"

    /** The confirmation behind unmarking a week that has a built plan (ROADMAP B162). */
    const val DELOAD_UNMARK_DIALOG = "adherence_deload_unmark_dialog"

    /** Correcting one day's occurrences (ROADMAP P3.13). */
    const val DAY_DIALOG = "adherence_day_dialog"
    const val DAY_EMPTY = "adherence_day_empty"
    const val DAY_CLOSE = "adherence_day_close"

    fun dayOccurrence(slotId: String) = "adherence_occurrence_$slotId"

    fun daySkipped(slotId: String) = "adherence_skipped_$slotId"

    /** The run of work in a row (ROADMAP P3.15). */
    const val STREAK = "adherence_streak"
    const val STREAK_START = "adherence_streak_start"

    /** The ratio as a history of months (ROADMAP P3.16). */
    const val HISTORY_TITLE = "adherence_history_title"
    const val HISTORY_CHART = "adherence_history_chart"
    const val HISTORY_CAPTION = "adherence_history_caption"

    /** The month read per slot and per lift (ROADMAP P3.14). */
    const val BY_SLOT_TITLE = "adherence_by_slot_title"
    const val BY_EXERCISE_TITLE = "adherence_by_lift_title"

    fun slotBreakdown(slotId: String) = "adherence_slot_$slotId"

    fun exerciseBreakdown(exerciseId: String) = "adherence_lift_$exerciseId"
}
