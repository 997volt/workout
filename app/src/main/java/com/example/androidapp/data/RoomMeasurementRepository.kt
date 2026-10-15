package com.example.androidapp.data

import androidx.room.withTransaction
import com.example.androidapp.data.local.MeasurementEntity
import com.example.androidapp.data.local.MeasurementSoreMuscleEntity
import com.example.androidapp.data.local.WorkoutDatabase
import com.example.androidapp.domain.DataResult
import com.example.androidapp.domain.NotFoundException
import com.example.androidapp.domain.TimeSource
import com.example.androidapp.domain.dataResultOf
import com.example.androidapp.domain.nowEpochMillis
import com.example.androidapp.domain.model.BodyMeasurement
import com.example.androidapp.domain.model.MeasurementKind
import com.example.androidapp.domain.model.SoreMuscle
import com.example.androidapp.domain.model.TapeSite
import com.example.androidapp.domain.model.localDayBounds
import com.example.androidapp.domain.repository.MeasurementRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.example.androidapp.domain.InvalidInputException
import com.example.androidapp.domain.model.TenPointScale

/**
 * Measurements in Room (ROADMAP N32, two records since N113).
 *
 * Thin on purpose: the rules that matter — a day has **one entry of each kind**, an unmeasured site
 * stays blank — are stated in the port and in DECISIONS.md, and this only carries them out.
 */
@Singleton
class RoomMeasurementRepository @Inject constructor(
    private val database: WorkoutDatabase,
    private val timeSource: TimeSource,
) : MeasurementRepository {

    private val dao = database.measurementDao()

    /** The sore list the daily record owns (ROADMAP N113). */
    private val soreDao = database.measurementSoreMuscleDao()

    /**
     * The timestamp the last write in this process took (ROADMAP B135).
     *
     * A restore correlates the sore rows a delete hid by the timestamp that delete wrote, and the save
     * path writes the same field when it replaces a list. Two writes that shared a millisecond would then
     * be indistinguishable — the undo would resurrect the soreness the lifter had already replaced — so
     * every write takes a value strictly greater than the last, falling back to the clock once it has
     * moved on.
     */
    private val lastWriteAt = AtomicLong(0)

    /** A write timestamp no other write here has used (ROADMAP B135). */
    private fun nextWriteAt(): Long {
        while (true) {
            val last = lastWriteAt.get()
            val next = maxOf(timeSource.nowEpochMillis(), last + 1)
            if (lastWriteAt.compareAndSet(last, next)) return next
        }
    }

    /**
     * Every live entry, newest first, each carrying its own sore list.
     *
     * One read of each table rather than a query per entry: the screen shows every measurement at once,
     * and the sore rows are grouped by the measurement they belong to here rather than in the screen.
     */
    override fun observeAll(): Flow<List<BodyMeasurement>> =
        combine(dao.observeAll(), soreDao.observeAll()) { rows, soreRows ->
            val byMeasurement = soreRows.groupBy { it.measurementId }
            rows.map { row -> row.toDomain(byMeasurement[row.id].orEmpty()) }
        }

    override suspend fun save(measurement: BodyMeasurement): DataResult<Unit> = dataResultOf {
        // The same 1–10 scale as a set's RPE and the per-exercise ratings, so the one validator covers
        // them (N8, N62). The picker clamps and cannot produce a bad score; this is the boundary, the
        // way the RPE check is on the set path — and an import may carry anything.
        if (measurement.soreMuscles.any { !TenPointScale.isValid(it.score) }) {
            throw InvalidInputException(
                "A sore-muscle score must be between ${TenPointScale.MIN} and ${TenPointScale.MAX}.",
            )
        }
        val now = nextWriteAt()
        // One entry of each kind for a day, so a tape entry edits its own row rather than the one the
        // scale wrote (N113). The day is the local one, not a fixed span (N32, B133).
        val (dayStart, dayEnd) = measurement.measuredAt.localDayBounds(ZoneId.systemDefault())
        val existing = dao.find(measurement.id)
            ?: dao.findOnDay(
                from = dayStart,
                to = dayEnd,
                kind = measurement.kind.name,
            )
        val row = measurement.toEntity(
            // An existing row keeps its identity: an edit is an edit, not a replacement.
            id = existing?.id ?: measurement.id.ifBlank { UUID.randomUUID().toString() },
            createdAt = existing?.createdAt ?: now,
            now = now,
        )
        // The entry and its sore list are one save: a failure between them would leave a daily record
        // whose soreness is a previous save's.
        database.withTransaction {
            if (existing == null) {
                dao.insert(row)
            } else if (dao.update(row) == 0) {
                throw NotFoundException("measurement ${row.id}")
            }
            // A save **replaces** the list, because the form shows exactly what is stored (N62). A tape
            // entry states none, so its rows go too — it never had any to keep.
            soreDao.softDeleteForMeasurement(row.id, now)
            soreDao.insertAll(measurement.soreMuscles.toRows(measurementId = row.id, now = now))
        }
    }

    override suspend fun delete(id: String): DataResult<Unit> = dataResultOf {
        val now = nextWriteAt()
        database.withTransaction {
            if (dao.softDelete(id, now) == 0) {
                throw NotFoundException("measurement $id")
            }
            // The rows hang off the entry, so they go with it (the FK only cascades a hard delete).
            soreDao.softDeleteForMeasurement(id, now)
        }
    }

    /**
     * Puts a deleted entry back, and refuses the one collision a restore can cause (ROADMAP N133).
     *
     * The row's own `deletedAt` is the identity of the write that hid it, so the restore clears exactly
     * that and nothing else — N90's rule, which is what stops an undo from resurrecting an entry deleted
     * deliberately a moment earlier. The sore rows went with the entry on the same timestamp, so they
     * come back whole — and that correlation is why every write takes a **unique** timestamp (B135): the
     * save path writes the same field when it replaces a list, so a save and a delete in one millisecond
     * would share a marker and the undo would restore soreness the lifter had replaced.
     *
     * What a restore of a *measurement* adds to that shape is the refusal: its identity is **its day and
     * its kind** (N32, N113), and the day it belongs to may have been re-entered since the delete. A
     * newer reading for that day and kind is a fact the lifter wrote, so the undo is refused with a
     * sentence rather than allowed to overwrite it — and "newer" is read from `createdAt`, because a
     * live row's `updatedAt` moves under an edit that was never a delete at all.
     */
    override suspend fun restore(id: String): DataResult<Unit> = dataResultOf {
        val removed = dao.findIncludingDeleted(id) ?: throw NotFoundException("measurement $id")
        val hiddenAt = removed.deletedAt
            ?: throw InvalidInputException("That measurement was not deleted")

        val (dayStart, dayEnd) = Instant.ofEpochMilli(removed.measuredAt)
            .localDayBounds(ZoneId.systemDefault())

        val now = nextWriteAt()
        database.withTransaction {
            // The guard is inside the transaction (B135): a save committing between the check and the
            // write would leave two live rows for one day and kind — the collision this refuses.
            if (dao.findLiveWrittenSince(dayStart, dayEnd, removed.kind.name, hiddenAt) != null) {
                throw InvalidInputException(
                    "That day has a newer ${removed.kind.name.lowercase()} measurement, " +
                        "so this one cannot come back.",
                )
            }
            if (dao.restore(id, hiddenAt, now) == 0) {
                throw NotFoundException("measurement $id")
            }
            // The rows the same delete hid, and nothing else: that is what the timestamp buys.
            soreDao.restoreForMeasurement(id, hiddenAt, now)
        }
    }
}

