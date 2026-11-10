package com.example.androidapp.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A named, ordered list of training slots (ROADMAP P3.3).
 *
 * The container the N16 weekday pins cannot be on their own: a pin says what happens on
 * a Tuesday, but nothing orders the pins against each other, so "which one is next" and
 * "was that a skip or a rest day" have no answer. A program supplies the order; a slot
 * supplies the day.
 *
 * [isActive] means home follows this program, and **more than one program may be active**
 * (ROADMAP P3.12, amending P3.3): home's today plan, the missed-day question and adherence
 * all read the *union* of the active programs. When no live program is active, the home
 * screen falls back to the template pins it already reads.
 *
 * [position] is the authored order, low first: which active program comes first is a choice,
 * not an accident of its name or when it was created (P3.12).
 */
data class WorkoutProgram(
    val id: String,
    val name: String,
    val slotCount: Int = 0,
    val isActive: Boolean = false,
    val position: Int = 0,
    /** A week that repeats (the default), or an order that ends (ROADMAP N174). */
    val kind: ProgramKind = ProgramKind.WEEKLY,
    /**
     * The day a block begins, or null for the day it was created (N174).
     *
     * A sequence reads it as the day its first session is due. A weekly program with phases reads it
     * as the anchor its rotation is measured from (N292), and falls back to [createdAtMillis].
     */
    val startDate: LocalDate? = null,
    /**
     * True when this program's plans adjust themselves from what was done (ROADMAP N299).
     *
     * A property of the program rather than the global progression mode: the lifter switches it on
     * for one program, and the movement's own answer still wins over it (N235). The app writes only
     * inside a mode this flag turns on, and every write is recorded and can be undone.
     */
    val autoProgression: Boolean = false,
    /**
     * When the row was written, as epoch milliseconds (ROADMAP N292).
     *
     * Carried for one question: a phased weekly program with no authored start measures its rotation
     * from the week it was created, the same fallback a sequence applies (N175). It is not display
     * data. A caller that builds a program by hand leaves it at zero; the fallback is then the epoch,
     * which only matters for a program that also authors phases and no start.
     */
    val createdAtMillis: Long = 0L,
)

/**
 * One slot of a program: a template, and the weekday it is done on (ROADMAP P3.3).
 *
 * [weekday] null means the slot is **order-only** — it takes part in "which one is next"
 * but can never be missed, because there is no day to miss.
 */
data class ProgramSlot(
    val id: String,
    val programId: String,
    val templateId: String,
    val position: Int,
    val weekday: DayOfWeek? = null,
    /**
     * The authored phase week this day belongs to, or null for a program with no phases (ROADMAP N292).
     *
     * Null is the old shape and keeps its old meaning: the program is one week that repeats. A
     * program that authors phases names a week on **every** slot, numbered from one; the editor and
     * the document validator both refuse the mixed shape, because an unphased day in a phased
     * program is never read.
     */
    val phaseWeek: Int? = null,
    /** The template's name, carried by the slot's read so a list needs one subscription. */
    val templateName: String = "",
    val exerciseCount: Int = 0,
)
