package com.example.androidapp.domain.model

/**
 * One muscle a session reported as sore, and how sore (ROADMAP N62).
 *
 * The readiness note ([WorkoutSession.readinessNote]) stays the free-text line for what a list
 * cannot say — "slept badly", "travel day" — and this is the structured fact beside it: "quads 8,
 * calves 3" is two numbers the app can read one at a time later, which one number for the whole
 * body is not.
 *
 * [score] is on the same [TenPointScale] as a set's RPE and the per-exercise ratings, so the app
 * keeps one vocabulary for "how much".
 */
data class SoreMuscle(
    val muscle: MuscleGroup,
    /** 1–10, on [TenPointScale]. */
    val score: Int,
)

/**
 * The muscle groups a readiness note may name, in taxonomy order (ROADMAP N62).
 *
 * [MuscleGroup.OTHER] is the "not specified yet" value a custom exercise is created with, so it is
 * not a muscle anybody can be sore *in* — offering it would let "Other, 6" be a fact with no
 * subject. A stored row that nevertheless carries it is read rather than hidden: the row is data,
 * and refusing to show it would be losing what a lifter wrote.
 */
val SORE_MUSCLE_GROUPS: List<MuscleGroup> = MuscleGroup.entries.filter { it != MuscleGroup.OTHER }
