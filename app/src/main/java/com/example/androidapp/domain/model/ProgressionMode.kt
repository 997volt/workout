package com.example.androidapp.domain.model

/**
 * What *Done* does with the step a plan earned (P3.17; N66's switch, made a choice).
 *
 * **SUGGEST** opens the question with nothing picked — the app's behaviour since N50, and the default.
 * **APPLY_AT_DONE** opens it with every earned step already picked, so the lifter reads the whole answer and
 * writes it with one *Done*; nothing is written until that press (N33). **OFF** finishes the exercise without
 * asking, for someone who does not want the app editing the plan.
 *
 * The question is still the only place a plan is written, so APPLY_AT_DONE is a starting answer rather than a
 * silent write: the lifter can un-pick any row before confirming.
 */
enum class ProgressionMode {
    SUGGEST,
    APPLY_AT_DONE,
    OFF,
    ;

    companion object {
        /** The mode a stored name means, or null for one this build does not know. */
        fun fromName(name: String?): ProgressionMode? = entries.firstOrNull { it.name == name }
    }
}
