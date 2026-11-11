package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.ProgramDeloadDayEntity
import com.example.androidapp.data.local.ProgramDeloadEntity
import com.example.androidapp.data.local.TemplateEntity
import com.example.androidapp.data.local.TemplateExerciseEntity
import com.example.androidapp.data.local.TemplateSetEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.data.local.toDeloadWeeks
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.model.DELOAD_TARGET_RPE_HALVES
import com.example.androidapp.domain.model.DeloadDay
import com.example.androidapp.domain.model.DeloadWeek
import com.example.androidapp.domain.model.ProgramSchedule
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.repository.DeloadCreation
import com.example.androidapp.domain.repository.DeloadDayInput
import com.example.androidapp.domain.repository.DeloadExerciseInput
import com.example.androidapp.domain.repository.DeloadRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed [DeloadRepository] (ROADMAP N147).
 *
 * The arithmetic stays in [com.example.androidapp.domain.model.deloadSetTargets]; this class stores what
 * the screen showed. The write is one transaction because its four tables are one act: a mark with no
 * plan, a plan with no mark, or a day naming a template that was never written would each be a week the
 * screens would have to guess about.
 */
@Singleton
class RoomDeloadRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : DeloadRepository {

    private val deloadDao = database.programDeloadDao()
    private val programDao = database.programDao()
    private val templateDao = database.templateDao()
    private val templateSetDao = database.templateSetDao()

    override fun observeWeekPlans(weekStart: LocalDate): Flow<List<DeloadWeek>> =
        deloadDao.observeDeloadDaysForWeek(weekStart.toEpochDay()).map { rows -> rows.toDeloadWeeks() }

    override fun observeMarkedPrograms(weekStart: LocalDate): Flow<Set<String>> =
        deloadDao.observeDeloadsForWeek(weekStart.toEpochDay())
            .map { rows -> rows.map { it.programId }.toSet() }

    override fun observeDeloadWorkout(templateId: String): Flow<Boolean> =
        deloadDao.observeDeloadDayCount(templateId).map { it > 0 }

    override suspend fun findLastPlan(programId: String, beforeWeek: LocalDate): DeloadWeek? {
        // The newest **built** week, not the newest mark (B405): a week exempted in one tap has no days
        // and must not hide the plan before it.
        val week = deloadDao.findLatestPlanWeek(programId, beforeWeek.toEpochDay()) ?: return null
        return deloadDao.findDeloadDaysForWeek(week)
            // The one-week read returns every program's days for the week; the last plan is this program's.
            .filter { it.programId == programId }
            .takeIf { it.isNotEmpty() }
            ?.let { rows ->
                DeloadWeek(
                    programId = programId,
                    weekStart = LocalDate.ofEpochDay(week),
                    days = rows.map { row ->
                        DeloadDay(
                            id = row.id,
                            programId = row.programId,
                            templateId = row.templateId,
                            templateName = row.templateName,
                            exerciseCount = row.exerciseCount,
                            position = row.position,
                            weekday = row.weekday,
                            exerciseIds = templateDao.findPlannedExercises(row.templateId)
                                .map { it.exerciseId },
                        )
                    },
                )
            }
    }

    override suspend fun findLastMark(programId: String, beforeWeek: LocalDate): LocalDate? =
        deloadDao.findLatestDeloadBefore(programId, beforeWeek.toEpochDay())
            ?.let { LocalDate.ofEpochDay(it.weekStart) }

    override suspend fun createDeload(
        programId: String,
        weekStart: LocalDate,
        days: List<DeloadDayInput>,
    ): DataResult<DeloadCreation> = dataResultOf {
        if (programDao.findProgram(programId) == null) {
            throw NotFoundException("program $programId")
        }
        requireUsable(days)

        // The creator plans the next week (N158), so every deload day is in the future; the current week is
        // still accepted for older plans and the adherence's own mark. Anything further ahead is refused
        // here, not only by the screen, so the one-week window holds for every caller.
        val todayWeek = ProgramSchedule.weekStartOf(
            timeSource.now().atZone(ZoneId.systemDefault()).toLocalDate(),
        )
        if (weekStart != todayWeek && weekStart != todayWeek.plusWeeks(1)) {
            throw InvalidInputException("A deload week starts this week or next.")
        }

        val now = timeSource.nowEpochMillis()
        val existing = deloadDao.findLiveDeload(programId, weekStart.toEpochDay())
        val created = mutableListOf<String>()

        database.withTransaction {
            val deloadId = ensureMark(programId, weekStart, existing, now)
            if (existing != null) {
                // Rebuilding replaces the week's days; the templates the last plan wrote stay, because a
                // rebuild is not a reason to delete the lifter's workouts (N147).
                deloadDao.softDeleteDeloadDays(deloadId, now)
                deloadDao.touchDeload(deloadId, now)
            }
            days.forEach { day ->
                if (day.exercises.isEmpty()) return@forEach
                created += writeDay(deloadId = deloadId, day = day, position = created.size, now = now)
            }
        }

        DeloadCreation(weekStart = weekStart, templateIds = created)
    }

