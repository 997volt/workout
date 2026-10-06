package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.TapeSite

/**
 * Test tags for shared controls (ROADMAP P1.17).
 *
 * Tests target these instead of English literals, so a translated string cannot
 * break a test — and so a test failure means behaviour changed, not wording.
 */
object TestTags {
    const val SET_WEIGHT_FIELD = "set_weight_field"
    const val SET_REPS_FIELD = "set_reps_field"

    /** RPE and the set comment (ROADMAP N6). The RPE is a stepper; the logging path always states a
     *  number, and the edit path shows "Not recorded" for a set that gave none (N59). */
    const val SET_RPE_FIELD = "set_rpe_field"
    const val SET_DECREASE_RPE = "set_decrease_rpe"
    const val SET_INCREASE_RPE = "set_increase_rpe"
    const val SET_NOTE_FIELD = "set_note_field"

    /** A logged set, tappable to edit it. */
    const val SET_ROW = "set_row"
    /** The role a performed set was (ROADMAP N14). */
    const val SET_ROLE = "set_role"

    fun setRole(role: String) = "set_role_$role"

    /** The control that arms the next log, or — with a role — one of its options (N19). */
    fun exercisePendingRole(id: String, role: String? = null) =
        if (role == null) "exercise_pending_role_$id" else "exercise_pending_role_${id}_$role"

    const val SET_SAVE = "set_save"
    const val SET_CANCEL = "set_cancel"
    const val SET_INCREASE_WEIGHT = "set_increase_weight"
    const val SET_DECREASE_WEIGHT = "set_decrease_weight"
    const val SET_INCREASE_REPS = "set_increase_reps"
    const val SET_DECREASE_REPS = "set_decrease_reps"

    /**
     * The correction dialog's own fields (ROADMAP N59).
     *
     * A tag addresses **one** control, and the dialog states a set that exists while the workout
     * screen's own fields state the next one. Both are composed while the dialog is open — it is a
     * window over the screen, not a replacement — so sharing the tags above made each match two
     * nodes. These are the edit path's half; the constants above are the logging path's.
     */
    const val SET_EDIT_WEIGHT_FIELD = "set_edit_weight_field"
    const val SET_EDIT_INCREASE_WEIGHT = "set_edit_increase_weight"
    const val SET_EDIT_DECREASE_WEIGHT = "set_edit_decrease_weight"
    const val SET_EDIT_REPS_FIELD = "set_edit_reps_field"
    const val SET_EDIT_INCREASE_REPS = "set_edit_increase_reps"
    const val SET_EDIT_DECREASE_REPS = "set_edit_decrease_reps"
    const val SET_EDIT_RPE_FIELD = "set_edit_rpe_field"
    const val SET_EDIT_INCREASE_RPE = "set_edit_increase_rpe"
    const val SET_EDIT_DECREASE_RPE = "set_edit_decrease_rpe"

    /**
     * Ending an exercise (ROADMAP N7). The two actions are mutually exclusive, so
     * a test can assert which state a row is in without matching on English.
     */
    const val EXERCISE_DONE = "exercise_done"
    const val EXERCISE_REOPEN = "exercise_reopen"
    const val EXERCISE_FINISHED_LABEL = "exercise_finished_label"

    /** The workout's Finish action, and the comment prompt behind it (ROADMAP N11). */
    const val ACTIVE_WORKOUT_FINISH = "active_workout_finish"

    /**
     * Leaving a workout that has something in it (ROADMAP N41): the overflow that holds the
     * destructive action, the action itself, and the prompt it asks before deleting anything.
     */
    const val ACTIVE_WORKOUT_MENU = "active_workout_menu"
    const val ACTIVE_WORKOUT_DISCARD = "active_workout_discard"
    const val ACTIVE_WORKOUT_DISCARD_TEXT = "active_workout_discard_text"
    const val ACTIVE_WORKOUT_DISCARD_PROGRAM = "active_workout_discard_program"
    const val ACTIVE_WORKOUT_DISCARD_CONFIRM = "active_workout_discard_confirm"
    const val ACTIVE_WORKOUT_DISCARD_CANCEL = "active_workout_discard_cancel"

