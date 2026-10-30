package com.example.androidapp.data.local

import androidx.room.Dao
import androidx.room.Query

/**
 * The retired ids a merge records (ROADMAP N238).
 *
 * A read-only DAO: the writes run as raw SQL in [MovementMergeRepair] because a merge runs inside the seeder's
 * transaction, where a DAO call would deadlock on the connection being opened. Its own type rather than a method
 * on [ExerciseDao] because the interface it would otherwise extend is at the function ceiling this project
 * enforces.
 */
@Dao
interface MovementMergeDao {

    /** Every retired id and what it became, for the document import to resolve through. */
    @Query("SELECT * FROM retired_movements")
    suspend fun retiredMovements(): List<RetiredMovementEntity>
}
