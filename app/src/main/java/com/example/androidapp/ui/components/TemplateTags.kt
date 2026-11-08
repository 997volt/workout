package com.example.androidapp.ui.components

/**
 * Tags for workout templates: the list, one template's editor, its planned sets and its exercise blocks.
 */
internal interface TemplateTags {
    /**
     * Workout templates (ROADMAP N3): the list, and one template's editor.
     *
     * Row-level tags are parameterised by id, so a test addresses the third
     * exercise by identity rather than by its position on screen.
     */
    val TEMPLATES_TITLE: String get() = "templates_title"

    val TEMPLATES_NEW: String get() = "templates_new"

    val TEMPLATES_EMPTY: String get() = "templates_empty"

    /**
     * The way across to Programs, in this screen's own bar (ROADMAP N93).
     *
     * The two are siblings with no other path between them, so the entry sits with the actions the bar
     * already carries rather than as a control floating over the list.
     */
    val TEMPLATES_PROGRAMS: String get() = "templates_programs"

    val TEMPLATE_EDIT_TITLE: String get() = "template_edit_title"

    val TEMPLATE_NAME_FIELD: String get() = "template_name_field"

    val TEMPLATE_NAME_SAVE: String get() = "template_name_save"

    val TEMPLATE_ADD_EXERCISE: String get() = "template_add_exercise"

    val TEMPLATE_NO_EXERCISES: String get() = "template_no_exercises"

    val TEMPLATE_DELETE: String get() = "template_delete"

    val TEMPLATE_DELETE_CONFIRM: String get() = "template_delete_confirm"

    /**
     * Keeping a version, and the versions already kept (ROADMAP N134).
     *
     * The two live in the editor's own overflow, because keeping a version is done at the moment the
     * lifter decides to change something.
     */
    val TEMPLATE_VERSIONS_MENU: String get() = "template_versions_menu"

    val TEMPLATE_KEEP_VERSION: String get() = "template_keep_version"

    val TEMPLATE_VERSIONS: String get() = "template_versions"

    /** The versions kept of one template, one row per version (ROADMAP N134). */
    val TEMPLATE_VERSIONS_DIALOG: String get() = "template_versions_dialog"

    fun templateVersion(id: String) = "template_version_$id"

    /** One kept version read as a plan, read-only (ROADMAP N134). */
    val TEMPLATE_VERSION_DIALOG: String get() = "template_version_dialog"

    val TEMPLATE_VERSION_TITLE: String get() = "template_version_title"

    val TEMPLATE_VERSION_EMPTY: String get() = "template_version_empty"

    val TEMPLATE_VERSION_CLOSE: String get() = "template_version_close"

    /** A template in the list; tapping it edits, its button starts a workout. */
    fun templateRow(id: String) = "template_row_$id"

    /**
     * The list's filter chips, and the two controls that bring a workout back (ROADMAP N214, N216, N217).
     *
     * The chips are modes — All, Hidden and Deleted — so a chip's selected state and the rows it stands
     * for are one question; a row's own button is the way back without opening the editor.
     */
    val TEMPLATES_FILTER_ALL: String get() = "templates_filter_all"

    val TEMPLATES_HIDDEN_FILTER: String get() = "templates_hidden_filter"

    val TEMPLATES_DELETED_FILTER: String get() = "templates_deleted_filter"

    fun templateUnhide(id: String) = "template_unhide_$id"

    fun templateRestore(id: String) = "template_restore_$id"

    /** The editor's overflow item that hides or brings back the plan (ROADMAP N214). */
    val TEMPLATE_HIDE: String get() = "template_hide"

    /**
     * A planned exercise's name, which opens and folds its block (ROADMAP N91).
     *
     * The exercise's own row tag stays the address of the row as a whole; this is the control inside it,
     * because the fold is the name's tap and not the ⋮ beside it.
     */
    fun templateFold(id: String) = "template_fold_$id"

    fun templateStart(id: String) = "template_start_$id"

    /** Why a template's Start is disabled — a session is already open (ROADMAP N78). */
    fun templateStartReason(id: String) = "template_start_reason_$id"

    fun templateExerciseRow(id: String) = "template_exercise_$id"

    /** A planned exercise's ⋮: the entries it opens are tagged by the helpers below (ROADMAP N71). */
    fun templateMenu(id: String) = "template_menu_$id"

    /** The ⋮ entry that opens the lift itself from a planned row (ROADMAP N121). */
    fun templateOpenExercise(id: String) = "template_open_exercise_$id"

