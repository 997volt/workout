package com.example.androidapp.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The name a refused load arrives under (ROADMAP N243).
 *
 * A copy that kept the name is a second program a lifter cannot tell from the first, so the offered name is
 * the one the document asked for where the list is free and a numbered one where it is not. Comparison ignores
 * case and surrounding space for the same reason: "push pull legs" and "Push Pull Legs" are one name to a
 * reader, and the dialog's own check refuses the typed one on the same rule.
 */
class ProgramNameTest {

    @Test
    fun aNameNothingHolds_isOfferedAsItIs() {
        assertThat(freeProgramName("Push Pull Legs", listOf("Upper/Lower", "Full Body"))).isEqualTo("Push Pull Legs")
    }

    @Test
    fun aNameTheListHolds_isNumbered() {
        assertThat(freeProgramName("Push Pull Legs", listOf("Push Pull Legs"))).isEqualTo("Push Pull Legs (2)")
    }

    @Test
    fun aNumberAlreadyTaken_movesOn() {
        assertThat(freeProgramName("PPL", listOf("PPL", "PPL (2)", "PPL (3)"))).isEqualTo("PPL (4)")
    }

    @Test
    fun theComparisonIgnoresCaseAndSurroundingSpace() {
        assertThat(freeProgramName("ppl", listOf("PPL"))).isEqualTo("ppl (2)")
        assertThat(freeProgramName("  Push Pull Legs  ", listOf("push pull legs"))).isEqualTo("Push Pull Legs (2)")
    }

    @Test
    fun theOfferedNameIsTrimmed() {
        assertThat(freeProgramName("  Fresh Plan  ", emptyList())).isEqualTo("Fresh Plan")
    }
}
