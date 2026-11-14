package com.example.androidapp.domain.model

/**
 * One performed work set's load, adopted into the plan's set at the same place (ROADMAP N309).
 *
 * Only the number to find moves: the plan's reps, range, hold and target effort are the plan's, and the
 * performed weight and assistance are what the session established.
 */
data class CalibrationTarget(
    val setId: String,
    val weightGrams: Long,
    val assistanceGrams: Long,
)
