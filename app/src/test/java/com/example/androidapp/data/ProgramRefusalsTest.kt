package com.example.androidapp.data

import com.example.androidapp.data.transfer.ProgramDocumentCodec
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File

/**
 * The document write boundary the merge cannot honour (N358).
 *
 * The shared validator names every problem; four reference codes are deliberately resolved or dropped by
 * the merge, and everything else is a row the merge would write as it stands. These pin both halves: a
 * repeated id and an out-of-bound rep target are refused before the merge, and a movement the document
 * does not carry is still left to the merge's drop-and-count.
 */
class ProgramRefusalsTest {

    private val document = ProgramDocumentCodec.decode(fixtureJson())

    @Test
    fun aSecondSlotSharingAnId_isRefusedBeforeTheMerge() {
        val duplicate = document.copy(slots = document.slots + document.slots.first())

        val thrown = assertThrows(InvalidInputException::class.java) {
            requireValidDocument(duplicate)
        }

        assertThat(thrown.message).contains("share an id")
    }

    @Test
    fun aRepTargetAboveTheWriteBound_isRefused() {
        val tooMany = document.copy(
            templateSets = document.templateSets.mapIndexed { index, set ->
                if (index == 0) set.copy(targetRepsMin = 5, targetRepsMax = 10_001) else set
            },
        )

        val thrown = assertThrows(InvalidInputException::class.java) {
            requireValidDocument(tooMany)
        }

        assertThat(thrown.message).contains("10,000")
    }

    @Test
    fun aMovementTheDocumentDoesNotCarry_isStillForTheMergeToDrop() {
        val missing = document.copy(
            templateExercises = document.templateExercises.mapIndexed { index, planned ->
                if (index == 0) planned.copy(exerciseId = "not-in-this-file") else planned
            },
        )

        // The merge resolves the movement or drops it and counts it; refusing here would take away the
        // import this document has always been able to make.
        requireValidDocument(missing)
    }

    /** The committed fixture, out of the one-line script assignment it is wrapped in. */
    private fun fixtureJson(): String =
        File(repoRoot, "docs/fixtures/program-document.js").readText()
            .substringAfter("window.PROGRAM_DOCUMENT_FIXTURE = ")
            .substringBefore(";")
            .trim()
}
