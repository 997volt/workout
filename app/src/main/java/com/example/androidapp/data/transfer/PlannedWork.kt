package com.example.androidapp.data.transfer

import java.time.DayOfWeek

/**
 * One of a program's days: the workout it names, when it runs, and where it sits in the list (B267).
 *
 * Deliberately not a row type. A document and the rows the device holds both reduce to this, so the comparison
 * does not have to care which side it is reading.
 */
internal data class PlannedDay(
    val templateId: String,
    val weekday: DayOfWeek?,
    val position: Int,
)

/** One planned movement: the day it belongs to, what it is, and where it sits in that day (B267). */
internal data class PlannedMovement(
    val templateId: String,
    val exerciseId: String,
    val position: Int,
)

/**
 * The part of a program a document and the device can be compared on (B267): its days and their movements.
 *
 * Counted rather than listed, so a duplicated row compares as the difference it is.
 */
internal data class PlannedWork(
    val days: Map<PlannedDay, Int>,
    val movements: Map<PlannedMovement, Int>,
)

/** This document's days and movements, in the shape [PlannedWork] compares (B267). */
internal fun ProgramDocument.plannedWork(): PlannedWork = PlannedWork(
    days = slots.groupingBy {
        PlannedDay(templateId = it.templateId, weekday = it.weekday, position = it.position)
    }.eachCount(),
    movements = templateExercises.groupingBy {
        PlannedMovement(templateId = it.templateId, exerciseId = it.exerciseId, position = it.position)
    }.eachCount(),
)

/** The same count over rows read from the device, so both sides are compared in one shape (B267). */
internal fun <T> List<T>.counted(): Map<T, Int> = groupingBy { it }.eachCount()

/**
 * Whether [this] document is the work the device already holds under the same id (B267).
 *
 * The answer decides what the import card says. An id that is already here has always meant "nothing to add", and
 * that stays true — the merge is insert-only and never edits a program (B180) — but it is **not** the same
 * statement as "this is the program you loaded". A different document can carry the id: a pasted table's seed is
 * its program name, so a second table that also says *Push Pull Legs* produces the first one's id, and the card
 * answered *That program is already here.* about a program the device had never seen. (B266 gave a dropped file an
 * identity of its own; a paste has no file to name it.)
 *
 * **Both halves are needed.** Days alone miss a changed movement in the same day; movements alone miss a day a
 * different program has and this one does not — which is exactly the shape a second program arrives in, and what a
 * version of this comparison that read movements only got wrong.
 *
 * **What it does not compare**: loads, rep ranges, rests, cues and notes. Those are not merged over a live program
 * either, so nothing is silent about them that the merge would have changed; what the card has to say is that the
 * document on screen is not the plan on the device.
 */
internal fun ProgramDocument.plansTheSameWorkAs(stored: PlannedWork): Boolean = plannedWork() == stored
