package com.example.androidapp.domain

import com.example.androidapp.domain.model.PlanStall
import com.example.androidapp.domain.model.PrescriptionHistory
import com.example.androidapp.domain.model.STALL_SESSIONS
import com.example.androidapp.domain.model.progressionSets
import com.example.androidapp.domain.model.stall
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.TrendsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The first stopped prescription in a program (ROADMAP N237, B351).
 *
 * The rule is the domain's ([stall]); this is the read it needs: each slot's plan, and the sessions that
 * trained each of its movements. Extracted from the program editor so the weekly review can ask the same
 * question of the same rules rather than a second copy (N247).
 *
 * **A failed read is a failure, not "no stall."** The first cut swallowed the trends error into an empty
 * session list, so a storage fault made the editor and the week review say the plan was moving. The result
 * is a [DataResult] now, and the callers show the error rather than inventing an answer.
 *
 * One stall, not a list: the app is suggesting a week off, and a screen of diagnoses is not the suggestion it
 * set out to make.
 */
@Singleton
class PlanStallFinder @Inject constructor(
    private val programs: ProgramRepository,
    private val templates: TemplateRepository,
    private val trends: TrendsRepository,
) {

    /** The first stall among [programId]'s plans, null when nothing has stopped moving, or the read's failure. */
    suspend fun firstStall(programId: String): DataResult<PlanStall?> = dataResultOf {
        val templateIds = programs.observeSlots(programId).first()
            .mapNotNull { it.templateId }
            .distinct()
        val reads = templateIds.map { firstStallInTemplate(it) }
        // Every plan is read before the answer is chosen: one failure is the answer, rather than a
        // short-circuit that would hide a later read's fault behind an earlier "no stall".
        reads.filterIsInstance<DataResult.Failure>()
            .firstOrNull()
            ?.let { throw it.error.asThrowable() }
        reads.filterIsInstance<DataResult.Success<PlanStall?>>()
            .firstNotNullOfOrNull { it.data }
    }

    private suspend fun firstStallInTemplate(templateId: String): DataResult<PlanStall?> = dataResultOf {
        val planned = templates.observeExercises(templateId).first()
        val sessions = when (val read = trends.templateSessions(templateId)) {
            is DataResult.Failure -> throw read.error.asThrowable()
            is DataResult.Success -> read.data
        }
        // Three sessions is the rule's own threshold (N237), and a plan with fewer cannot have one yet.
        planned.takeIf { it.isNotEmpty() && sessions.size >= STALL_SESSIONS }
            ?.firstNotNullOfOrNull { exercise ->
                // One entry per session that **trained** the movement: absence is not a failure.
                val performed = sessions.mapNotNull { session ->
                    session.performed
                        .firstOrNull { it.exerciseId == exercise.exerciseId }
                        ?.sets
                }
                PrescriptionHistory(
                    exerciseName = exercise.exerciseName,
                    planned = exercise.progressionSets(),
                    performedBySession = performed,
                ).stall()
            }
    }
}
