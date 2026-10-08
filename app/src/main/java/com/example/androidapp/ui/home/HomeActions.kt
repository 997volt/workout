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
 * Starts a next-up row's substitute, and writes no substitution (ROADMAP N85).
 *
 * A substitution is an event keyed by slot *and week* (P3.11), and there is no week to key one by here: the
 * run is deliberately calendar-free (P3.9), advancing when a slot is trained or skipped rather than because
 * a day passed, and `ProgramRun` carries the slot and a flag with the week nowhere in it. Recording it
 * against the week of *today* — what [substituteOccurrence] does for the card — would write an event for an
 * occurrence other than the one being started: on a Sunday it keys the next-up Monday to a week whose Monday
 * has already gone. So the pick starts the session and records no substitution, and the accepted cost is
 * that history does not say the run was substituted.
 *
 * That is **not** the same as the run standing still, which an earlier version of this comment claimed. The
 * session names the template it trained, and P3.9's run follows the last slot trained, so a pick naming
 * another slot of the same program moves the run past that slot — exactly what starting that template from
 * anywhere else does. The alternative was rejected: a session that names a template but settles no
 * occurrence would need the session to carry a second fact, and it contradicts P3.9. DECISIONS.md's N85
 * entry holds the rule. The card keeps its write, because the card *is* today's occurrence by construction.
 */
internal fun startSubstitute(
    requestStart: (StartIntent) -> Unit,
): (TodayPlan, String?, String?) -> Unit = { plan, templateId, templateName ->
    val slotId = plan.slotId
    if (slotId != null && templateId != null) {
        requestStart(
            StartIntent(
                templateId = templateId,
                // The slot is what the row stood for, so its prescription still seeds what it can
                // (P3.8) — the same start a recorded substitute performs.
                slotId = slotId,
                // The **picked** template's name, not the scheduled row's: the start's own question names
                // the workout it is about to begin (B93). The scheduled name stays the fallback.
                label = templateName ?: plan.name,
            ),
        )
    }
}

/**
 * Records a substitute for one occurrence and starts it (ROADMAP P3.11).
 *
 * The pick is made at the point of starting, so the write and the start are one action — and
 * clearing writes nothing to start.
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
                            // The slot is what it was scheduled as, so its prescription still
                            // seeds what it can (P3.8, P3.11).
                            slotId = slotId,
                            // The picked substitute's name, for the reason [startSubstitute] gives (B93).
                            label = templateName ?: plan.name,
                        ),
                    )
                }

                is DataResult.Failure -> onFailure(result.error)
            }
        }
    }
}
