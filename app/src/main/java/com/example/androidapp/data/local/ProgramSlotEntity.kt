package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/**
 * One slot of a program: a template, and the weekday it is done on (ROADMAP P3.3).
 *
 * [weekday] is nullable and means the same thing it does on a template: a slot with
 * no day is **order-only**. It is part of the program's running order — "which one is
 * next" reads it — but it can never be missed, because there is no day to miss.
 *
 * [position] is the program's order, the thing a standing weekday pin cannot express:
 * a pin says what happens on a Tuesday but nothing orders the pins against each other.
 * A slot is deleted softly for the same reason a template is: an export carries it.
 */
@Entity(
    tableName = "program_slots",
    indices = [Index(value = ["programId"]), Index(value = ["templateId"])],
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],
            childColumns = ["programId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class ProgramSlotEntity(
    @PrimaryKey val id: String,
    val programId: String,
    val templateId: String,
    val position: Int,
    val weekday: DayOfWeek? = null,
    /**
     * The authored phase week this day belongs to, or null for a single week that repeats (ROADMAP N292).
     *
     * [position] is the day's place **within its week**, so two weeks may both have a day at zero.
     * Null is every row written before phases existed, and means exactly what it did: the program is
     * one week and this day is in it.
     */
    val phaseWeek: Int? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