    /** The empty workout's prompt-free discard, which N41 deliberately leaves alone. */
    const val ACTIVE_WORKOUT_DISCARD_EMPTY = "active_workout_discard_empty"

    /** Removing an exercise, and the confirmation it now asks for (ROADMAP B2). */
    const val EXERCISE_REMOVE = "exercise_remove"
    const val EXERCISE_REMOVE_CONFIRM = "exercise_remove_confirm"
    const val EXERCISE_REMOVE_CANCEL = "exercise_remove_cancel"

    /**
     * One exercise's own overflow menu (ROADMAP N53), which is where [EXERCISE_REMOVE] and
     * [supersetToggle] now live: the rare actions moved rather than changed, so a test reaches them
     * through the menu the way a thumb does.
     */
    fun exerciseMenu(id: String) = "exercise_menu_$id"

    /**
     * Moving one exercise in the session's own order (ROADMAP N54).
     *
     * One helper for both directions, the shape [templateMove] already uses, because up and down are
     * one idea and two names for a pair of entries is how they drift.
     */
    fun exerciseMove(id: String, up: Boolean) = "exercise_move_${if (up) "up" else "down"}_$id"

    /**
     * How an exercise felt (ROADMAP N8): the muscle-feel stepper, the dialog's buttons, and the
     * workout detail's row that reaches it. The joint half moved into [Rating], because it is a
     * picked list whose controls are addressed by the joint they act on (ROADMAP N63).
     */
    const val RATING_MUSCLE_FIELD = "rating_muscle_field"
    const val RATING_MUSCLE_DECREASE = "rating_muscle_decrease"
    const val RATING_MUSCLE_INCREASE = "rating_muscle_increase"
    const val RATING_SAVE = "rating_save"
    const val RATING_DISMISS = "rating_dismiss"
    const val EXERCISE_RATING_ROW = "exercise_rating_row"

    /** The rating a picked list retires when it is saved (ROADMAP N63), shown read-only. */
    const val RATING_LEGACY_JOINT = "rating_legacy_joint"

    /**
     * The steps a plan earned, offered when an exercise is done (ROADMAP N50, N74).
     *
     * One row per working set, so all but the question's own controls are addressed by the planned set
     * they act on: a set is named by what it would change rather than by its position, which is what
     * keeps a tag stable when the plan's order is edited. The two bulk controls are the exception,
     * because they have no one set to name.
     */
    object Progression {
        /** The question's own controls: writing every pick, declining all of them. */
        const val CONFIRM = "progression_confirm"
        const val NOT_NOW = "progression_not_now"

        /** The bulk picks, which act on every row that offers that direction (N74). */
        const val LOAD_ALL = "progression_load_all"
        const val REPS_ALL = "progression_reps_all"

        /** One set's row and the parts inside it. */
        fun set(setId: String) = "progression_set_$setId"
        fun plan(setId: String) = "progression_plan_$setId"
        fun done(setId: String) = "progression_done_$setId"
        fun load(setId: String) = "progression_load_$setId"
        fun reps(setId: String) = "progression_reps_$setId"
    }

    /**
     * The joints an exercise reported painful (ROADMAP N63), grouped so this object stays under its
     * ceiling: every control is addressed by the joint and side it acts on — the id
     * `jointSiteKey` builds from the enum names, so left and right are separate entries and no
     * control is reached by position.
     */
    object Rating {
        /** The control that opens the joint picker. */
        const val JOINT_ADD = "${RATING_JOINT_TAG_PREFIX}_add"

        /** One option of the picker, addressed by the `Joint` and `Side` names. */
        fun jointOption(site: String) = "${RATING_JOINT_TAG_PREFIX}_option_$site"

