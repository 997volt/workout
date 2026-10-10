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

    val all: List<Exercise> = listOf(
        // ---- Chest ----
        barbell("barbell-bench-press", "Barbell Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, MovementPattern.PRESS, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
        barbell("incline-dumbbell-press", "Incline Dumbbell Press", MuscleGroup.CHEST, Equipment.DUMBBELL, MovementPattern.PRESS, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
        barbell("cable-fly", "Cable Fly", MuscleGroup.CHEST, Equipment.CABLE, MovementPattern.ISOLATION, MuscleGroup.SHOULDERS),
        barbell("push-up", "Push-Up", MuscleGroup.CHEST, Equipment.BODYWEIGHT, MovementPattern.PRESS, MuscleGroup.TRICEPS, MuscleGroup.CORE),

        // ---- Back ----
        barbell("deadlift", "Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, MovementPattern.HINGE, MuscleGroup.GLUTES, MuscleGroup.FOREARMS, MuscleGroup.LOWER_BACK),
        barbell("pull-up", "Pull-Up", MuscleGroup.LATS, Equipment.BODYWEIGHT, MovementPattern.PULL, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
        barbell("barbell-row", "Barbell Row", MuscleGroup.UPPER_BACK, Equipment.BARBELL, MovementPattern.PULL, MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
        barbell("lat-pulldown", "Lat Pulldown", MuscleGroup.LATS, Equipment.CABLE, MovementPattern.PULL, MuscleGroup.BICEPS),
        barbell("seated-cable-row", "Seated Cable Row", MuscleGroup.UPPER_BACK, Equipment.CABLE, MovementPattern.PULL, MuscleGroup.BICEPS),

        // ---- Shoulders ----
        barbell("overhead-press", "Overhead Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, MovementPattern.PRESS, MuscleGroup.TRICEPS, MuscleGroup.CORE),
        barbell("lateral-raise", "Lateral Raise", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, MovementPattern.ISOLATION),
        barbell("face-pull", "Face Pull", MuscleGroup.SHOULDERS, Equipment.CABLE, MovementPattern.ISOLATION, MuscleGroup.UPPER_BACK),

        // ---- Arms ----
        barbell("barbell-curl", "Barbell Curl", MuscleGroup.BICEPS, Equipment.BARBELL, MovementPattern.ISOLATION, MuscleGroup.FOREARMS),
        barbell("hammer-curl", "Hammer Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, MovementPattern.ISOLATION, MuscleGroup.FOREARMS),
        barbell("triceps-pushdown", "Triceps Pushdown", MuscleGroup.TRICEPS, Equipment.CABLE, MovementPattern.ISOLATION),
        barbell("overhead-triceps-extension", "Overhead Triceps Extension", MuscleGroup.TRICEPS, Equipment.DUMBBELL, MovementPattern.ISOLATION),
        barbell("dip", "Dip", MuscleGroup.TRICEPS, Equipment.BODYWEIGHT, MovementPattern.PRESS, MuscleGroup.CHEST, MuscleGroup.SHOULDERS),

        // ---- Legs ----
        barbell("back-squat", "Back Squat", MuscleGroup.QUADS, Equipment.BARBELL, MovementPattern.SQUAT, MuscleGroup.GLUTES, MuscleGroup.CORE),
        barbell("leg-press", "Leg Press", MuscleGroup.QUADS, Equipment.MACHINE, MovementPattern.SQUAT, MuscleGroup.GLUTES),
        barbell("bulgarian-split-squat", "Bulgarian Split Squat", MuscleGroup.QUADS, Equipment.DUMBBELL, MovementPattern.LUNGE, MuscleGroup.GLUTES),
        barbell("leg-extension", "Leg Extension", MuscleGroup.QUADS, Equipment.MACHINE, MovementPattern.ISOLATION),
        barbell("romanian-deadlift", "Romanian Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, MovementPattern.HINGE, MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK),
        barbell("lying-leg-curl", "Lying Leg Curl", MuscleGroup.HAMSTRINGS, Equipment.MACHINE, MovementPattern.ISOLATION),
        barbell("hip-thrust", "Hip Thrust", MuscleGroup.GLUTES, Equipment.BARBELL, MovementPattern.HINGE, MuscleGroup.HAMSTRINGS),
        barbell("walking-lunge", "Walking Lunge", MuscleGroup.GLUTES, Equipment.DUMBBELL, MovementPattern.LUNGE, MuscleGroup.QUADS),
        barbell("standing-calf-raise", "Standing Calf Raise", MuscleGroup.CALVES, Equipment.MACHINE, MovementPattern.ISOLATION),
        barbell("seated-calf-raise", "Seated Calf Raise", MuscleGroup.CALVES, Equipment.MACHINE, MovementPattern.ISOLATION),

        // ---- Core ----
        barbell("plank", "Plank", MuscleGroup.CORE, Equipment.BODYWEIGHT, MovementPattern.CORE),
        barbell("hanging-leg-raise", "Hanging Leg Raise", MuscleGroup.CORE, Equipment.BODYWEIGHT, MovementPattern.CORE, MuscleGroup.FOREARMS),
        barbell("cable-crunch", "Cable Crunch", MuscleGroup.CORE, Equipment.CABLE, MovementPattern.CORE),
        // ---- Competition and paused variants, plus accessory work ----
        barbell("competition-bench-press", "Competition Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, MovementPattern.PRESS, MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS),
        barbell("bench-press-speed-day", "Bench Press — Speed Day", MuscleGroup.CHEST, Equipment.BARBELL, MovementPattern.PRESS, MuscleGroup.TRICEPS),
        barbell("paused-bench-press-3s", "3-Second Paused Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, MovementPattern.PRESS, MuscleGroup.TRICEPS),
        barbell("conventional-deadlift", "Conventional Deadlift", MuscleGroup.HAMSTRINGS, Equipment.BARBELL, MovementPattern.HINGE, MuscleGroup.GLUTES, MuscleGroup.FOREARMS, MuscleGroup.LOWER_BACK),
        barbell("paused-back-squat", "Paused Back Squat", MuscleGroup.QUADS, Equipment.BARBELL, MovementPattern.SQUAT, MuscleGroup.GLUTES, MuscleGroup.CORE),
        barbell("push-press", "Push Press", MuscleGroup.SHOULDERS, Equipment.BARBELL, MovementPattern.PRESS, MuscleGroup.TRICEPS, MuscleGroup.QUADS),
        barbell("machine-row", "Machine Row", MuscleGroup.UPPER_BACK, Equipment.MACHINE, MovementPattern.PULL, MuscleGroup.BICEPS),
        barbell("assisted-pull-up", "Assisted Pull-Up", MuscleGroup.LATS, Equipment.MACHINE, MovementPattern.PULL, MuscleGroup.BICEPS),
        barbell("dumbbell-fly", "Dumbbell Fly", MuscleGroup.CHEST, Equipment.DUMBBELL, MovementPattern.ISOLATION),
        barbell("incline-dumbbell-arm-curl", "Incline Dumbbell Arm Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, MovementPattern.ISOLATION, MuscleGroup.FOREARMS),
        barbell("dumbbell-skullcrusher", "Dumbbell Skullcrusher", MuscleGroup.TRICEPS, Equipment.DUMBBELL, MovementPattern.ISOLATION),
        // Rotator work: the two the user asked for by description rather than by name.
        barbell("dumbbell-rotator-raise", "Dumbbell Rotator Raise", MuscleGroup.SHOULDERS, Equipment.DUMBBELL, MovementPattern.ISOLATION),
        barbell("rotator-cable-to-side", "Rotator Cable to Side", MuscleGroup.SHOULDERS, Equipment.CABLE, MovementPattern.ISOLATION),
        barbell("two-arm-cable-pushdown", "Two-Arm Cable Pushdown", MuscleGroup.TRICEPS, Equipment.CABLE, MovementPattern.ISOLATION),
        barbell("cable-arm-curl", "Cable Arm Curl", MuscleGroup.BICEPS, Equipment.CABLE, MovementPattern.ISOLATION, MuscleGroup.FOREARMS),
    )

    /**
     * The families the seed ships (ROADMAP N95).
     *
     * A category is a name and nothing else: it holds others rather than being performed, so its taxonomy
     * columns are placeholders and the app never offers it. The slugs are permanent for
     * [all]'s reason — a movement filed under a category references its id.
     */
    val categories: List<SeedCategory> = listOf(
        SeedCategory("bench-press", "Bench Press"),
        SeedCategory("incline-press", "Incline Press"),
        SeedCategory("overhead-press-family", "Overhead Press"),
        SeedCategory("squat", "Squat"),
        SeedCategory("deadlift-family", "Deadlift"),
        SeedCategory("lunge", "Lunge"),
        SeedCategory("hip-thrust-family", "Hip Thrust"),
        SeedCategory("row", "Row"),
        SeedCategory("lat-pulldown-family", "Lat Pulldown"),
        SeedCategory("fly", "Fly"),
        SeedCategory("curl", "Curl"),
        SeedCategory("triceps-extension", "Triceps Extension"),
        SeedCategory("lateral-raise-family", "Lateral Raise"),
        SeedCategory("calf-raise-family", "Calf Raise"),
        SeedCategory("core", "Core"),
    )

    /**
     * Which family each seeded movement is filed under (ROADMAP N95), by the slugs above.
     *
     * The **one** place this mapping lives: the seeder reads it so the movements a database is populated
     * with arrive filed, and the migration that creates the families on an existing database reads the same
     * map. A second copy in SQL would be a second thing to keep in step, and the two would disagree the
     * first time a movement was re-filed in the seed.
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

    /** Terse constructor: the seed list is long and every row repeats the same shape. */
    private fun barbell(
        id: String,
        name: String,
        primary: MuscleGroup,
        equipment: Equipment,
        pattern: MovementPattern,
        vararg secondary: MuscleGroup,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary.toList(),
        equipment = equipment,
        movementPattern = pattern,
    )

}
/**
 * A family the seed ships (ROADMAP N95).
 *
 * A name and a permanent slug: a category has no muscles, no equipment and no pattern of its own, and the
 * app never offers it. It exists so the movements filed under it can be read together.
 */
internal data class SeedCategory(val id: String, val name: String)

