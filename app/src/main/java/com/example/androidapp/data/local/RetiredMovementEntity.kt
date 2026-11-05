package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A movement id the library no longer holds, and the one it became (ROADMAP N238).
 *
 * The merge deletes the row it merged away, so its id exists nowhere afterwards — and a program document that
 * names that id would bring the duplicate back by creating a movement of the same name. This is the table the
 * document import resolves through: an id it finds here is read as the survivor, so an old file joins the one
 * movement rather than splitting it again.
 *
 * A row is written once and never updated. It travels in the backup (B347): it is what *this* device did to its
 * own library, and a restore that lost it would let a document exported before the merge recreate the twin.
 */
@Entity(tableName = "retired_movements")
data class RetiredMovementEntity(
    /** The id that is gone. */
    @PrimaryKey val id: String,
    /** The id it was merged into, which the library holds. */
    val survivorId: String,
)
