package com.example.androidapp.data

/**
 * One movement that is another one under a second name (ROADMAP N238).
 *
 * **The survivor is the row the library keeps**: its id is what the session's and the plan's rows are
 * re-pointed onto, and the source row is deleted afterwards — deleted rather than soft-deleted, because after the
 * re-point it holds nothing and a tombstone would only be a row the library must filter and every backup carry.
 */
data class MovementMerge(
    val sourceId: String,
    val survivorId: String,
)

/**
 * The library row a merge decision looks at: only the facts that decide it (ROADMAP N238).
 *
 * Four fields and no more, because they are the whole of the rule: what it is called, whether the seed ships it,
 * and whether it is **bare** — no taxonomy, no family — which is the shape an import leaves behind and the one an
 * edit never produces. Carrying the taxonomy itself would invite a second rule about what "no taxonomy" means.
 */
data class MergeCandidate(
    val id: String,
    val name: String,
    /** True for a row the seed ships: a seeded row is never merged away, only merged into. */
    val seeded: Boolean = false,
    /** No muscle, no equipment, no pattern and no family — what an imported row arrives as. */
    val isBare: Boolean = false,
)

/**
 * The merges one library needs, given what the seed ships (ROADMAP N238).
 *
 * Two sources and one answer. **The mechanical ones** are the import's: a document that says *Back Squat* and
 * resolves to nothing creates a movement of its own, so a second *Back Squat* appears beside the seeded one with
 * the history split across them. Such a row is recognised by its **shape** — the seeded name, no taxonomy, no
 * family — because nothing an edit produces looks like that, and a hand-written list would miss the next import.
 *
 * **The ruled ones** are the author's: the roadmap's table, where a retired seed row and its twin, or two names
 * for one machine, are one movement. A ruling names the rows by **name**, because that is what the ruling is
 * about, and a name is resolved against what is on this device: a table line whose source is not here is
 * nothing to do, and one whose survivor is not here is skipped rather than merged into nothing. A ruled source
 * is held to the **mechanical rule's shape** too (B298): the table was derived from import leftovers, so a row
 * the lifter configured states something and is theirs whatever its name.
 *
 * [seedNames] is what the seed ships *now*. A row whose name is in it and which is not itself seeded is a
 * mechanical duplicate; the seed's own row is the survivor.
 *
 * **A name can be held by more than one row** (B323). The survivor is the seed's own row when it ships the
 * name — an import's duplicate beside it is merged away in the same pass — and every survivor is followed to
 * the row that survives the whole pass, so no pair re-points history onto a row another pair deletes.
 */
fun mergedMovementPairs(
    library: List<MergeCandidate>,
    /** What the seed ships **now**, by name. Defaulted to the seed itself, which is what the repair passes. */
    seedNames: Set<String> = SeedExercises.all.map { it.name }.toSet(),
): List<MovementMerge> {
    // Grouped rather than one per name: the duplicate this repair exists for can share its name with the row the
    // ruling names as the survivor, and "last wins" chose the duplicate (B323).
    val byName = library.groupBy { it.name }
    val seededByName = library.filter { it.seeded && it.name in seedNames }.associateBy { it.name }
    val merged = linkedMapOf<String, MovementMerge>()

    fun add(source: MergeCandidate, survivor: MergeCandidate) {
        // A row cannot be merged into itself, and the seed's own rows are never sources.
        if (source.id == survivor.id || source.seeded) return
        merged[source.id] = MovementMerge(sourceId = source.id, survivorId = survivor.id)
    }

    // The mechanical rule first, so the rulings apply to what is left: an imported *Back Squat* is a duplicate
    // of the seed's, not a movement of the lifter's own.
    library.filter { it.name in seedNames && it.isBare }.forEach { row ->
        seededByName[row.name]?.let { survivor -> add(row, survivor) }
    }

    MOVEMENT_MERGE_RULINGS.forEach { ruling ->
        // The seed's row when it ships the name: a duplicate beside it is merged away below, and a ruling that
        // pointed at the duplicate would re-point onto a row this pass deletes (B323).
        val survivor = seededByName[ruling.into] ?: byName[ruling.into]?.lastOrNull() ?: return@forEach
        // The table is about import leftovers, which is the shape the mechanical rule reads (B298). A row the
        // lifter configured -- a taxonomy, a family -- is theirs, and deleting it would take what they made.
        val source = byName[ruling.away]?.firstOrNull { !it.seeded && it.isBare } ?: return@forEach
        add(source, survivor)
    }

    // A survivor can itself be a source: the rulings name rows, and one line's target may be another line's
    // leftover. Follow each chain to the row that survives the whole pass, so the repair never writes a
    // reference onto a row it deletes later in the same transaction (B323).
    return merged.values.mapNotNull { merge ->
        val survivor = finalSurvivor(merge.survivorId, merged)
        if (survivor == merge.sourceId) null else MovementMerge(merge.sourceId, survivor)
    }
}

