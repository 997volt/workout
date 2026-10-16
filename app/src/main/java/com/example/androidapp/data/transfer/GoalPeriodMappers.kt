package com.example.androidapp.data.transfer

import com.example.androidapp.data.local.MetricGoalEntity

/**
 * The goal periods' half of the entity <-> DTO mapping (ROADMAP N137).
 *
 * Its own file because [BackupMappers] had reached this project's function ceiling, and because the periods are
 * the one table whose three columns are all nullable for three different reasons — which is worth the room to
 * say.
 */
internal fun MetricGoalEntity.toDto() = GoalPeriodDto(
    id = id,
    metricId = metricId,
    kind = kind,
    value = value,
    startedAt = startedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun GoalPeriodDto.toEntity() = MetricGoalEntity(
    id = id,
    metricId = metricId,
    kind = kind,
    value = value,
    startedAt = startedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
