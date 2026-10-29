package com.example.androidapp.domain.model

/**
 * A name this device does not hold, from the one the document asks for (ROADMAP N243).
 *
 * "Push Pull Legs" is tried first, then "Push Pull Legs (2)", "(3)" and so on, and the answer is the offered
 * name when nothing holds it — a copy that arrives under the name it asked for is the least surprising result.
 * Comparison ignores case and surrounding space, because two programs a lifter cannot tell apart are the thing
 * this exists to avoid.
 */
fun freeProgramName(asked: String, taken: List<String>): String {
    val wanted = asked.trim()
    val used = taken.mapTo(mutableSetOf()) { it.trim().lowercase() }
    if (wanted.lowercase() !in used) return wanted
    var suffix = 2
    while (true) {
        val candidate = "$wanted ($suffix)"
        if (candidate.lowercase() !in used) return candidate
        suffix++
    }
}
