package com.example.androidapp.data

import com.example.androidapp.repoRoot
import com.example.androidapp.shared.PlanResult
import com.example.androidapp.shared.ProgramDocumentCodec
import com.example.androidapp.shared.plan
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import java.io.File

/**
 * The one fixture set the page and the app must agree on (ROADMAP N173).
 *
 * Each case is a snapshot, the edit made from it, and the answer the shared planner must give. The
 * page's `editor-tests.js` runs `planJson` over the same cases; this runs `plan` in Kotlin. The two
 * hosts are therefore held to one fixture rather than to two implementations that merely look alike.
 *
 * `docs/fixtures/generate.js` writes the file from the same program document the sheet fixture uses,
 * so the cases are documents the app's own codec writes, not invented shapes.
 */
class ProgramEditFixtureTest {

    @Test
    fun theFixtureCasesAreAcceptedOrRefusedAsTheySay() {
        val cases = cases()
        assertThat(cases).isNotEmpty()

        cases.forEach { case ->
            val name = case["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val snapshot = ProgramDocumentCodec.decode(case["snapshot"].toString())
            val edit = ProgramDocumentCodec.decode(case["edit"].toString())
            val expected = case["expected"]!!.jsonObject
            val expectedOk = expected["ok"]!!.jsonPrimitive.boolean

            when (val result = plan(snapshot, edit)) {
                is PlanResult.Ready -> {
                    assertWithMessage("$name: the fixture expected a refusal").that(expectedOk).isTrue()
                    val kinds = result.changes.map { it.kind.name }
                    expected["kinds"]?.jsonArray?.forEach { kind ->
                        assertWithMessage("$name: changes").that(kinds).contains(kind.jsonPrimitive.content)
                    }
                    val warnings = result.warnings.map { it.code.name }
                    expected["warnings"]?.jsonArray?.forEach { code ->
                        assertWithMessage("$name: warnings").that(warnings)
                            .contains(code.jsonPrimitive.content)
                    }
                }

                is PlanResult.Refused -> {
                    assertWithMessage("$name: the fixture expected an edit").that(expectedOk).isFalse()
                    val codes = result.problems.map { it.code.name }
                    expected["codes"]?.jsonArray?.forEach { code ->
                        assertWithMessage("$name: problems").that(codes)
                            .contains(code.jsonPrimitive.content)
                    }
                }
            }
        }
    }

    /** The cases out of the one-line script assignment the generated file wraps them in. */
    private fun cases(): List<JsonObject> = Json.parseToJsonElement(
        File(repoRoot, "docs/fixtures/program-edit.js").readText()
            .substringAfter("window.PROGRAM_EDIT_CASES = ")
            .trim()
            .removeSuffix(";"),
    ).jsonArray.map { it.jsonObject }
}
