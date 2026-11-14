package com.example.androidapp.domain

import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.PrescriptionHistory
import com.example.androidapp.domain.model.ProgressionPerformance
import com.example.androidapp.domain.model.ProgressionPlanSet
import com.example.androidapp.domain.model.ProgressionPrompt
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TemplateExercise
import com.example.androidapp.domain.model.TemplateSession
import com.example.androidapp.domain.model.VolumeReason
import com.example.androidapp.domain.model.VolumeSuggestion
import com.example.androidapp.domain.model.progressionPromptFor
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.domain.model.stall
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TemplateSetEdit
import com.example.androidapp.domain.repository.TrendsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the plan has earned a working set back (ROADMAP N311).
 *
 * Two triggers, in this order: the **extra set** — the plan prescribes N working sets and each of the last
 * two sessions logged N+1, which is read as "the plan is one set short" rather than a one-off; and the
 * **comfortable** — every working set earned a step through the progression offer for three sessions running,
 * the offer's own gate rather than the RPE field (decision 1b).
 *
 * The gates are the same on both surfaces: no open stall (a deload is the answer to a stall, not more work),
 * the plan's working-set count below [MAX_WORK_SETS], and a working plan that names sets at all. The card
 * holds while the rule holds; the finish fires once when the pattern reaches its threshold — the extra-set
 * first, one suggestion at a time.
 *
 * The read is the trends' finished sessions and the plan; nothing is stored. The write is the repository's
 * append, which the caller makes only when the lifter presses Add.
 */
