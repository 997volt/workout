package com.example.androidapp.ui.programs

import com.example.androidapp.domain.Weight
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.getOrNull
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.TemplateSet
import com.example.androidapp.domain.repository.DeloadRepository
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.SettingsRepository
import com.example.androidapp.domain.repository.TemplateRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One read of everything the deload creator needs (ROADMAP N147).
 *
 * A snapshot rather than a flow: the screen is a draft, the program cannot be edited underneath it on a
 * phone, and every control re-derives from this one read. It is extracted from the ViewModel so the
 * constructor stays the four dependencies the screen has rather than the five repositories this read
 * happens to touch.
 */
@Singleton
class DeloadSourceLoader @Inject constructor(
    private val programRepository: ProgramRepository,
    private val templateRepository: TemplateRepository,
    private val workoutRepository: WorkoutRepository,
    private val deloadRepository: DeloadRepository,
    private val settingsRepository: SettingsRepository,
) {

    /**
     * The program's days as the creator reads them, or null when the program is gone (N147).
     *
     * A slot whose template plans no exercises is not a day: there is nothing to tick, and a day that
     * writes an empty workout would be a plan the lifter has to clean up.
     */
    suspend fun load(programId: String, weekStart: LocalDate): DeloadSourceSnapshot? {
        val program = programRepository.observeProgram(programId).first() ?: return null
        val appUnit = settingsRepository.observeWeightUnit().first()
        val slots = programRepository.observeSlots(programId).first().sortedBy { it.position }
        val hasPlan = deloadRepository.observeWeekPlans(weekStart).first()
            .firstOrNull { it.programId == programId }
            ?.days
            ?.isNotEmpty() == true

        val days = slots.mapNotNull { slot ->
            val exercises = templateRepository.observeExercises(slot.templateId).first()
            if (exercises.isEmpty()) return@mapNotNull null
            DeloadSourceDay(
                sourceSlotId = slot.id,
                name = slot.templateName,
                weekday = slot.weekday,
                exercises = exercises.map { exercise ->
                    val unit = exercise.weightUnit ?: appUnit
                    DeloadSourceExercise(
                        templateExerciseId = exercise.id,
                        exerciseId = exercise.exerciseId,
                        name = exercise.exerciseName,
                        restSeconds = exercise.restSeconds,
                        techniqueNote = exercise.techniqueNote,
                        supersetGroup = exercise.supersetGroup,
                        sets = exercise.sets,
                        weightUnit = unit,
                        stepGrams = Weight.stepGramsFor(exercise.stepGrams, unit),
                        // No session is current when a plan is being built; the read is the last
                        // finished one for the exercise, exactly as the workout's own prefill is.
                        previous = workoutRepository
                            .previousPerformance(exercise.exerciseId, currentSessionId = NO_CURRENT_SESSION)
                            .getOrNull()
                            ?: PreviousPerformance(emptyList()),
                    )
                },
            )
        }
        return DeloadSourceSnapshot(
            programName = program.name,
            days = days,
            hasExistingPlan = hasPlan,
        )
    }

    private companion object {
        /**
         * The "current session" a plan-building history read is not (P1.3).
         *
         * The prefill read excludes the session in progress; while a plan is built there is none, and
         * the empty id matches no row, so the read is simply the exercise's last finished session.
         */
        const val NO_CURRENT_SESSION = ""
    }
}

/** What [DeloadSourceLoader] found, and what the screen shows (ROADMAP N147). */
data class DeloadSourceSnapshot(
    val programName: String,
    val days: List<DeloadSourceDay>,
    val hasExistingPlan: Boolean,
)

/**
 * One planned exercise as the creator read it, before any control narrows it (ROADMAP N147).
 *
 * It carries the last performance because the rule falls back to it where the plan states no load or no
 * reps, and the weight unit and step because the derived load has to be a number the exercise can be
 * shown and loaded in (N64, N77).
 */
data class DeloadSourceExercise(
    val templateExerciseId: String,
    val exerciseId: String,
    val name: String,
    val restSeconds: Int?,
    val techniqueNote: String?,
    val supersetGroup: Int?,
    val sets: List<TemplateSet>,
    val weightUnit: WeightUnit,
    val stepGrams: Long,
    val previous: PreviousPerformance,
)

/** One of the program's days as the creator read it (ROADMAP N147). */
data class DeloadSourceDay(
    val sourceSlotId: String,
    val name: String,
    val weekday: DayOfWeek?,
    val exercises: List<DeloadSourceExercise>,
) {
    /** The weekday it is offered on once the lifter's move is taken into account (N147). */
    fun weekdayAfter(weekdayOf: (DeloadSourceDay) -> DayOfWeek?): DayOfWeek? = weekdayOf(this)
}
