package com.example.androidapp.platform

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** The one-file rule for the share cache (N344). */
class StagedReportFilesTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun clearingTheFolder_removesEveryPreviouslyStagedReport() {
        val first = folder.newFile("workout-block-a.csv")
        val second = folder.newFile("workout-block-b.csv")
        assertThat(listOf(first, second).all { it.exists() }).isTrue()

        clearStagedReports(folder.root)

        assertThat(folder.root.listFiles().orEmpty()).isEmpty()
    }

    @Test
    fun aMissingFolder_isNotAFailure() {
        // The first share on a fresh install: the folder is created before staging, but the helper is
        // asked to clear whatever is there, and "nothing" is a valid answer rather than a crash.
        clearStagedReports(File(folder.root, "not-created-yet"))
    }
}
