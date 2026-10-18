package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.DeloadSetTarget
import com.example.androidapp.domain.model.DeloadWeek
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A program's built deload week (ROADMAP N147).
 *
 * The creator writes **ordinary templates** and a plan for one week, and every read of "what is planned"
 * takes that plan instead of the program's slots for the week it names. Nothing here computes a deload on
 * its own: the draft is written when the lifter asks for it, and the saved plan never changes itself.
 */
interface DeloadRepository {

    /**
     * The plans built for [weekStart]'s week, one per program, empty where none was built (N147).
     *
     * A program marked before this table existed — or marked and never built — is absent, which is the
     * P3.10 shape: the mark exempts the week and the slots still prescribe their own workouts.
     */
    fun observeWeekPlans(weekStart: LocalDate): Flow<List<DeloadWeek>>

    /**
     * Writes [days] as this week's deload, marking the week if it is not already (N147).
     *
     * One transaction over the mark, the templates and the plan, so a half-built week is not a state any
     * screen can read. Re-running for a week that already has a plan replaces its days and leaves the
     * templates the previous plan wrote in the library: they are ordinary workouts, and a rebuild is not a
     * reason to delete the lifter's data. Unmarking the week (P3.10) leaves the plan unreachable rather
     * than rewriting it — a mark that comes back starts from P3.10's own shape.
     */
    suspend fun createDeload(
        programId: String,
        weekStart: LocalDate,
        days: List<DeloadDayInput>,
    ): DataResult<DeloadCreation>

    /**
     * The most recent built plan before [beforeWeek], or null (N150).
     *
     * A one-shot read for the creator's *Use last deload*: the plan's days carry the library exercises they
     * kept, so the draft can preselect those and re-derive every number from the current plan. The load share
     * is not part of the plan, so it is not reused; the day count and the weekdays are.
     *
     * **Loader-internal, and a raw read** (B169): it can throw, and the creator's loader wraps the whole load
     * in `dataResultOf`; a caller outside that boundary must add its own. The exercises are read from the
     * plan's **current templates** (N16), not a choice snapshot — a deload template is an ordinary living
     * template, so editing one changes what a later reuse sees as the last choices (N155).
     */
    suspend fun findLastPlan(programId: String, beforeWeek: LocalDate): DeloadWeek?
}

/**
 * One day the creator writes (ROADMAP N147).
 *
 * A name, the day it is offered on, and the exercises it keeps — already reduced to what the deload
 * prescribes. The derivation is [com.example.androidapp.domain.model.deloadSetTargets], a pure function,
 * so the repository only stores what the screen showed.
 */
data class DeloadDayInput(
    val name: String,
    val weekday: DayOfWeek?,
    val exercises: List<DeloadExerciseInput>,
)

/** One exercise of a deload day, with the sets the draft derived for it (ROADMAP N147). */
data class DeloadExerciseInput(
    val exerciseId: String,
    val restSeconds: Int?,
    val techniqueNote: String?,
    val supersetGroup: Int?,
    val sets: List<DeloadSetTarget>,
)

/** What building a week wrote: the templates, in the order the days were listed (ROADMAP N147). */
data class DeloadCreation(
    val weekStart: LocalDate,
    val templateIds: List<String>,
) {
    val dayCount: Int get() = templateIds.size
}
