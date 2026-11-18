package com.example.androidapp.data.transfer

import com.example.androidapp.data.local.SessionPlanSetEntity

/**
 * Entity <-> backup DTO for the prescription a session started under (ROADMAP B471).
 *
 * Its own file so [BackupMappers] stays under the function ceiling, the same reason the program
 * tables live in [ProgramBackupMappers]. Field for field: every column here is what the stall and
 * volume rules read, so a dropped one changes a restored session's judgement.
 */
internal fun SessionPlanSetEntity.toDto() = SessionPlanSetDto(
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRepsCurrent = targetRepsCurrent,
    targetDurationSeconds = targetDurationSeconds,
    targetRpeHalves = targetRpeHalves,
)

internal fun SessionPlanSetDto.toEntity() = SessionPlanSetEntity(
    sessionExerciseId = sessionExerciseId,
    setIndex = setIndex,
    role = role,
    targetWeightGrams = targetWeightGrams,
    targetAssistanceGrams = targetAssistanceGrams,
    targetRepsMin = targetRepsMin,
    targetRepsMax = targetRepsMax,
    targetRepsCurrent = targetRepsCurrent,
    targetDurationSeconds = targetDurationSeconds,
    targetRpeHalves = targetRpeHalves,
)
