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
import com.example.androidapp.domain.model.localDay
import com.example.androidapp.domain.repository.MeasurementRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
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
        val now = timeSource.nowEpochMillis()
        // One entry of each kind for a day, so a tape entry edits its own row rather than the one the
        // scale wrote (N113).
        val existing = dao.find(measurement.id)
            ?: dao.findOnDay(
                from = startOfDay(measurement.measuredAt),
                to = startOfDay(measurement.measuredAt.plusSeconds(SECONDS_PER_DAY)),
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
        val now = timeSource.nowEpochMillis()
        database.withTransaction {
            if (dao.softDelete(id, now) == 0) {
                throw NotFoundException("measurement $id")
            }
            // The rows hang off the entry, so they go with it (the FK only cascades a hard delete).
            soreDao.softDeleteForMeasurement(id, now)
        }
    }

    /**
     * Midnight where the entry was taken, which is the day it belongs to.
     *
     * The device's own zone, read at the moment of writing: N25's rule for a session's time, applied to
     * a measurement's date.
     */
    private fun startOfDay(instant: Instant): Long {
        val zone = ZoneId.systemDefault()
        return instant.localDay(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

private const val SECONDS_PER_DAY = 86_400L

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
