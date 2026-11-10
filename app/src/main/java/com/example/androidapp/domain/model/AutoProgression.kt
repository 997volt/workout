package com.example.androidapp.domain.model

/**
 * One exercise the enablement preflight can say something about (ROADMAP N299).
 *
 * The rule is the plan's own sentence, read through [progressionRuleFor], and [mode] is the
 * movement's answer where it has one. Both matter: a plan that could progress, on a movement that
 * says *Ask*, will not be adjusted by the program's switch.
 */
data class AutoProgressionCheck(
    val templateName: String,
    val exerciseName: String,
    val rule: ProgressionRule,
    val mode: ProgressionMode? = null,
)

/**
 * What the preflight groups the program's exercises into (ROADMAP N299).
 *
 * Three groups rather than one list because each asks the lifter to do something different:
 * nothing (the plan will move), fix the plan (no target effort, no reps, nothing to move), or
 * follow the movement's own answer.
 */
data class AutoProgressionPreflight(val checks: List<AutoProgressionCheck> = emptyList()) {

    /** Exercises the program's switch will adjust. */
    val willProgress: List<AutoProgressionCheck>
        get() = checks.filter { it.mode == null && it.rule.progresses }

    /** Exercises it cannot adjust until the plan is fixed. */
    val blocked: List<AutoProgressionCheck>
        get() = checks.filter { it.mode == null && !it.rule.progresses }

    /** Exercises whose movement answers for itself, so the program's switch does not decide. */
    val overridden: List<AutoProgressionCheck>
        get() = checks.filter { it.mode != null }

    /** True when the switch would have nothing to do at all. */
    val hasNothing: Boolean get() = willProgress.isEmpty()
}

/** Whether this plan can move at all with the targets it wrote (N299's preflight). */
val ProgressionRule.progresses: Boolean
    get() = when (this) {
        ProgressionRule.REPS_THEN_LOAD,
        ProgressionRule.REPS_ONLY,
        ProgressionRule.LOAD_ONLY,
        ProgressionRule.HOLD,
        -> true

        ProgressionRule.NO_TARGET_RPE,
        ProgressionRule.NO_REPS,
        ProgressionRule.NOTHING_TO_MOVE,
        -> false
    }

/**
 * Reads a program's plans into the groups the enablement sheet shows (ROADMAP N299).
 *
 * One row per **planned exercise**, deduplicated by template: two slots naming one workout share one
 * plan, so a preflight that listed it twice would promise two adjustments where one happens. A
 * movement the template names twice is two rows, because it is two prescriptions.
 */
fun autoProgressionPreflight(
    slots: List<ProgramSlot>,
    exercisesByTemplate: Map<String, List<TemplateExercise>>,
): AutoProgressionPreflight {
    val checks = slots.distinctBy { it.templateId }.flatMap { slot ->
        exercisesByTemplate[slot.templateId].orEmpty().map { exercise ->
            AutoProgressionCheck(
                templateName = slot.templateName,
                exerciseName = exercise.exerciseName,
                rule = progressionRuleFor(exercise.targetRpeHalves, exercise.progressionSets()),
                mode = exercise.progressionMode,
            )
        }
    }
    return AutoProgressionPreflight(checks)
}
