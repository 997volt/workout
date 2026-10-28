package com.example.androidapp.data.local

import com.example.androidapp.repoRoot
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The migration chain, checked as a chain rather than one step at a time (ROADMAP B286).
 *
 * Each migration has its own test for what it does to rows; this is the rule none of those can
 * state: the list is **complete and contiguous** from the store's first version to the newest
 * exported schema. Before this, the registration lived only as ten arguments at the builder, which
 * a test cannot read — so forgetting `MIGRATION_11_12` on the next bump passed every gate and failed
 * on every upgrading install with `A migration from 11 to 12 was required but not found`.
 *
 * The exported schemas are the other half of the contract: `app/schemas` must have one file per
 * version the code can produce, and the newest one is what the chain has to reach.
 */
class AllMigrationsTest {

    @Test
    fun theChain_startsAtTheFirstVersion_andEndsAtTheNewestSchema() {
        assertThat(versions()).isNotEmpty()
        assertThat(ALL_MIGRATIONS.first().startVersion).isEqualTo(1)
        assertThat(ALL_MIGRATIONS.last().endVersion).isEqualTo(versions().max())
    }

    @Test
    fun everyStep_isTheVersionAfterTheOneBefore() {
        assertThat(ALL_MIGRATIONS.map { it.startVersion })
            .containsExactlyElementsIn(versions().dropLast(1))
            .inOrder()
        assertThat(ALL_MIGRATIONS.map { it.endVersion })
            .containsExactlyElementsIn(versions().drop(1))
            .inOrder()
    }

    @Test
    fun everyExportedVersion_isConsecutive() {
        // A gap in the schema folder is a version this build can write but not read.
        assertThat(versions()).isEqualTo((1..versions().max()).toList())
    }

    /** The versions `app/schemas` declares, ascending. */
    private fun versions(): List<Int> =
        File(repoRoot, SCHEMAS)
            .listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension == "json" }
            .map { it.nameWithoutExtension.toInt() }
            .sorted()

    private companion object {
        const val SCHEMAS =
            "app/schemas/com.example.androidapp.data.local.WorkoutDatabase"
    }
}