        /** One picked joint's row, and that row's own score controls. */
        fun jointRow(site: String) = "${RATING_JOINT_TAG_PREFIX}_row_$site"
        fun jointScore(site: String) = "${RATING_JOINT_TAG_PREFIX}_score_$site"
        fun jointDecrease(site: String) = "${RATING_JOINT_TAG_PREFIX}_decrease_$site"
        fun jointIncrease(site: String) = "${RATING_JOINT_TAG_PREFIX}_increase_$site"

        /** Takes the joint off the list. */
        fun jointRemove(site: String) = "${RATING_JOINT_TAG_PREFIX}_remove_$site"
    }

    const val HOME_TITLE = "home_title"
    const val HOME_START = "home_start"
    const val HOME_RESUME = "home_resume"
    /** The way into the template list, in the action row (ROADMAP N3, N42). */
    const val HOME_TEMPLATES = "home_templates"
    /** The way into programs, in the action row rather than the overflow (ROADMAP P3.3, N42). */
    const val HOME_PROGRAMS = "home_programs"

    /**
     * Export and import (ROADMAP B1, N43). Tagged generically because the point is *which screen*
     * offers them: a test asserts presence in Settings and absence in the library, using the same
     * two tags. They moved here from the home overflow (N43), which is where the app's data is
     * what the screen is about.
     */
    const val DATA_EXPORT = "data_export"
    const val DATA_IMPORT = "data_import"

    const val DETAIL_SAVE_AS_PLAN = "detail_save_as_plan"
    const val DETAIL_PLAN_NAME = "detail_plan_name"
    const val DETAIL_PLAN_CONFIRM = "detail_plan_confirm"
    const val DETAIL_OPEN_NEW_PLAN = "detail_open_new_plan"
    const val HOME_RECENT_ROW = "home_recent_row"
    const val HOME_FIRST_RUN = "home_first_run"
    const val HOME_NO_RECENT = "home_no_recent"

    const val LIBRARY_TITLE = "library_title"
    const val LIBRARY_SEARCH_FIELD = "library_search_field"

    /**
     * Two empty states, two tags — deliberately. The distinction is the feature
     * (P1.1a): an empty library wants different words from a search with no hits.
     */
    const val LIBRARY_EMPTY_LIBRARY = "library_empty_library"
    const val LIBRARY_NO_MATCH = "library_no_match"


    /** A read that failed, shown where the data would have been (ROADMAP B4). */
    const val LIBRARY_READ_ERROR = "library_read_error"
    const val EXERCISE_READ_ERROR = "exercise_read_error"

    /**
     * The picker's "new exercise" action (ROADMAP N2) and the dialog it opens.
     * The library destination never renders these, so their presence is itself
     * the behaviour under test.
     */
    const val LIBRARY_NEW_EXERCISE = "library_new_exercise"
    const val NEW_EXERCISE_NAME = "new_exercise_name"
    const val NEW_EXERCISE_SAVE = "new_exercise_save"
    const val NEW_EXERCISE_CANCEL = "new_exercise_cancel"

    /** The exercise detail screen's edit mode (ROADMAP N2, N5). */
    const val EXERCISE_EDIT = "exercise_edit"
    const val EXERCISE_EDIT_NAME = "exercise_edit_name"
    const val EXERCISE_EDIT_MUSCLE = "exercise_edit_muscle"
    const val EXERCISE_EDIT_EQUIPMENT = "exercise_edit_equipment"
    const val EXERCISE_EDIT_PATTERN = "exercise_edit_pattern"
    const val EXERCISE_EDIT_REST = "exercise_edit_rest"
    const val EXERCISE_EDIT_CUE = "exercise_edit_cue"
    const val EXERCISE_EDIT_SAVE = "exercise_edit_save"
    const val EXERCISE_EDIT_CANCEL = "exercise_edit_cancel"

    /**
     * The readiness note (ROADMAP N4): the prompt/edit dialog and the header row
     * that reaches it after the prompt has been answered or skipped.
     */
    const val READINESS_ROW = "readiness_row"
    const val READINESS_NOTE = "readiness_note"
    const val READINESS_SAVE = "readiness_save"
    const val READINESS_DISMISS = "readiness_dismiss"

