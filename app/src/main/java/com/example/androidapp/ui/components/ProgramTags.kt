package com.example.androidapp.ui.components

/**
 * Tags for programs (ROADMAP P3.3): the balance view's body.
 */
internal interface ProgramTags {
    /** A program's balance view (ROADMAP N119): the destination, its empty state, its total and its rows. */
    val PROGRAM_BALANCE: String get() = "program_balance"

    val PROGRAM_BALANCE_EMPTY: String get() = "program_balance_empty"

    val PROGRAM_BALANCE_TOTAL: String get() = "program_balance_total"

    fun programBalanceRow(muscle: String) = "program_balance_row_$muscle"

    /** One side of the plan/trained toggle, named by its enum rather than by English (ROADMAP N289). */
    fun programBalanceView(view: String) = "program_balance_view_$view"

    /** One trained window, named by its enum rather than by English (ROADMAP N289). */
    fun programBalanceWindow(window: String) = "program_balance_window_$window"

    /** The trained side's total, its empty state, and one muscle's count in it (ROADMAP N289). */
    val PROGRAM_BALANCE_TRAINED_TOTAL: String get() = "program_balance_trained_total"

    val PROGRAM_BALANCE_TRAINED_EMPTY: String get() = "program_balance_trained_empty"

    /** The read failed; the screen says so rather than drawing a zero (ROADMAP N289). */
    val PROGRAM_BALANCE_TRAINED_FAILED: String get() = "program_balance_trained_failed"

    fun programBalanceTrainedRow(muscle: String) = "program_balance_trained_row_$muscle"
}

/**
 * Programs (ROADMAP P3.3): the same shape as templates, rows addressed by id.
 */
