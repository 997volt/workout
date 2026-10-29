package com.example.androidapp.data.transfer

import com.example.androidapp.shared.EditMarker
import kotlinx.serialization.Serializable

/**
 * One program and the templates it orders, as a document that can be handed to someone (ROADMAP N47).
 *
 * Not a small backup. [BackupFile] requires four session collections and carries every other table
 * besides, so a program-only file cannot be one of those with fields missing — this is its own
 * document with its own [formatVersion], reusing the backup's DTOs because they are already the
 * raw-storage shape the mappers speak.
 *
 * **What travels is the program's *definition***: the program, its slots, the templates those
 * slots name, and their planned exercises and sets — a slot prescribes nothing of its own (N73).
 * **What does not** is
 * history — skips, deload weeks and their plans, and substitutions are keyed to weeks and are the
 * lifter's record
 * rather than the program, so a received program starts with no past rather than someone else's.
 *
 * Every collection but the program itself is defaulted, so a hand-written or older document still
 * decodes: an empty program is a name, and a slot-less one is harmless.
 */
@Serializable
data class ProgramDocument(
    /**
     * This document's own version, checked before anything is read.
     *
     * Separate from [BackupFile.schemaVersion] because the two formats change for different
     * reasons: a new backup column does not make an old program document wrong.
     */
    val formatVersion: Int,
    val exportedAt: Long,
    val program: ProgramDto,
    val slots: List<ProgramSlotDto> = emptyList(),
    val templates: List<TemplateDto> = emptyList(),
    val templateExercises: List<TemplateExerciseDto> = emptyList(),
    val templateSets: List<TemplateSetDto> = emptyList(),
    /**
     * The definition of every exercise the templates name (ROADMAP N47).
     *
     * Carried whole rather than referenced by id alone. The seeded library's ids are permanent
     * slugs that mean the same thing everywhere, but an exercise the *user* created carries a
     * generated id that means nothing on the receiving device — so the document brings the
     * definition with it, and the receiver creates whatever it does not already have under the id
     * the document used. Seeded rows are carried too and simply find themselves already present.
     */
    val exercises: List<ExerciseDto> = emptyList(),
    /**
     * Present only on an edit (ROADMAP N171), as the shared core defines it.
     *
     * The field lives in the shared model so both hosts read it; the app keeps it here because the
     * import has to tell a proposal to add from a proposal to change before it touches anything.
     */
    val edit: EditMarker? = null,
)

/**
 * The same document, naming the movements this library merged away by the rows they became (ROADMAP N238).
 *
 * A document exported before a merge carries an id the device no longer holds. Without this it would **create
 * the duplicate again** — the very twin the merge removed — because a name that resolves to nothing is what makes
 * the import mint a movement. Resolving through the retired map makes an old file join the one movement.
 *
 * The retired exercise's own definition is dropped rather than carried: the survivor is already here under
 * another id, and carrying a second definition of it would be adding back the row the merge deleted.
 */
fun ProgramDocument.withResolvedExercises(retired: Map<String, String>): ProgramDocument {
    if (retired.isEmpty()) return this
    fun resolve(id: String): String = retired[id] ?: id
    return copy(
        exercises = exercises.filterNot { it.id in retired }.map { it.copy(id = resolve(it.id)) },
        templateExercises = templateExercises.map { it.copy(exerciseId = resolve(it.exerciseId)) },
    )
}
