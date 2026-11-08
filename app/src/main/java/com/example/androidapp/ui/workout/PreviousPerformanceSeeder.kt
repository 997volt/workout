package com.example.androidapp.ui.workout

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.SessionExercise
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.domain.repository.ProgramRepository
import com.example.androidapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.update
import java.time.ZoneId

// The history lookup a workout opens with (ROADMAP N73, P3.8; moved out of ActiveWorkoutViewModel by N285):
// one read per exercise, made once, so the next-set prefill never re-queries on a tick and the fields fall
// back to the app's defaults only while saying so.

/**
 * Loads each exercise's previous performance once, when it first appears.
 *
 * The plan's own history is the template's (N73), but a slot's is its own when the workout came from one
 * (P3.8), so two slots naming one template progress apart. A read that fails is reported rather than
 * swallowed: the fields then fall back to the app's defaults, and B272 is what keeps that fallback from
 * being a silent claim about a lift the lifter has actually done before.
 */
internal class PreviousPerformanceSeeder(
    private val sessionExercises: Flow<List<SessionExercise>>,
    private val activeSession: SharedFlow<WorkoutSession?>,
    /** The cache itself, so an exercise read once is never asked for again. */
    private val previousByExercise: MutableStateFlow<Map<String, PreviousPerformance>>,
    private val slotId: String?,
    private val programRepository: ProgramRepository,
    private val workoutRepository: WorkoutRepository,
) {
    suspend fun seed(onError: (DataError) -> Unit) {
        sessionExercises.collect { exercises ->
            val sessionId = activeSession.replayCache.firstOrNull()?.id ?: return@collect
            exercises
                .map { it.exerciseId }
                .distinct()
                .filterNot { it in previousByExercise.value }
                .forEach { exerciseId ->
                    // A slot's own history where there is a slot (P3.8), so two slots naming
                    // one template progress apart; otherwise the exercise's last session.
                    val result = slotId?.let { slot ->
                        programRepository.slotPreviousPerformance(
                            slotId = slot,
                            exerciseId = exerciseId,
                            currentSessionId = sessionId,
                            // Read when it is needed rather than captured (B45).
                            zone = ZoneId.systemDefault(),
                        )
                    } ?: workoutRepository.previousPerformance(exerciseId, sessionId)
                    if (result is DataResult.Success) {
                        previousByExercise.update { it + (exerciseId to result.data) }
                    } else if (result is DataResult.Failure) {
                        // The fields fall back to the app's defaults when history cannot be read (B272);
                        // saying so is what keeps that fallback from being a silent claim about a lift
                        // the lifter has actually done before.
                        onError(result.error)
                    }
                }
        }
    }
}
