package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.kind
import com.example.androidapp.domain.model.statedWeightGrams
import com.example.androidapp.domain.model.weightGoalOf
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * One **period** of a metric's goal (ROADMAP N137).
 *
 * The shape the rows are written in rather than a domain model: [kind] and [value] are exactly the two columns
 * a goal has, so a metric whose goal is a single number (every metric but weight) reads without being forced
 * through a weight-shaped type. [weightGoal] is the reading for the one that *is* a weight goal.
 *
 * [startedAt] is null for the first period of a goal that was already stored when periods arrived — the move
 * cannot know when it began, and inventing the upgrade's own timestamp would be inventing history.
 *
 * [id] is the row's, and it is what makes the past **editable** (ROADMAP N215): without it a period could be
 * read and judged but never addressed, so a stretch entered wrongly could only be overlaid with another.
 */
data class GoalPeriod(
    val id: String,
    val startedAt: Instant?,
    val kind: String?,
    val value: Double?,
) {
    /** This period read as a weight goal, or null when it asks for none (ROADMAP N135). */
    val weightGoal: WeightGoal? get() = weightGoalOf(kind, value)
}

/**
 * What each metric's goal is, and what it was (ROADMAP N39, N135, N137).
 *
 * The store is **rows with periods**, not a settings string: the past is what makes "did I hit the cut I set in
 * March" answerable at all, and a prefs string rewritten on every change cannot hold it. It also removes a seam
 * the import path named out loud — goals used to be restored "outside the database transaction … because goals
 * live outside the database" — since a goal is now a row like any other.
 *
 * **The derived rate is not stored** (N135): the goal is the input, and the band's arithmetic is not a fact, so
 * a period re-derives under today's rule. That trade is named rather than hidden — the honest reading of an old
 * period is "by today's rule, March was X".
 */
interface GoalRepository {

    /**
     * Every metric's goal **now**, keyed by `MetricKey.id` — the period that is open.
     *
     * One map rather than a read per metric, for the reason the settings map was one: the screen draws whichever
     * goal belongs to the metric on show, and the number of metrics is the registry's, not the user's.
     */
    fun observeCurrent(): Flow<Map<String, GoalPeriod>>

    /** One metric's periods, **oldest first**, so a reader can close each at the next one's start. */
    fun observePeriods(metricId: String): Flow<List<GoalPeriod>>

    /**
     * Writes one period — the row [id] names, or a new one beginning on [start] (ROADMAP N215).
     *
     * **Days, not moments**: [start] and [end] name local days, so a goal "from 1 March" owns 1 March's
     * weigh-in. A start that already carries a boundary — at midnight or at a clock time written before N215 —
     * edits that boundary rather than opening a second one that day. A goal-only correction keeps the stored
     * instant, so the day's earlier readings stay with the period that had them; an added or moved boundary is
     * local midnight.
     *
     * **The end is the next boundary.** Passing [end] closes the period there; when no boundary already sits on
     * that day, the goal that covered it before is written back as one restoring row, so the timeline stays
     * contiguous without the app inventing a stretch. A period still in force has no end — a null [end] is the
     * ordinary case, and an end on the newest period is refused because nothing would follow it.
     *
     * **A new period that restates the goal in force writes nothing** (B140, B224); an end is a statement, so a
     * finished stretch is written even when its goal repeats.
     *
     * Refused with its reason: a day after today, an end not after its start, a moved start onto a day another
     * period begins, a start that crosses a neighbour, and an end past the boundary that follows it.
     */
    suspend fun savePeriod(
        metricId: String,
        id: String?,
        start: LocalDate,
        end: LocalDate?,
        goal: WeightGoal?,
    ): DataResult<Unit>

    /** The same for a metric whose goal is one number — every metric but weight (ROADMAP N39). */
    suspend fun setLevelGoal(metricId: String, value: Double?): DataResult<Unit>

    /**
     * Removes one period, so the stretch it covered merges into the period before it (ROADMAP N215).
     *
     * **The lifter's correction, not the app's tidy-up**: removing the newest period is how a change that was
     * not a change is taken back, and removing the last one leaves the metric with no goal rather than a period
     * saying there is none — [restorePeriod] is what the snackbar offers straight after.
     */
    suspend fun removePeriod(id: String): DataResult<Unit>

    /** Puts a removed period back, **id and all**, which is what the removal's undo is (ROADMAP N215). */
    suspend fun restorePeriod(metricId: String, period: GoalPeriod): DataResult<Unit>
}

/** The kind's stored name, or null for a goal that states none (a plain level target). */
internal fun WeightGoal?.storedKind(): String? = this?.kind()?.name

/** The weight this goal states, as the stored column holds it (ROADMAP N135). */
internal fun WeightGoal?.storedValue(): Double? = this?.statedWeightGrams()?.toDouble()
