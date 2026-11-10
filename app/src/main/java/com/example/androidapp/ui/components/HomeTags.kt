package com.example.androidapp.ui.components

/**
 * Tags for Home and the ways it starts, skips or substitutes a planned session, and the week in review.
 */
internal interface HomeTags {
    val HOME_TITLE: String get() = "home_title"

    /**
     * The scrolling body itself (ROADMAP N115).
     *
     * A lazy list composes only what is near the viewport, so a test that reaches today's plan — which N115
     * puts last — has to scroll the *list* to it; addressing the row directly cannot work, because the node
     * it would scroll to does not exist until the scroll has happened.
     */
    val HOME_LIST: String get() = "home_list"

    /** The body's row that starts an empty workout (ROADMAP N102). */
    val HOME_START: String get() = "home_start"

    /** The banner that says the followed program's week is exempt from scoring (ROADMAP N300). */
    val HOME_EXEMPT_WEEK: String get() = "home_exempt_week"

    /** The bar's pill, drawn only while a workout is running (ROADMAP N102). */
    val HOME_RESUME: String get() = "home_resume"

    /** The ways in, as rows in the body rather than words in the action row (ROADMAP N3, N42, N102). */
    val HOME_TEMPLATES: String get() = "home_templates"

    val HOME_PROGRAMS: String get() = "home_programs"

    val HOME_MEASUREMENTS: String get() = "home_measurements"

    /**
     * The row that writes today's measurements (ROADMAP N114), drawn only while today has none.
     *
     * Its own tag rather than the measurements row's: the two open the same screen, but one opens it with the
     * daily form already up, and a test has to say which control it means.
     */
    val HOME_MEASURE_TODAY: String get() = "home_measure_today"

    /**
     * The line that says the day has nothing on it (ROADMAP N126).
     *
     * One tag for both states: what a test has to tell apart is *which line* it is, and the two sentences
     * are user-visible strings a translation changes.
     */
    val HOME_NO_PLAN: String get() = "home_no_plan"

    /**
     * The question a planned start asks while a workout is already running (ROADMAP N89), and its two
     * answers. Constants rather than a per-id helper: there is one of this dialog, whatever it is starting.
     */
    val HOME_ACTIVE_WORKOUT_DIALOG: String get() = "home_active_workout_dialog"

    val HOME_ACTIVE_WORKOUT_CONTINUE: String get() = "home_active_workout_continue"

    val HOME_ACTIVE_WORKOUT_DISCARD: String get() = "home_active_workout_discard"

    /** The week in review (ROADMAP N247): its way in, the summary, the stall and one tag per block. */
    val HOME_WEEKLY_REVIEW: String get() = "home_weekly_review"

    val WEEKLY_REVIEW_SCREEN: String get() = "weekly_review_screen"

    val WEEKLY_REVIEW_SUMMARY: String get() = "weekly_review_summary"

    val WEEKLY_REVIEW_STALL: String get() = "weekly_review_stall"

    /** A followed block's line, by name: one tag per line, so two blocks are two assertable nodes (N260). */
    fun weeklyReviewBlock(name: String) = "weekly_review_block_$name"

    /** The role armed for the next one-tap log (ROADMAP N19). */
    /** Starting over (ROADMAP N18): the menu entry, the field and the confirm button. */
    val HOME_CLEAR_DATA: String get() = "home_clear_data"

    val CLEAR_CONFIRM_FIELD: String get() = "clear_confirm_field"

    val CLEAR_CONFIRM_ACTION: String get() = "clear_confirm_action"

    val CLEAR_EXPORT_FIRST: String get() = "clear_export_first"

    val CLEAR_CANCEL: String get() = "clear_cancel"

    /** Substituting one occurrence (ROADMAP P3.11): the action, the dialog and its rows. */
    val HOME_SUBSTITUTE_DIALOG: String get() = "home_substitute_dialog"

    val HOME_SUBSTITUTE_CLEAR: String get() = "home_substitute_clear"

    fun homeSubstitute(planId: String) = "home_substitute_$planId"

    /** A block's *Skip*, the only way past a session that cannot be trained (ROADMAP B195). */
    fun homeSkipBlock(planId: String) = "home_skip_block_$planId"

    /** The confirmation's own Skip, so a tap that writes is named (B356). */
    fun homeSkipConfirm(planId: String) = "home_skip_confirm_$planId"

    fun homeSkipCancel(planId: String) = "home_skip_cancel_$planId"

    fun homeSubstituteTemplate(id: String) = "home_substitute_template_$id"
}

/**
 * Home's own tags (ROADMAP N16, N55, N125).
 */
