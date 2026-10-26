package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.Activity
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.WeightGoalKind

/**
 * Test tags for shared controls (ROADMAP P1.17).
 *
 * Tests target these instead of English literals, so a translated string cannot
 * break a test — and so a test failure means behaviour changed, not wording.
 */
// The registry is one name per control the app has, so its count grows with the app by design and this ceiling
// is the wrong shape of check for it. The principled remedy is the one this file already uses — a nested object
// per area, which `Programs`, `Home` and `Measurements` are — and the template tags are the next group to move
// there; that is a rename across ninety-odd call sites, so it is its own change rather than a rider on N121.
@Suppress("TooManyFunctions")
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
     * Leaving a workout, empty or not (ROADMAP N41, N87 amended): the overflow that holds the
     * destructive action, the action itself, and the prompt it asks before deleting anything.
     */
    const val ACTIVE_WORKOUT_MENU = "active_workout_menu"
    const val ACTIVE_WORKOUT_DISCARD = "active_workout_discard"

    const val ACTIVE_WORKOUT_DISCARD_TEXT = "active_workout_discard_text"
    const val ACTIVE_WORKOUT_DISCARD_PROGRAM = "active_workout_discard_program"
    const val ACTIVE_WORKOUT_DISCARD_CONFIRM = "active_workout_discard_confirm"
    const val ACTIVE_WORKOUT_DISCARD_CANCEL = "active_workout_discard_cancel"

    /** What an empty workout says, with the add button drawn under it (N82). */
    const val ACTIVE_WORKOUT_EMPTY = "active_workout_empty"

    /** Adding a movement: the FAB over the list until N82, and the list's own last item since. */
    const val ACTIVE_WORKOUT_ADD_EXERCISE = "active_workout_add_exercise"

    /**
     * The way into an activity, and the list of them (ROADMAP N141).
     *
     * The row is drawn only while the session is empty and is not already an activity, and one tag per
     * activity means a test names the one it picks rather than a position.
     */
    const val ACTIVE_WORKOUT_ADD_ACTIVITY = "active_workout_add_activity"
    const val ACTIVITY_PICKER = "activity_picker"
    const val ACTIVE_WORKOUT_ACTIVITY = "active_workout_activity"
    const val ACTIVE_WORKOUT_ACTIVITY_CLEAR = "active_workout_activity_clear"

    fun activity(activity: Activity) = "activity_${activity.name.lowercase()}"

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

        /** Takes each row's own step, whatever direction it earned (P3.17). */
        const val EVERY = "progression_every"

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

    /**
     * The scrolling body itself (ROADMAP N115).
     *
     * A lazy list composes only what is near the viewport, so a test that reaches today's plan — which N115
     * puts last — has to scroll the *list* to it; addressing the row directly cannot work, because the node
     * it would scroll to does not exist until the scroll has happened.
     */
    const val HOME_LIST = "home_list"

    /** The body's row that starts an empty workout (ROADMAP N102). */
    const val HOME_START = "home_start"

    /** The bar's pill, drawn only while a workout is running (ROADMAP N102). */
    const val HOME_RESUME = "home_resume"

    /** The ways in, as rows in the body rather than words in the action row (ROADMAP N3, N42, N102). */
    const val HOME_TEMPLATES = "home_templates"
    const val HOME_PROGRAMS = "home_programs"
    const val HOME_MEASUREMENTS = "home_measurements"

    /**
     * The row that writes today's measurements (ROADMAP N114), drawn only while today has none.
     *
     * Its own tag rather than the measurements row's: the two open the same screen, but one opens it with the
     * daily form already up, and a test has to say which control it means.
     */
    const val HOME_MEASURE_TODAY = "home_measure_today"

    /**
     * The line that says the day has nothing on it (ROADMAP N126).
     *
     * One tag for both states: what a test has to tell apart is *which line* it is, and the two sentences
     * are user-visible strings a translation changes.
     */
    const val HOME_NO_PLAN = "home_no_plan"

    /**
     * The question a planned start asks while a workout is already running (ROADMAP N89), and its two
     * answers. Constants rather than a per-id helper: there is one of this dialog, whatever it is starting.
     */
    const val HOME_ACTIVE_WORKOUT_DIALOG = "home_active_workout_dialog"
    const val HOME_ACTIVE_WORKOUT_CONTINUE = "home_active_workout_continue"
    const val HOME_ACTIVE_WORKOUT_DISCARD = "home_active_workout_discard"

    /**
     * Export and import (ROADMAP B1, N43). Tagged generically because the point is *which screen*
     * offers them: a test asserts presence in Settings and absence in the library, using the same
     * two tags. They moved here from the home overflow (N43), which is where the app's data is
     * what the screen is about.
     */
    const val DATA_EXPORT = "data_export"
    const val DATA_IMPORT = "data_import"

    const val DETAIL_SAVE_AS_PLAN = "detail_save_as_plan"

    /** The schedule offer on the saved plan (P3.18), and its dialog. */
    const val DETAIL_SCHEDULE = "detail_schedule"
    const val DETAIL_SCHEDULE_CONFIRM = "detail_schedule_confirm"

    /** One weekday chip in the schedule dialog, by `DayOfWeek` name. */
    fun detailScheduleDay(day: String) = "detail_schedule_day_$day"

    /** The past workout's title — its plan's name, or its date — and the date under it (N84). */
    const val HISTORY_DETAIL_TITLE = "history_detail_title"
    const val HISTORY_DETAIL_DATE = "history_detail_date"

    /** The one ⋮ on a past workout, and the two entries that write (N84). */
    /** N146's search and its chip, named so a test addresses them rather than their English label. */
    const val HISTORY_SEARCH_FIELD = "history_search_field"
    const val HISTORY_FILTER_ALL = "history_filter_all"
    const val HISTORY_FILTER_ACTIVITIES = "history_filter_activities"
    const val HISTORY_FILTER_WORKOUTS = "history_filter_workouts"
    const val HISTORY_NO_MATCH = "history_no_match"
    const val HISTORY_DETAIL_MENU = "history_detail_menu"
    const val HISTORY_DETAIL_EDIT = "history_detail_edit"
    const val HISTORY_DETAIL_DELETE = "history_detail_delete"
    const val DETAIL_PLAN_NAME = "detail_plan_name"
    const val DETAIL_PLAN_CONFIRM = "detail_plan_confirm"
    const val DETAIL_OPEN_NEW_PLAN = "detail_open_new_plan"
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

    /**
     * The primary-muscle dropdown and its options (ROADMAP N75, B71).
     *
     * Grouped, both because the object is at the function ceiling this file keeps to and because the
     * two belong together: the field, and one tag per muscle addressed by the enum's **name** — its
     * label is an enum literal today, so matching on it would make a test read English and would break
     * the day P5.4 translates them.
     */
    object Muscle {
        const val FIELD = "exercise_edit_muscle"

        fun option(muscle: String) = "exercise_edit_muscle_$muscle"
    }
    const val EXERCISE_EDIT_EQUIPMENT = "exercise_edit_equipment"
    const val EXERCISE_EDIT_PATTERN = "exercise_edit_pattern"

    /** The category a movement is filed under, as the detail screen reads it (ROADMAP N95). */
    const val EXERCISE_DETAIL_CATEGORY = "exercise_detail_category"

    /** Makes a category of the lifter's own (ROADMAP N95): the action, its name field and its save. */
    const val LIBRARY_NEW_CATEGORY = "library_new_category"
    const val NEW_CATEGORY_NAME = "new_category_name"
    const val NEW_CATEGORY_SAVE = "new_category_save"

    /** The category a movement is filed under (ROADMAP N95), and one selectable head. */
    const val EXERCISE_EDIT_CATEGORY = "exercise_edit_category"

    fun exerciseCategoryOption(id: String) = "exercise_edit_category_$id"
    const val EXERCISE_EDIT_REST = "exercise_edit_rest"
    const val EXERCISE_EDIT_CUE = "exercise_edit_cue"

    /** The exercise's own weight step (ROADMAP N77), in the form and as a read-only row. */
    const val EXERCISE_EDIT_STEP = "exercise_edit_step"
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
     * The readiness note's own controls (ROADMAP N4, N62).
     *
     * The sore-muscle *list* left this dialog for the daily measurement at N113, so what is left is the
     * add control the picker used to open — kept so its **absence** can be asserted, which is how "a
     * workout no longer asks for it" is held — and the read-only line history still draws for the
     * soreness an older session recorded.
     */
    object Readiness {
        /** The control that opened the muscle picker, kept so a test can assert it is gone (N113). */
        const val SORE_ADD = "${READINESS_SORE_TAG_PREFIX}_add"

        /**
         * One muscle's **read-only** line in history (ROADMAP N62).
         *
         * A live session no longer writes the list (N113), and the rows an older one recorded still read
         * here rather than nowhere.
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

    /**
     * The way across to Programs, in this screen's own bar (ROADMAP N93).
     *
     * The two are siblings with no other path between them, so the entry sits with the actions the bar
     * already carries rather than as a control floating over the list.
     */
    const val TEMPLATES_PROGRAMS = "templates_programs"
    const val TEMPLATE_EDIT_TITLE = "template_edit_title"
    const val TEMPLATE_NAME_FIELD = "template_name_field"
    const val TEMPLATE_NAME_SAVE = "template_name_save"
    const val TEMPLATE_ADD_EXERCISE = "template_add_exercise"
    const val TEMPLATE_NO_EXERCISES = "template_no_exercises"
    const val TEMPLATE_DELETE = "template_delete"
    const val TEMPLATE_DELETE_CONFIRM = "template_delete_confirm"

    /**
     * Keeping a version, and the versions already kept (ROADMAP N134).
     *
     * The two live in the editor's own overflow, because keeping a version is done at the moment the
     * lifter decides to change something.
     */
    const val TEMPLATE_VERSIONS_MENU = "template_versions_menu"
    const val TEMPLATE_KEEP_VERSION = "template_keep_version"
    const val TEMPLATE_VERSIONS = "template_versions"

    /** The versions kept of one template, one row per version (ROADMAP N134). */
    const val TEMPLATE_VERSIONS_DIALOG = "template_versions_dialog"
    fun templateVersion(id: String) = "template_version_$id"

    /** One kept version read as a plan, read-only (ROADMAP N134). */
    const val TEMPLATE_VERSION_DIALOG = "template_version_dialog"
    const val TEMPLATE_VERSION_TITLE = "template_version_title"
    const val TEMPLATE_VERSION_EMPTY = "template_version_empty"
    const val TEMPLATE_VERSION_CLOSE = "template_version_close"

    /** A template in the list; tapping it edits, its button starts a workout. */
    fun templateRow(id: String) = "template_row_$id"

    /**
     * The list's filter chips, and the two controls that bring a workout back (ROADMAP N214, N216, N217).
     *
     * The chips are modes — All, Hidden and Deleted — so a chip's selected state and the rows it stands
     * for are one question; a row's own button is the way back without opening the editor.
     */
    const val TEMPLATES_FILTER_ALL = "templates_filter_all"
    const val TEMPLATES_HIDDEN_FILTER = "templates_hidden_filter"
    const val TEMPLATES_DELETED_FILTER = "templates_deleted_filter"
    fun templateUnhide(id: String) = "template_unhide_$id"
    fun templateRestore(id: String) = "template_restore_$id"

    /** The editor's overflow item that hides or brings back the plan (ROADMAP N214). */
    const val TEMPLATE_HIDE = "template_hide"

    /**
     * A planned exercise's name, which opens and folds its block (ROADMAP N91).
     *
     * The exercise's own row tag stays the address of the row as a whole; this is the control inside it,
     * because the fold is the name's tap and not the ⋮ beside it.
     */
    fun templateFold(id: String) = "template_fold_$id"

    /**
     * A library family's head, which folds its children (ROADMAP N95).
     *
     * A tag as well as the `onClickLabel`: the label is what a screen reader announces, while a test has to
     * address the control by identity rather than by the English it happens to show.
     */
    fun libraryCategory(id: String) = "library_category_$id"

    /** A program's balance view (ROADMAP N119): the destination, its empty state, its total and its rows. */
    const val PROGRAM_BALANCE = "program_balance"
    const val PROGRAM_BALANCE_EMPTY = "program_balance_empty"
    const val PROGRAM_BALANCE_TOTAL = "program_balance_total"

    fun programBalanceRow(muscle: String) = "program_balance_row_$muscle"

    /** What holding a family shows (ROADMAP N117): the facts it states for its movements. */
    const val LIBRARY_CATEGORY_DETAILS = "library_category_details_dialog"

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

    /** The why-line under the next set's fields (P3.17). */
    fun exerciseSuggestionReason(id: String) = "exercise_suggestion_reason_$id"
    const val TEMPLATE_SET_ROLE = "template_set_role"
    const val TEMPLATE_SET_WEIGHT = "template_set_weight"

    /** The value a drop run takes off its anchor (ROADMAP N79), on the rung that holds it. */
    const val TEMPLATE_SET_DROP_VALUE = "template_set_drop_value"
    const val TEMPLATE_SET_REPS_MIN = "template_set_reps_min"
    const val TEMPLATE_SET_REPS_MAX = "template_set_reps_max"
    const val TEMPLATE_SET_NOTE = "template_set_note"
    const val TEMPLATE_SET_SAVE = "template_set_save"
    const val TEMPLATE_SET_CANCEL = "template_set_cancel"

    /**
     * The ± pairs the planned-set dialog shares with the workout's fields (ROADMAP N111).
     *
     * Its own tags rather than the logging path's, for the reason the correction dialog has a set:
     * a tag addresses **one** control, and every one of these fields is composed while a workout
     * screen holding the same four may be behind the dialog.
     */
    const val TEMPLATE_SET_INCREASE_WEIGHT = "template_set_increase_weight"
    const val TEMPLATE_SET_DECREASE_WEIGHT = "template_set_decrease_weight"
    const val TEMPLATE_SET_INCREASE_DROP_VALUE = "template_set_increase_drop_value"
    const val TEMPLATE_SET_DECREASE_DROP_VALUE = "template_set_decrease_drop_value"
    const val TEMPLATE_SET_INCREASE_REPS_MIN = "template_set_increase_reps_min"
    const val TEMPLATE_SET_DECREASE_REPS_MIN = "template_set_decrease_reps_min"
    const val TEMPLATE_SET_INCREASE_REPS_MAX = "template_set_increase_reps_max"
    const val TEMPLATE_SET_DECREASE_REPS_MAX = "template_set_decrease_reps_max"
    const val TEMPLATE_REST_FIELD = "template_rest_field"
    const val TEMPLATE_CUE_FIELD = "template_cue_field"

    /** The exercise's one target RPE, on the line the rest and the accept share (N59, N80, N116). */
    const val TEMPLATE_EXERCISE_RPE = "template_exercise_rpe"
    const val TEMPLATE_EXERCISE_INCREASE_RPE = "template_exercise_increase_rpe"
    const val TEMPLATE_EXERCISE_DECREASE_RPE = "template_exercise_decrease_rpe"
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
    const val EXERCISE_WEIGHT_STEP = "exercise_weight_step"
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

    /** One chip of the progression mode, by enum name (P3.17). */
    fun settingsProgressionMode(mode: String) = "settings_progression_$mode"

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

    /** A block's *Skip*, the only way past a session that cannot be trained (ROADMAP B195). */
    fun homeSkipBlock(planId: String) = "home_skip_block_$planId"

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
     * Home's own tags (ROADMAP N16, N55, N125), grouped so this object stays under its ceiling —
     * the same reason Programs, Measurements and Statistics are objects.
     */
    object Home {
        /**
         * Today's card's *Start* (ROADMAP N16).
         *
         * The card's own name is tagged by [planPreview], which is the separate gesture: looking and
         * starting stopped being the same thing (N55, N127), so a test says which one it means.
         */
        fun startPlan(id: String) = "home_start_plan_$id"

        /**
         * The field that opens what a card has planned, without starting it (ROADMAP N55, N127).
         *
         * Per card, because a day can carry slots from more than one program (P3.12) and two cards can
         * name the same workout.
         */
        fun planPreview(id: String) = "home_plan_preview_$id"

        /** What is planned, opened from a card (ROADMAP N55). */
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

        /**
         * The active program at the foot of the list (ROADMAP N92).
         *
         * The card that makes the thing to open the most reachable thing on the screen, rather than the
         * row under the app bar.
         */
        const val ACTIVE_CARD = "program_active_card"
        const val ADD_SLOT = "program_add_slot"
        const val NO_SLOTS = "program_no_slots"
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

    /**
     * Building a deload week (ROADMAP N147), grouped so this object stays under its ceiling.
     *
     * The days and exercises are addressed by the source slot and the planned exercise they came from,
     * so a test names the row it means rather than a position in a list the day count changes.
     */
    object Deload {
        const val SCREEN = "deload_screen"

        /** The scrolling body, so a test can reach a day below the fold (N147). */
        const val LIST = "deload_list"
        const val DAYS_DECREASE = "deload_days_decrease"
        const val DAYS_INCREASE = "deload_days_increase"
        const val DAYS_VALUE = "deload_days_value"
        const val CREATE = "deload_create"
        const val DONE = "deload_done"
        const val CREATED = "deload_created"

        /** The success card's way into the workouts it wrote (ROADMAP N148). */
        const val VIEW_WORKOUTS = "deload_view_workouts"

        /** Prefill from the most recent built plan (ROADMAP N150). */
        const val USE_LAST = "deload_use_last"

        fun load(percent: Int) = "deload_load_$percent"

        fun weekday(slotId: String) = "deload_weekday_$slotId"

        fun weekdayOption(slotId: String, day: String) = "deload_weekday_${slotId}_$day"

        fun day(slotId: String) = "deload_day_$slotId"

        /** One row of the program-wide pool, listed once (ROADMAP N156). */
        fun poolExercise(templateExerciseId: String) = "deload_pool_exercise_$templateExerciseId"

        /** A pool row's chip for one deload day, by 0-based day index (ROADMAP N156). */
        fun poolDay(templateExerciseId: String, dayIndex: Int) =
            "deload_pool_day_${templateExerciseId}_$dayIndex"
    }

    /** The measurements screen's own tags (ROADMAP N32), grouped so this object stays under its ceiling. */
    object Measurements {
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

    /** The Statistics screen's own tags (ROADMAP N35), grouped so this object stays under its ceiling. */
    object Statistics {
        const val OVERVIEW_WORKOUTS = "statistics_overview_workouts"
        const val OVERVIEW_VOLUME = "statistics_overview_volume"
        const val OVERVIEW_RECORDS = "statistics_overview_records"
        const val METRIC = "statistics_metric"

        /** The metric menu's filter field and its empty answer (N198). */
        const val METRIC_SEARCH = "statistics_metric_search"
        const val METRIC_NO_MATCHES = "statistics_metric_no_matches"
        const val CHART = "statistics_chart"
        const val CHART_FIRST_DATE = "statistics_chart_first_date"
        const val CHART_LAST_DATE = "statistics_chart_last_date"

        /** The tick between the axis's ends (N209). */
        const val CHART_MIDDLE_DATE = "statistics_chart_middle_date"
        const val CHART_MIN = "statistics_chart_min"

        /** The figure's scale: the gutter holding its top, middle and foot (N196). */
        const val CHART_AXIS = "statistics_chart_axis"

        /** The date and value a finger on the figure is over (N200). */
        const val SCRUB_READOUT = "statistics_scrub_readout"

        /** The full-height view of the figure, its own chart and the way back (N201). */
        const val CHART_EXPAND = "statistics_chart_expand"
        const val CHART_EXPANDED = "statistics_chart_expanded"
        const val CHART_EXPANDED_FIGURE = "statistics_chart_expanded_figure"

        /** The expanded view's scrolling body, so a short window is proved to scroll rather than clip (B219). */
        const val CHART_EXPANDED_BODY = "statistics_chart_expanded_body"

        /** The expanded view's name, announced with the dialog (N206). */
        const val CHART_EXPANDED_TITLE = "statistics_chart_expanded_title"
        const val CHART_COLLAPSE = "statistics_chart_collapse"
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

        /** The energy adjustment a weight trend implies (ROADMAP N98). */
        const val ENERGY = "statistics_energy"
        const val ENERGY_STATEMENT = "statistics_energy_statement"

        /**
         * The derived rate itself (ROADMAP N135).
         *
         * A value rather than the button it used to be: the rate is derived from the goal, so there is
         * nothing here to press — the goal row above is where the number that sets it is chosen.
         */
        const val ENERGY_TARGET = "statistics_energy_target_value"

        /** The band the goal's trend is judged against (ROADMAP N149). */
        const val ENERGY_BAND = "statistics_energy_band"

        /** The date a target weight's rate implies, or its reached line (ROADMAP N151). */
        const val ENERGY_TARGET_DATE = "statistics_energy_target_date"

        /**
         * The weight goal's own row, its kind choices and its field (ROADMAP N135).
         *
         * One tag per kind rather than a position, so a test names the goal it sets — the shape
         * [tape] and the other per-value tags here already use, named by the enum rather than by an
         * ordinal that a reordering would silently change.
         */
        const val WEIGHT_GOAL = "statistics_weight_goal"
        const val WEIGHT_GOAL_SET = "statistics_weight_goal_set"
        const val WEIGHT_GOAL_FIELD = "statistics_weight_goal_field"
        const val WEIGHT_GOAL_CONFIRM = "statistics_weight_goal_confirm"

        fun weightGoalKind(kind: WeightGoalKind) = "statistics_weight_goal_kind_${kind.name.lowercase()}"

        /**
         * The period editor, and the kinds it can state (ROADMAP N215).
         *
         * *No goal* is a kind of period rather than a clear button: a stretch with nothing asked for is what a
         * cleared goal always wrote, and stating it lets one be entered in the past like any other.
         */
        const val GOAL_KIND_NONE = "statistics_goal_kind_none"
        const val GOAL_PERIOD_DIALOG = "statistics_goal_period_dialog"
        const val GOAL_PERIOD_START = "statistics_goal_period_start"
        const val GOAL_PERIOD_END = "statistics_goal_period_end"
        const val GOAL_PERIOD_REMOVE = "statistics_goal_period_remove"
        const val GOAL_PERIOD_ADD = "statistics_goal_period_add"

        /**
         * The goal's history: the way in, the list, and one row per period (ROADMAP N137).
         *
         * Rows are addressed by their **place in the list**, newest first, because a period has no id of its
         * own that a test could name — the thing a row says is its verdict, and the verdict is what a test
         * reads back from the row's own label.
         */
        const val GOAL_HISTORY = "statistics_goal_history"
        const val GOAL_HISTORY_DIALOG = "statistics_goal_history_dialog"
        const val GOAL_HISTORY_CLOSE = "statistics_goal_history_close"

        fun goalOutcome(index: Int) = "statistics_goal_outcome_$index"

        /**
         * The line under the chart naming the plans whose targets are drawn on it (ROADMAP N39).
         *
         * The canvas cannot be asserted and a screen reader cannot read it, so this is the chart's own account
         * of which lines are which — the same reason the axis labels exist.
         */
        const val PLAN_TARGETS = "statistics_plan_targets"

        /**
         * One group's heading in the picker (ROADMAP N130), by the group's own name.
         *
         * The name rather than the enum, so this file keeps its one dependency direction: tags are read by
         * every feature, and a shared file reaching into one of them is the dependency this avoids.
         *
         * The picker had no headings until two series could share a label, so this is what makes the two
         * *Left knee* entries — a workout series and a per-lift one — tellable apart.
         */
        fun metricGroup(groupName: String) = "statistics_group_${groupName.lowercase()}"

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

        /** The control the rare ranges sit behind (N197). */
        const val RANGE_MORE = "statistics_range_more"

        /** The group chips the series menu opens on (N199). */
        fun metricGroupChoice(group: String) = "statistics_metric_group_$group"

        /** The one row the joint sites sit behind, and the row that goes back (N199). */
        const val METRIC_JOINT = "statistics_metric_joint"
        const val METRIC_JOINT_BACK = "statistics_metric_joint_back"

        /** A joint's heading over its sides (N205). */
        fun metricJointHeading(joint: String) = "statistics_metric_joint_$joint"

        fun metric(id: String) = "statistics_metric_$id"

        /** A series in the recently used row, named by its key (N212). */
        fun recentSeries(id: String) = "statistics_recent_$id"
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
}