    /** Reordering a template's exercises: one helper, because up and down are one idea. */
    fun templateMove(id: String, up: Boolean) = "template_move_${if (up) "up" else "down"}_$id"

    fun templateRemove(id: String) = "template_remove_$id"

    /**
     * The warm-up ramp this exercise's ⋮ offers (ROADMAP N81), tagged per row: it is an entry on the
     * shared menu, so two planned exercises on screen are two entries (the shape [templateMenu] uses).
     */
    fun templateAddWarmUps(id: String) = "template_add_warmups_$id"

    /**
     * A plan's sets (ROADMAP N14, unfolded into the block by N81), tagged per row like the ramp above:
     * every exercise block carries its own empty note and its own *Add set*, so two planned exercises on
     * screen are two of each (B76, the shape [templatePlanSet] uses).
     */
    fun templatePlanEmpty(id: String) = "template_plan_empty_$id"

    fun templatePlanAdd(id: String) = "template_plan_add_$id"

    /** The plan's quick entry: count, reps, load and the write (P3.18). */
    fun templateQuickSets(id: String) = "template_quick_sets_$id"

    fun templateQuickReps(id: String) = "template_quick_reps_$id"

    fun templateQuickLoad(id: String) = "template_quick_load_$id"

    fun templateQuickAdd(id: String) = "template_quick_add_$id"

    val TEMPLATE_SET_ROLE: String get() = "template_set_role"

    val TEMPLATE_SET_WEIGHT: String get() = "template_set_weight"

    /** The value a drop run takes off its anchor (ROADMAP N79), on the rung that holds it. */
    val TEMPLATE_SET_DROP_VALUE: String get() = "template_set_drop_value"

    val TEMPLATE_SET_REPS_MIN: String get() = "template_set_reps_min"

    val TEMPLATE_SET_REPS_MAX: String get() = "template_set_reps_max"

    val TEMPLATE_SET_HOLD: String get() = "template_set_hold"

    val TEMPLATE_SET_INCREASE_HOLD: String get() = "template_set_increase_hold"

    val TEMPLATE_SET_DECREASE_HOLD: String get() = "template_set_decrease_hold"

    val TEMPLATE_SET_NOTE: String get() = "template_set_note"

    val TEMPLATE_SET_SAVE: String get() = "template_set_save"

    val TEMPLATE_SET_CANCEL: String get() = "template_set_cancel"

    /**
     * The ± pairs the planned-set dialog shares with the workout's fields (ROADMAP N111).
     *
     * Its own tags rather than the logging path's, for the reason the correction dialog has a set:
     * a tag addresses **one** control, and every one of these fields is composed while a workout
     * screen holding the same four may be behind the dialog.
     */
    val TEMPLATE_SET_INCREASE_WEIGHT: String get() = "template_set_increase_weight"

    val TEMPLATE_SET_DECREASE_WEIGHT: String get() = "template_set_decrease_weight"

    val TEMPLATE_SET_INCREASE_DROP_VALUE: String get() = "template_set_increase_drop_value"

    val TEMPLATE_SET_DECREASE_DROP_VALUE: String get() = "template_set_decrease_drop_value"

    val TEMPLATE_SET_INCREASE_REPS_MIN: String get() = "template_set_increase_reps_min"

    val TEMPLATE_SET_DECREASE_REPS_MIN: String get() = "template_set_decrease_reps_min"

    val TEMPLATE_SET_INCREASE_REPS_MAX: String get() = "template_set_increase_reps_max"

    val TEMPLATE_SET_DECREASE_REPS_MAX: String get() = "template_set_decrease_reps_max"

    val TEMPLATE_REST_FIELD: String get() = "template_rest_field"

    val TEMPLATE_CUE_FIELD: String get() = "template_cue_field"

    /** The exercise's one target RPE, on the line the rest and the accept share (N59, N80, N116). */
    val TEMPLATE_EXERCISE_RPE: String get() = "template_exercise_rpe"

    val TEMPLATE_EXERCISE_INCREASE_RPE: String get() = "template_exercise_increase_rpe"

    val TEMPLATE_EXERCISE_DECREASE_RPE: String get() = "template_exercise_decrease_rpe"

    val TEMPLATE_REST_CUE_SAVE: String get() = "template_rest_cue_save"

    fun templatePlanSet(id: String) = "template_plan_set_$id"

    fun templatePlanRemove(id: String) = "template_plan_remove_$id"

    fun templateSetRole(role: String) = "template_set_role_$role"

    /** The editor's scrolling exercise list, so a test can scroll to a row. */
    val TEMPLATE_EXERCISE_LIST: String get() = "template_exercise_list"
}
