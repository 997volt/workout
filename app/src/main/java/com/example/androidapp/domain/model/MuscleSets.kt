package com.example.androidapp.domain.model

import com.example.androidapp.domain.effectivePrimaryMuscle

/**
 * One muscle's share of a plan, in planned working sets (ROADMAP N119).
 *
 * [muscle] is null for the bucket a plan's unresolved work lands in: a movement that states no primary muscle
 * and whose category states none either has no muscle to be counted against, and **dropping it would make the
 * rest of the plan look more balanced than it is**. Naming it is the honest answer.
 */
data class MuscleSets(val muscle: MuscleGroup?, val sets: Int)

/**
 * How a program's planned work spreads across muscles (ROADMAP N119).
 *
 * The shape of a week read *before* it is trained, which is why it reads the plan rather than the log: what a
 * lifter wants to know while authoring is whether the week trains what they think it does, and by the time the
 * history can answer it the week is over. (A rolling window over logged work is the parked P2.8, a different
 * question with a verdict in it.)
 *
 * Two rules are inherited rather than restated here:
 *
 * - **A warm-up is not work** (N112), so a ramp does not inflate the muscle it warms up. Counting every planned
 *   set would make a squat day look like it trained quads twice as hard as it does.
 * - **A movement's muscle is own-if-stated, else its category's** (N110), which is why the library is a
 *   parameter: the answer for a movement filed under a family that states one is the family's, and the resolver
 *   that decides it is the same one the library, the picker and the session rows read.
 *
 * A set counts **once**, for its primary muscle. Assisted work is not split off and secondary muscles are not
 * credited, because a set is not half a set for a muscle that assists — splitting one would invent a fraction
 * the plan never stated, and the count is meant to be a fact about the plan rather than a model of it.
 *
 * Ordered by count, largest first, ties in the taxonomy's own order, and the unresolved bucket last: the row a
 * lifter is looking for is the one at the top, and the one that says "this plan states no muscle" is a caveat
 * rather than a category.
 */
fun muscleBalance(
    planned: List<TemplateExercise>,
    library: List<Exercise>,
): List<MuscleSets> {
    val byId = library.associateBy { it.id }

    return planned
        .groupBy { row -> byId[row.exerciseId]?.effectivePrimaryMuscle(library) }
        .map { (muscle, rows) -> MuscleSets(muscle, rows.sumOf { it.sets.count { set -> set.role.isWork } }) }
        .filter { it.sets > 0 }
        .sortedWith(
            compareBy({ it.muscle == null }, { -it.sets }, { it.muscle?.ordinal ?: 0 }),
        )
}
