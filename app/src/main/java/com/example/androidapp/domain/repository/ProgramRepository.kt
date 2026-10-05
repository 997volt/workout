package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.PendingOccurrence
import com.example.androidapp.domain.model.PreviousPerformance
import com.example.androidapp.domain.model.ProgramRun
import com.example.androidapp.domain.model.ProgramSlot
import com.example.androidapp.domain.model.WorkoutProgram
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

/**
 * Programs, their slots and their missed days (ROADMAP P3.3).
 *
 * Writes return [DataResult] for the same reason every other repository's do (F7): a
 * slot that silently failed to save is a schedule the user thinks they have.
 */
interface ProgramRepository {

    /** Every live program, in the authored order, with its slot count. */
    fun observePrograms(): Flow<List<WorkoutProgram>>

    /** One program, re-emitting when it is renamed, reordered or activated. */
    fun observeProgram(programId: String): Flow<WorkoutProgram?>

    /**
     * Every program home follows, in the authored order (ROADMAP P3.12).
     *
     * A list because more than one may be active. Empty is the state home falls back to the
     * template pins from, and the state in which adherence has no schedule to score.
     */
    fun observeActivePrograms(): Flow<List<WorkoutProgram>>

    /** A program's slots in its own order, each carrying its template's name. */
    fun observeSlots(programId: String): Flow<List<ProgramSlot>>

    /**
     * Stores a new program named [name] and returns its id.
     *
     * Creation asks for the name only, exactly as a template does (N3): the slots are
     * added afterwards. A new program is **not** made active — following one is a
     * deliberate choice, and the editor offers it as one.
     */
    suspend fun createProgram(name: String): DataResult<String>

    suspend fun renameProgram(programId: String, name: String): DataResult<Unit>

    /** Soft-deletes the program; its rows stay for an export to carry. */
    suspend fun deleteProgram(programId: String): DataResult<Unit>

    /**
     * Starts following this program, **without** stopping any other (ROADMAP P3.12).
     *
     * P3.3's "one active program only" is deliberately amended: a lifting block and a
     * conditioning one are two schedules at once, and the union is what every reader takes.
     */
    suspend fun activateProgram(programId: String): DataResult<Unit>

    /** Stops following this program, leaving the others active (P3.12). */
    suspend fun deactivateProgram(programId: String): DataResult<Unit>

    /** Moves a program one place: [delta] -1 for up, +1 for down. Past either end is a no-op. */
    suspend fun moveProgram(programId: String, delta: Int): DataResult<Unit>

    /**
     * Sets or clears the workout that stands in for one occurrence (ROADMAP P3.11).
     *
     * Keyed by slot and week, so **no other week changes** — which is the whole point, since
     * editing the program would change every week that references the template (N16, inherited by
     * P3.3). A [templateId] of null restores the slot's own workout, because a mis-pick would
     * otherwise be permanent and the record is the lifter's statement rather than the app's.
     *
     * The chosen template must be a live one: a replacement pointing at nothing would be invisible
     * on every screen the moment it was written.
     */
    suspend fun setSubstitution(slotId: String, weekStart: LocalDate, templateId: String?): DataResult<Unit>

    /**
     * Appends a slot for [templateId], on [weekday] or order-only when it is null.
     *
     * The template must be a live one: a slot pointing at nothing would be invisible on
     * every screen the moment it was written.
     */
    suspend fun addSlot(
        programId: String,
        templateId: String,
        weekday: DayOfWeek? = null,
    ): DataResult<Unit>

    /** Pins a slot to a weekday, or makes it order-only with null (P3.3). */
    suspend fun setSlotWeekday(slotId: String, weekday: DayOfWeek?): DataResult<Unit>

    /** Moves a slot one place: [delta] -1 for up, +1 for down. Past either end is a no-op. */
    suspend fun moveSlot(slotId: String, delta: Int): DataResult<Unit>

    suspend fun removeSlot(slotId: String): DataResult<Unit>

    /**
     * The previous performance of [exerciseId] for **one slot**, before [currentSessionId] (P3.8).
     *
     * A slot's own history rather than the exercise's: two slots may name one template, and the
     * heavy Monday and the light Friday have to progress apart. The session is found the way an
     * occurrence is matched (P3.3) — by template, then attributed to a slot by its own week and
     * weekday — and [zone] is the device's, used only for a session recorded before N25.
     */
    suspend fun slotPreviousPerformance(
        slotId: String,
        exerciseId: String,
        currentSessionId: String,
        zone: ZoneId,
    ): DataResult<PreviousPerformance>

    /**
     * Where one program's run is, or null when it has no slots (ROADMAP P3.9).
     *
     * Derived from finished sessions and recorded skips, never a stored cursor, so editing the
     * program re-derives its place rather than leaving a pointer at a slot that is gone. It
     * re-emits as either source changes.
     */
    fun observeProgramRun(programId: String): Flow<ProgramRun?>

    /**
     * The occurrences of every active program's current week that were missed (P3.3, unioned
     * by P3.12).
     *
     * [today] and [zone] are the *device's* day and zone, which is what "this week" means
     * when the question is asked now; each session's own week still comes from its own
     * zone (N25). Empty when no program is active — there is then nothing to be late for.
     * With several active, the misses are merged and ordered earliest first.
     */
    suspend fun pendingOccurrences(today: LocalDate, zone: ZoneId): DataResult<List<PendingOccurrence>>

    /**
     * Records a skip for each of [slotIds] in [weekStart]'s week (P3.3).
     *
     * One call for the whole week, because *Continue* settles every pending occurrence at
     * once: asking again for the next one would turn two misses into two interrogations.
     * Idempotent, so a repeated continue cannot double-record.
     */
    suspend fun skipOccurrences(slotIds: List<String>, weekStart: LocalDate): DataResult<Unit>

    /**
     * One program as a shareable document, or a failure when it is gone (ROADMAP N47).
     *
     * The program's **definition**: its slots, the templates they name, their planned work, what
     * each slot prescribes, and the definition of every exercise those templates reference. History
     * — skips, deload weeks, substitutions — stays behind, because it belongs to the device that
     * trained it.
     */
    suspend fun exportProgramDocument(programId: String): DataResult<String>

    /**
     * Merges a program document: adds what it holds and overwrites nothing (ROADMAP N47).
     *
     * A row whose id is already present is left alone, so loading the same file twice is a no-op
     * rather than a second copy, and a document can never cost the user a program they wrote. An
     * exercise the device does not have is created from the definition the document carries; a
     * planned exercise whose exercise is nowhere is dropped, and the rest of the program arrives.
     */
    suspend fun importProgramDocument(text: String): DataResult<ProgramImportSummary>
}

/**
 * What loading a program document brought in (ROADMAP N47).
 *
 * Counts rather than a sentence: the screen words them. [droppedMovements] is the number of planned
 * exercises whose exercise the device neither had **in its library** nor received — one it deleted
 * here is one of those, so the sentence says the library rather than the device (B51).
 */
data class ProgramImportSummary(
    val programs: Int,
    val templates: Int,
    val exercises: Int,
    val droppedMovements: Int,
) {
    /**
     * True when the load added no row at all, which is the same file loaded a second time (B57).
     *
     * The whole summary rather than `programs` alone: a program whose id is already here but whose
     * templates are not does add templates, and calling that "already here" would be false.
     */
    val addedNothing: Boolean get() = programs == 0 && templates == 0 && exercises == 0
}
