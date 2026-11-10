package com.example.androidapp.ui.components

internal object ProgramsTags {
    const val NEW = "programs_new"
    const val EMPTY = "programs_empty"

    /** One of the shipped examples, addressed by the asset it inserts (P3.19). */
    fun example(asset: String) = "program_example_$asset"
    const val NAME_FIELD = "program_name_field"
    const val NAME_SAVE = "program_name_save"
    const val ACTIVE = "program_active"

    /** The two answers to "a week that repeats, or an order that ends" (ROADMAP N178). */
    const val KIND_WEEKLY = "program_kind_weekly"
    const val KIND_SEQUENCE = "program_kind_sequence"

    /** The authored day a block begins, and the button that opens its picker (ROADMAP N178). */
    const val START_DATE = "program_start_date"
    const val START_DATE_CHANGE = "program_start_date_change"

    /** The picker's own two answers (B201): named and addressable, not just "Set" and "Cancel". */
    const val START_DATE_SET = "program_start_date_set"
    const val START_DATE_CANCEL = "program_start_date_cancel"

    /** A finished block says so, rather than marking its last day as next (B197). */
    const val BLOCK_COMPLETE = "program_block_complete"

    /** The authored phase weeks: one chip each, plus add and remove (ROADMAP N292). */
    const val PHASE_ADD = "program_phase_add"
    const val PHASE_REMOVE = "program_phase_remove"
    const val PHASE_REMOVE_CONFIRM = "program_phase_remove_confirm"

    fun phaseWeek(week: Int) = "program_phase_week_$week"

    /**
     * The active program at the foot of the list (ROADMAP N92).
     *
     * The card that makes the thing to open the most reachable thing on the screen, rather than the
     * row under the app bar.
     */
    const val ACTIVE_CARD = "program_active_card"
    const val ADD_SLOT = "program_add_slot"
    const val NO_SLOTS = "program_no_slots"

    /** A plan that stopped moving, and the way to a deload (ROADMAP N237). */
    const val STALL = "program_stall"
    const val STALL_DELOAD = "program_stall_deload"

    const val DELETE = "program_delete"
    const val DELETE_CONFIRM = "program_delete_confirm"
    const val PICKER = "program_template_picker"
    const val PICKER_EMPTY = "program_template_picker_empty"

    /** The picker's *New workout* entry, which creates and opens one inline (P3.18). */
    const val PICKER_NEW = "program_template_picker_new"

    /** Carrying a program as a file (ROADMAP N47): the load action and the export action. */
    const val LOAD = "program_load"
    const val EXPORT = "program_export"

    /** The confirmation an edit document stops at, and its two answers (ROADMAP N171). */
    const val EDIT_DIALOG = "program_edit_dialog"
    const val EDIT_CONFIRM = "program_edit_confirm"
    const val EDIT_CANCEL = "program_edit_cancel"

    /** The name a refused load is asked for, and its two answers (ROADMAP N243). */
    const val RENAME_DIALOG = "program_rename_dialog"
    const val RENAME_NAME = "program_rename_name"
    const val RENAME_CONFIRM = "program_rename_confirm"
    const val RENAME_CANCEL = "program_rename_cancel"

    /**
     * The way across to Templates, in this screen's own bar (ROADMAP N93).
     *
     * Beside *Load*, which is the other thing this bar does; a way across rather than a third way to
     * start anything, which is the templates list's own withholding (N78, B43).
     */
    const val TEMPLATES = "program_templates"

    /** The point-of-start question (P3.3): the missed day, and the two answers. */
    const val SKIP_PROMPT = "program_skip_prompt"
    const val SKIP_DO_NOW = "program_skip_do_now"
    const val SKIP_CONTINUE = "program_skip_continue"

    /** A slot's ⋮: the order and remove entries it opens (ROADMAP N72). */
    fun slotMenu(id: String) = "program_slot_menu_$id"

    /** The slot menu's entry that opens the template it names (ROADMAP N120). */
    fun openSlot(id: String) = "program_open_slot_$id"

    /** The bar's ⋮ (ROADMAP N119), which carries the balance view, export and delete. */
    const val MENU = "program_menu"

    /** The bar's ⋮ entry that opens the balance view (ROADMAP N119). */
    const val BALANCE = "program_balance_open"

    /** The bar's ⋮ entry that opens the deload creator (ROADMAP N147). */
    const val DELOAD = "program_deload_open"

    /** The bar's ⋮ entry that opens the recovery-week choices (ROADMAP N300). */
    const val WEEKS_MENU = "program_weeks_open"

    /** The program screen's visible recovery entry (ROADMAP N300). */
    const val RECOVERY_WEEK = "program_recovery_week"

    /** The recovery choices: the screen, its two week actions, the builder and the unmark question. */
    const val WEEKS_SCREEN = "program_weeks_screen"
    const val WEEKS_THIS = "program_weeks_this"
    const val WEEKS_NEXT = "program_weeks_next"
    const val WEEKS_BUILD = "program_weeks_build"
    const val WEEKS_UNMARK_DIALOG = "program_weeks_unmark_dialog"
    const val WEEKS_UNMARK_CONFIRM = "program_weeks_unmark_confirm"

    /** The bar's ⋮ entry that opens auto progression (ROADMAP N299). */
    const val AUTO_PROGRESSION = "program_auto_progression_open"

    /** The auto progression screen and its controls (ROADMAP N299). */
    const val AUTO_PROGRESSION_SCREEN = "program_auto_progression"
    const val AUTO_SWITCH = "program_auto_switch"
    const val AUTO_ENABLE = "program_auto_enable"
    const val AUTO_RESET = "program_auto_reset"
    const val AUTO_RESET_CONFIRM = "program_auto_reset_confirm"

    /** The read-only view of what a slot's template prescribes (ROADMAP N72). */
    const val PREVIEW = "program_preview"
    const val PREVIEW_EMPTY = "program_preview_empty"
    const val PREVIEW_CLOSE = "program_preview_close"

    /**
     * The slot name that opens that view (ROADMAP N72).
     *
     * A tag as well as the `onClickLabel`: the label is what a screen reader announces, while a
     * test has to address the control by identity rather than by the English it happens to show.
     */
    fun previewOpen(id: String) = "program_preview_open_$id"

    /** The question behind a slot's remove (ROADMAP N72). */
    const val SLOT_REMOVE_CONFIRM = "program_slot_remove_confirm"
    const val SLOT_REMOVE_CANCEL = "program_slot_remove_cancel"

    /** The slot the run is at, marked in the editor (ROADMAP P3.9). */
    fun runSlot(id: String) = "program_run_$id"

    /** A program in the list; tapping it edits, its button makes it the active one. */
    fun row(id: String) = "program_row_$id"

    fun use(id: String) = "program_use_$id"

    /** The authored order of the programs themselves (ROADMAP P3.12). */
    fun moveProgram(id: String, up: Boolean) = "program_move_${if (up) "up" else "down"}_$id"

    fun slot(id: String) = "program_slot_$id"

    fun move(id: String, up: Boolean) = "program_slot_move_${if (up) "up" else "down"}_$id"

    fun slotWeekday(id: String, day: String) = "program_slot_weekday_${day}_$id"

    fun removeSlot(id: String) = "program_slot_remove_$id"

    fun pickTemplate(id: String) = "program_pick_template_$id"
}
