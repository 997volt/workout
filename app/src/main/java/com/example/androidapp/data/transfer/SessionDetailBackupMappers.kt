package com.example.androidapp.data.transfer

import com.example.androidapp.data.local.SessionExerciseJointEntity
import com.example.androidapp.data.local.SessionSoreMuscleEntity

/**
 * The session-detail tables' half of the backup codec (ROADMAP N62, N63).
 *
 * Split out of [BackupMappers] at the file ceiling the project enforces, the way
 * [ProgramBackupMappers] carries the program tables' half: the two tables N62 and N63 added are one
 * subject — what a session reported about the body — and keeping their field-for-field pairs
 * together is the same contract [BackupMappers] states, in a file that can hold it.
 *
 * Both directions, so an export followed by an import is a no-op on the data.
 */

internal fun SessionSoreMuscleEntity.toDto() = SessionSoreMuscleDto(
    id = id,
    sessionId = sessionId,
    muscle = muscle,
    score = score,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun SessionSoreMuscleDto.toEntity() = SessionSoreMuscleEntity(
    id = id,
    sessionId = sessionId,
    muscle = muscle,
    score = score,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun SessionExerciseJointEntity.toDto() = SessionExerciseJointDto(
    id = id,
    sessionExerciseId = sessionExerciseId,
    joint = joint,
    side = side,
    score = score,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

internal fun SessionExerciseJointDto.toEntity() = SessionExerciseJointEntity(
    id = id,
    sessionExerciseId = sessionExerciseId,
    joint = joint,
    side = side,
    score = score,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
