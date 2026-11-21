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
     * last eight weeks, named [fallbackName]. It never invents a block.
     *
     * The name is the caller's (N343): the domain has no `Context`, and a report called "Last 8
     * weeks" in the sheet must carry that name, not the ISO range beside the localized window.
     */
    suspend fun lastBlock(fallbackName: String): DataResult<TrainingReport>

    /** One named sequence block, for **Change**. */
    suspend fun block(programId: String): DataResult<TrainingReport>

    /** An explicit range, named by the caller (N343), for the custom option. */
    suspend fun range(start: LocalDate, end: LocalDate, name: String): DataResult<TrainingReport>

    /** The sequence programs the Change list offers, newest start first. */
    suspend fun recentBlocks(): DataResult<List<TrainingBlockChoice>>
}
