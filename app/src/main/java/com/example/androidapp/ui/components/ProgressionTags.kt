package com.example.androidapp.ui.components

/**
 * The steps a plan earned, offered when an exercise is done (ROADMAP N50, N74).
 */
internal object ProgressionTags {
    /** The question's own controls: writing every pick, declining all of them. */
    const val CONFIRM = "progression_confirm"
    const val NOT_NOW = "progression_not_now"

    /** The bulk picks, which act on every row that offers that direction (N74). */
    const val LOAD_ALL = "progression_load_all"
    const val REPS_ALL = "progression_reps_all"

    /** The bulk pick for a plan that prescribes holds (ROADMAP N242). */
    const val SECONDS_ALL = "progression_seconds_all"

    /** Takes each row's own step, whatever direction it earned (P3.17). */
    const val EVERY = "progression_every"

    /** The one reason every set missed for, stated above the rows rather than once per row (N226). */
    const val SHARED_MISS = "progression_shared_miss"

    /** One set's row and the parts inside it. */
    fun set(setId: String) = "progression_set_$setId"
    fun plan(setId: String) = "progression_plan_$setId"
    fun done(setId: String) = "progression_done_$setId"
    fun load(setId: String) = "progression_load_$setId"
    fun reps(setId: String) = "progression_reps_$setId"
    fun seconds(setId: String) = "progression_seconds_$setId"
}
