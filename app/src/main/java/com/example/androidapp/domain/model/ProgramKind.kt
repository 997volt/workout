package com.example.androidapp.domain.model

/**
 * Whether a program is a week that repeats or an order that ends (ROADMAP N174).
 *
 * **WEEKLY is every program written before this choice existed**, and it keeps the rule P3.3 set: a
 * slot's weekday recurs, so the schedule is the same every week. The migration makes that the column's
 * value for every row already on disk, so nothing a lifter has reads differently.
 *
 * **SEQUENCE is a block**: its slots are done once, in order, and the program ends. A slot's weekday is
 * a *placement* there rather than a recurrence — which is the distinction the two names carry, and the
 * only difference the kind makes to anything that reads a program.
 */
enum class ProgramKind {
    WEEKLY,
    SEQUENCE,
    ;

    companion object {
        /** The kind a stored name means, or null for one this build does not know. */
        fun fromName(name: String?): ProgramKind? = entries.firstOrNull { it.name == name }
    }
}
