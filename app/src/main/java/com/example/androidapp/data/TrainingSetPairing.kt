package com.example.androidapp.data

import com.example.androidapp.data.local.SessionPlanSetEntity
import com.example.androidapp.domain.WeightUnit
import com.example.androidapp.domain.model.SetEntry
import com.example.androidapp.domain.model.SetType
import com.example.androidapp.domain.model.TrainingSet

/**
 * Pairs a session exercise's prescribed sets with what was performed (N356).
 *
 * The two sides do not share an index space. The snapshot copies the template's raw `setIndex`, while a
 * logged set is appended after the highest row the exercise has ever held — including a soft-deleted one
 * — so delete-then-Undo leaves 0, 2, 3 against a plan of 0, 1, 2, and a document plan may be numbered
 * from one. They are paired the way the workout screen pairs them, by **plan place**
 * ([SetType.occupiesAPlanPlace]): every performed row that holds a place answers the next prescribed
 * row, a failed attempt is performed but answers none, and a prescribed row nothing answered is not done.
 *
 * Rows keep the order the session ran: a failed attempt sits where it was logged, and the prescribed
 * rows no set answered follow. Their [TrainingSet.setIndex] is that report order, not either stored
 * number — the CSV's own set number is counted from the work rows instead (N353).
 */
internal fun pairedTrainingSets(
    prescriptions: List<SessionPlanSetEntity>,
    performed: List<SetEntry>,
    unit: WeightUnit,
): List<TrainingSet> {
    val orderedPlans = prescriptions.sortedBy { it.setIndex }
    val orderedPerformed = performed.sortedBy { it.setIndex }
    val rows = ArrayList<TrainingSet>(maxOf(orderedPlans.size, orderedPerformed.size))
    var nextPlan = 0
    orderedPerformed.forEach { done ->
        val plan = if (done.setType.occupiesAPlanPlace && nextPlan < orderedPlans.size) {
            orderedPlans[nextPlan++]
        } else {
            null
        }
        rows += trainingSet(rows.size, plan, done, unit)
    }
    while (nextPlan < orderedPlans.size) {
        rows += trainingSet(rows.size, orderedPlans[nextPlan++], null, unit)
    }
    return rows
}

private fun trainingSet(
    index: Int,
    plan: SessionPlanSetEntity?,
    performed: SetEntry?,
    unit: WeightUnit,
): TrainingSet = TrainingSet(
    setIndex = index,
    performed = performed != null,
    role = performed?.setType ?: plan?.role ?: SetType.NORMAL,
    unit = unit,
    reps = performed?.reps ?: 0,
    durationSeconds = performed?.durationSeconds,
    weightGrams = performed?.weightGrams ?: 0L,
    assistanceGrams = performed?.assistanceGrams ?: 0L,
    bodyweightGrams = performed?.bodyweightGrams,
    rpeHalves = performed?.rpeHalves,
    comment = performed?.note,
    plannedRole = plan?.role,
    plannedReps = plan?.let { it.targetRepsCurrent ?: it.targetRepsMax ?: it.targetRepsMin },
    plannedWeightGrams = plan?.targetWeightGrams,
    plannedAssistanceGrams = plan?.targetAssistanceGrams,
    plannedDurationSeconds = plan?.targetDurationSeconds,
    plannedRpeHalves = plan?.targetRpeHalves,
)
