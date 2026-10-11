package com.example.androidapp.data

import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.Exercise
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup

/**
 * The exercise library shipped with the app (ROADMAP P1.1).
 *
 * Ids are permanent slugs: a logged set references one of them, so renaming an
 * entry is safe but changing its id would orphan history. Add new entries at
 * the end and never renumber.
 *
 * This list becomes a prepackaged Room database in F5 so first launch stays
 * instant and works offline; until then it is the in-memory source of truth.
 */
internal object SeedExercises {

    /**
     * Every movement the seed ships (ROADMAP P1.1, N103).
     *
     * **A movement states its muscle, its equipment and its secondary list, and no pattern.** The pattern
     * belongs to its family — stated once on the category — and the muscle and equipment are the movement's
     * own, because a family spans equipment and a category states neither. A **variation** states nothing at
     * all: see [variation]. A movement the seed leaves unfiled states no pattern either, so a loose row has
     * none until its lifter gives it one, which is what N96 asked for and N103 keeps.
     */
    val all: List<Exercise> = listOf(
        // ---- Chest ----
        movement("barbell-bench-press", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
        movement("incline-dumbbell-press", "Incline Dumbbell Press", MuscleGroup.CHEST, Equipment.DUMBBELL, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
        movement("cable-fly", "Cable Fly", MuscleGroup.CHEST, Equipment.CABLE, MuscleGroup.SHOULDERS),
        movement("push-up", "Push-Up", MuscleGroup.CHEST, Equipment.BODYWEIGHT, MuscleGroup.TRICEPS, MuscleGroup.CORE),

        // ---- Back ----
        movement("deadlift", "Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, MuscleGroup.GLUTES, MuscleGroup.FOREARMS, MuscleGroup.LOWER_BACK),
        movement("pull-up", "Pull-Up", MuscleGroup.LATS, Equipment.BODYWEIGHT, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
        movement("barbell-row", "Barbell Row", MuscleGroup.UPPER_BACK, Equipment.BARBELL, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
        movement("lat-pulldown", "Lat Pulldown", MuscleGroup.LATS, Equipment.CABLE, MuscleGroup.BICEPS),
        movement("seated-cable-row", "Seated Cable Row", MuscleGroup.UPPER_BACK, Equipment.CABLE, MuscleGroup.BICEPS),

        // ---- Shoulders ----
        movement("overhead-press", "Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, MuscleGroup.TRICEPS, MuscleGroup.CORE),
        movement("lateral-raise", "Lateral Raise", MuscleGroup.SHOULDERS, Equipment.DUMBBELL),
        movement("face-pull", "Face Pull", MuscleGroup.SHOULDERS, Equipment.CABLE, MuscleGroup.UPPER_BACK),

        // ---- Arms ----
        movement("barbell-curl", "Barbell Curl", MuscleGroup.BICEPS, Equipment.BARBELL, MuscleGroup.FOREARMS),
        movement("hammer-curl", "Hammer Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, MuscleGroup.FOREARMS),
        movement("triceps-pushdown", "Triceps Pushdown", MuscleGroup.TRICEPS, Equipment.CABLE),
        movement("overhead-triceps-extension", "Overhead Triceps Extension", MuscleGroup.TRICEPS, Equipment.DUMBBELL),
        movement("dip", "Dip", MuscleGroup.TRICEPS, Equipment.BODYWEIGHT, MuscleGroup.CHEST, MuscleGroup.SHOULDERS),

        // ---- Legs ----
        movement("back-squat", "Back Squat", MuscleGroup.QUADS, Equipment.BARBELL, MuscleGroup.GLUTES, MuscleGroup.CORE),
        movement("leg-press", "Leg Press", MuscleGroup.QUADS, Equipment.MACHINE, MuscleGroup.GLUTES),
        movement("bulgarian-split-squat", "Bulgarian Split Squat", MuscleGroup.QUADS, Equipment.DUMBBELL, MuscleGroup.GLUTES),
        movement("leg-extension", "Leg Extension", MuscleGroup.QUADS, Equipment.MACHINE),
        movement("romanian-deadlift", "Romanian Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
        movement("lying-leg-curl", "Lying Leg Curl", MuscleGroup.HAMSTRINGS, Equipment.MACHINE),
        movement("hip-thrust", "Hip Thrust", MuscleGroup.GLUTES, Equipment.BARBELL, MuscleGroup.HAMSTRINGS),
        movement("walking-lunge", "Walking Lunge", MuscleGroup.GLUTES, Equipment.DUMBBELL, MuscleGroup.QUADS),
        movement("standing-calf-raise", "Standing Calf Raise", MuscleGroup.CALVES, Equipment.MACHINE),
        movement("seated-calf-raise", "Seated Calf Raise", MuscleGroup.CALVES, Equipment.MACHINE),

        // ---- Core ----
        movement("plank", "Plank", MuscleGroup.CORE, Equipment.BODYWEIGHT),
        movement("hanging-leg-raise", "Hanging Leg Raise", MuscleGroup.CORE, Equipment.BODYWEIGHT, MuscleGroup.FOREARMS),
        movement("cable-crunch", "Cable Crunch", MuscleGroup.CORE, Equipment.CABLE),
        // ---- The paused, competition and speed variations, plus accessory work ----
        variation("competition-bench-press", "Competition Bench Press"),
        variation("bench-press-speed-day", "Bench Press — Speed Day"),
        variation("paused-bench-press-3s", "3-Second Paused Bench Press"),
        movement("conventional-deadlift", "Conventional Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, MuscleGroup.GLUTES, MuscleGroup.FOREARMS, MuscleGroup.LOWER_BACK),
        variation("paused-back-squat", "Paused Back Squat"),
        variation("push-press", "Push Press"),
        movement("machine-row", "Machine Row", MuscleGroup.UPPER_BACK, Equipment.MACHINE, MuscleGroup.BICEPS),
        movement("assisted-pull-up", "Assisted Pull-Up", MuscleGroup.LATS, Equipment.MACHINE, MuscleGroup.BICEPS),
        movement("dumbbell-fly", "Dumbbell Fly", MuscleGroup.CHEST, Equipment.DUMBBELL),
        movement("incline-dumbbell-arm-curl", "Incline Dumbbell Arm Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, MuscleGroup.FOREARMS),
        movement("dumbbell-skullcrusher", "Dumbbell Skullcrusher", MuscleGroup.TRICEPS, Equipment.DUMBBELL),
        // Rotator work: the two the user asked for by description rather than by name.
        movement("dumbbell-rotator-raise", "Dumbbell Rotator Raise", MuscleGroup.SHOULDERS, Equipment.DUMBBELL),
        movement("rotator-cable-to-side", "Rotator Cable to Side", MuscleGroup.SHOULDERS, Equipment.CABLE),
        movement("two-arm-cable-pushdown", "Two-Arm Cable Pushdown", MuscleGroup.TRICEPS, Equipment.CABLE),
        movement("cable-arm-curl", "Cable Arm Curl", MuscleGroup.BICEPS, Equipment.CABLE, MuscleGroup.FOREARMS),
    )

    /**
     * The families the seed ships (ROADMAP N95, N103).
     *
     * A category is a name and a **pattern**: it holds others rather than being performed, so its muscle and
     * equipment are unstated (`null`) and the app never offers it — but the pattern is the fact it owns,
     * stated here once so no movement under it has to. The slugs are permanent for [all]'s reason — a
     * movement filed under a category references its id.
     */
    val categories: List<SeedCategory> = listOf(
        SeedCategory("bench-press", "Bench Press", MovementPattern.PRESS),
        SeedCategory("incline-press", "Incline Press", MovementPattern.PRESS),
        SeedCategory("overhead-press-family", "Overhead Press", MovementPattern.PRESS),
        SeedCategory("squat", "Squat", MovementPattern.SQUAT),
        SeedCategory("deadlift-family", "Deadlift", MovementPattern.HINGE),
        SeedCategory("lunge", "Lunge", MovementPattern.LUNGE),
        SeedCategory("hip-thrust-family", "Hip Thrust", MovementPattern.HINGE),
        SeedCategory("row", "Row", MovementPattern.PULL),
        SeedCategory("lat-pulldown-family", "Lat Pulldown", MovementPattern.PULL),
        SeedCategory("fly", "Fly", MovementPattern.PRESS),
        SeedCategory("curl", "Curl", MovementPattern.ISOLATION),
        SeedCategory("triceps-extension", "Triceps Extension", MovementPattern.ISOLATION),
        SeedCategory("lateral-raise-family", "Lateral Raise", MovementPattern.ISOLATION),
        SeedCategory("calf-raise-family", "Calf Raise", MovementPattern.ISOLATION),
        SeedCategory("core", "Core", MovementPattern.CORE),
    )

    /**
     * Which family each seeded movement is filed under (ROADMAP N95), by the slugs above.
     *
     * The **one** place this mapping lives: the seeder reads it so the movements a database is populated
     * with arrive filed. A second copy in SQL would be a second thing to keep in step, and the two would
     * disagree the first time a movement was re-filed in the seed.
     *
     * The equipment variants are **separate movements under one head** rather than variations of each
     * other, because the number the category exists to produce is a lie about the training with the dumbbell
     * and machine work missing from it — and each keeps its own records, step and prefill. The paused and
     * competition bench presses are the reverse: the same lift performed differently, so they hang under the
     * barbell one.
     *
     * A movement absent from this map is unfiled, which is a real place to be.
     */
    val parentOf: Map<String, String> = mapOf(
        // Bench Press: three equipment variants, and the barbell one's own variations under it.
        "barbell-bench-press" to "bench-press",
        "competition-bench-press" to "barbell-bench-press",
        "bench-press-speed-day" to "barbell-bench-press",
        "paused-bench-press-3s" to "barbell-bench-press",
        "incline-dumbbell-press" to "incline-press",
        "overhead-press" to "overhead-press-family",
        "push-press" to "overhead-press",
        "back-squat" to "squat",
        "leg-press" to "squat",
        "paused-back-squat" to "back-squat",
        "conventional-deadlift" to "deadlift-family",
        "romanian-deadlift" to "deadlift-family",
        "walking-lunge" to "lunge",
        "bulgarian-split-squat" to "lunge",
        "hip-thrust" to "hip-thrust-family",
        "barbell-row" to "row",
        "seated-cable-row" to "row",
        "machine-row" to "row",
        "lat-pulldown" to "lat-pulldown-family",
        "pull-up" to "lat-pulldown-family",
        "assisted-pull-up" to "lat-pulldown-family",
        "cable-fly" to "fly",
        "dumbbell-fly" to "fly",
        "barbell-curl" to "curl",
        "hammer-curl" to "curl",
        "cable-arm-curl" to "curl",
        "incline-dumbbell-arm-curl" to "curl",
        "triceps-pushdown" to "triceps-extension",
        "two-arm-cable-pushdown" to "triceps-extension",
        "overhead-triceps-extension" to "triceps-extension",
        "dumbbell-skullcrusher" to "triceps-extension",
        "lateral-raise" to "lateral-raise-family",
        "dumbbell-rotator-raise" to "lateral-raise-family",
        "rotator-cable-to-side" to "lateral-raise-family",
        "standing-calf-raise" to "calf-raise-family",
        "seated-calf-raise" to "calf-raise-family",
        "plank" to "core",
        "hanging-leg-raise" to "core",
        "cable-crunch" to "core",
        // Deliberately unfiled, so the seed demonstrates both shapes: `face-pull`, `leg-extension`,
        // `lying-leg-curl`, `dip`, `push-up` and `deadlift`'s own name are movements a lifter files
        // themselves if they want a family around them.
    )

    /**
     * Terse constructor for a movement: the seed list is long and every row repeats the same shape.
     *
     * It takes **no pattern** (ROADMAP N103): a movement's pattern is its family's, stated on the category,
     * and a movement the seed leaves unfiled states none — a loose row has no pattern until a lifter gives
     * it one.
     */
    private fun movement(
        id: String,
        name: String,
        primary: MuscleGroup,
        equipment: Equipment,
        vararg secondary: MuscleGroup,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary.toList(),
        equipment = equipment,
    )

    /**
     * Terse constructor for a **variation** (ROADMAP N103): a seeded variation states nothing but its name.
     *
     * It is the same lift performed differently, so it inherits the muscle, the equipment, the secondary
     * muscles and the pattern from the exercise it hangs under, live. That is the whole of the model's
     * economy: only the name, the cue and the rest are ever the variation's own.
     */
    private fun variation(id: String, name: String) = Exercise(id = id, name = name)

}
/**
 * A family the seed ships (ROADMAP N95, extended by N96 and N103).
 *
 * A name, a permanent slug and the family's **movement pattern** — which is the category's fact: the one
 * value a head states, and the reason nothing under it has to. Its muscle and equipment stay unstated,
 * because a family spans equipment and its movements say their own.
 */
internal data class SeedCategory(
    val id: String,
    val name: String,
    val pattern: MovementPattern,
)

