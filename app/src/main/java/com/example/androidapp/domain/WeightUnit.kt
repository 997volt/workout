package com.example.androidapp.domain

/**
 * The unit a weight is shown and typed in (ROADMAP N64).
 *
 * **Presentation only.** Every weight is stored as whole grams, so switching this changes nothing on
 * disk and a stored value survives the switch exactly — which is what [Weight]'s whole design exists
 * for. Nothing here may be used to compare or convert storage; it is what the screen says and what a
 * field's text is parsed in.
 *
 * Stored **by name** wherever it is stored at all — the app default in preferences, and an
 * exercise's own override on its row — so a build that does not know a name reads it as absent
 * rather than as a different unit.
 */
enum class WeightUnit {
    KILOGRAMS,
    POUNDS,
    ;

    /** The other one, for a two-way control that does not care which it is showing. */
    fun toggled(): WeightUnit = if (this == KILOGRAMS) POUNDS else KILOGRAMS

    companion object {
        /**
         * The unit a stored name means, or null for one this build does not know.
         *
         * Null rather than a default, because the two callers want different things from "unknown":
         * a preference falls back to kilograms, and an exercise's override falls back to *the
         * preference* — which is not the same answer.
         */
        fun fromName(name: String?): WeightUnit? = entries.firstOrNull { it.name == name }
    }
}
