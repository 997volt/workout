package com.example.androidapp.domain.model

import com.example.androidapp.domain.Load

/**
 * Where a rung sits in its group (ROADMAP N79).
 *
 * A *rung* is a drop or cluster set, and it hangs off the set above it. The plan stores no link
 * between the two, so the run is read from what is already there: it is **contiguous**, its members
 * share a role, and its anchor is the nearest set above it that stands on its own. That is why this
 * is a lookup rather than a column.
 */
data class RungRun(
    /** The set this run hangs off: the one that carries the group's target and its rating. */
    val anchorIndex: Int,
    /** This rung's place in the run, 1-based — the *k* of `anchor − k × value`. */
    val rung: Int,
    /**
     * The value the run takes off the anchor per rung, in grams, or null.
     *
     * Held by the run's **first rung** and inherited by the rest, so a later rung carrying null is
     * still part of the run and takes the same value. Null on the first rung means the run names no
     * value, which is how a cluster run reads — and how a drop run written before N79 reads.
     */
    val dropValueGrams: Long?,
)

/**
 * The run the set at [index] belongs to, or null when it is not a rung (ROADMAP N79).
 *
 * [index] is a position in **stored order**, which is the order the plan reads and writes its sets in.
 * The anchor has to be a set that stands on its own — a working set, a top set or a failure set, never
 * a warm-up and never another kind of rung — so a run that begins with nothing above it, or above
 * another kind of rung, has no anchor and reads as carrying no derived weight rather than failing.
 */
fun List<TemplateSet>.runAt(index: Int): RungRun? {
    val role = getOrNull(index)?.role?.takeIf { it.isRung }
    val start = if (role == null) index else runStart(index, role)

    // The row above the run has to stand on its own, or there is nothing to derive from and nothing
    // to rate — which is what a plan beginning with a rung, or a run above another kind of rung, is.
    val anchored = role != null && getOrNull(start - 1)?.role?.recordsEffort == true

    return if (anchored) {
        RungRun(
            anchorIndex = start - 1,
            rung = index - start + 1,
            dropValueGrams = this[start].dropValueGrams,
        )
    } else {
        null
    }
}

/** The first row of the contiguous same-role run that ends at [index]. */
private fun List<TemplateSet>.runStart(index: Int, role: SetType): Int {
    var start = index
    while (start > 0 && this[start - 1].role == role) start--
    return start
}

/**
 * The load one rung carries, given what its anchor loaded (ROADMAP N79).
 *
 * A cluster rung repeats the anchor's load **exactly**, assistance included — it is the same load, so
 * there is nothing to guard. A drop rung is the anchor's added weight less the run's value per rung,
 * which means it only exists where the anchor *has* added weight: `null` comes back for an assisted or
 * bodyweight anchor (there is no 20 kg to take off), for a run that names no value, and for a rung
 * whose ladder has run out. Null is the safe answer rather than a guess — the caller falls back to
 * whatever the set itself carries, so a legacy row or an imported file reads rather than becomes an
 * assisted set with a negative weight.
 */
fun rungLoad(anchor: Load, role: SetType, run: RungRun): Load? = when {
    !role.isRung -> null

    role == SetType.CLUSTER -> anchor

    else -> {
        val added = anchor.weightGrams.takeIf { it > 0L && anchor.assistanceGrams == 0L } ?: return null
        val value = run.dropValueGrams?.takeIf { it > 0L } ?: return null
        (added - value * run.rung).takeIf { it > 0L }?.let { Load(weightGrams = it, assistanceGrams = 0L) }
    }
}

/**
 * What the rung at [index] loads, or null when it is not a rung or nothing can be derived (N79).
 *
 * [anchorLoad] is what the anchor carries — the session passes what it **actually did**, because a drop
 * is taken off the bar in front of you, and the plan passes nothing, which reads the anchor's own
 * target. Null is the same answer as [rungLoad] gives: an anchor with no added weight, a run naming no
 * value, or a ladder that has run out.
 */
fun List<TemplateSet>.rungWeightAt(index: Int, anchorLoad: Load? = null): Long? {
    val run = runAt(index)
    val anchor = run?.let { getOrNull(it.anchorIndex) }
    val load = anchorLoad
        ?: anchor?.let { Load(it.targetWeightGrams ?: 0L, it.targetAssistanceGrams ?: 0L) }
    return if (run == null || load == null) {
        null
    } else {
        rungLoad(load, this[index].role, run)?.weightGrams
    }
}

/**
 * Whether the set at [index] is followed by another rung of its own run (ROADMAP N79).
 *
 * The rest belongs to the group rather than to each set, so it waits for the run to close: an anchor
 * whose next row is its first rung has not finished the group, and each rung but the last has one after
 * it. A set with no run after it — the ordinary case — carries on to nothing and rests as it always has.
 */
fun List<TemplateSet>.continuesItsRunAt(index: Int): Boolean {
    val mine = getOrNull(index)
    val next = getOrNull(index + 1)
    return when {
        mine == null || next == null -> false
        mine.role.isRung -> next.role == mine.role
        else -> next.role.isRung && runAt(index + 1)?.anchorIndex == index
    }
}

/**
 * The set indexes whose run carries on into the next planned row (ROADMAP N79).
 *
 * Precomputed where the plan is in hand, because the rest is decided while a set is being logged and
 * that path has the row rather than the plan.
 */
fun List<TemplateSet>.runContinuesAfter(): Set<Int> =
    indices.filter { continuesItsRunAt(it) }.map { this[it].setIndex }.toSet()
