package com.example.androidapp.ui.programs

import com.example.androidapp.domain.repository.ProgramImportSummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Which sentence a load ends with (B57, B267).
 *
 * The three cases are one decision: a document can be the same one loaded twice, a different one whose id a
 * pasted table's name produced, or a load that wrote rows. Only the middle is a warning, and it has to win over
 * "already here", because both add nothing and only one is true.
 */
class ProgramFileActionsTest {

    private fun say(summary: ProgramImportSummary): String = importSentence(
        summary = summary,
        loaded = "Program loaded.",
        alreadyLoaded = "That program is already here.",
        nameTaken = "A different plan is already here under that name, so nothing was added.",
    ) { count -> "$count movements were not in your library." }

    private val nothing = ProgramImportSummary(programs = 0, templates = 0, exercises = 0, droppedMovements = 0)

    @Test
    fun aLoadThatWroteRows_saysSo() {
        assertThat(say(nothing.copy(programs = 1, templates = 3, exercises = 1)))
            .isEqualTo("Program loaded.")
    }

    @Test
    fun theSameDocumentAgain_isAlreadyHere() {
        assertThat(say(nothing)).isEqualTo("That program is already here.")
    }

    @Test
    fun aNameTakenByAnotherPlan_isSaid_ratherThanAlreadyHere() {
        // B267: the id matched, so nothing was written — but this is not the program it names.
        assertThat(say(nothing.copy(nameTakenByAnotherPlan = true)))
            .isEqualTo("A different plan is already here under that name, so nothing was added.")
    }

    @Test
    fun theTakenSentence_stillNamesTheMovementsItCouldNotPlace() {
        // A clash is reported with the same tail every load carries, so the two facts cannot disagree.
        assertThat(say(nothing.copy(nameTakenByAnotherPlan = true, droppedMovements = 2)))
            .isEqualTo(
                "A different plan is already here under that name, so nothing was added. " +
                    "2 movements were not in your library.",
            )
    }
}
