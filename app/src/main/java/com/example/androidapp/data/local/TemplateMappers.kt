package com.example.androidapp.data.local

import com.example.androidapp.domain.model.WorkoutTemplate
import java.time.Instant

/**
 * Translation between the template storage types and the domain model (ROADMAP N3).
 *
 * Same boundary argument as the exercise mappers: the domain type stays free of
 * persistence metadata, so a storage change cannot ripple into the UI.
 * `TemplateExerciseDetail`'s mapper lives beside it.
 */
internal fun TemplateSummaryRow.toDomain(): WorkoutTemplate = WorkoutTemplate(
    id = id,
    name = name,
    exerciseCount = exerciseCount,
    versionOf = versionOf,
    // Only a version keeps the timestamp: a template's age is a fact nothing reads, and null is what
    // says "this row is a template" to the surface that shows the two (ROADMAP N134).
    keptAt = versionOf?.let { Instant.ofEpochMilli(createdAt) },
)
