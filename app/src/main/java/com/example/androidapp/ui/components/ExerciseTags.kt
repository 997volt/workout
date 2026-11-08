package com.example.androidapp.ui.components

/**
 * Tags for the exercise library and one movement's own screen: the list, its filters and creation
 * dialogs, and the editor and detail facts.
 */
internal interface ExerciseTags {
    val LIBRARY_TITLE: String get() = "library_title"

    val LIBRARY_SEARCH_FIELD: String get() = "library_search_field"

    /** The library's order control and its choices, addressed by enum name (ROADMAP N230). */
    val LIBRARY_SORT: String get() = "library_sort"

    fun librarySortOption(sort: String) = "library_sort_$sort"

    /** One of the library's groups, addressed by enum name (ROADMAP N231). */
    fun libraryFilter(filter: String) = "library_filter_$filter"

    /**
     * Two empty states, two tags — deliberately. The distinction is the feature
     * (P1.1a): an empty library wants different words from a search with no hits.
     */
    val LIBRARY_EMPTY_LIBRARY: String get() = "library_empty_library"

    val LIBRARY_NO_MATCH: String get() = "library_no_match"

    /** A read that failed, shown where the data would have been (ROADMAP B4). */
    val LIBRARY_READ_ERROR: String get() = "library_read_error"

    val EXERCISE_READ_ERROR: String get() = "exercise_read_error"

    /**
     * The picker's "new exercise" action (ROADMAP N2) and the dialog it opens.
     * The library destination never renders these, so their presence is itself
     * the behaviour under test.
     */
    val LIBRARY_NEW_EXERCISE: String get() = "library_new_exercise"

    val NEW_EXERCISE_NAME: String get() = "new_exercise_name"

    val NEW_EXERCISE_SAVE: String get() = "new_exercise_save"

    /** The family a new movement is filed under while it is made (ROADMAP N232). */
    val NEW_EXERCISE_FAMILY_NONE: String get() = "new_exercise_family_none"

    fun newExerciseFamily(familyId: String) = "new_exercise_family_$familyId"

    val NEW_EXERCISE_CANCEL: String get() = "new_exercise_cancel"

    /** The exercise detail screen's edit mode (ROADMAP N2, N5). */
    val EXERCISE_EDIT: String get() = "exercise_edit"

    val EXERCISE_EDIT_NAME: String get() = "exercise_edit_name"

    val EXERCISE_EDIT_EQUIPMENT: String get() = "exercise_edit_equipment"

    val EXERCISE_EDIT_PATTERN: String get() = "exercise_edit_pattern"

    /** The category a movement is filed under, as the detail screen reads it (ROADMAP N95). */
    val EXERCISE_DETAIL_CATEGORY: String get() = "exercise_detail_category"

    /** Makes a category of the lifter's own (ROADMAP N95): the action, its name field and its save. */
    val LIBRARY_NEW_CATEGORY: String get() = "library_new_category"

    /** The action that makes a movement or a family (ROADMAP N232), and the movement dialog it opens. */
    val LIBRARY_CREATE: String get() = "library_create"

    /** The empty search that offers to make what was typed (ROADMAP N232). */
    val LIBRARY_CREATE_FROM_SEARCH: String get() = "library_create_from_search"

    val NEW_MOVEMENT_NAME: String get() = "new_movement_name"

    val NEW_MOVEMENT_SAVE: String get() = "new_movement_save"

    val NEW_CATEGORY_NAME: String get() = "new_category_name"

    val NEW_CATEGORY_SAVE: String get() = "new_category_save"

    /** The category a movement is filed under (ROADMAP N95), and one selectable head. */
    val EXERCISE_EDIT_CATEGORY: String get() = "exercise_edit_category"

    fun exerciseCategoryOption(id: String) = "exercise_edit_category_$id"

    val EXERCISE_EDIT_REST: String get() = "exercise_edit_rest"

    val EXERCISE_EDIT_CUE: String get() = "exercise_edit_cue"

    /** The exercise's own weight step (ROADMAP N77), in the form and as a read-only row. */
    val EXERCISE_EDIT_STEP: String get() = "exercise_edit_step"

