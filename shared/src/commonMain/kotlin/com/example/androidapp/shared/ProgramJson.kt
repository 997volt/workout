@file:OptIn(ExperimentalJsExport::class)

package com.example.androidapp.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.js.ExperimentalJsExport
import kotlin.js.JsExport

/**
 * The string-in, string-out door the page uses (ROADMAP N170).
 *
 * The page is plain JavaScript with no module loader and no build for its own code; passing JSON
 * across the boundary keeps the interop to one function per question instead of mapping Kotlin data
 * classes into JS objects. The app does not use these wrappers — it calls [plan] and [validate]
 * directly — so this file is the only JS-specific shape in the core.
 */
private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@Serializable
private data class ProblemOut(val code: String, val path: String, val detail: String? = null)

@Serializable
private data class ChangeOut(val kind: String, val id: String, val name: String? = null)

@Serializable
private data class WarningOut(val code: String, val id: String? = null, val name: String? = null)

@Serializable
private data class ValidateOut(val problems: List<ProblemOut>)

@Serializable
private data class PlanOut(
    val ok: Boolean,
    val changes: List<ChangeOut> = emptyList(),
    val warnings: List<WarningOut> = emptyList(),
    val problems: List<ProblemOut> = emptyList(),
)

/** The format version the page writes, so the two hosts cannot disagree about it. */
@JsExport
fun programFormatVersion(): Int = PROGRAM_FORMAT_VERSION

/** [validate] over JSON: `{"problems":[…]}`; an empty list means the document may be written. */
@JsExport
fun validateJson(documentJson: String): String {
    val problems = try {
        validate(ProgramDocumentCodec.decode(documentJson)).map { it.toOut() }
    } catch (refused: ProgramFormatException) {
        listOf(refused.problem.toOut())
    }
    return json.encodeToString(ValidateOut(problems))
}

/**
 * [plan] over JSON: `{"ok":true,"changes":[…],"warnings":[…]}` or `{"ok":false,"problems":[…]}`.
 *
 * The page previews exactly this, then downloads the edit document; the app runs the same function on
 * the same two documents, so a preview cannot promise what the write does not do.
 */
@JsExport
fun planJson(snapshotJson: String, editJson: String): String {
    val result = try {
        plan(ProgramDocumentCodec.decode(snapshotJson), ProgramDocumentCodec.decode(editJson))
    } catch (refused: ProgramFormatException) {
        return json.encodeToString(PlanOut(ok = false, problems = listOf(refused.problem.toOut())))
    }
    return when (result) {
        is PlanResult.Refused -> json.encodeToString(
            PlanOut(ok = false, problems = result.problems.map { it.toOut() }),
        )

        is PlanResult.Ready -> json.encodeToString(
            PlanOut(
                ok = true,
                changes = result.changes.map { ChangeOut(it.kind.name, it.id, it.name) },
                warnings = result.warnings.map { WarningOut(it.code.name, it.id, it.name) },
            ),
        )
    }
}

private fun ProgramProblem.toOut() = ProblemOut(code = code.name, path = path, detail = detail)
