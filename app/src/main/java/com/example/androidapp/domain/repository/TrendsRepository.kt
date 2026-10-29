package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.ExerciseTrendPoint
import com.example.androidapp.domain.model.TemplateSession
import com.example.androidapp.domain.model.TrendPoint
import kotlinx.coroutines.flow.Flow

/**
 * Reads the signals the app collects back out, as trends (ROADMAP N13).
 *
 * Read-only by design: nothing here writes, because a trend is a view of what was
 * already recorded rather than a thing the user maintains.
 */
interface TrendsRepository {

    /**
     * The series for one lift — or for a **head** and everything filed under it — over the most recent
     * [limit] finished sessions that recorded any of [exerciseIds], **oldest first** (ROADMAP N17, N97).
     *
     * The window counts *sessions*, not sets: a workout with eight sets of the lift is
     * one point on the chart, the same way N13's trends count sessions. It counts them across the whole
     * family, so a head's series is the family's sessions rather than each lift's window stitched together.
     */
    fun observeExerciseTrends(
        exerciseIds: List<String>,
        limit: Int = TREND_WINDOW,
    ): Flow<DataResult<List<ExerciseTrendPoint>>>

    /**
     * The most recent [limit] finished workouts that carry at least one signal,
     * **oldest first** — the order a chart is drawn in.
     *
     * Returns a [DataResult] like every other read (ROADMAP B4): a failure here is a
     * message on the screen, not an exception lost inside the flow.
     */
    fun observeTrends(limit: Int = DEFAULT_LIMIT): Flow<DataResult<List<TrendPoint>>>

    /**
     * The last [limit] finished sessions started from [templateId], with the sets each exercise logged
     * (ROADMAP N237).
     *
     * The stall rule's read: one plan's own sessions, oldest first. A session started from no template has no
     * row here at all, which is the rule's own scope — the plan is what a deload is about, so a session that
     * belongs to no plan cannot count for one.
     */
    suspend fun templateSessions(
        templateId: String,
        limit: Int = 12,
    ): DataResult<List<TemplateSession>>

    companion object {
        /**
         * Ten workouts: enough for a shape to appear, few enough that each point is
         * still a workout the user remembers rather than a smear of history.
         */
        const val DEFAULT_LIMIT = 10
    }
}

/** The default window: the last ten sessions that recorded the exercise. */
const val TREND_WINDOW = 10
