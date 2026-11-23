package com.example.androidapp.data

import com.example.androidapp.data.local.WorkoutDatabase

/**
 * Puts a merged plan's live rows on one position each (N358, N361).
 *
 * A backup restore or a program-document add inserts template exercises and program slots with the
 * file's own `position`. The primary key is the row id, so a row whose position a live row already
 * holds is inserted anyway and the plan has two rows in one place — the order is ambiguous, and the
 * stricter document boundary refuses a document exported from that device. The sets half of the same
 * merge is renumbered in the import itself; this is the two position spaces beside it.
 *
 * The live rows are sorted by their current position (ties by id, which only duplicate positions
 * create), so a plan whose positions are already unique keeps its order and only gaps and duplicates
 * are closed. The order is per template, and per program **and phase week**: a phased program has a
 * place zero in every week.
 */
internal suspend fun renumberPlanPositions(
    database: WorkoutDatabase,
    templateIds: Set<String>,
    programIds: Set<String>,
    now: Long,
) {
    val templates = database.templateDao()
    templateIds.forEach { templateId ->
        templates.findTemplateExercises(templateId)
            .sortedWith(compareBy({ it.position }, { it.id }))
            .forEachIndexed { position, row ->
                if (row.position != position) templates.setPosition(row.id, position, now)
            }
    }

    val programs = database.programDao()
    programIds.forEach { programId ->
        programs.findSlots(programId)
            .groupBy { it.phaseWeek }
            .forEach { (_, rows) ->
                rows.sortedWith(compareBy({ it.position }, { it.id }))
                    .forEachIndexed { position, row ->
                        if (row.position != position) {
                            programs.setSlotPosition(row.id, position, now)
                        }
                    }
            }
    }
}
