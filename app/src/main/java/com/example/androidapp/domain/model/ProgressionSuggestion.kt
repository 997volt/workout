package com.example.androidapp.domain.model

/**
 * The smallest step worth adding to a load, in grams: a pair of 1.25 kg plates.
 *
 * This is what is left of N22's double progression. **The suggestion itself is withdrawn** with
 * N59, which made the next set editable on the workout screen: the app's own proposal had no
 * surface left to be accepted on once the values it proposed were already fields the lifter
 * reads before logging, and a rule nothing calls is not kept for a future that may not want it.
 * ROADMAP N50 still owns reworking what the proposal should be.
 *
 * The **step** stays, because the warm-up ramp still rounds to it: a warm-up of 61.7 kg is not a
 * weight anybody can load.
 */
const val DEFAULT_PROGRESSION_STEP_GRAMS = 2_500L