    /** The week's mark, reused when it is already there or inserted in this transaction (N147). */
    private suspend fun ensureMark(
        programId: String,
        weekStart: LocalDate,
        existing: ProgramDeloadEntity?,
        now: Long,
    ): String {
        if (existing != null) return existing.id
        val id = UUID.randomUUID().toString()
        deloadDao.insertDeload(
            ProgramDeloadEntity(
                id = id,
                programId = programId,
                weekStart = weekStart.toEpochDay(),
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        return id
    }

    /** One day's ordinary template, its exercises and sets, and the row that points at it (N147). */
    private suspend fun writeDay(
        deloadId: String,
        day: DeloadDayInput,
        position: Int,
        now: Long,
    ): String {
        val templateId = UUID.randomUUID().toString()
        templateDao.insertTemplate(
            TemplateEntity(
                id = templateId,
                name = day.name.trim(),
                versionOf = null,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        day.exercises.forEachIndexed { exercisePosition, exercise ->
            writeExercise(templateId, exercisePosition, exercise, now)
        }
        deloadDao.insertDeloadDay(
            ProgramDeloadDayEntity(
                id = UUID.randomUUID().toString(),
                deloadId = deloadId,
                templateId = templateId,
                position = position,
                weekday = day.weekday,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        return templateId
    }

    private suspend fun writeExercise(
        templateId: String,
        position: Int,
        exercise: DeloadExerciseInput,
        now: Long,
    ) {
        val plannedId = UUID.randomUUID().toString()
        templateDao.insertTemplateExercise(
            TemplateExerciseEntity(
                id = plannedId,
                templateId = templateId,
                exerciseId = exercise.exerciseId,
                position = position,
                restSeconds = exercise.restSeconds,
                techniqueNote = exercise.techniqueNote,
                // A deload effort, not the plan's and not the session's 9.0 default (N300): leaving it
                // blank opened the workout's effort field on the opposite of a deload.
                targetRpeHalves = DELOAD_TARGET_RPE_HALVES,
                supersetGroup = exercise.supersetGroup,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
            ),
        )
        exercise.sets.forEachIndexed { index, set ->
            templateSetDao.insertTemplateSet(
                TemplateSetEntity(
                    id = UUID.randomUUID().toString(),
                    templateExerciseId = plannedId,
                    setIndex = index,
                    // The week's work is plain: no ramp, no rung (N147).
                    role = SetType.NORMAL,
                    targetWeightGrams = set.weightGrams,
                    targetAssistanceGrams = set.assistanceGrams,
                    targetRepsMin = if (set.durationSeconds == null) set.reps else null,
                    targetRepsMax = if (set.durationSeconds == null) set.reps else null,
                    targetRepsCurrent = null,
                    targetDurationSeconds = set.durationSeconds,
                    dropValueGrams = null,
                    note = null,
                    createdAt = now,
                    updatedAt = now,
                    deletedAt = null,
                ),
            )
        }
    }

    /**
     * Refuses an input no screen should produce, before the transaction opens.
     *
     * A day with no exercises is skipped by the write (its source could empty out between the read and the
     * tap); a week with nothing at all, a nameless day, or a set with no targets is a caller bug that would
     * otherwise write a plan nobody can train.
     */
    private fun requireUsable(days: List<DeloadDayInput>) {
        val problem = when {
            days.isEmpty() || days.none { it.exercises.isNotEmpty() } ->
                "Keep at least one exercise in the deload week."

            days.any { it.name.isBlank() } ->
                "A deload workout needs a name."

            days.any { day -> day.exercises.any { it.sets.isEmpty() } } ->
                "A deload exercise needs a set."

            else -> null
        }
        if (problem != null) throw InvalidInputException(problem)
    }
}
