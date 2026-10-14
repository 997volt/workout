package com.example.androidapp.ui.home

import com.example.androidapp.domain.DataError
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.WorkoutSession
import com.example.androidapp.ui.programs.StartIntent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The home screen's writes that are more than a call (ROADMAP P3.11).
 *
 * File-level rather than members of the route's file, which is at the length and function count
 * this project allows, and because neither reads any composition state: they build a callback for
 * the screen and take their sinks as parameters.
 */

/**
 * Ends the workout in progress, if there is one (ROADMAP N89).
 *
 * The same soft delete the workout screen's own discard performs, and the reason it sits here: home is
 * where the "start anyway" request is made, and `startOrResumeSession` is find-or-create, so without this
 * the plan being started would be dropped and the running session handed back. Nothing open is not a
 * failure — the caller's start then simply creates the session it asked for — and a delete that fails is
 * returned rather than thrown so the caller can say it (F7).
 *
 * Takes the active read and the delete as parameters rather than a repository, so the rule is testable
 * without one; the ViewModel is the only thing that knows which repository it is.
 */
internal suspend fun discardActiveSession(
    active: Flow<WorkoutSession?>,
    delete: suspend (String) -> DataResult<Unit>,
): DataResult<Unit> {
    val session = active.first() ?: return DataResult.Success(Unit)
    return delete(session.id)
}

/**
 * Records a substitute for one occurrence and starts it (ROADMAP P3.11).
 *
 * The pick is made at the point of starting, so the write and the start are one action — and
 * clearing writes nothing to start.
 *
 * The former next-up variant, which started the pick and wrote nothing (N85), went with the next-up rows
 * (N125): every substitute offered now belongs to a card, and a card *is* today's occurrence, so all of
 * them are recorded.
 */
internal fun substituteOccurrence(
    scope: CoroutineScope,
    viewModel: WorkoutsHomeViewModel,
    requestStart: (StartIntent) -> Unit,
    onFailure: (DataError) -> Unit,
): (TodayPlan, String?, String?) -> Unit = { plan, templateId, templateName ->
    val slotId = plan.slotId
    if (slotId != null) {
        scope.launch {
            when (val result = viewModel.setSubstitution(slotId, templateId)) {
                is DataResult.Success -> if (templateId != null) {
                    requestStart(
                        StartIntent(
                            templateId = templateId,
                            // The slot is what it was scheduled as, so it travels for the prefill
                            // history (N73, P3.11).
                            slotId = slotId,
                            // The picked substitute's name, so the start's own question names the workout
                            // it is about to begin rather than the one that was scheduled (B93).
                            label = templateName ?: plan.name,
                        ),
                    )
                }

                is DataResult.Failure -> onFailure(result.error)
            }
        }
    }
}
