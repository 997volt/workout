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
 * The first stopped prescription in a program (ROADMAP N237).
 *
 * The rule is the domain's ([stall]); this is the read it needs: each slot's plan, and the sessions that
 * trained each of its movements. Extracted from the program editor so the weekly review can ask the same
 * question of the same rules rather than a second copy (N247).
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

    /** The first stall among [programId]'s plans, or null when nothing has stopped moving. */
    suspend fun firstStall(programId: String): PlanStall? =
        programs.observeSlots(programId).first()
            .mapNotNull { it.templateId }
            .distinct()
            .firstNotNullOfOrNull { templateId -> firstStallInTemplate(templateId) }

    private suspend fun firstStallInTemplate(templateId: String): PlanStall? {
        val planned = templates.observeExercises(templateId).first()
        val sessions = (trends.templateSessions(templateId) as? DataResult.Success)?.data.orEmpty()
        // Three sessions is the rule's own threshold (N237), and a plan with fewer cannot have one yet.
        return planned.takeIf { it.isNotEmpty() && sessions.size >= STALL_SESSIONS }
            ?.firstNotNullOfOrNull { exercise ->
                // One entry per session that **trained** the movement: absence is not a failure (N237).
                val performed = sessions.mapNotNull { session ->
                    session.performed.firstOrNull { it.exerciseId == exercise.exerciseId }?.sets
                }
                PrescriptionHistory(
                    exerciseName = exercise.exerciseName,
                    planned = exercise.progressionSets(),
                    performedBySession = performed,
                ).stall()
            }
    }
}