    /**
     * The sore-muscle list beside that note (ROADMAP N62), grouped so this object stays under its
     * ceiling: every control is addressed by the muscle's own enum name rather than by position.
     */
    object Readiness {
        /** The control that opens the muscle picker. */
        const val SORE_ADD = "${READINESS_SORE_TAG_PREFIX}_add"

        /** One option of the picker, addressed by the `MuscleGroup` name. */
        fun soreOption(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_option_$muscle"

        /** One picked muscle's row, and that row's own score controls. */
        fun soreRow(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_row_$muscle"
        fun soreScore(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_score_$muscle"
        fun soreDecrease(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_decrease_$muscle"
        fun soreIncrease(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_increase_$muscle"

        /** Takes the muscle off the list. */
        fun soreRemove(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_remove_$muscle"

        /**
         * One muscle's **read-only** line where the editor is not open (ROADMAP N62).
         *
         * Separate from [soreRow] because a tag addresses one control: the workout header draws the
         * stored list under the note and the editor draws the same muscles while it is open, so the
         * shared name matched two nodes whenever the dialog was up over the screen.
         */
        fun soreLine(muscle: String) = "${READINESS_SORE_TAG_PREFIX}_line_$muscle"
    }

    /** The workout comment asked for on Finish (ROADMAP N11), and its row in history. */
    const val WORKOUT_NOTE = "workout_note"
    const val WORKOUT_NOTE_SAVE = "workout_note_save"
    const val WORKOUT_NOTE_SKIP = "workout_note_skip"

    /**
     * Workout templates (ROADMAP N3): the list, and one template's editor.
     *
     * Row-level tags are parameterised by id, so a test addresses the third
     * exercise by identity rather than by its position on screen.
     */
    const val TEMPLATES_TITLE = "templates_title"
    const val TEMPLATES_NEW = "templates_new"
    const val TEMPLATES_EMPTY = "templates_empty"
    const val TEMPLATE_EDIT_TITLE = "template_edit_title"
    const val TEMPLATE_NAME_FIELD = "template_name_field"
    const val TEMPLATE_NAME_SAVE = "template_name_save"
    const val TEMPLATE_ADD_EXERCISE = "template_add_exercise"
    const val TEMPLATE_NO_EXERCISES = "template_no_exercises"
    const val TEMPLATE_DELETE = "template_delete"
    const val TEMPLATE_DELETE_CONFIRM = "template_delete_confirm"

    /** A template in the list; tapping it edits, its button starts a workout. */
    fun templateRow(id: String) = "template_row_$id"

    fun templateStart(id: String) = "template_start_$id"

    fun templateExerciseRow(id: String) = "template_exercise_$id"

    /** A planned exercise's ⋮: the entries it opens are tagged by the helpers below (ROADMAP N71). */
    fun templateMenu(id: String) = "template_menu_$id"

    /** Reordering a template's exercises: one helper, because up and down are one idea. */
    fun templateMove(id: String, up: Boolean) = "template_move_${if (up) "up" else "down"}_$id"

    fun templateRemove(id: String) = "template_remove_$id"

    /** A plan's sets (ROADMAP N14): the list, one target, and its rest and cue. */
    const val TEMPLATE_PLAN_ROW = "template_plan_row"
    const val TEMPLATE_PLAN_EMPTY = "template_plan_empty"
    const val TEMPLATE_PLAN_ADD = "template_plan_add"
    const val TEMPLATE_ADD_WARMUPS = "template_add_warmups"
    const val TEMPLATE_PLAN_CLOSE = "template_plan_close"
    const val TEMPLATE_SET_ROLE = "template_set_role"
    const val TEMPLATE_SET_WEIGHT = "template_set_weight"
    const val TEMPLATE_SET_REPS_MIN = "template_set_reps_min"
    const val TEMPLATE_SET_REPS_MAX = "template_set_reps_max"
    const val TEMPLATE_SET_NOTE = "template_set_note"
    const val TEMPLATE_SET_SAVE = "template_set_save"
    const val TEMPLATE_SET_CANCEL = "template_set_cancel"
    const val TEMPLATE_REST_FIELD = "template_rest_field"
    const val TEMPLATE_CUE_FIELD = "template_cue_field"
    /** The exercise's one target RPE, beside the rest and cue it carries (N59, amended). */
    const val TEMPLATE_EXERCISE_RPE = "template_exercise_rpe"
    const val TEMPLATE_REST_CUE_SAVE = "template_rest_cue_save"

    fun templatePlanSet(id: String) = "template_plan_set_$id"

    fun templatePlanRemove(id: String) = "template_plan_remove_$id"

    fun templateSetRole(role: String) = "template_set_role_$role"

    /** The editor's scrolling exercise list, so a test can scroll to a row. */
    const val TEMPLATE_EXERCISE_LIST = "template_exercise_list"

    /** The record just set (ROADMAP N23). */
    const val PERSONAL_RECORD = "personal_record"

    /** The bottom bar's five roots (ROADMAP N34). */
    const val TAB_BAR = "tab_bar"
    const val TAB_WORKOUTS = "tab_workouts"
    const val TAB_HISTORY = "tab_history"
    const val TAB_STATISTICS = "tab_statistics"
    const val TAB_LIBRARY = "tab_library"
    const val TAB_SETTINGS = "tab_settings"


    /** The settings screen (ROADMAP N21): the screen, the current value, and each choice. */
    /** The exercise's own weight unit (ROADMAP N64): its row on the detail, and the edit choice. */
    const val EXERCISE_WEIGHT_UNIT = "exercise_weight_unit"
    const val EXERCISE_EDIT_WEIGHT_UNIT_DEFAULT = "exercise_edit_weight_unit_default"
    const val EXERCISE_EDIT_WEIGHT_UNIT_KG = "exercise_edit_weight_unit_kg"
    const val EXERCISE_EDIT_WEIGHT_UNIT_LB = "exercise_edit_weight_unit_lb"

    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_REST_CURRENT = "settings_rest_current"
    const val SETTINGS_REST_CUE = "settings_rest_cue"
    const val SETTINGS_KEEP_SCREEN_ON = "settings_keep_screen_on"
    /** The rest-timer switch (ROADMAP N44), and the workout screen's static prescription it turns on. */
    const val SETTINGS_REST_TIMER = "settings_rest_timer"
    const val EXERCISE_REST_PRESCRIPTION = "exercise_rest_prescription"
    /** The switch that decides whether *Done* asks about the next step a plan earned (ROADMAP N66). */
    const val SETTINGS_PROGRESSION = "settings_progression"
    /** The two unit chips on Settings (ROADMAP N64). */
    const val SETTINGS_WEIGHT_UNIT_KG = "setting_weight_unit_kg"
    const val SETTINGS_WEIGHT_UNIT_LB = "setting_weight_unit_lb"
    /** The row that opens the typed delete-everything confirmation (ROADMAP N43). */
    const val SETTINGS_CLEAR_DATA = "settings_clear_data"

    fun settingRest(seconds: Int) = "setting_rest_$seconds"

    /** Joining or leaving the superset above (ROADMAP N24). */
    fun supersetToggle(id: String) = "superset_toggle_$id"

    /** The review a finished workout gets (ROADMAP N20). */
    const val SUMMARY_DIALOG = "summary_dialog"
    const val SUMMARY_DONE = "summary_done"

    /**
     * The button that writes the next set (ROADMAP N59).
     *
     * It sits beside the fields stating that set, so what it commits is what is on screen.
     */
    const val SET_LOG = "set_log"

    /**
     * The workout screen's scrolling exercise list (ROADMAP N53).
     *
     * Tagged so a test can reach a row that is below the fold — an exercise's own overflow is only
     * composed once the row is — rather than asserting on whatever happens to fit.
     */
    const val EXERCISE_LIST = "exercise_list"

    /**
     * The notice that an exercise's planned sets are all written (ROADMAP N52).
     *
     * Beside [SET_LOG], because the two answer one question: the label says the next set is extra,
     * and this says why. The same line states the remainder while there is one (N70), and that state
     * answers to [EXERCISE_PLAN_LEFT] instead: one tag cannot mean "finished" and "not finished yet".
     */
    const val EXERCISE_PLAN_DONE = "exercise_plan_done"

    /** How many planned sets are still to write (ROADMAP N70). */
    const val EXERCISE_PLAN_LEFT = "exercise_plan_left"

    /** The role armed for the next one-tap log (ROADMAP N19). */
    /** Starting over (ROADMAP N18): the menu entry, the field and the confirm button. */
    const val HOME_CLEAR_DATA = "home_clear_data"
    const val CLEAR_CONFIRM_FIELD = "clear_confirm_field"
    const val CLEAR_CONFIRM_ACTION = "clear_confirm_action"
    const val CLEAR_EXPORT_FIRST = "clear_export_first"
    const val CLEAR_CANCEL = "clear_cancel"

    /** Substituting one occurrence (ROADMAP P3.11): the action, the dialog and its rows. */
    const val HOME_SUBSTITUTE_DIALOG = "home_substitute_dialog"
    const val HOME_SUBSTITUTE_CLEAR = "home_substitute_clear"

    fun homeSubstitute(planId: String) = "home_substitute_$planId"

    fun homeSubstituteTemplate(id: String) = "home_substitute_template_$id"

    /** One exercise's own trends (ROADMAP N17). */
    const val EXERCISE_TRENDS = "exercise_trends"

    fun historyExerciseTrends(id: String) = "history_exercise_trends_$id"

    /**
     * Repeating one finished workout from a History row (ROADMAP N48).
     *
     * Per row, because the action is addressed to the workout the row names: a tag that named the
     * screen would not tell a test *which* workout a tap would repeat.
     */
    fun historyRepeat(sessionId: String) = "history_repeat_$sessionId"

    /**
     * Home's own tags (ROADMAP N16, P3.9, N55), grouped so this object stays under its ceiling —
     * the same reason Programs, Measurements and Statistics are objects.
     */
    object Home {
        /** Today's plan on home (ROADMAP N16). */
        fun startPlan(id: String) = "home_start_plan_$id"

        /**
         * The run's next-up field (ROADMAP P3.9, N55).
         *
         * It tags the *field*, which opens what is planned, and [nextUpStart] tags the separate start
         * beside it: looking and starting stopped being the same gesture, so a test has to say which
         * one it means.
         */
        fun nextUp(id: String) = "home_next_up_$id"

        fun nextUpStart(id: String) = "home_next_up_start_$id"

        /** What is planned, opened from that field (ROADMAP N55). */
        const val PLANNED_WORKOUT = "home_planned_workout"
        const val PLANNED_WORKOUT_TITLE = "home_planned_workout_title"
        const val PLANNED_WORKOUT_EMPTY = "home_planned_workout_empty"
        const val PLANNED_WORKOUT_CLOSE = "home_planned_workout_close"
    }

    /**
     * Programs (ROADMAP P3.3), grouped so this object stays under its ceiling.
     *
     * The same shape as templates: a flat list with rows addressed by id, so a test names
     * the slot it means rather than counting positions.
     */
    object Programs {
        const val NEW = "programs_new"
        const val EMPTY = "programs_empty"
        const val NAME_FIELD = "program_name_field"
        const val NAME_SAVE = "program_name_save"
        const val ACTIVE = "program_active"
        const val ADD_SLOT = "program_add_slot"
        const val NO_SLOTS = "program_no_slots"
        const val DELETE = "program_delete"
        const val DELETE_CONFIRM = "program_delete_confirm"
        const val PICKER = "program_template_picker"
        const val PICKER_EMPTY = "program_template_picker_empty"

        /** Carrying a program as a file (ROADMAP N47): the load action and the export action. */
        const val LOAD = "program_load"
        const val EXPORT = "program_export"

        /** The point-of-start question (P3.3): the missed day, and the two answers. */
        const val SKIP_PROMPT = "program_skip_prompt"
        const val SKIP_DO_NOW = "program_skip_do_now"
        const val SKIP_CONTINUE = "program_skip_continue"

        /** A slot's ⋮: the order and remove entries it opens (ROADMAP N72). */
        fun slotMenu(id: String) = "program_slot_menu_$id"

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

    /** The measurements screen's own tags (ROADMAP N32), grouped so this object stays under its ceiling. */
    object Measurements {
        const val ADD = "measurements_add"
        const val WEIGHT = "measurements_weight"
        const val BODY_FAT = "measurements_body_fat"
        const val MUSCLE = "measurements_muscle"
        const val CONFIRM = "measurements_confirm"
    
        /** One per tape site, so a test names the site rather than a position. */
        fun tape(site: TapeSite) = "measurements_tape_${site.name.lowercase()}"
    
        fun delete(id: String) = "measurements_delete_$id"
    }

    /** The Statistics screen's own tags (ROADMAP N35), grouped so this object stays under its ceiling. */
    object Statistics {        /** The Statistics screen (ROADMAP N35). */
        const val OVERVIEW_WORKOUTS = "statistics_overview_workouts"
        const val OVERVIEW_VOLUME = "statistics_overview_volume"
        const val OVERVIEW_RECORDS = "statistics_overview_records"
        const val METRIC = "statistics_metric"
        const val CHART = "statistics_chart"
        const val CHART_FIRST_DATE = "statistics_chart_first_date"
        const val CHART_LAST_DATE = "statistics_chart_last_date"
        const val CHART_MIN = "statistics_chart_min"
        const val CHART_MAX = "statistics_chart_max"
        const val CHOOSE_LIFT = "statistics_choose_lift"
        const val LIFT = "statistics_lift"
        const val MEASUREMENTS = "statistics_measurements"

        /** The other screen this tab pushes: adherence over a month (ROADMAP P3.5). */
        const val ADHERENCE = "statistics_adherence"
        const val READINGS_TOGGLE = "statistics_readings_toggle"
        const val READINGS_AVERAGE = "statistics_readings_average"
        const val READINGS_TREND = "statistics_readings_trend"
        const val MOVING_AVERAGE = "statistics_moving_average"
        const val GOAL = "statistics_goal"
        const val GOAL_SET = "statistics_goal_set"
        const val GOAL_FIELD = "statistics_goal_field"
        const val GOAL_CONFIRM = "statistics_goal_confirm"
        const val GOAL_CLEAR = "statistics_goal_clear"

        /** One period the moving average can be taken over. */
        fun movingAveragePeriod(period: Int) = "statistics_moving_average_$period"

        /** One reading, addressed by its place in the list — newest first, so 0 is the latest. */
        fun reading(index: Int) = "statistics_reading_$index"

        /** One row of the lift picker, named by the exercise it selects. */
        fun lift(id: String) = "statistics_lift_$id"
        const val CUSTOM_FROM = "statistics_custom_from"
        const val CUSTOM_TO = "statistics_custom_to"
        const val CUSTOM_APPLY = "statistics_custom_apply"
    
        /** One range chip, and one picker row, named by what they select. */
        fun range(kind: String) = "statistics_range_$kind"
    
        fun metric(id: String) = "statistics_metric_$id"
    }

    /**
     * The Adherence screen's own tags (ROADMAP P3.5), grouped for the same reason.
     *
     * A day is addressed by its ISO date rather than its position in the grid, so a test names
     * the day it means and a leap year cannot shift which cell a tag lands on.
     */
    object Adherence {
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

        /** The weeks that can be marked as a deload (ROADMAP P3.10). */
        const val DELOAD_TITLE = "adherence_deload_title"

        fun dayCell(date: String) = "adherence_day_$date"

        fun deloadWeek(date: String, programId: String) = "adherence_deload_${date}_$programId"

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
}