@Singleton
class PlanVolumeFinder @Inject constructor(
    private val programs: ProgramRepository,
    private val templates: TemplateRepository,
    private val trends: TrendsRepository,
) {

    /** Every suggestion in [programId]'s plans, in program then plan order (the card's read). */
    suspend fun suggestionsForProgram(programId: String): DataResult<List<VolumeSuggestion>> = dataResultOf {
        val templateIds = programs.observeSlots(programId).first()
            .mapNotNull { it.templateId }
            .distinct()
        val reads = templateIds.map { suggestionsForTemplate(it) }
        reads.filterIsInstance<DataResult.Failure>()
            .firstOrNull()
            ?.let { throw it.error.asThrowable() }
        reads.filterIsInstance<DataResult.Success<List<VolumeSuggestion>>>()
            .flatMap { it.data }
            .distinctBy { it.templateExerciseId }
    }

    /** Every suggestion in one plan's exercises, in the plan's exercise order. */
    suspend fun suggestionsForTemplate(templateId: String): DataResult<List<VolumeSuggestion>> =
        dataResultOf {
            val plan = templates.observeExercises(templateId).first()
            val sessions = when (val read = trends.templateSessions(templateId)) {
                is DataResult.Failure -> throw read.error.asThrowable()
                is DataResult.Success -> read.data
            }
            plan.mapNotNull { exercise -> planSuggestion(exercise, sessions) }
        }

    /**
     * The finish's one-shot, or null.
     *
     * [current] is the session that just ended (the *Done* row's prompt, so it is the same judgement the
     * question used); [templateId] and [exerciseId] name the plan it belongs to. The pattern "reaches" its
     * threshold here: the session before it must not already have held, or the earlier *Done* would have
     * said it.
     */
    suspend fun doneSuggestion(
        templateId: String,
        exerciseId: String,
        current: ProgressionPrompt,
    ): DataResult<VolumeSuggestion?> = dataResultOf {
        val plan = templates.observeExercises(templateId).first()
        val exercise = plan.firstOrNull { it.exerciseId == exerciseId } ?: return@dataResultOf null
        val sessions = when (val read = trends.templateSessions(templateId)) {
            is DataResult.Failure -> throw read.error.asThrowable()
            is DataResult.Success -> read.data
        }
        val previous = sessions.map { session -> promptFor(exercise, session) }
        doneSuggestion(exercise, current, previous)
    }

    /**
     * The pure finish decision: [previous] is oldest first, without the session in [current].
     *
     * Each early `return null` is one gate (the cap, the stall, the pattern) stated where it is read; the
     * suppression keeps those gates in the rule's order instead of nesting them.
     */
    @Suppress("ReturnCount")
    fun doneSuggestion(
        exercise: TemplateExercise,
        current: ProgressionPrompt,
        previous: List<ProgressionPrompt>,
    ): VolumeSuggestion? {
        val planned = exercise.progressionSets()
        val work = planned.workSets()
        if (!isOpen(work, exercise, previous)) return null

        // The current makes the pair/trio; the session behind it must not have held, or the threshold
        // was reached on an earlier Done and the card carries it instead.
        val previousHeldExtra = previous.lastOrNull()?.extraCount() == 1
        val beforePreviousHeldExtra = previous.getOrNull(previous.size - 2)?.extraCount() == 1
        if (current.extraCount() == EXTRA_SET_COUNT && previousHeldExtra && !beforePreviousHeldExtra) {
            return suggestion(exercise, work, VolumeReason.EXTRA_SET, current.extraPerformed())
        }
        val previousTwoEarned = previous.takeLast(EXTRA_SESSIONS).let { last ->
            last.size == EXTRA_SESSIONS && last.all { it.everyWorkSetEarned() }
        }
        val fourthBackEarned =
            previous.getOrNull(previous.size - COMFORTABLE_SESSIONS)?.everyWorkSetEarned() == true
        if (current.everyWorkSetEarned() && previousTwoEarned && !fourthBackEarned) {
            return suggestion(exercise, work, VolumeReason.COMFORTABLE, null)
        }
        return null
    }

    @Suppress("ReturnCount")
    private fun planSuggestion(
        exercise: TemplateExercise,
        sessions: List<TemplateSession>,
    ): VolumeSuggestion? {
        val planned = exercise.progressionSets()
        val work = planned.workSets()
        if (!isOpen(work, exercise, sessions.map { promptFor(exercise, it, planned) })) return null

        val prompts = sessions.map { promptFor(exercise, it, planned) }
        if (prompts.takeLast(EXTRA_SESSIONS).size == EXTRA_SESSIONS &&
            prompts.takeLast(EXTRA_SESSIONS).all { it.extraCount() == 1 }
        ) {
            return suggestion(
                exercise,
                work,
                VolumeReason.EXTRA_SET,
                prompts.last().extraPerformed(),
            )
        }
        if (prompts.takeLast(COMFORTABLE_SESSIONS).size == COMFORTABLE_SESSIONS &&
            prompts.takeLast(COMFORTABLE_SESSIONS).all { it.everyWorkSetEarned() }
        ) {
            return suggestion(exercise, work, VolumeReason.COMFORTABLE, null)
        }
        return null
    }

    /**
     * The shared gates: the plan has a working set, it is under the cap, and it is not stalled.
     *
     * The stall read needs the same history the stall card uses, and an Off movement never stalls (N304), so
     * an Off plan with the pattern still earns its suggestion — the volume rule is about the plan, not the
     * switch.
     */
    private fun isOpen(
        work: List<ProgressionPlanSet>,
        exercise: TemplateExercise,
        history: List<ProgressionPrompt>,
    ): Boolean {
        if (work.isEmpty() || work.size >= MAX_WORK_SETS) return false
        return stall(exercise, history) == null
    }

    private fun stall(exercise: TemplateExercise, prompts: List<ProgressionPrompt>): PlanStall? {
        // The prompts are per session, so their planned halves are the same plan; the stall rule reads the
        // performed halves, which is what makes this the same judgement as the stall card's.
        val planned = prompts.firstOrNull()?.sets.orEmpty().mapNotNull { it.planned }
        if (planned.isEmpty()) return null
        val performedBySession = prompts.map { prompt ->
            prompt.sets.filter { it.planned != null }.mapNotNull { it.performed }
        }.filter { it.isNotEmpty() }
        return PrescriptionHistory(
            exerciseName = exercise.exerciseName,
            planned = planned,
            performedBySession = performedBySession,
            progressionMode = exercise.progressionMode,
            templateId = exercise.templateId,
            templateExerciseId = exercise.id,
            exerciseId = exercise.exerciseId,
        ).stall()
    }

    private fun promptFor(
        exercise: TemplateExercise,
        session: TemplateSession,
        planned: List<ProgressionPlanSet> = exercise.progressionSets(),
    ): ProgressionPrompt {
        val performed = session.performed
            .firstOrNull { it.exerciseId == exercise.exerciseId }
            ?.sets
            .orEmpty()
        return progressionPromptFor(planned, performed)
    }

    @Suppress("ReturnCount")
    private fun suggestion(
        exercise: TemplateExercise,
        work: List<ProgressionPlanSet>,
        reason: VolumeReason,
        extra: ProgressionPerformance?,
    ): VolumeSuggestion? {
        val edit = when (reason) {
            VolumeReason.EXTRA_SET -> {
                val performed = extra ?: return null
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = performed.weightGrams.takeIf { it > 0 },
                    targetAssistanceGrams = performed.assistanceGrams.takeIf { it > 0 },
                    targetRepsMin = performed.reps.takeIf { performed.durationSeconds == null },
                    targetRepsMax = performed.reps.takeIf { performed.durationSeconds == null },
                    targetDurationSeconds = performed.durationSeconds,
                )
            }

            VolumeReason.COMFORTABLE -> {
                val last = exercise.sets.lastOrNull { it.role.isWork && !it.role.isRung }
                    ?: return null
                TemplateSetEdit(
                    role = SetType.NORMAL,
                    targetWeightGrams = last.targetWeightGrams,
                    targetAssistanceGrams = last.targetAssistanceGrams,
                    targetRepsMin = last.targetRepsMin,
                    targetRepsMax = last.targetRepsMax,
                    targetRepsCurrent = last.targetRepsCurrent,
                    targetDurationSeconds = last.targetDurationSeconds,
                )
            }
        }
        return VolumeSuggestion(
            templateId = exercise.templateId,
            templateExerciseId = exercise.id,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.exerciseName,
            reason = reason,
            plannedSets = work.size,
            edit = edit,
        )
    }

    private fun List<ProgressionPlanSet>.workSets(): List<ProgressionPlanSet> =
        filter { it.role.isWork && !it.role.isRung }

    private fun ProgressionPrompt.extraCount(): Int = sets.count { it.planned == null }

    private fun ProgressionPrompt.extraPerformed(): ProgressionPerformance? =
        sets.firstOrNull { it.planned == null }?.performed

    private fun ProgressionPrompt.everyWorkSetEarned(): Boolean {
        val work = sets.filter {
            val role = it.planned?.role ?: return@filter false
            role.isWork && !role.isRung
        }
        return work.isNotEmpty() && work.all { it.offer != null }
    }

    private companion object {
        /** The N311 cap: six working sets is the most the app will ever suggest (decision 8b). */
        const val MAX_WORK_SETS = 6

        /** Two sessions make the extra-set pattern; one is a day that ran long. */
        const val EXTRA_SESSIONS = 2

        /** Exactly one extra set above the plan: a session that logged two is a different story. */
        const val EXTRA_SET_COUNT = 1

        /** Three sessions make the comfortable pattern (decision 1b). */
        const val COMFORTABLE_SESSIONS = 3
    }
}
