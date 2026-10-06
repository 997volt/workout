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
 * **Order is the pickers' order** — storage is by name, so the list is free — and N75 uses that
 * freedom: the back group sits where *Back* sat rather than being appended at the end, because a
 * picker that reads *Adductors* after *Other* is a worse map than one whose order moved once.
 *
 * [BACK] is **legacy and never offered** (ROADMAP N75): it was one group until the split into [LATS],
 * [UPPER_BACK] and [LOWER_BACK]. It stays in the enum because every muscle is stored and read back by
 * name, so a row that still says `BACK` — or an export written before the split — has to keep
 * resolving rather than throw on read. Choices come from [SELECTABLE_MUSCLE_GROUPS], and a row that
 * carries it is read and shown as *Back*.
 *
 * [OTHER] is the "not specified yet" value a custom exercise is created with (ROADMAP N2), next to
 * [Equipment.OTHER] which already existed; it sits last with the legacy value.
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
    BACK("Back"),
    OTHER("Other"),
}

/**
 * The muscle groups a lifter can choose (ROADMAP N75).
 *
 * The taxonomy minus the legacy [MuscleGroup.BACK]. Everything that *offers* a muscle reads this
 * rather than [MuscleGroup.entries] — the exercise's own dropdown, the secondary-muscle editor and the
 * readiness note's sore list — so nothing new is ever tagged with the value the split retired, while a
 * stored row that carries it still reads.
 */
val SELECTABLE_MUSCLE_GROUPS: List<MuscleGroup> =
    MuscleGroup.entries.filter { it != MuscleGroup.BACK }

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

/** Movement pattern, the basis for the push/pull balance check (P2.8). */
enum class MovementPattern(val label: String) {
    HORIZONTAL_PUSH("Horizontal push"),
    VERTICAL_PUSH("Vertical push"),
    HORIZONTAL_PULL("Horizontal pull"),
    VERTICAL_PULL("Vertical pull"),
    SQUAT("Squat"),
    HINGE("Hinge"),
    LUNGE("Lunge"),
    CARRY("Carry"),
    ISOLATION("Isolation"),
    CORE("Core"),
    OTHER("Other"),
}

/**
 * The `Quads · Barbell` line shown under an exercise's name, with any part that
 * has not been filled in yet left out rather than rendered as `Other · Other`
 * (ROADMAP N2).
 *
 * Returns null when nothing is known: a custom exercise is created with a name
 * only, and an empty line is more honest than one that says "Other · Other".
 * Shared by the library/picker row and the active workout's exercise header so
 * the two cannot drift apart.
 */
fun taxonomySubtitle(primaryMuscle: MuscleGroup, equipment: Equipment): String? {
    val parts = buildList {
        if (primaryMuscle != MuscleGroup.OTHER) add(primaryMuscle.label)
        if (equipment != Equipment.OTHER) add(equipment.label)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
