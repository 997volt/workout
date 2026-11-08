package com.example.androidapp.ui.components

import com.example.androidapp.domain.model.Activity

/**
 * Tags for the live workout: logging a set, its row and menu, finishing an exercise, rating it, and
 * the readiness note its header carries.
 */
internal interface WorkoutTags {
    val SET_WEIGHT_FIELD: String get() = "set_weight_field"

    val SET_REPS_FIELD: String get() = "set_reps_field"

    /** The hold this set was logged at, which replaces reps on a timed movement (ROADMAP N220). */
    val SET_DURATION_FIELD: String get() = "set_duration_field"

    val SET_INCREASE_DURATION: String get() = "set_increase_duration"

    val SET_DECREASE_DURATION: String get() = "set_decrease_duration"

    /** RPE and the set comment (ROADMAP N6). The RPE is a stepper; the logging path always states a
     *  number, and the edit path shows "Not recorded" for a set that gave none (N59). */
    val SET_RPE_FIELD: String get() = "set_rpe_field"

    val SET_DECREASE_RPE: String get() = "set_decrease_rpe"

    val SET_INCREASE_RPE: String get() = "set_increase_rpe"

    val SET_NOTE_FIELD: String get() = "set_note_field"

    /** A logged set, tappable to edit it. */
    val SET_ROW: String get() = "set_row"

    /** The role a performed set was (ROADMAP N14). */
    val SET_ROLE: String get() = "set_role"

    fun setRole(role: String) = "set_role_$role"

    /** The control that arms the next log, or — with a role — one of its options (N19). */
    fun exercisePendingRole(id: String, role: String? = null) =
        if (role == null) "exercise_pending_role_$id" else "exercise_pending_role_${id}_$role"

    val SET_SAVE: String get() = "set_save"

    val SET_CANCEL: String get() = "set_cancel"

    val SET_INCREASE_WEIGHT: String get() = "set_increase_weight"

    val SET_DECREASE_WEIGHT: String get() = "set_decrease_weight"

    val SET_INCREASE_REPS: String get() = "set_increase_reps"

    val SET_DECREASE_REPS: String get() = "set_decrease_reps"

    /**
     * The correction dialog's own fields (ROADMAP N59).
     *
     * A tag addresses **one** control, and the dialog states a set that exists while the workout
     * screen's own fields state the next one. Both are composed while the dialog is open — it is a
     * window over the screen, not a replacement — so sharing the tags above made each match two
     * nodes. These are the edit path's half; the constants above are the logging path's.
     */
    val SET_EDIT_WEIGHT_FIELD: String get() = "set_edit_weight_field"

    val SET_EDIT_INCREASE_WEIGHT: String get() = "set_edit_increase_weight"

    val SET_EDIT_DECREASE_WEIGHT: String get() = "set_edit_decrease_weight"

    val SET_EDIT_REPS_FIELD: String get() = "set_edit_reps_field"

    /** The correction dialog's hold field (ROADMAP N220). */
    val SET_EDIT_DURATION_FIELD: String get() = "set_edit_duration_field"

    val SET_EDIT_INCREASE_DURATION: String get() = "set_edit_increase_duration"

    val SET_EDIT_DECREASE_DURATION: String get() = "set_edit_decrease_duration"

    val SET_EDIT_INCREASE_REPS: String get() = "set_edit_increase_reps"

    val SET_EDIT_DECREASE_REPS: String get() = "set_edit_decrease_reps"

    val SET_EDIT_RPE_FIELD: String get() = "set_edit_rpe_field"

    val SET_EDIT_INCREASE_RPE: String get() = "set_edit_increase_rpe"

    val SET_EDIT_DECREASE_RPE: String get() = "set_edit_decrease_rpe"

    /**
     * Ending an exercise (ROADMAP N7). The two actions are mutually exclusive, so
     * a test can assert which state a row is in without matching on English.
     */
    val EXERCISE_DONE: String get() = "exercise_done"

    val EXERCISE_REOPEN: String get() = "exercise_reopen"

    val EXERCISE_FINISHED_LABEL: String get() = "exercise_finished_label"

    /** The workout's Finish action, and the comment prompt behind it (ROADMAP N11). */
    val ACTIVE_WORKOUT_FINISH: String get() = "active_workout_finish"

    val ACTIVE_WORKOUT_FINISH_REASON: String get() = "active_workout_finish_reason"

    /**
     * Leaving a workout, empty or not (ROADMAP N41, N87 amended): the overflow that holds the
     * destructive action, the action itself, and the prompt it asks before deleting anything.
     */
    val ACTIVE_WORKOUT_MENU: String get() = "active_workout_menu"

    val ACTIVE_WORKOUT_DISCARD: String get() = "active_workout_discard"

    val ACTIVE_WORKOUT_DISCARD_TEXT: String get() = "active_workout_discard_text"

    val ACTIVE_WORKOUT_DISCARD_PROGRAM: String get() = "active_workout_discard_program"

    val ACTIVE_WORKOUT_DISCARD_CONFIRM: String get() = "active_workout_discard_confirm"

