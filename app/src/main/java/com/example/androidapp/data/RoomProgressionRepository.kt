package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.ProgressionBaselineEntity
import com.example.androidapp.data.local.ProgressionDao
import com.example.androidapp.data.local.ProgressionWriteEntity
import com.example.androidapp.data.local.TemplateSetDao
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.ProgressionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [ProgressionRepository] (ROADMAP N299).
 *
 * The plan's write path is [RoomTemplateRepository.applyProgression], because it owns the
 * validation and the transaction that puts a step and its record together. This class is the other
 * half: the baseline a reset reads, and the undo that has to check the plan still holds what the step
 * wrote before it overwrites anything.
 */
@Singleton
class RoomProgressionRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : ProgressionRepository {

    private val writes: ProgressionDao = database.progressionDao()
    private val sets: TemplateSetDao = database.templateSetDao()
    private val programs = database.programDao()

    /**
     * The switch and its baseline are one act (ROADMAP N299).
     *
     * Enabling without a snapshot would be a program that says auto is on with nothing to reset to;
     * disabling leaves the snapshot alone, because the point the first switch was made from is still
     * the honest answer to "where did this start". Re-enabling captures the current targets again —
     * the lifter may have edited the plan while auto was off, and the later baseline is the one they
     * meant.
     */
    override suspend fun setAutoProgression(programId: String, enabled: Boolean): DataResult<Unit> =
        dataResultOf {
            val now = timeSource.nowEpochMillis()
            database.withTransaction {
                val updated = programs.setAutoProgression(programId, enabled, now)
                if (updated == 0) throw NotFoundException("program $programId")
                if (enabled) {
                    writes.clearBaselines(programId)
                    writes.insertBaselines(
                        programs.findProgramSets(programId).map { it.toBaseline(programId, now) },
                    )
                }
            }
        }

    override suspend fun autoProgramForTemplate(templateId: String): DataResult<String?> =
        dataResultOf { programs.findAutoProgramForTemplate(templateId) }

    override suspend fun baselineCapturedAt(programId: String): DataResult<Long?> =
        dataResultOf { writes.baselineCapturedAt(programId) }

    /**
     * Restores every planned set the baseline names, skipping one that no longer exists.
     *
     * A baseline is a memory of the switch being turned on, not a second plan: a set the lifter has
     * since deleted is left deleted rather than resurrected by a reset.
     */
    override suspend fun resetToBaseline(programId: String): DataResult<Int> = dataResultOf {
        val baselines = writes.findBaselines(programId)
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            var restored = 0
            baselines.forEach { baseline ->
                val set = sets.findTemplateSet(baseline.templateSetId) ?: return@forEach
                val updated = set.copy(
                    targetWeightGrams = baseline.weightGrams,
                    targetRepsCurrent = baseline.repsCurrent,
                    targetDurationSeconds = baseline.durationSeconds,
                    updatedAt = now,
                )
                if (sets.updateTemplateSet(updated) > 0) restored++
            }
            restored
        }
    }

    /**
     * Takes one step back, and only while the plan still holds what the step wrote (ROADMAP N299).
     *
     * The equality check is the whole safety property: a later step or the lifter's own edit has moved
     * the set since, and restoring the old target would silently overwrite that work. A step that was
     * already undone is refused for the same reason — there is nothing left of it to undo.
     */
    override suspend fun undo(writeId: String): DataResult<Unit> = dataResultOf {
        val write = writes.findWrite(writeId)
            ?: throw NotFoundException("progression write $writeId")
        if (write.revertedAt != null) throw InvalidInputException("this step has already been undone")
        val set = sets.findTemplateSet(write.templateSetId)
            ?: throw NotFoundException("template set ${write.templateSetId}")
        if (!set.holds(write)) {
            throw InvalidInputException("the plan has changed since this step, so it cannot be undone")
        }
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            val restored = set.copy(
                targetWeightGrams = write.beforeWeightGrams,
                targetRepsCurrent = write.beforeRepsCurrent,
                targetDurationSeconds = write.beforeDurationSeconds,
                updatedAt = now,
            )
            if (sets.updateTemplateSet(restored) == 0) {
                throw NotFoundException("template set ${write.templateSetId}")
            }
            if (writes.markReverted(writeId, now) == 0) {
                throw InvalidInputException("this step has already been undone")
            }
        }
    }
}

/** True when this set still carries the whole target the write left it with. */
private fun TemplateSetEntity.holds(write: ProgressionWriteEntity): Boolean =
    targetWeightGrams == write.afterWeightGrams &&
        targetRepsCurrent == write.afterRepsCurrent &&
        targetDurationSeconds == write.afterDurationSeconds

/** What the switch snapshots for one planned set (ROADMAP N299). */
private fun TemplateSetEntity.toBaseline(programId: String, now: Long) = ProgressionBaselineEntity(
    programId = programId,
    templateSetId = id,
    weightGrams = targetWeightGrams,
    repsCurrent = targetRepsCurrent,
    durationSeconds = targetDurationSeconds,
    capturedAt = now,
)