/** The last row of [merged]'s chain from [id], or [id] itself when it is not merged away. */
private fun finalSurvivor(id: String, merged: Map<String, MovementMerge>): String {
    var current = id
    val seen = mutableSetOf<String>()
    while (seen.add(current)) {
        current = merged[current]?.survivorId ?: return current
    }
    return current
}

/**
 * The merge list, as ruled (ROADMAP N238): the name before the arrow goes, the one after it is the survivor.
 *
 * Every line is a ruling about one gym's equipment, so each rests on the condition the roadmap states: one
 * machine under three names (*Machine Arm Extension*, *Triceps Extension (Machine)*), a word-for-word variant
 * (*One-Arm Dumbbell Row*), or the implement the seed's own note already puts second (*Two-Arm Cable
 * Pushdown*). The rows a ruling does **not** name are the lifter's own movements, which is N239's half.
 *
 * Kept as names rather than ids on purpose: the source row's id is the import's invention, so it differs
 * between devices, while the name is what the ruling is about.
 */
val MOVEMENT_MERGE_RULINGS: List<MergeRuling> = listOf(
    MergeRuling("Deadlift", "Conventional Deadlift"),
    MergeRuling("Calf Press", "Calf Press (Machine)"),
    MergeRuling("Single Leg Standing Calf Raise (Dumbbell)", "Single-Leg Calf Raise"),
    MergeRuling("Bicep Curl (Dumbbell)", "Dumbbell Curl"),
    MergeRuling("Machine Arm Extension", "Machine Triceps Extension"),
    MergeRuling("Triceps Extension (Machine)", "Machine Triceps Extension"),
    MergeRuling("Single Arm Tricep Extension (Dumbbell)", "Dumbbell Single-Arm Extension"),
    MergeRuling("Dumbbell Flat Bench Press", "Dumbbell Bench Press"),
    MergeRuling("One-Arm Dumbbell Row", "Single-Arm Dumbbell Row"),
    MergeRuling("Rope Crunch", "Cable Crunch"),
    MergeRuling("Dumbbell Overhead Triceps Extension", "Overhead Triceps Extension"),
    MergeRuling("Machine Fly", "Pec Deck"),
    MergeRuling("Chest Fly (Machine)", "Pec Deck"),
    MergeRuling("Two-Arm Cable Pushdown", "Triceps Pushdown"),
    MergeRuling("Machine Row", "Seated Row (Machine)"),
    MergeRuling("Cable Row", "Seated Cable Row"),
    MergeRuling("Calf Raise", "Standing Calf Raise"),
    MergeRuling("Dumbbell Shoulder Press", "Seated Dumbbell Press"),
    MergeRuling("Forearm Flexion", "Wrist Curl"),
)

/** One line of the table: the name that goes, and the name that stays. */
data class MergeRuling(val away: String, val into: String)
