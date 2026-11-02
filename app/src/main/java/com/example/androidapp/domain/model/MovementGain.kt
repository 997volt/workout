package com.example.androidapp.domain.model

/**
 * One movement the week moved (ROADMAP N251).
 *
 * "Moved" is one of two honest claims: the week trained it for the first time, or its heaviest working set
 * this week beat the heaviest before the week. A hold or a bodyweight row has no weight to compare, so a
 * first-time one still appears, and a return to one shows only the number it has rather than claiming a
 * "was 0 kg" (B332).
 */
data class MovementGain(
    val exerciseId: String,
    val name: String,
    /** The week's heaviest working set in grams, or 0 for a first-time movement trained at bodyweight. */
    val topGrams: Long,
    /**
     * The heaviest set with a weight before the week, or null when there is none to compare: a first training,
     * or one trained before only at bodyweight, assisted or held.
     */
    val previousGrams: Long?,
    /** True when no finished session trained this movement before the week. */
    val firstTime: Boolean,
)
