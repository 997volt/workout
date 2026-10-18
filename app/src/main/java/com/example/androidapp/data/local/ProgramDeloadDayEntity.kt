package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * One day of a program's built deload week (ROADMAP N147).
 *
 * It hangs off the week's [ProgramDeloadEntity] — the mark P3.10 already records — so the plan and the
 * exemption it belongs to cannot exist apart: unmarking the week soft-deletes the deload row, and the
 * days become unreachable with it while the templates they named stay in the library as ordinary
 * workouts. That is also why this table arrived without changing `program_deloads`: the mark is keyed by
 * program and week, and a plan is what that mark *prescribes* for those days.
 *
 * [weekday] is nullable for the same reason a slot's is: a source slot may be order-only, and the plan
 * does not invent a day for it. The row is stored per day rather than derived from the source slot,
 * because the week may keep fewer days than the program has slots and the lifter may move one.
 */
@Entity(
    tableName = "program_deload_days",
    indices = [Index("deloadId"), Index("templateId")],
    foreignKeys = [
        ForeignKey(
            entity = ProgramDeloadEntity::class,
            parentColumns = ["id"],
            childColumns = ["deloadId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProgramDeloadDayEntity(
    @PrimaryKey val id: String,
    val deloadId: String,
    val templateId: String,
    /** The day's place in the week, low first (N147). */
    val position: Int,
    /** The weekday it is offered on, or null for an order-only source slot. */
    val weekday: DayOfWeek?,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)

/**
 * One deload day with its template's name and exercise count, for the week's reads (ROADMAP N147).
 *
 * The program id and the week travel from the deload row so a screen can group a week without a second
 * read, and a deleted template drops the day out of every read — a day pointing at no workout would be
 * invisible the moment it was written, the trap `addSlot` already guards.
 */
data class DeloadDayRow(
    val id: String,
    val programId: String,
    val weekStart: Long,
    val deloadId: String,
    val templateId: String,
    val templateName: String,
    val exerciseCount: Int,
    val position: Int,
    val weekday: DayOfWeek?,
)