/** The picked list as stored rows, in the order it was picked (ROADMAP N62, N113). */
private fun List<SoreMuscle>.toRows(measurementId: String, now: Long): List<MeasurementSoreMuscleEntity> =
    mapIndexed { index, sore ->
        MeasurementSoreMuscleEntity(
            id = UUID.randomUUID().toString(),
            measurementId = measurementId,
            muscle = sore.muscle,
            score = sore.score,
            position = index,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
        )
    }

private fun MeasurementEntity.toDomain(soreRows: List<MeasurementSoreMuscleEntity>) = BodyMeasurement(
    id = id,
    measuredAt = Instant.ofEpochMilli(measuredAt),
    kind = kind,
    weightGrams = weightGrams,
    bodyFatTenths = bodyFatTenths,
    muscleTenths = muscleTenths,
    tape = buildMap {
        neckMm?.let { put(TapeSite.NECK, it) }
        chestMm?.let { put(TapeSite.CHEST, it) }
        waistMm?.let { put(TapeSite.WAIST, it) }
        hipsMm?.let { put(TapeSite.HIPS, it) }
        upperArmMm?.let { put(TapeSite.UPPER_ARM, it) }
        thighMm?.let { put(TapeSite.THIGH, it) }
        calfMm?.let { put(TapeSite.CALF, it) }
    },
    soreMuscles = soreRows.sortedBy { it.position }.map { SoreMuscle(it.muscle, it.score) },
)

private fun BodyMeasurement.toEntity(id: String, createdAt: Long, now: Long) = MeasurementEntity(
    id = id,
    measuredAt = measuredAt.toEpochMilli(),
    kind = kind,
    weightGrams = weightGrams,
    bodyFatTenths = bodyFatTenths,
    muscleTenths = muscleTenths,
    // An absent site stays absent: carrying a value forward would invent a measurement. And a daily
    // entry states no site at all, so the split's halves cannot drift into each other (N113).
    neckMm = tape[TapeSite.NECK].takeIf { kind == MeasurementKind.TAPE },
    chestMm = tape[TapeSite.CHEST].takeIf { kind == MeasurementKind.TAPE },
    waistMm = tape[TapeSite.WAIST].takeIf { kind == MeasurementKind.TAPE },
    hipsMm = tape[TapeSite.HIPS].takeIf { kind == MeasurementKind.TAPE },
    upperArmMm = tape[TapeSite.UPPER_ARM].takeIf { kind == MeasurementKind.TAPE },
    thighMm = tape[TapeSite.THIGH].takeIf { kind == MeasurementKind.TAPE },
    calfMm = tape[TapeSite.CALF].takeIf { kind == MeasurementKind.TAPE },
    createdAt = createdAt,
    updatedAt = now,
    deletedAt = null,
)
