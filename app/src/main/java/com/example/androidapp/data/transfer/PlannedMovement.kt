package com.example.androidapp.data.transfer

/**
 * One planned movement: the day it belongs to, what it is, and where it sits in that day (B267).
 *
 * Deliberately not a row type. It is the part of a planned row that answers "is this the same work?", and it is
 * built from either side — a document's `templateExercises`, or the rows the device already holds — so the
 * comparison does not have to care which side it is reading.
 */
internal data class PlannedMovement(
    val templateId: String,
    val exerciseId: String,
    val position: Int,
)

/** This document's planned movements, counted so a duplicated row compares as a difference (B267). */
internal fun ProgramDocument.plannedMovements(): Map<PlannedMovement, Int> =
    templateExercises.groupingBy {
        PlannedMovement(templateId = it.templateId, exerciseId = it.exerciseId, position = it.position)
    }.eachCount()

/** The same count over rows read from the device, so both sides are compared in one shape (B267). */
internal fun List<PlannedMovement>.counted(): Map<PlannedMovement, Int> = groupingBy { it }.eachCount()

/**
 * Whether [this] document plans the work the device already holds under the same id (B267).
 *
 * The answer decides what the import card says. An id that is already here has always meant "nothing to add", and
 * that stays true — the merge is insert-only and never edits a program (B180) — but it is **not** the same
 * statement as "this is the program you loaded". A different document can carry the id: a pasted table's seed is
 * its program name, so a second table that also says *Push Pull Legs* produces the first one's id, and the card
 * answered *That program is already here.* about a program the device had never seen. (B266 gave a dropped file an
 * identity of its own; a paste has no file to name it.)
 *
 * The comparison is the days and their movements — template id, exercise id and place, with multiplicity — which
 * is the part of a plan two documents can differ in while sharing the seed's id. **What it does not compare**:
 * loads, rep ranges, rests, cues and notes. Those are not merged over a live program either, so nothing is silent
 * about them that the merge would have changed; what the card has to say is that the document on screen is not
 * the plan on the device.
 */
internal fun ProgramDocument.plansTheSameWorkAs(stored: Map<PlannedMovement, Int>): Boolean =
    plannedMovements() == stored
