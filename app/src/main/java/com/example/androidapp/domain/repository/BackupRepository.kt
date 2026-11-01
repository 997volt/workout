package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult

/**
 * Backup and restore to a file the user chooses (ROADMAP P1.12).
 *
 * This exists because platform backup is off (F1): it is the *only* way data
 * survives an uninstall, a lost signing key or a signature change. Because the
 * app has no `INTERNET` permission, the destination is always a local file the
 * user picked through the Storage Access Framework — never a server.
 */
interface BackupRepository {

    /** The whole database as a JSON document. */
    suspend fun export(): DataResult<String>

    /**
     * Brings the database up to what [text] describes.
     *
     * The rule is **"bring back what is gone; never overwrite what is there"**,
     * which gives three cases per row:
     *
     *  - **Missing locally** → inserted.
     *  - **Present but deleted** → restored from the file, `deletedAt` cleared.
     *  - **Present and live** → left alone. A local edit could be newer than the
     *    file, and guessing wrong there loses work the user did after exporting.
     *
     * The middle case is why this cannot be insert-only. A delete in this app is a
     * *soft* delete, so the row and its id survive: an insert-only import skipped
     * exactly the rows a restore is for, and reported "nothing to do" — the bug
     * that motivated this shape.
     *
     * The file's metric targets (N39) follow the same rule even though they are settings rather
     * than rows: one this device lacks comes back, one it already has is left alone. The other
     * settings are device preferences and are not in the file at all (N21).
     */
    suspend fun import(text: String): DataResult<ImportSummary>

    /**
     * What a backup file holds, before anything is merged (ROADMAP N246).
     *
     * The same decode the import runs, so a file the import would refuse is refused here too. It is a question,
     * not a dry run: nothing is written, and each count is split into what is already here, what would come
     * back and what would be added (N248).
     */
    suspend fun preview(text: String): DataResult<BackupPreview>

    /**
     * Deletes everything the user made and puts the seeded library back (ROADMAP N18).
     *
     * **Hard, not soft.** Every other delete in this app sets `deletedAt`, which is right
     * for a mistaken tap and wrong for "start over": a soft-deleted row survives, and an
     * export taken afterwards would still carry it. This is the one action that removes
     * rows.
     *
     * It leaves the library the app ships (those are app content, not the user's), the
     * database file itself, and any file the user has exported — that one belongs to them
     * and lives outside the app, which is exactly why the screen offers an export first.
     */
    suspend fun clearAllUserData(): DataResult<Unit>
}

/**
 * What an import actually did, so the UI can say something specific rather than
 * "done" — which reads as failure when nothing was added because it was already
 * there.
 */
data class ImportSummary(
    /** Rows the file had and the database did not. */
    val added: Int,
    /** Rows that were deleted locally and came back from the file. */
    val restored: Int,
) {
    val total: Int get() = added + restored

    /** Everything in the file was already present and live. A success, not a no-op. */
    val wasAlreadyComplete: Boolean get() = total == 0
}

/**
 * What one kind of row in a backup would do on this device (ROADMAP N246, N248).
 *
 * The buckets are the merge's own three cases: a row that is already live here, a row this device hid and the
 * merge would bring back, and a row it does not have. They partition [inFile].
 */
data class PreviewCounts(
    /** Rows the file holds. */
    val inFile: Int,
    /**
     * Rows this device already has live — or, for [BackupPreview.measurements], rows whose day it already has:
     * the merge yields to the day, so calling that "here" is what the row will do.
     */
    val alreadyHere: Int,
    /** Rows this device has hidden; the merge would bring them back. */
    val restored: Int,
    /** Rows this device does not have. */
    val added: Int,
)

/**
 * What a backup file would do to this device, for the question asked before it is merged (ROADMAP N246, N248).
 *
 * Read-only: the counts come from decoding the file and listing what is here, never from a dry-run write. The
 * four kinds are the ones the question names; plans and programs ride along in the merge but are not part of
 * the question.
 */
data class BackupPreview(
    val sessions: PreviewCounts,
    val sets: PreviewCounts,
    val measurements: PreviewCounts,
    val exercises: PreviewCounts,
)
