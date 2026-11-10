package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult

/**
 * The state auto progression keeps beside the plan (ROADMAP N299).
 *
 * The plan itself is still the truth: there is no cursor and no cycle counter, and a target moved
 * by a step is read from the template like every other target. What lives here is the two facts the
 * plan cannot carry — **where the program started** when auto was switched on, so it can be reset,
 * and **which writes were the app's**, so one can be undone.
 */
interface ProgressionRepository {

    /**
     * Switches the program's own progression on or off (ROADMAP N299).
     *
     * Enabling snapshots every scheduled set's current target as the baseline in the same
     * transaction as the flag; disabling leaves that snapshot, so a later reset still has the point
     * the first switch was made from.
     */
    suspend fun setAutoProgression(programId: String, enabled: Boolean): DataResult<Unit>

    /**
     * The program whose switch covers [templateId], or null (ROADMAP N299).
     *
     * Resolved through the template because a session records only its template; the first of
     * several auto programs naming one shared plan is enough, because the plan moves once.
     */
    suspend fun autoProgramForTemplate(templateId: String): DataResult<String?>

    /** When this program's baseline was captured, or null when auto has never been on. */
    suspend fun baselineCapturedAt(programId: String): DataResult<Long?>

    /**
     * Puts every planned set back to the target it held when auto was switched on.
     *
     * Returns how many sets were restored. A set that no longer exists, or whose template is gone,
     * is skipped rather than resurrected: the baseline is a memory, not a second plan.
     */
    suspend fun resetToBaseline(programId: String): DataResult<Int>

    /**
     * Takes one applied step back, or refuses when the plan no longer holds what the step wrote.
     *
     * The check is the point: a later step, or the lifter's own edit, has moved the set since, and
     * restoring the old target would silently overwrite that work.
     */
    suspend fun undo(writeId: String): DataResult<Unit>
}
