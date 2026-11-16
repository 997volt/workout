package com.example.androidapp.data.local

/**
 * The SQL a DAO annotation runs, also planned by `IndexUsageTest` (N324).
 *
 * The test cannot capture the generated DAO's statement on Room's driver path, so the production
 * query and the index test share one constant instead of copying the text.
 */
internal object QuerySql {

    /** The query `ProgramDao.sessionsStartedBetween` runs. */
    const val SESSIONS_STARTED_BETWEEN = """
        SELECT id AS sessionId,
               templateId AS templateId,
               startedAt AS startedAt,
               zoneOffsetMinutes AS zoneOffsetMinutes
        FROM workout_sessions
        WHERE templateId IS NOT NULL
          AND deletedAt IS NULL
          AND startedAt >= :fromMillis
          AND startedAt < :toMillis
        ORDER BY startedAt ASC
    """

    /** The query `MeasurementDao.findOnDay` runs. */
    const val FIND_MEASUREMENT_ON_DAY = """
        SELECT * FROM measurements
        WHERE measuredAt >= :from AND measuredAt < :to AND deletedAt IS NULL AND kind = :kind
        ORDER BY measuredAt DESC LIMIT 1
    """
}