    val EXERCISE_EDIT_SAVE: String get() = "exercise_edit_save"

    val EXERCISE_EDIT_CANCEL: String get() = "exercise_edit_cancel"

    /**
     * A library family's head, which folds its children (ROADMAP N95).
     *
     * A tag as well as the `onClickLabel`: the label is what a screen reader announces, while a test has to
     * address the control by identity rather than by the English it happens to show.
     */
    fun libraryCategory(id: String) = "library_category_$id"

    /** What holding a family shows (ROADMAP N117): the facts it states for its movements. */
    val LIBRARY_CATEGORY_DETAILS: String get() = "library_category_details_dialog"

    /** The settings screen (ROADMAP N21): the screen, the current value, and each choice. */
    /** The exercise's own weight unit (ROADMAP N64): its row on the detail, and the edit choice. */
    val EXERCISE_WEIGHT_UNIT: String get() = "exercise_weight_unit"

    val EXERCISE_WEIGHT_STEP: String get() = "exercise_weight_step"

    val EXERCISE_EDIT_WEIGHT_UNIT_DEFAULT: String get() = "exercise_edit_weight_unit_default"

    /** The bodyweight switch in the exercise editor (ROADMAP N219). */
    val EXERCISE_EDIT_BODYWEIGHT: String get() = "exercise_edit_bodyweight"

    /** The timed switch in the exercise editor (ROADMAP N220). */
    val EXERCISE_EDIT_TIMED: String get() = "exercise_edit_timed"

    val EXERCISE_EDIT_WEIGHT_UNIT_KG: String get() = "exercise_edit_weight_unit_kg"

    val EXERCISE_EDIT_WEIGHT_UNIT_LB: String get() = "exercise_edit_weight_unit_lb"

    /** What the plan will do with the set, in the block's small print (ROADMAP N227). */
    val EXERCISE_PROGRESSION: String get() = "exercise_progression"

    /** Merging one movement into another (ROADMAP N245): the detail's action and the picker. */
    val EXERCISE_MORE: String get() = "exercise_more"

    val EXERCISE_MERGE: String get() = "exercise_merge"

    val MERGE_DIALOG: String get() = "merge_dialog"

    val MERGE_SEARCH: String get() = "merge_search"

    val MERGE_CONFIRM: String get() = "merge_confirm"

    val MERGE_CANCEL: String get() = "merge_cancel"

    val MERGE_CONFIRMED: String get() = "merge_confirmed"

    val MERGE_CONFIRMED_OK: String get() = "merge_confirmed_ok"

    fun mergeCandidate(id: String) = "merge_candidate_$id"

    /** One exercise's own trends (ROADMAP N17). */
    val EXERCISE_TRENDS: String get() = "exercise_trends"

    /** The monitor's own answer to Done, or *App default* (ROADMAP N235). */
    val EXERCISE_EDIT_PROGRESSION_DEFAULT: String get() = "exercise_edit_progression_default"

    val EXERCISE_EDIT_PROGRESSION_SUGGEST: String get() = "exercise_edit_progression_suggest"

    val EXERCISE_EDIT_PROGRESSION_APPLY: String get() = "exercise_edit_progression_apply"

    val EXERCISE_EDIT_PROGRESSION_OFF: String get() = "exercise_edit_progression_off"

    /** The exercise's own history section (ROADMAP N233). */
    val EXERCISE_HISTORY: String get() = "exercise_history"

    val EXERCISE_HISTORY_LAST_TRAINED: String get() = "exercise_history_last_trained"

    val EXERCISE_HISTORY_BEST_HOLD: String get() = "exercise_history_best_hold"

    val EXERCISE_HISTORY_EMPTY: String get() = "exercise_history_empty"

    val EXERCISE_HISTORY_ERROR: String get() = "exercise_history_error"

    fun historyExerciseTrends(id: String) = "history_exercise_trends_$id"
}

/**
 * The primary-muscle dropdown and one option per muscle (ROADMAP N75, B71).
 */
