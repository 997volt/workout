package com.example.androidapp.ui.components

/**
 * Building a deload week (ROADMAP N147).
 */
internal object DeloadTags {
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
