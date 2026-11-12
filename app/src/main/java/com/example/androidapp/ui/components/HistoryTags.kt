package com.example.androidapp.ui.components

/**
 * Tags for history and one past workout: its search and filters, its detail actions, and the detail
 * screen it opens.
 */
internal interface HistoryTags {
    val DETAIL_SAVE_AS_PLAN: String get() = "detail_save_as_plan"

    /** The schedule offer on the saved plan (P3.18), and its dialog. */
    val DETAIL_SCHEDULE: String get() = "detail_schedule"

    val DETAIL_SCHEDULE_CONFIRM: String get() = "detail_schedule_confirm"

    /** One weekday chip in the schedule dialog, by `DayOfWeek` name. */
    fun detailScheduleDay(day: String) = "detail_schedule_day_$day"

    /** The past workout's title — its plan's name, or its date — and the date under it (N84). */
    val HISTORY_DETAIL_TITLE: String get() = "history_detail_title"

    val HISTORY_DETAIL_DATE: String get() = "history_detail_date"

    /** The one ⋮ on a past workout, and the two entries that write (N84). */
    /** N146's search and its chip, named so a test addresses them rather than their English label. */
    val HISTORY_SEARCH_FIELD: String get() = "history_search_field"

    val HISTORY_FILTER_ALL: String get() = "history_filter_all"

    val HISTORY_FILTER_ACTIVITIES: String get() = "history_filter_activities"

    val HISTORY_FILTER_WORKOUTS: String get() = "history_filter_workouts"

    val HISTORY_NO_MATCH: String get() = "history_no_match"

    val HISTORY_DETAIL_MENU: String get() = "history_detail_menu"

    val HISTORY_DETAIL_EDIT: String get() = "history_detail_edit"

    val HISTORY_DETAIL_DELETE: String get() = "history_detail_delete"

    val HISTORY_DETAIL_CHANGE_DAY: String get() = "history_detail_change_day"

    val HISTORY_DETAIL_CHANGE_DAY_CONFIRM: String get() = "history_detail_change_day_confirm"

    val DETAIL_PLAN_NAME: String get() = "detail_plan_name"

    val DETAIL_PLAN_CONFIRM: String get() = "detail_plan_confirm"

    val DETAIL_OPEN_NEW_PLAN: String get() = "detail_open_new_plan"

    /** The review's line about a step offered and not taken (ROADMAP N236). */

    val HISTORY_STEP_DECLINED: String get() = "history_step_declined"

    /** The previous training the detail compares each set with (ROADMAP N306). */
    val HISTORY_VS_LAST_TIME: String get() = "history_vs_last_time"

    /** One set's change from the same place in it (N306). */
    val HISTORY_SET_DELTA: String get() = "history_set_delta"

    /**
     * Repeating one finished workout from a History row (ROADMAP N48).
     *
     * Per row, because the action is addressed to the workout the row names: a tag that named the
     * screen would not tell a test *which* workout a tap would repeat.
     */
    fun historyRepeat(sessionId: String) = "history_repeat_$sessionId"
}
