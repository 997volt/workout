package com.example.androidapp.data

import com.example.androidapp.data.transfer.ProgramSlotExerciseDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What a load may place, against what its template trains (ROADMAP P3.8, B56).
 *
 * The predicate rather than the merge: the merge needs Room, and this is the rule it reads. The
 * interactive writes enforce the same rule through `requireExerciseInTemplate`, so the case that
 * matters here is the one only an import can reach — a slot naming a template whose id already
 * existed on this device and has since diverged.
 */
class ProgramPrescriptionPlacementTest {

    private val prescription = prescription(slotId = "slot-1", exerciseId = "back-squat")

    @Test
    fun aPrescription_itsTemplateTrainsAndTheLibraryHas_isPlaced() {
        assertThat(
            prescriptionFits(
                prescription,
                templateOfSlot = mapOf("slot-1" to "template-1"),
                trainedByTemplate = mapOf("template-1" to setOf("back-squat")),
                presentExercises = setOf("back-squat"),
            ),
        ).isTrue()
    }

    @Test
    fun aPrescription_forAMovementTheTemplateDoesNotTrain_isNotPlaced() {
        // The imported slot would have prescribed a lift this template no longer holds, which no
        // screen would ever show it.
        assertThat(
            prescriptionFits(
                prescription,
                templateOfSlot = mapOf("slot-1" to "template-1"),
                trainedByTemplate = mapOf("template-1" to setOf("front-squat")),
                presentExercises = setOf("back-squat"),
            ),
        ).isFalse()
    }

    @Test
    fun aPrescription_whoseExerciseIsNotHere_isNotPlaced() {
        assertThat(
            prescriptionFits(
                prescription,
                templateOfSlot = mapOf("slot-1" to "template-1"),
                trainedByTemplate = mapOf("template-1" to setOf("back-squat")),
                presentExercises = emptySet(),
            ),
        ).isFalse()
    }

    @Test
    fun aPrescription_whoseSlotIsNotInTheDocument_isNotPlaced() {
        // A slot the load itself dropped takes its prescriptions with it, which the empty lookup
        // answers without a second membership test.
        assertThat(
            prescriptionFits(
                prescription,
                templateOfSlot = emptyMap(),
                trainedByTemplate = emptyMap(),
                presentExercises = setOf("back-squat"),
            ),
        ).isFalse()
    }

    private fun prescription(slotId: String, exerciseId: String) = ProgramSlotExerciseDto(
        id = "prescription-1",
        slotId = slotId,
        exerciseId = exerciseId,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
