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
     * Every movement the seed ships (ROADMAP P1.1, N103, N110).
     *
     * **A movement states its equipment, and the taxonomy only where its family's is wrong.** The movement
     * pattern, the primary muscle and the secondary list belong to the **category** — stated once, on
     * [categories] — and a movement filed under one reads them live (N110). A movement that states nothing
     * but its equipment is the ordinary row here.
     *
     * A movement is the **only** kind of row in this list (N110): the variation level it used to hold is
     * gone, and the paused, competition and speed lifts are ordinary movements filed under the same category
     * as the lift they are a version of, which is exactly the family the category roll-up reads.
     *
     * A movement the seed leaves **unfiled** states its own three facts, because it has no category to read
     * them from — and one whose family would claim a muscle it does not work states its own for that field,
     * which is what own-if-stated is for. The lunge family is the standing case: it holds no single primary
     * muscle, so each of its two movements states its own.
     */
    val all: List<Exercise> = listOf(
        // ---- Chest ----
        movement("barbell-bench-press", "Barbell Bench Press", Equipment.BARBELL),
        movement("incline-dumbbell-press", "Incline Dumbbell Press", Equipment.DUMBBELL),
        movement("cable-fly", "Cable Fly", Equipment.CABLE),
        movement("dumbbell-fly", "Dumbbell Fly", Equipment.DUMBBELL),
        // The same lift performed differently, so they are filed under the lift's own category rather than
        // under it (N110) — the family reads as one number either way.
        movement("competition-bench-press", "Competition Bench Press", Equipment.BARBELL),
        movement("bench-press-speed-day", "Bench Press — Speed Day", Equipment.BARBELL),
        movement("paused-bench-press-3s", "3-Second Paused Bench Press", Equipment.BARBELL),
        movement(
            "push-up",
            "Push-Up",
            Equipment.BODYWEIGHT,
            MuscleGroup.CHEST,
            listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
        ),

        // ---- Back ----
        movement(
            "deadlift",
            "Deadlift",
            Equipment.BARBELL,
            MuscleGroup.HAMSTRINGS,
            listOf(MuscleGroup.GLUTES, MuscleGroup.FOREARMS, MuscleGroup.LOWER_BACK),
        ),
        movement("conventional-deadlift", "Conventional Deadlift", Equipment.BARBELL),
        movement("romanian-deadlift", "Romanian Deadlift", Equipment.BARBELL),
        movement("pull-up", "Pull-Up", Equipment.BODYWEIGHT, secondary = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)),
        movement(
            "assisted-pull-up",
            "Assisted Pull-Up",
            Equipment.MACHINE,
        ),
        movement("barbell-row", "Barbell Row", Equipment.BARBELL, secondary = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)),
        movement("seated-cable-row", "Seated Cable Row", Equipment.CABLE),
        movement("machine-row", "Machine Row", Equipment.MACHINE),
        movement("lat-pulldown", "Lat Pulldown", Equipment.CABLE),

        // ---- Shoulders ----
        movement("overhead-press", "Overhead Press", Equipment.BARBELL),
        movement("push-press", "Push Press", Equipment.BARBELL),
        movement("lateral-raise", "Lateral Raise", Equipment.DUMBBELL),
        movement("dumbbell-rotator-raise", "Dumbbell Rotator Raise", Equipment.DUMBBELL),
        movement("rotator-cable-to-side", "Rotator Cable to Side", Equipment.CABLE),
        movement("face-pull", "Face Pull", Equipment.CABLE, MuscleGroup.SHOULDERS, listOf(MuscleGroup.UPPER_BACK)),

        // ---- Arms ----
        movement("barbell-curl", "Barbell Curl", Equipment.BARBELL),
        movement("hammer-curl", "Hammer Curl", Equipment.DUMBBELL),
        movement("cable-arm-curl", "Cable Arm Curl", Equipment.CABLE),
        movement("incline-dumbbell-arm-curl", "Incline Dumbbell Arm Curl", Equipment.DUMBBELL),
        movement("triceps-pushdown", "Triceps Pushdown", Equipment.CABLE),
        movement("two-arm-cable-pushdown", "Two-Arm Cable Pushdown", Equipment.CABLE),
        movement("overhead-triceps-extension", "Overhead Triceps Extension", Equipment.DUMBBELL),
        movement("dumbbell-skullcrusher", "Dumbbell Skullcrusher", Equipment.DUMBBELL),
        movement("dip", "Dip", Equipment.BODYWEIGHT, MuscleGroup.TRICEPS, listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS)),

        // ---- Legs ----
        movement("back-squat", "Back Squat", Equipment.BARBELL),
        // The same lift performed differently: filed under the squat category, not under the bar (N110).
        movement("paused-back-squat", "Paused Back Squat", Equipment.BARBELL),
        movement("leg-press", "Leg Press", Equipment.MACHINE),
        movement("bulgarian-split-squat", "Bulgarian Split Squat", Equipment.DUMBBELL),
        // The family holds no single primary muscle, so each lunge states its own (N110).
        movement(
            "walking-lunge",
            "Walking Lunge",
            Equipment.DUMBBELL,
            MuscleGroup.GLUTES,
            listOf(MuscleGroup.QUADS),
        ),
        movement("leg-extension", "Leg Extension", Equipment.MACHINE, MuscleGroup.QUADS),
        movement("lying-leg-curl", "Lying Leg Curl", Equipment.MACHINE, MuscleGroup.HAMSTRINGS),
        movement("hip-thrust", "Hip Thrust", Equipment.BARBELL),
        movement("standing-calf-raise", "Standing Calf Raise", Equipment.MACHINE),
        movement("seated-calf-raise", "Seated Calf Raise", Equipment.MACHINE),

        // ---- Core ----
        movement("plank", "Plank", Equipment.BODYWEIGHT),
        movement("hanging-leg-raise", "Hanging Leg Raise", Equipment.BODYWEIGHT, secondary = listOf(MuscleGroup.FOREARMS)),
        movement("cable-crunch", "Cable Crunch", Equipment.CABLE),
    )

    /**
     * The families the seed ships (ROADMAP N95, N103, N110).
     *
     * A category is a name, a **movement pattern**, a **primary muscle** and a **secondary list** — the
     * facts it holds rather than performs, which is why its own equipment stays unstated: a family spans
     * equipment. It is never offered and never logged (N95).
     *
     * The slugs are permanent for [all]'s reason — a movement filed under a category references its id.
     */
    val categories: List<SeedCategory> = listOf(
        SeedCategory("bench-press", "Bench Press", MovementPattern.PRESS, MuscleGroup.CHEST, listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)),
        SeedCategory("incline-press", "Incline Press", MovementPattern.PRESS, MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)),
        SeedCategory("overhead-press-family", "Overhead Press", MovementPattern.PRESS, MuscleGroup.SHOULDERS, listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE)),
        SeedCategory("squat", "Squat", MovementPattern.SQUAT, MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES, MuscleGroup.CORE)),
        SeedCategory("deadlift-family", "Deadlift", MovementPattern.HINGE, MuscleGroup.HAMSTRINGS, listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK)),
        SeedCategory("lunge", "Lunge", MovementPattern.LUNGE, MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES)),
        SeedCategory("hip-thrust-family", "Hip Thrust", MovementPattern.HINGE, MuscleGroup.GLUTES, listOf(MuscleGroup.HAMSTRINGS)),
        SeedCategory("row", "Row", MovementPattern.PULL, MuscleGroup.UPPER_BACK, listOf(MuscleGroup.BICEPS)),
        SeedCategory("lat-pulldown-family", "Lat Pulldown", MovementPattern.PULL, MuscleGroup.LATS, listOf(MuscleGroup.BICEPS)),
        SeedCategory("fly", "Fly", MovementPattern.PRESS, MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS)),
        SeedCategory("curl", "Curl", MovementPattern.ISOLATION, MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS)),
        SeedCategory("triceps-extension", "Triceps Extension", MovementPattern.ISOLATION, MuscleGroup.TRICEPS, emptyList()),
        SeedCategory("lateral-raise-family", "Lateral Raise", MovementPattern.ISOLATION, MuscleGroup.SHOULDERS, emptyList()),
        SeedCategory("calf-raise-family", "Calf Raise", MovementPattern.ISOLATION, MuscleGroup.CALVES, emptyList()),
        SeedCategory("core", "Core", MovementPattern.CORE, MuscleGroup.CORE, emptyList()),
    )

    /**
     * Which family each seeded movement is filed under (ROADMAP N95, N110), by the slugs above.
     *
     * The **one** place this mapping lives: the seeder reads it so the movements a database is populated
     * with arrive filed, and the migration reads it so a database populated by an earlier build arrives
     * filed too. A second copy in SQL would be a second thing to keep in step.
     *
     * The equipment variants are **separate movements under one head** rather than one movement's versions,
     * because the number the category exists to produce is a lie about the training with the dumbbell and
     * machine work missing from it — and each keeps its own records, step and prefill. What used to be a
     * movement's own *variations* are now its brothers: the paused and competition bench presses are filed
     * under `bench-press` beside the barbell one, because the family is what reads them as one number.
     *
     * A movement absent from this map is unfiled, which is a real place to be.
     */
    val parentOf: Map<String, String> = mapOf(
        // Bench Press: three equipment variants, plus the competition, speed and paused lifts.
        "barbell-bench-press" to "bench-press",
        "competition-bench-press" to "bench-press",
        "bench-press-speed-day" to "bench-press",
        "paused-bench-press-3s" to "bench-press",
        "incline-dumbbell-press" to "incline-press",
        "overhead-press" to "overhead-press-family",
        "push-press" to "overhead-press-family",
        "back-squat" to "squat",
        "leg-press" to "squat",
        "paused-back-squat" to "squat",
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
     * **The equipment is the one fact every movement states** (N110): a family spans equipment — the lat
     * pulldown family alone holds a cable, a bodyweight and a machine movement — so a category cannot state
     * it for its members.
     *
     * [primary] is null for a movement that reads its **category's** primary muscle, and [secondary] is
     * empty for one that reads the category's list: the ordinary row here states its equipment and nothing
     * else. A movement with no category, or one whose family would claim a muscle it does not work, states
     * its own — which is the whole of own-if-stated (N103).
     */
    private fun movement(
        id: String,
        name: String,
        equipment: Equipment,
        primary: MuscleGroup? = null,
        secondary: List<MuscleGroup> = emptyList(),
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
    )
}

/**
 * A family the seed ships (ROADMAP N95, extended by N96, N103 and N110).
 *
 * A name, a permanent slug, and the three facts its members read from it: the **movement pattern**, the
 * **primary muscle** and the **secondary muscles**. Its own muscle and equipment stay unstated — a family
 * spans equipment, and the primary muscle is what it *holds* rather than what it is.
 */
internal data class SeedCategory(
    val id: String,
    val name: String,
    val pattern: MovementPattern,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
)
