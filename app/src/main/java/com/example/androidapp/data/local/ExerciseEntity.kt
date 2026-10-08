package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.androidapp.domain.model.Equipment
import com.example.androidapp.domain.model.MovementPattern
import com.example.androidapp.domain.model.MuscleGroup
import com.example.androidapp.domain.model.RowKind

/**
 * A persisted library exercise (ROADMAP F5).
 *
 * The schema is deliberately written **sync-ready** even though storage is
 * local-only today:
 *
 *  - [id] is a stable string key (a slug for seeded rows, a UUID for
 *    user-created ones) rather than an autoincrement rowid, so a row can be
 *    created on any device without negotiating a key with a server.
 *  - [createdAt] / [updatedAt] / [deletedAt] are what a future sync (P4.9) needs
 *    to resolve conflicts and propagate deletions. [deletedAt] is a soft delete
 *    because a hard delete is invisible to a peer that has not synced yet.
 *
 * Retrofitting those three columns later would mean a migration *plus* a
 * backfill with invented timestamps, which is precisely the cost this avoids.
 * Nothing reads the sync columns yet — that is intentional, not dead weight.
 */
@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup>,
    val equipment: Equipment,
    val movementPattern: MovementPattern,
    val isCustom: Boolean,
    /**
     * The row this one hangs under, or null for a top-level row (ROADMAP N95).
     *
     * One column for both levels of the two-rule shape, because the *kind* decides what it means: a
     * variation's parent is the exercise it is performed as a version of, and an exercise's parent is the
     * category it is filed under. Every existing row is null, which reads as "top level" and is exactly
     * what a flat library was.
     *
     * Deliberately **not** a foreign key with a cascade: a head is soft-deleted like every other row
     * (P1.12 keeps it for the export), and N58's rule is that a removed head still *names* its children —
     * both of which a cascade would take away.
     */
    val parentId: String? = null,
    /**
     * Whether this row is a lift or a category head (ROADMAP N95), stored by name.
     *
     * Backed by a Kotlin default rather than a SQL default so the migration is a plain ALTER plus one
     * UPDATE: Room's default value would have to be the string literal, and this keeps the value's home
     * in the enum. Every pre-existing row is backfilled to [RowKind.MOVEMENT].
     */
    val rowKind: RowKind = RowKind.MOVEMENT,
    /**
     * This exercise's own rest between sets, or null to use the app default
     * (ROADMAP N5). Nullable with no default, so the migration that adds it is a
     * plain ALTER and existing rows read as "use the default".
     */
    val restSeconds: Int? = null,
    /** A short cue to read *while* lifting, not a description (ROADMAP N5). */
    val techniqueNote: String? = null,
    /**
     * This exercise's own display unit, or null to follow the app setting (ROADMAP N64).
     *
     * A nullable TEXT column holding the enum's **name**, so a build that does not know a name
     * reads it as "follow the app" rather than as a different unit. Nullable with no default, so
     * the migration that adds it is a plain ALTER and existing rows read as unset.
     */
    val weightUnit: String? = null,
    /**
     * This exercise's own weight step in whole grams, or null for the unit's (ROADMAP N77).
     *
     * Nullable with no default, so the migration that adds it is a plain ALTER and every existing row
     * reads as the unit's own step, which is what the ± buttons, the warm-up ramp and the progression
     * offer meant before an exercise could say otherwise.
     */
    val stepGrams: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
