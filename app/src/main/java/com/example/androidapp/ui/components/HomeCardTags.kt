package com.example.androidapp.ui.components

internal object HomeCardTags {
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
