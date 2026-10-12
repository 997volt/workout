package com.example.androidapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.androidapp.domain.model.MeasurementKind

/**
 * One dated set of body measurements (ROADMAP N32, split in two by N113).
 *
 * **One row per record, carrying only its own kind's numbers.** A [MeasurementKind.DAILY] row holds
 * weight and the two percentages and leaves every tape column null; a [MeasurementKind.TAPE] row does
 * the opposite. They share the table rather than being two tables because they are read as one series
 * per metric — the statistics screen maps this list for a body metric and for a tape site alike — and
 * because an export's list is the table, so a second one would be a second collection to keep whole.
 *
 * **Weight is no longer required**, which is the change that made the transfer format move (N113): a
 * soreness-only morning is a real entry, and a zero would be indistinguishable from a measurement of
 * nothing — the one reading no body ever gives (N32).
 *
 * Units follow what is settled elsewhere in the schema: grams in a `Long` for weight, **tenths of a
 * percent in an `Int`** for the percentages — the same reason RPE is held in halves, since 18.3 has no
 * exact binary representation — and **millimetres in a `Long`** for the tape, displayed as centimetres
 * so a half-centimetre is a value rather than a rounding.
 *
 * Sync-shaped like every other table (id, createdAt, updatedAt, deletedAt), so it rides the export.
 */
@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey val id: String,
    /** When it was measured, which is the date the entry belongs to. */
    val measuredAt: Long,
    /**
     * Which of the two records this row is (ROADMAP N113), stored by name like every other enum.
     *
     * A row written before the split is a daily one: it was a whole reading of the body, and its tape
     * values were copied into a tape row of their own by [MIGRATION_2_3].
     */
    val kind: MeasurementKind,
    /** Whole grams, or null when the scale was not stepped on (ROADMAP N113). */
    val weightGrams: Long? = null,
    /** Body fat, in tenths of a percent. */
    val bodyFatTenths: Int? = null,
    /** Muscle mass, in tenths of a percent. */
    val muscleTenths: Int? = null,
    /** The tape sites, in millimetres, every one optional and left blank when not taken. */
    val neckMm: Long? = null,
    val chestMm: Long? = null,
    val waistMm: Long? = null,
    val hipsMm: Long? = null,
    val upperArmMm: Long? = null,
    val thighMm: Long? = null,
    val calfMm: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long?,
)
