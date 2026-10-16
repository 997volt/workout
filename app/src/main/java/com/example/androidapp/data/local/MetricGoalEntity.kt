package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One **period** of one metric's goal (ROADMAP N137).
 *
 * The store used to be a newline-joined `metricId=value` string in preferences, **rewritten on every change**,
 * so the past did not exist: changing a goal silently re-judged the whole past, and "did I hit the cut I set in
 * March" had no answer because March's goal was gone. A row per period is what makes the past a fact rather
 * than a lost setting, and it is also what lets the chart draw the line that actually applied.
 *
 * **The period runs from [startedAt] to the next row's**, and the newest row per metric is the one that is
 * open. Timestamps are the store's (the caller's clock), not a date, because a goal starts when the lifter
 * changes it rather than at midnight.
 *
 * [startedAt] is **nullable on purpose**: the migration cannot know when the goal that was already stored was
 * set, so the first row records its start as unknown rather than inventing the moment of the upgrade.
 * Inventing that date would be inventing history, which is the thing this feature exists to stop.
 *
 * [kind] is N135's goal kind **by name** (the app's rule for every enum), or null for a goal written before
 * kinds existed — which reads as a target, exactly what a level goal was. [value] is the weight the goal
 * states in stored units, or null for a direction-only goal. A row with **neither** is a period of no goal:
 * clearing one closes the period rather than deleting the row, so the history keeps what was asked for and
 * when it stopped being asked for.
 */
@Entity(
    tableName = "metric_goals",
    indices = [Index(value = ["metricId"])],
)
data class MetricGoalEntity(
    @PrimaryKey val id: String,
    val metricId: String,
    val kind: String?,
    val value: Double?,
    val startedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)
