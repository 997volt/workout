package com.example.androidapp.domain.model

/**
 * The smallest step worth adding to a load, in grams: a pair of 1.25 kg plates.
 *
 * The step N50's progression offer raises a load by (see `ProgressionOffer.kt`), and the one the
 * warm-up ramp still rounds to: a warm-up of 61.7 kg is not a weight anybody can load.
 *
 * It is all that is left of N22's double progression, and that shape stays withdrawn: N22 added reps
 * to the plan's rep ceiling and then the load, computed from the plan alone. N50 offers the lifter the
 * same two numbers as a *choice*, earned by the session having met the plan's target RPE, and writes
 * only the one that is accepted.
 */
const val DEFAULT_PROGRESSION_STEP_GRAMS = 2_500L
