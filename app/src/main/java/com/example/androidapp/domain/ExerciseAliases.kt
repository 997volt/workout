package com.example.androidapp.domain

/**
 * The words a lifter says for a movement that its own name never contains (ROADMAP N224).
 *
 * Shorthand is vocabulary rather than data: "ohp" is the Overhead Press to anyone who has trained one, and
 * "rdl" is a Romanian Deadlift, yet neither string appears in the movement's name, its muscles, its equipment
 * or its pattern. So the table lives beside the search that reads it rather than on a row — which is what keeps
 * it out of the backup, out of the library screen and out of every write path, and leaves a movement a lifter
 * makes themselves with no aliases at all, which is the honest answer rather than a missing feature.
 *
 * Keyed by the seeded slug. `ExerciseSearchTest` proves every key is a movement the seed still ships, so a
 * retired or retyped id is a red test rather than an alias that quietly stops working.
 */
internal object ExerciseAliases {

    /**
     * Aliases per seeded movement id, each a phrase the search matches as it matches a name.
     *
     * Kept to what people actually type and what the name cannot answer: initials the name does not already
     * produce ("bbp" for Barbell Bench Press needs no entry, since the initials rule finds it), and phrases
     * whose words are not all in the name. A single letter is never an entry.
     */
    val byExerciseId: Map<String, List<String>> = mapOf(
        "barbell-bench-press" to listOf("bp"),
        "dumbbell-bench-press" to listOf("db bench"),
        "overhead-press" to listOf("ohp", "military press"),
        "romanian-deadlift" to listOf("rdl"),
        "close-grip-bench-press" to listOf("cgbp"),
        "bulgarian-split-squat" to listOf("bss"),
        "barbell-row" to listOf("bent over row"),
        "seated-cable-row" to listOf("cable row"),
        "hanging-leg-raise" to listOf("hlr"),
        "turkish-get-up" to listOf("tgu"),
        "farmers-carry" to listOf("farmers walk"),
    )

    /** The aliases this movement answers to, or none — the ordinary case, and every lifter-made movement. */
    fun of(exerciseId: String): List<String> = byExerciseId[exerciseId].orEmpty()
}
