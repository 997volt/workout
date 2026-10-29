package com.example.androidapp.domain

import com.example.androidapp.domain.model.Exercise

/**
 * Search over the exercise library.
 *
 * Deliberately pure and free of Android types so it is covered by fast JVM
 * tests rather than instrumented ones — the same split as the original
 * `Greeting`/`GreetingTest` scaffold pair.
 *
 * **A lift is found by how it is said, not only by how it is written down** (ROADMAP N224). Substring
 * containment was the whole rule, so *Barbell Bench Press* answered "bench" and not "bench pr", "bbp" or
 * "pullup" — and the words a lifter types are the ones that come out of a session, not the ones a library uses.
 * Three rules, all here because a search that is not one pure function is one nobody can test:
 *
 *  - **The words of the name.** The name and the query are compared with every separator removed, so a word is
 *    a word however it was typed: "bench pr" and "benchpr" both reach *Barbell Bench Press*, and "pullup" reaches
 *    *Pull-Up*.
 *  - **Initials.** "bbp" is *Barbell Bench Press*, and "cgbp" is *Close-Grip Bench Press* without needing an alias.
 *  - **Aliases** — the words the name cannot carry ([ExerciseAliases]) are searched beside it.
 */
object ExerciseSearch {

    /**
     * True when [query] finds this row: its name, either muscle group, its equipment or its movement pattern.
     *
     * Two deliberate choices (ROADMAP P1.1a):
     *
     *  - **Secondary muscles are searched too.** They were not, so typing
     *    "forearms" missed every row where forearms were *secondary* — a dead end,
     *    because nothing on screen explained why a row the user could see was not
     *    found.
     *  - **The enum name is matched as well as the label.** `label` is user-facing
     *    text that localization translates, so a search keyed only on it quietly
     *    stops working in another language. `name` is locale-stable, so
     *    `FOREARMS` still matches "forearms" whatever the display language says.
     */
    internal fun Exercise.matches(query: String): Boolean {
        // Normalised once, so the three rules cannot disagree about what was typed. A query that is nothing but
        // separators normalises to "" and matches everything, which is the blank search that was always
        // `contains("")` — the caller's "no search" case, kept rather than special-cased.
        val wanted = normalise(query)
        return matchesName(wanted) || matchesTaxonomy(wanted) || matchesAnAlias(wanted)
    }

    /** The name, by the two rules a name can answer: its letters in order, and its initials (N224). */
    private fun Exercise.matchesName(wanted: String): Boolean =
        normalise(name).contains(wanted) || initials(name).startsWith(wanted)

    /** The taxonomy, by label and by locale-stable key (P1.1a). */
    private fun Exercise.matchesTaxonomy(wanted: String): Boolean =
        (listOfNotNull(primaryMuscle) + secondaryMuscles).any { matches(it.label, it.name, wanted) } ||
            listOfNotNull(equipment).any { matches(it.label, it.name, wanted) } ||
            listOfNotNull(movementPattern).any { matches(it.label, it.name, wanted) }

    /**
     * The words the name cannot carry (N224).
     *
     * An alias is compared the way a name is — separators removed, and it may be the longer side — because an
     * alias is only ever a phrase a lifter types: "bent over row", "bentoverrow" and "db bench" are one query
     * each, and "bent over" finds the row.
     */
    private fun Exercise.matchesAnAlias(wanted: String): Boolean =
        ExerciseAliases.of(id).any { normalise(it).contains(wanted) }

    /** Matches against the display text *and* the locale-stable key, both separator-blind. */
    private fun matches(label: String, stableKey: String, wanted: String): Boolean =
        normalise(label).contains(wanted) || normalise(stableKey).contains(wanted)
}

/**
 * Every character that is not a letter or a digit goes, and case with it (ROADMAP N224).
 *
 * This is what makes a word a word: "Pull-Up", "pull up" and "pullup" are one movement however it was typed,
 * and a name is compared against a query this way rather than by a rule per separator.
 */
private fun normalise(text: String): String = text.lowercase().filter { it.isLetterOrDigit() }

/**
 * The first character of every word of [name], lowercased — "Barbell Bench Press" is "bbp" (ROADMAP N224).
 *
 * A word starts at a letter or a digit that follows something else, which is why "3-Second Paused Bench Press"
 * begins with '3': the number is part of how the movement is named, so it is part of how it is abbreviated.
 */
private fun initials(name: String): String = buildString {
    var atWordStart = true
    name.forEach { character ->
        if (character.isLetterOrDigit()) {
            if (atWordStart) append(character.lowercaseChar())
            atWordStart = false
        } else {
            atWordStart = true
        }
    }
}
