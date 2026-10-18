package com.example.androidapp.domain.model

/**
 * The nearest loadable weight, never below one step: a zero would be an empty bar with a claim.
 *
 * Shared rather than copied (B53): the warm-up ramp rounds to it (N28), and the deload's draft is its
 * second caller (N147) — a deload of 61.7 kg is the same plan nobody can load.
 */
internal fun roundToStep(grams: Double, stepGrams: Long): Long {
    val steps = Math.round(grams / stepGrams)
    return (steps * stepGrams).coerceAtLeast(stepGrams)
}
