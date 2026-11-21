package com.example.androidapp.domain.repository

import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.model.TrainingReport
import java.time.LocalDate

/** A sequence block the History sheet can offer under **Change**. */
data class TrainingBlockChoice(
    val programId: String,
    val name: String,
    val start: LocalDate,
)

/**
 * The last training block as the one CSV a lifter sends their coach (ROADMAP N331).
 *
 * The repository is read-only: it assembles the block's training and body, with the plan beside what
 * was performed, and hands the report to the CSV writer. It never writes.
 */
interface TrainingReportRepository {

    /**
     * The most recently started sequence program, or — when the lifter does not use blocks — the
     * last eight weeks, named as such. It never invents a block.
     */
    suspend fun lastBlock(): DataResult<TrainingReport>

    /** One named sequence block, for **Change**. */
    suspend fun block(programId: String): DataResult<TrainingReport>

    /** An explicit range, for the custom option. */
    suspend fun range(start: LocalDate, end: LocalDate): DataResult<TrainingReport>

    /** The sequence programs the Change list offers, newest start first. */
    suspend fun recentBlocks(): DataResult<List<TrainingBlockChoice>>
}
