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
    // A template carries no weekday (ROADMAP N56). The pin lived here from N16, and it was the weaker
    // of two places answering "what am I doing on Tuesday" — a template has no order, no next-up and
    // no adherence to belong to. The day belongs to a program's slot, and the column was dropped in
    // v25 rather than left unread.
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
