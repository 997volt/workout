package com.example.androidapp.domain.model

/**
 * How an exercise is classified. Kept as enums rather than free text so
 * filtering, balance warnings (ROADMAP P2.8) and auto-progression (P3.4) can
 * reason about them instead of parsing strings.
 *
 * The [label] values are user-facing. They live here for now to keep the seed
 * library compact; extracting them into `strings.xml` is part of the
 * localization pass (ROADMAP P5.4).
 */

/**
 * Primary muscle worked. Drives grouping and the volume-per-group charts (P2.3).
 *
 * **Order is the pickers' order** — storage is by name, so the list is free — and the back group sits
 * together where it belongs in anatomy order rather than being appended, because the order *is* the
 * map a picker draws.
 *
 * **The retired *Back* is gone from the enum** (N104, N105): it was one group until the split into
 * [LATS], [UPPER_BACK] and [LOWER_BACK], and it was kept only so a 1.x row — which stored every muscle
 * by name and read it back through `valueOf` — still resolved. 2.0 starts a store that never held one
 * and refuses a 1.x file before the decoder, so nothing can reach the name.
 *
 * [OTHER] is the "not specified yet" value a custom exercise is created with (ROADMAP N2), next to
 * [Equipment.OTHER] which already existed.
 */
enum class MuscleGroup(val label: String) {
    CHEST("Chest"),
    LATS("Lats"),
    UPPER_BACK("Upper back"),
    LOWER_BACK("Lower back"),
    SHOULDERS("Shoulders"),
    BICEPS("Biceps"),
    TRICEPS("Triceps"),
    FOREARMS("Forearms"),
    QUADS("Quads"),
    HAMSTRINGS("Hamstrings"),
    ADDUCTORS("Adductors"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CORE("Core"),
    OTHER("Other"),
}

/** Equipment required, used to filter a library down to what the user has access to. */
enum class Equipment(val label: String) {
    BARBELL("Barbell"),
    DUMBBELL("Dumbbell"),
    MACHINE("Machine"),
    CABLE("Cable"),
    BODYWEIGHT("Bodyweight"),
    KETTLEBELL("Kettlebell"),
    BAND("Band"),
    OTHER("Other"),
}

/**
 * What a row in the exercise library *is* (ROADMAP N95).
 *
 * The library holds two kinds of row, and the difference is what the rest of the app asks about rather
 * than how either is displayed:
 *
 *  - [MOVEMENT] is a lift somebody performs. Every picker offers it, and it is the only thing a logged
 *    set may reference.
 *  - [CATEGORY] is a head that holds others — *Bench Press* over a barbell, a dumbbell and a machine
 *    variant. It is **never offered and never logged**: it exists so the movements filed under it can be
 *    read together, which is what makes it a taxonomy rather than a second kind of exercise.
 *
 * **Stored by name, never an ordinal**, like every other enum here: an ordinal would silently re-label
 * every row the first time a value was inserted above another. [MOVEMENT] is what every row that existed
 * before categories is backfilled to, because it is what they already were.
 */
enum class RowKind {
    MOVEMENT,
    CATEGORY,
    ;

    /** True for the rows a picker may offer and a logged set may name (ROADMAP N95). */
    val isLoggable: Boolean get() = this == MOVEMENT
}

/**
 * Movement pattern, the basis for the push/pull balance check (P2.8, reduced by N96).
 *
 * **Nine values, where there were eleven.** `HORIZONTAL_PUSH` and `VERTICAL_PUSH` became [PRESS], and the two
 * pulls became [PULL]: the split bought nothing a lifter asks a question with — "how much pressing" is not two
 * questions because one of them was overhead — while it cost a decision per exercise that nothing checked, so
 * a dip was filed one way by one lifter and the other way by the next. **LUNGE stays out of SQUAT**, the one
 * judgement inside the set: both are knee-dominant and differ by stance rather than by joint action, but "have
 * I been squatting?" is a question a lifter asks and lunges are not the answer to it.
 *
 * **The four directional values are gone** (ROADMAP N104). N96 kept them so a 1.x row — or an export written
 * before the merge — still resolved instead of throwing, and 2.0 refuses a 1.x file before the decoder and
 * starts a store that never held one, so nothing can reach a name that no longer exists. The list *is* the
 * offered set again: no filter, no legacy values.
 */
enum class MovementPattern(val label: String) {
    PRESS("Press"),
    PULL("Pull"),
    SQUAT("Squat"),
    HINGE("Hinge"),
    LUNGE("Lunge"),
    CARRY("Carry"),
    ISOLATION("Isolation"),
    CORE("Core"),

    /**
     * A movement whose point is the jump rather than the load (ROADMAP N241).
     *
     * Additive like every constant here — the document format carries a pattern as a string, and this app
     * stores names — but an older build has no constant for it, which is the cost N141 accepted for
     * `activity`: a movement with this pattern reads as an unstated one rather than refusing the whole file.
     */
    JUMP("Jump"),
    OTHER("Other"),
}

/**
 * The `Quads · Barbell` line shown under an exercise's name, with any part that
 * is not stated left out rather than rendered as `Other · Other` (ROADMAP N2).
 *
 * Returns null when nothing is stated: a custom exercise is created with a name
 * only, and an empty line is more honest than one that says "Other · Other".
 * Under 2.0's model (N103) `null` is *unstated* — the row inherits it, or has none — so the parts are
 * [MuscleGroup.OTHER]/[Equipment.OTHER] **or absent**, and both answer nothing.
 * Shared by the library/picker row and the active workout's exercise header so
 * the two cannot drift apart.
 */
fun taxonomySubtitle(primaryMuscle: MuscleGroup?, equipment: Equipment?): String? {
    val parts = buildList {
        primaryMuscle?.takeIf { it != MuscleGroup.OTHER }?.let { add(it.label) }
        equipment?.takeIf { it != Equipment.OTHER }?.let { add(it.label) }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
