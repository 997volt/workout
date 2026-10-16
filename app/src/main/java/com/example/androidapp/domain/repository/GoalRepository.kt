package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WeightGoal
import com.example.androidapp.domain.model.kind
import com.example.androidapp.domain.model.statedWeightGrams
import com.example.androidapp.domain.model.weightGoalOf
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * One **period** of a metric's goal (ROADMAP N137).
 *
 * The shape the rows are written in rather than a domain model: [kind] and [value] are exactly the two columns
 * a goal has, so a metric whose goal is a single number (every metric but weight) reads without being forced
 * through a weight-shaped type. [weightGoal] is the reading for the one that *is* a weight goal.
 *
 * [startedAt] is null for the first period of a goal that was already stored when periods arrived — the move
 * cannot know when it began, and inventing the upgrade's own timestamp would be inventing history.
 */
data class GoalPeriod(
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
     * Closes the open period and opens one asking for [goal] (ROADMAP N135's kinds).
     *
     * Null opens a period of **no goal** rather than deleting the open one: clearing is a change like any
     * other, and a deleted row would take the answer to "what was I asking for until today" with it.
     */
    suspend fun setGoal(metricId: String, goal: WeightGoal?): DataResult<Unit>

    /** The same for a metric whose goal is one number — every metric but weight (ROADMAP N39). */
    suspend fun setLevelGoal(metricId: String, value: Double?): DataResult<Unit>
}

/** The kind's stored name, or null for a goal that states none (a plain level target). */
internal fun WeightGoal?.storedKind(): String? = this?.kind()?.name

/** The weight this goal states, as the stored column holds it (ROADMAP N135). */
internal fun WeightGoal?.storedValue(): Double? = this?.statedWeightGrams()?.toDouble()
