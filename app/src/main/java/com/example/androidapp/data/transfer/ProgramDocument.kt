package com.example.androidapp.data.transfer

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
 * slots name, their planned exercises and sets, and what each slot prescribes. **What does not** is
 * history — skips, deload weeks and substitutions are keyed to weeks and are the lifter's record
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
    /** What each slot prescribes, per exercise (ROADMAP P3.8). */
    val slotExercises: List<ProgramSlotExerciseDto> = emptyList(),
    val slotSets: List<ProgramSlotSetDto> = emptyList(),
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
)
