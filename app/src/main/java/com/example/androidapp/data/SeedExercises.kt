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
     * **The list is a strength and bodybuilding set**: the powerlifting bar lifts with the assistance work
     * that carries them, and the machine, cable, dumbbell and band work a bodybuilding program is built from.
     * The three equipment values that had no exercise of their own — `KETTLEBELL`, `BAND` and `OTHER` for the
     * ab wheel — and the three muscles that had no movement of their own — `FOREARMS`, `ADDUCTORS` and
     * `LOWER_BACK` as a primary — are used here, so every value the pickers offer can be found in the library.
     *
     * **Olympic lifts are deliberately absent.** A category states one primary muscle (N110), and a clean, a
     * snatch or a jerk has no honest single answer — filing them under a family would invent a fact about the
     * training rather than record one. They need a decision about whether a category's primary may be
     * unstated before they need rows.
     *
     * **A row retired from this list is not deleted from a device that already has it.** The seeder only ever
     * inserts what is missing (F15), and a logged set names its exercise by id, so retiring a name is a
     * change to what a *fresh* install ships — `two-arm-cable-pushdown` and `machine-row` are the standing
     * cases, both of them a second name for a movement that is still here.
     *
     * A movement the seed leaves **unfiled** states its own three facts, because it has no category to read
     * them from — and one whose family would claim a muscle it does not work states its own for that field,
     * which is what own-if-stated is for. Two rows are the standing cases: the walking and reverse lunges are
     * filed under *Lunge* while stating the glutes that family does not name as its primary, and a close-grip
     * bench press is filed under *Bench Press* while stating the triceps it is actually trained for.
     */
    val all: List<Exercise> = listOf(
        // ---- Chest ----
        movement("barbell-bench-press", "Barbell Bench Press", Equipment.BARBELL),
        movement("dumbbell-bench-press", "Dumbbell Bench Press", Equipment.DUMBBELL),
        movement("decline-bench-press", "Decline Bench Press", Equipment.BARBELL),
        movement("machine-chest-press", "Machine Chest Press", Equipment.MACHINE),
        movement("floor-press", "Floor Press", Equipment.BARBELL),
        // Filed under *Bench Press* and stating the triceps anyway: own-if-stated is what lets a movement be
        // trained for a muscle its family does not name (N110).
        movement(
            "close-grip-bench-press",
            "Close-Grip Bench Press",
            Equipment.BARBELL,
            MuscleGroup.TRICEPS,
            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
        ),
        // The same lift performed differently, so they are filed under the lift's own category rather than
        // under it (N110) — the family reads as one number either way.
        movement("competition-bench-press", "Competition Bench Press", Equipment.BARBELL),
        movement("bench-press-speed-day", "Bench Press — Speed Day", Equipment.BARBELL),
        movement("paused-bench-press-3s", "3-Second Paused Bench Press", Equipment.BARBELL),
        movement("incline-dumbbell-press", "Incline Dumbbell Press", Equipment.DUMBBELL),
        movement("incline-barbell-bench-press", "Incline Barbell Bench Press", Equipment.BARBELL),
        movement("incline-machine-press", "Incline Machine Press", Equipment.MACHINE),
        movement("cable-fly", "Cable Fly", Equipment.CABLE),
        movement("dumbbell-fly", "Dumbbell Fly", Equipment.DUMBBELL),
        movement("pec-deck", "Pec Deck", Equipment.MACHINE),
        movement(
            "push-up",
            "Push-Up",
            Equipment.BODYWEIGHT,
            MuscleGroup.CHEST,
            listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE),
            bodyweight = true
        ),

        // ---- Back ----
        // **No plain "Deadlift"** (N229): the family is named after the lift, so a movement of that name sat
        // beside it and the two were one row too many. The bar lift a lifter means is *Conventional
        // Deadlift*, and the row that said otherwise is merged into it on a device that already had one.
        movement("conventional-deadlift", "Conventional Deadlift", Equipment.BARBELL),
        movement("sumo-deadlift", "Sumo Deadlift", Equipment.BARBELL),
        movement("deficit-deadlift", "Deficit Deadlift", Equipment.BARBELL),
        movement("rack-pull", "Rack Pull", Equipment.BARBELL),
        movement("romanian-deadlift", "Romanian Deadlift", Equipment.BARBELL),
        movement("good-morning", "Good Morning", Equipment.BARBELL),
        movement("kettlebell-swing", "Kettlebell Swing", Equipment.KETTLEBELL),
        movement(
            "pull-up",
            "Pull-Up",
            Equipment.BODYWEIGHT,
            secondary = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS),
            bodyweight = true
        ),
        movement("chin-up", "Chin-Up", Equipment.BODYWEIGHT, bodyweight = true),
        // Loaded by the body with the machine taking weight off it (N15, N219), so it is bodyweight even
        // though its equipment is the stack that helps.
        movement(
            "assisted-pull-up",
            "Assisted Pull-Up",
            Equipment.MACHINE,
            bodyweight = true
        ),
        movement("lat-pulldown", "Lat Pulldown", Equipment.CABLE),
        movement("straight-arm-pulldown", "Straight-Arm Pulldown", Equipment.CABLE),
        movement(
            "barbell-row",
            "Barbell Row",
            Equipment.BARBELL,
            secondary = listOf(MuscleGroup.BICEPS, MuscleGroup.FOREARMS)
        ),
        movement("pendlay-row", "Pendlay Row", Equipment.BARBELL),
        movement("t-bar-row", "T-Bar Row", Equipment.BARBELL),
        movement("seated-cable-row", "Seated Cable Row", Equipment.CABLE),
        movement("chest-supported-row", "Chest-Supported Row", Equipment.MACHINE),
        movement("single-arm-dumbbell-row", "Single-Arm Dumbbell Row", Equipment.DUMBBELL),
        movement("inverted-row", "Inverted Row", Equipment.BODYWEIGHT, bodyweight = true),
        movement("barbell-shrug", "Barbell Shrug", Equipment.BARBELL),
        movement("dumbbell-shrug", "Dumbbell Shrug", Equipment.DUMBBELL),
        movement("back-extension", "Back Extension", Equipment.MACHINE),
        movement("reverse-hyperextension", "Reverse Hyperextension", Equipment.MACHINE),

        // ---- Shoulders ----
        movement("overhead-press", "Overhead Press", Equipment.BARBELL),
        movement("push-press", "Push Press", Equipment.BARBELL),
        movement("seated-dumbbell-press", "Seated Dumbbell Press", Equipment.DUMBBELL),
        movement("arnold-press", "Arnold Press", Equipment.DUMBBELL),
        movement("machine-shoulder-press", "Machine Shoulder Press", Equipment.MACHINE),
        movement("z-press", "Z Press", Equipment.BARBELL),
        movement("lateral-raise", "Lateral Raise", Equipment.DUMBBELL),
        movement("cable-lateral-raise", "Cable Lateral Raise", Equipment.CABLE),
        movement("machine-lateral-raise", "Machine Lateral Raise", Equipment.MACHINE),
        movement("dumbbell-rotator-raise", "Dumbbell Rotator Raise", Equipment.DUMBBELL),
        movement("rotator-cable-to-side", "Rotator Cable to Side", Equipment.CABLE),
        movement("rear-delt-fly", "Rear Delt Fly", Equipment.DUMBBELL),
        movement("reverse-pec-deck", "Reverse Pec Deck", Equipment.MACHINE),
        movement("band-pull-apart", "Band Pull-Apart", Equipment.BAND),
        movement("face-pull", "Face Pull", Equipment.CABLE, MuscleGroup.SHOULDERS, listOf(MuscleGroup.UPPER_BACK)),

        // ---- Arms ----
        movement("barbell-curl", "Barbell Curl", Equipment.BARBELL),
        movement("hammer-curl", "Hammer Curl", Equipment.DUMBBELL),
        movement("preacher-curl", "Preacher Curl", Equipment.BARBELL),
        movement("concentration-curl", "Concentration Curl", Equipment.DUMBBELL),
        movement("machine-curl", "Machine Curl", Equipment.MACHINE),
        movement("cable-arm-curl", "Cable Arm Curl", Equipment.CABLE),
        // The single-arm curl taken behind the back: it holds tension on the biceps at the bottom of the
        // range, which the plain cable curl does not, and the family had no movement for it.
        movement("behind-the-back-cable-curl", "Behind-the-Back Cable Curl", Equipment.CABLE),
        movement("incline-dumbbell-arm-curl", "Incline Dumbbell Arm Curl", Equipment.DUMBBELL),
        movement("triceps-pushdown", "Triceps Pushdown", Equipment.CABLE),
        movement("overhead-triceps-extension", "Overhead Triceps Extension", Equipment.DUMBBELL),
        movement("cable-overhead-triceps-extension", "Cable Overhead Triceps Extension", Equipment.CABLE),
        movement("dumbbell-skullcrusher", "Dumbbell Skullcrusher", Equipment.DUMBBELL),
        movement("bench-dip", "Bench Dip", Equipment.BODYWEIGHT, bodyweight = true),
        movement("diamond-push-up", "Diamond Push-Up", Equipment.BODYWEIGHT, bodyweight = true),
        movement(
            "dip",
            "Dip",
            Equipment.BODYWEIGHT,
            MuscleGroup.TRICEPS,
            listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
            bodyweight = true
        ),
        movement("wrist-curl", "Wrist Curl", Equipment.DUMBBELL),
        movement("reverse-wrist-curl", "Reverse Wrist Curl", Equipment.DUMBBELL),
        movement("reverse-curl", "Reverse Curl", Equipment.BARBELL),

        // ---- Legs ----
        movement("back-squat", "Back Squat", Equipment.BARBELL),
        // The same lift performed differently: filed under the squat category, not under the bar (N110).
        movement("paused-back-squat", "Paused Back Squat", Equipment.BARBELL),
        movement("front-squat", "Front Squat", Equipment.BARBELL),
        movement("box-squat", "Box Squat", Equipment.BARBELL),
        movement("hack-squat", "Hack Squat", Equipment.MACHINE),
        movement("goblet-squat", "Goblet Squat", Equipment.KETTLEBELL),
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
        movement(
            "reverse-lunge",
            "Reverse Lunge",
            Equipment.DUMBBELL,
            MuscleGroup.GLUTES,
            listOf(MuscleGroup.QUADS),
        ),
        movement("step-up", "Step-Up", Equipment.DUMBBELL),
        movement("leg-extension", "Leg Extension", Equipment.MACHINE, MuscleGroup.QUADS),
        movement("lying-leg-curl", "Lying Leg Curl", Equipment.MACHINE, MuscleGroup.HAMSTRINGS),
        movement("seated-leg-curl", "Seated Leg Curl", Equipment.MACHINE, MuscleGroup.HAMSTRINGS),
        movement("nordic-curl", "Nordic Curl", Equipment.BODYWEIGHT, MuscleGroup.HAMSTRINGS, bodyweight = true),
        movement("hip-thrust", "Hip Thrust", Equipment.BARBELL),
        movement("glute-bridge", "Glute Bridge", Equipment.BARBELL),
        movement("cable-kickback", "Cable Kickback", Equipment.CABLE),
        movement("hip-adduction", "Hip Adduction", Equipment.MACHINE),
        movement(
            "copenhagen-plank",
            "Copenhagen Plank",
            Equipment.BODYWEIGHT,
            secondary = listOf(MuscleGroup.CORE),
            bodyweight = true,
            timed = true
        ),
        movement("standing-calf-raise", "Standing Calf Raise", Equipment.MACHINE),
        movement("seated-calf-raise", "Seated Calf Raise", Equipment.MACHINE),
        movement("donkey-calf-raise", "Donkey Calf Raise", Equipment.MACHINE),

        // ---- Core and carries ----
        movement("plank", "Plank", Equipment.BODYWEIGHT, bodyweight = true, timed = true),
        movement("side-plank", "Side Plank", Equipment.BODYWEIGHT, bodyweight = true, timed = true),
        movement(
            "hanging-leg-raise",
            "Hanging Leg Raise",
            Equipment.BODYWEIGHT,
            secondary = listOf(MuscleGroup.FOREARMS),
            bodyweight = true
        ),
        movement("cable-crunch", "Cable Crunch", Equipment.CABLE),
        movement("ab-wheel-rollout", "Ab Wheel Rollout", Equipment.OTHER, bodyweight = true),
        movement("pallof-press", "Pallof Press", Equipment.CABLE),
        movement("turkish-get-up", "Turkish Get-Up", Equipment.KETTLEBELL, secondary = listOf(MuscleGroup.SHOULDERS)),
        movement("farmers-carry", "Farmer's Carry", Equipment.DUMBBELL, timed = true),
        movement("suitcase-carry", "Suitcase Carry", Equipment.DUMBBELL, timed = true),

        // ---- Plyometric ----
        // The jumps the seed had no movement for (N241): body-loaded, so they are prefilled as "added weight"
        // and a set at their rep ceiling earns nothing rather than a heavier bar it does not load (N74, B293).
        movement("box-jump", "Box Jump", Equipment.OTHER, bodyweight = true),
        movement("jump-squat", "Jump Squat", Equipment.BODYWEIGHT, bodyweight = true),
        movement("broad-jump", "Broad Jump", Equipment.BODYWEIGHT, bodyweight = true),
        movement("depth-jump", "Depth Jump", Equipment.OTHER, bodyweight = true),
        movement("pogo-hop", "Pogo Hop", Equipment.BODYWEIGHT, bodyweight = true),
        movement("skater-bound", "Skater Bound", Equipment.BODYWEIGHT, bodyweight = true),
        // The one throw, and the one that states its own taxonomy: a medicine-ball throw is upper-body power,
        // so the family's quads would be a fact invented about the movement (N103, own-if-stated).
        movement(
            "medicine-ball-throw",
            "Medicine-Ball Throw",
            Equipment.OTHER,
            MuscleGroup.SHOULDERS,
            listOf(MuscleGroup.CHEST, MuscleGroup.CORE),
            pattern = MovementPattern.JUMP
        ),
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
        SeedCategory(
            "bench-press",
            "Bench Press",
            MovementPattern.PRESS,
            MuscleGroup.CHEST,
            listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS)
        ),
        SeedCategory(
            "incline-press",
            "Incline Press",
            MovementPattern.PRESS,
            MuscleGroup.CHEST,
            listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS)
        ),
        SeedCategory(
            "overhead-press-family",
            "Overhead Press",
            MovementPattern.PRESS,
            MuscleGroup.SHOULDERS,
            listOf(MuscleGroup.TRICEPS, MuscleGroup.CORE)
        ),
        SeedCategory(
            "squat",
            "Squat",
            MovementPattern.SQUAT,
            MuscleGroup.QUADS,
            listOf(MuscleGroup.GLUTES, MuscleGroup.CORE)
        ),
        SeedCategory(
            "deadlift-family",
            "Deadlift",
            MovementPattern.HINGE,
            MuscleGroup.HAMSTRINGS,
            listOf(MuscleGroup.GLUTES, MuscleGroup.LOWER_BACK)
        ),
        SeedCategory("lunge", "Lunge", MovementPattern.LUNGE, MuscleGroup.QUADS, listOf(MuscleGroup.GLUTES)),
        SeedCategory(
            "hip-thrust-family",
            "Hip Thrust",
            MovementPattern.HINGE,
            MuscleGroup.GLUTES,
            listOf(MuscleGroup.HAMSTRINGS)
        ),
        // **The lats are half of what a row is for** (N240): the family named upper back and biceps only, so a
        // plan's balance figure counted no lat work for the rows that train them most.
        SeedCategory(
            "row",
            "Row",
            MovementPattern.PULL,
            MuscleGroup.UPPER_BACK,
            listOf(MuscleGroup.BICEPS, MuscleGroup.LATS)
        ),
        // The mirror of the row family's correction (N240): a pulldown retracts the shoulder blades, and the
        // upper back that does it was missing from the one family that trains the lats hardest.
        SeedCategory(
            "lat-pulldown-family",
            "Lat Pulldown",
            MovementPattern.PULL,
            MuscleGroup.LATS,
            listOf(MuscleGroup.BICEPS, MuscleGroup.UPPER_BACK)
        ),
        SeedCategory("fly", "Fly", MovementPattern.PRESS, MuscleGroup.CHEST, listOf(MuscleGroup.SHOULDERS)),
        SeedCategory("curl", "Curl", MovementPattern.ISOLATION, MuscleGroup.BICEPS, listOf(MuscleGroup.FOREARMS)),
        SeedCategory(
            "triceps-extension",
            "Triceps Extension",
            MovementPattern.ISOLATION,
            MuscleGroup.TRICEPS,
            emptyList()
        ),
        SeedCategory(
            "lateral-raise-family",
            "Lateral Raise",
            MovementPattern.ISOLATION,
            MuscleGroup.SHOULDERS,
            emptyList()
        ),
        // The two families the knee isolation had no home in: a leg extension is quad work that *Squat*
        // would name wrongly, and a leg curl is knee flexion rather than the hinge *Deadlift* holds (N229).
        SeedCategory(
            "leg-curl",
            "Leg Curl",
            MovementPattern.ISOLATION,
            MuscleGroup.HAMSTRINGS,
            listOf(MuscleGroup.GLUTES)
        ),
        // `-family` for the reason B148 named one: the movement *Leg Extension* is a row of its own, so a
        // family under that same id would be dropped by `INSERT OR IGNORE` (SeedExercisesTest asserts the two
        // lists never intersect).
        SeedCategory(
            "leg-extension-family",
            "Leg Extension",
            MovementPattern.ISOLATION,
            MuscleGroup.QUADS,
            emptyList()
        ),
        SeedCategory("calf-raise-family", "Calf Raise", MovementPattern.ISOLATION, MuscleGroup.CALVES, emptyList()),
        SeedCategory("core", "Core", MovementPattern.CORE, MuscleGroup.CORE, emptyList()),
        // **The one family whose pattern is the jump** (N241). A family states one muscle and one pattern, and
        // a box jump has an honest claim on the squat pattern, on the calves, the quads and the glutes - so the
        // pattern is the movement's own answer and the glide-and-land work files under it.
        SeedCategory(
            "plyometric",
            "Plyometric",
            MovementPattern.JUMP,
            MuscleGroup.QUADS,
            listOf(MuscleGroup.GLUTES, MuscleGroup.CALVES)
        ),
        // The families added with the strength and bodybuilding set. Each holds a cluster that had no home:
        // the rear delts are not lateral raises, a shrug is not a row, and a carry is the one family whose
        // pattern is `CARRY`.
        SeedCategory(
            "rear-delt",
            "Rear Delt",
            MovementPattern.ISOLATION,
            MuscleGroup.SHOULDERS,
            listOf(MuscleGroup.UPPER_BACK)
        ),
        SeedCategory("shrug", "Shrug", MovementPattern.ISOLATION, MuscleGroup.UPPER_BACK, listOf(MuscleGroup.FOREARMS)),
        // B148: a slug in **both** lists is a movement `INSERT OR IGNORE` silently drops, so the family
        // must not take the movement's bare name. `SeedExercisesTest` asserts they never intersect.
        SeedCategory(
            id = "back-extension-family",
            name = "Back Extension",
            pattern = MovementPattern.HINGE,
            primaryMuscle = MuscleGroup.LOWER_BACK,
            secondaryMuscles = listOf(MuscleGroup.GLUTES, MuscleGroup.HAMSTRINGS),
        ),
        SeedCategory("forearm", "Forearm", MovementPattern.ISOLATION, MuscleGroup.FOREARMS, emptyList()),
        SeedCategory("adductor", "Adductor", MovementPattern.ISOLATION, MuscleGroup.ADDUCTORS, emptyList()),
        SeedCategory(
            "carry",
            "Carry",
            MovementPattern.CARRY,
            MuscleGroup.CORE,
            listOf(MuscleGroup.FOREARMS, MuscleGroup.UPPER_BACK)
        ),
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
        // Bench Press: the equipment variants, the competition, speed and paused lifts, and the press that
        // is trained for the triceps.
        "barbell-bench-press" to "bench-press",
        "competition-bench-press" to "bench-press",
        "bench-press-speed-day" to "bench-press",
        "paused-bench-press-3s" to "bench-press",
        "dumbbell-bench-press" to "bench-press",
        "decline-bench-press" to "bench-press",
        "machine-chest-press" to "bench-press",
        "floor-press" to "bench-press",
        "close-grip-bench-press" to "bench-press",
        "incline-dumbbell-press" to "incline-press",
        "incline-barbell-bench-press" to "incline-press",
        "incline-machine-press" to "incline-press",
        "cable-fly" to "fly",
        "dumbbell-fly" to "fly",
        "pec-deck" to "fly",
        // Deadlift: the bar lifts, the hinges that assist them and the lower back.
        "conventional-deadlift" to "deadlift-family",
        "sumo-deadlift" to "deadlift-family",
        "deficit-deadlift" to "deadlift-family",
        "rack-pull" to "deadlift-family",
        "romanian-deadlift" to "deadlift-family",
        "good-morning" to "deadlift-family",
        "kettlebell-swing" to "deadlift-family",
        "back-extension" to "back-extension-family",
        "reverse-hyperextension" to "back-extension-family",
        // Pulling: the vertical pull, the rows, the shrugs.
        "lat-pulldown" to "lat-pulldown-family",
        "pull-up" to "lat-pulldown-family",
        "chin-up" to "lat-pulldown-family",
        "assisted-pull-up" to "lat-pulldown-family",
        "straight-arm-pulldown" to "lat-pulldown-family",
        "barbell-row" to "row",
        "pendlay-row" to "row",
        "t-bar-row" to "row",
        "seated-cable-row" to "row",
        "chest-supported-row" to "row",
        "single-arm-dumbbell-row" to "row",
        "inverted-row" to "row",
        "barbell-shrug" to "shrug",
        "dumbbell-shrug" to "shrug",
        // Shoulders: the press, the lateral raise, the rear delt.
        "overhead-press" to "overhead-press-family",
        "push-press" to "overhead-press-family",
        "seated-dumbbell-press" to "overhead-press-family",
        "arnold-press" to "overhead-press-family",
        "machine-shoulder-press" to "overhead-press-family",
        "z-press" to "overhead-press-family",
        "lateral-raise" to "lateral-raise-family",
        "cable-lateral-raise" to "lateral-raise-family",
        "machine-lateral-raise" to "lateral-raise-family",
        "dumbbell-rotator-raise" to "lateral-raise-family",
        "rotator-cable-to-side" to "lateral-raise-family",
        "rear-delt-fly" to "rear-delt",
        "reverse-pec-deck" to "rear-delt",
        "band-pull-apart" to "rear-delt",
        // Arms.
        "barbell-curl" to "curl",
        "hammer-curl" to "curl",
        "preacher-curl" to "curl",
        "concentration-curl" to "curl",
        "machine-curl" to "curl",
        "cable-arm-curl" to "curl",
        "behind-the-back-cable-curl" to "curl",
        "incline-dumbbell-arm-curl" to "curl",
        "triceps-pushdown" to "triceps-extension",
        "overhead-triceps-extension" to "triceps-extension",
        "cable-overhead-triceps-extension" to "triceps-extension",
        "dumbbell-skullcrusher" to "triceps-extension",
        "bench-dip" to "triceps-extension",
        "diamond-push-up" to "triceps-extension",
        "wrist-curl" to "forearm",
        "reverse-wrist-curl" to "forearm",
        "reverse-curl" to "forearm",
        // Legs.
        "back-squat" to "squat",
        "paused-back-squat" to "squat",
        "front-squat" to "squat",
        "box-squat" to "squat",
        "hack-squat" to "squat",
        "goblet-squat" to "squat",
        "leg-press" to "squat",
        "walking-lunge" to "lunge",
        "reverse-lunge" to "lunge",
        "bulgarian-split-squat" to "lunge",
        "step-up" to "lunge",
        "hip-thrust" to "hip-thrust-family",
        "glute-bridge" to "hip-thrust-family",
        "cable-kickback" to "hip-thrust-family",
        "hip-adduction" to "adductor",
        "copenhagen-plank" to "adductor",
        "standing-calf-raise" to "calf-raise-family",
        "seated-calf-raise" to "calf-raise-family",
        "donkey-calf-raise" to "calf-raise-family",
        // Core and carries.
        "plank" to "core",
        "side-plank" to "core",
        "hanging-leg-raise" to "core",
        "cable-crunch" to "core",
        "ab-wheel-rollout" to "core",
        "pallof-press" to "core",
        "turkish-get-up" to "core",
        "farmers-carry" to "carry",
        "suitcase-carry" to "carry",
        // **The last unfiled movements, filed** (N229): a fresh install used to draw a flat alphabetical pile
        // of loose lifts under its families, which read as the library's leftovers. A face pull is rear-delt
        // work and a dip is the compound its family already holds; the knee isolation needed two families.
        "face-pull" to "rear-delt",
        "dip" to "triceps-extension",
        "lying-leg-curl" to "leg-curl",
        "seated-leg-curl" to "leg-curl",
        "nordic-curl" to "leg-curl",
        "leg-extension" to "leg-extension-family",
        "box-jump" to "plyometric",
        "jump-squat" to "plyometric",
        "broad-jump" to "plyometric",
        "depth-jump" to "plyometric",
        "pogo-hop" to "plyometric",
        "skater-bound" to "plyometric",
        "medicine-ball-throw" to "plyometric",
        // **One movement stays unfiled on purpose** (N229): every press family is a *lift's* family, so a
        // push-up is none of them. One loose movement the seed can stand behind is a different thing from
        // eight of them sitting there because nothing was done.
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
        /**
         * Whether the movement is loaded by the body, or held rather than counted (ROADMAP N219, N220, B293).
         *
         * The seeder did not write either column, so every seeded row arrived as an ordinary loaded movement:
         * a weighted pull-up was read as its belt alone, and a plank could not be logged as a hold. The two
         * flags are the movement's own, which is exactly what a seed has to state.
         */
        bodyweight: Boolean = false,
        timed: Boolean = false,
        /** The pattern this movement states when its family's is wrong for it, or null (N103, N241). */
        pattern: MovementPattern? = null,
    ) = Exercise(
        id = id,
        name = name,
        primaryMuscle = primary,
        secondaryMuscles = secondary,
        equipment = equipment,
        movementPattern = pattern,
        isBodyweight = bodyweight,
        isTimed = timed,
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