    val ACTIVE_WORKOUT_DISCARD_CANCEL: String get() = "active_workout_discard_cancel"

    /** What an empty workout says, with the add button drawn under it (N82). */
    val ACTIVE_WORKOUT_EMPTY: String get() = "active_workout_empty"

    /** Adding a movement: the FAB over the list until N82, and the list's own last item since. */
    val ACTIVE_WORKOUT_ADD_EXERCISE: String get() = "active_workout_add_exercise"

    /**
     * The way into an activity, and the list of them (ROADMAP N141).
     *
     * The row is drawn only while the session is empty and is not already an activity, and one tag per
     * activity means a test names the one it picks rather than a position.
     */
    val ACTIVE_WORKOUT_ADD_ACTIVITY: String get() = "active_workout_add_activity"

    val ACTIVITY_PICKER: String get() = "activity_picker"

    val ACTIVE_WORKOUT_ACTIVITY: String get() = "active_workout_activity"

    val ACTIVE_WORKOUT_ACTIVITY_CLEAR: String get() = "active_workout_activity_clear"

    fun activity(activity: Activity) = "activity_${activity.name.lowercase()}"

    /** Removing an exercise, and the confirmation it now asks for (ROADMAP B2). */
    val EXERCISE_REMOVE: String get() = "exercise_remove"

    val EXERCISE_REMOVE_CONFIRM: String get() = "exercise_remove_confirm"

    val EXERCISE_REMOVE_CANCEL: String get() = "exercise_remove_cancel"

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
    val RATING_MUSCLE_FIELD: String get() = "rating_muscle_field"

    val RATING_MUSCLE_DECREASE: String get() = "rating_muscle_decrease"

    val RATING_MUSCLE_INCREASE: String get() = "rating_muscle_increase"

    val RATING_SAVE: String get() = "rating_save"

    val RATING_DISMISS: String get() = "rating_dismiss"

    val EXERCISE_RATING_ROW: String get() = "exercise_rating_row"

    /**
     * The readiness note (ROADMAP N4): the prompt/edit dialog and the header row
     * that reaches it after the prompt has been answered or skipped.
     */
    val READINESS_ROW: String get() = "readiness_row"

    val READINESS_NOTE: String get() = "readiness_note"

    val READINESS_SAVE: String get() = "readiness_save"

    val READINESS_DISMISS: String get() = "readiness_dismiss"

    /** The workout comment asked for on Finish (ROADMAP N11), and its row in history. */
    val WORKOUT_NOTE: String get() = "workout_note"

    val WORKOUT_NOTE_SAVE: String get() = "workout_note_save"

    val WORKOUT_NOTE_SKIP: String get() = "workout_note_skip"

    /** A running session's own way into the exercise's screen (ROADMAP N234). */
    fun exerciseOpenExercise(id: String) = "exercise_open_exercise_$id"

    /** The why-line under the next set's fields (P3.17). */
    fun exerciseSuggestionReason(id: String) = "exercise_suggestion_reason_$id"

    /** The record just set (ROADMAP N23). */
    val PERSONAL_RECORD: String get() = "personal_record"

    /** The prompt a bodyweight set raises when no weight is on record (ROADMAP N219). */
    val BODYWEIGHT_FIELD: String get() = "bodyweight_field"

    val BODYWEIGHT_SAVE: String get() = "bodyweight_save"

    val BODYWEIGHT_DISMISS: String get() = "bodyweight_dismiss"

    val EXERCISE_REST_PRESCRIPTION: String get() = "exercise_rest_prescription"

    /** Joining or leaving the superset above (ROADMAP N24). */
    fun supersetToggle(id: String) = "superset_toggle_$id"

    /** The review a finished workout gets (ROADMAP N20). */
    val SUMMARY_DIALOG: String get() = "summary_dialog"

    val SUMMARY_DONE: String get() = "summary_done"

    /**
     * The button that writes the next set (ROADMAP N59).
     *
     * It sits beside the fields stating that set, so what it commits is what is on screen.
     */
    val SET_LOG: String get() = "set_log"

    val SET_LOG_REASON: String get() = "set_log_reason"

    /**
     * The workout screen's scrolling exercise list (ROADMAP N53).
     *
     * Tagged so a test can reach a row that is below the fold — an exercise's own overflow is only
     * composed once the row is — rather than asserting on whatever happens to fit.
     */
    val EXERCISE_LIST: String get() = "exercise_list"

    /**
     * The notice that an exercise's planned sets are all written (ROADMAP N52).
     *
     * Beside [SET_LOG], because the two answer one question: the label says the next set is extra,
     * and this says why. The same line states the remainder while there is one (N70), and that state
     * answers to [EXERCISE_PLAN_LEFT] instead: one tag cannot mean "finished" and "not finished yet".
     */
    val EXERCISE_PLAN_DONE: String get() = "exercise_plan_done"

    /** How many planned sets are still to write (ROADMAP N70). */
    val EXERCISE_PLAN_LEFT: String get() = "exercise_plan_left"
}
