package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A named, reusable workout (ROADMAP N3).
 *
 * Written **sync-ready** like every other table: a stable string id, and
 * `createdAt`/`updatedAt`/`deletedAt` so a future sync (P4.9) can resolve conflicts
 * and propagate deletions without a migration. Nothing reads those columns yet.
 *
 * The soft delete matters here for the same reason it does for exercises: a
 * template the user deletes is still part of the data an export carries.
 */
@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    /**
     * The template this row is a **kept version** of, or null when it is a template (ROADMAP N134).
     *
     * A version is a template row of its own rather than a second tree: its exercises and sets live in
     * the tables they already live in, every read path applies unchanged, and the templates list adds
     * one predicate — `versionOf IS NULL` — so a version never appears as a template. Nullable with no
     * default so [MIGRATION_3_4] is a plain `ALTER`, and an existing row is simply not a version.
     *
     * It stores its own [name]: the live template's name is read **live** by history, so renaming the
     * live row to mark a version would rewrite how past sessions read.
     */
    val versionOf: String? = null,
    // A template carries no weekday (ROADMAP N56). The pin lived here from N16, and it was the weaker
    // of two places answering "what am I doing on Tuesday" — a template has no order, no next-up and
    // no adherence to belong to. The day belongs to a program's slot, and the column was dropped
    // rather than left unread.
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
    /**
     * When the lifter took this workout out of the choices, or null (ROADMAP N214).
     *
     * **Hidden is not deleted.** The row, its exercises and its sets are untouched, so every program
     * slot that names it keeps scheduling and starting it and history keeps reading its name; what
     * hiding takes it out of is the templates list and the picker a program's day chooses from. The
     * write deliberately **does not move [updatedAt]** — hiding changes no plan, and that column is
     * the clock N171's staleness check reads, so moving it would make every program carrying the
     * workout read as changed.
     */
    val hiddenAt: Long? = null,
)
