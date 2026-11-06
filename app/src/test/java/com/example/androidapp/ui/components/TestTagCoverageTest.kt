package com.example.androidapp.ui.components

import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * Every tag names a control someone uses (D4, N274).
 *
 * D4 keeps tags that production already applies even when no test asserts on them yet, so this does not
 * demand a test. It does catch the one failure that is never useful: a tag constant or helper nothing refers
 * to, which is a name with no control behind it.
 */
class TestTagCoverageTest {

    @Test
    fun every_test_tag_isUsedSomewhereOutsideItsOwnFile() {
        val tagsFile = File(
            repoRoot,
            "app/src/main/java/com/example/androidapp/ui/components/TestTags.kt",
        )
        val names = buildSet {
            Regex("""const val ([A-Z0-9_]+)""").findAll(tagsFile.readText()).forEach { add(it.groupValues[1]) }
            Regex("""fun ([a-zA-Z0-9_]+)\(""").findAll(tagsFile.readText()).forEach { add(it.groupValues[1]) }
        }
        val used = File(repoRoot, "app/src").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it != tagsFile }
            .joinToString("\n") { it.readText() }

        assertThat(names).isNotEmpty()
        assertThat(names.filterNot { it in used }).isEmpty()
    }
}
