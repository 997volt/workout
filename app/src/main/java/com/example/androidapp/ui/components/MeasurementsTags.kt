package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.TapeSite

/**
 * The measurements screen's own tags (ROADMAP N32).
 */
internal object MeasurementsTags {
    const val ADD = "measurements_add"

    /** The two adds the screen offers (ROADMAP N113): a daily record and a tape one. */
    const val ADD_DAILY = "measurements_add_daily"
    const val ADD_TAPE = "measurements_add_tape"
    const val WEIGHT = "measurements_weight"
    const val BODY_FAT = "measurements_body_fat"
    const val MUSCLE = "measurements_muscle"
    const val CONFIRM = "measurements_confirm"

    /**
     * The mode toggle (ROADMAP N132): the control that enters and leaves the mode a delete lives in.
     *
     * One tag rather than two, because it is one button — its label says which mode the list is in.
     */
    const val EDIT = "measurements_edit"

    /**
     * The list itself (ROADMAP B130), so a test can scroll it rather than assume the first screenful
     * is everything — which is the defect it answers.
     */
    const val LIST = "measurements_list"

    /** The sentence an empty list says, an item of the list rather than a screen of its own. */
    const val EMPTY = "measurements_empty"

    /**
     * The day an edited record belongs to, and the way to change it (a request in use; no roadmap id
     * covers it).
     *
     * Only the edit form carries it: a new entry is today's, so a test can name the difference between
     * the two forms by this tag alone.
     */
    const val EDIT_DATE = "measurements_edit_date"
    const val EDIT_DATE_CHANGE = "measurements_edit_date_change"

    /** The calendar's own confirmation, so a test can prove the picker opened. */
    const val EDIT_DATE_CONFIRM = "measurements_edit_date_confirm"

    /**
     * One record opened to read (a request in use; no roadmap id covers it).
     *
     * The row is the field that opens it, so a test names the entry rather than a position in the
     * list — the same shape [delete] and the other per-row tags use.
     */
    fun row(id: String) = "measurements_row_$id"

    /** The dialog a row opens, its title, and its one control. */
    const val DETAILS = "measurements_details"
    const val DETAILS_TITLE = "measurements_details_title"
    const val DETAILS_CLOSE = "measurements_details_close"

    /**
     * The soreness list a daily entry owns (ROADMAP N113), in the shared scored-picks editor.
     *
     * Its own prefix rather than the readiness note's, for the reason every tag has one: a tag
     * addresses **one** control, and both dialogs exist in the same build.
     */
    internal val SORE = ScoredPickTags("measurements_sore")

    /** One per tape site, so a test names the site rather than a position. */
    fun tape(site: TapeSite) = "measurements_tape_${site.name.lowercase()}"

    fun delete(id: String) = "measurements_delete_$id"
}
