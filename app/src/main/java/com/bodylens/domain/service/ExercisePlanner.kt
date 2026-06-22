package com.bodylens.domain.service

import com.bodylens.domain.model.*

/**
 * Rules-based weekly plan generator. Pure and offline.
 *
 * Filters a catalog by equipment + experience + goal, then distributes movements
 * across the requested number of training days using a simple split strategy.
 * (This is the deterministic baseline; an optional LLM layer can later refine
 * the TEXT of the plan from these structured inputs — never from user photos.)
 */
class ExercisePlanner {

    fun generate(
        catalog: List<Exercise>,
        goal: FitnessGoal,
        experience: ExperienceLevel,
        equipment: Equipment,
        focus: FocusArea,
        daysPerWeek: Int,
    ): ExercisePlan {
        val days = daysPerWeek.coerceIn(2, 6)
        val eligible = catalog.filter { ex ->
            ex.requiredEquipment.isAvailableWith(equipment) &&
                experience.meets(ex.minLevel) &&
                (goal in ex.goals)
        }

        val split = splitFor(days, focus)
        val rx = prescriptionFor(goal, experience)

        val workoutDays = split.mapIndexed { index, area ->
            val forArea = eligible
                .filter { it.focusArea == area || it.focusArea == FocusArea.FULL_BODY }
                .ifEmpty { eligible }
                .take(5)
            WorkoutDay(
                dayLabel = "Day ${index + 1} - ${area.label()}",
                focusArea = area,
                prescriptions = forArea.map { ExercisePrescription(it, rx.sets, rx.reps, rx.rest) },
            )
        }
        return ExercisePlan(goal = goal, daysPerWeek = days, days = workoutDays)
    }

    private data class Rx(val sets: Int, val reps: String, val rest: Int)

    private fun prescriptionFor(goal: FitnessGoal, level: ExperienceLevel): Rx {
        val base = when (goal) {
            FitnessGoal.MUSCLE_GAIN -> Rx(4, "8-12", 90)
            FitnessGoal.FAT_LOSS -> Rx(3, "12-15", 45)
            FitnessGoal.RECOMPOSITION -> Rx(3, "10-12", 60)
            FitnessGoal.ENDURANCE -> Rx(3, "15-20", 30)
            FitnessGoal.GENERAL_HEALTH -> Rx(3, "10-12", 60)
        }
        return if (level == ExperienceLevel.BEGINNER) base.copy(sets = (base.sets - 1).coerceAtLeast(2)) else base
    }

    private fun splitFor(days: Int, focus: FocusArea): List<FocusArea> {
        if (focus != FocusArea.FULL_BODY) return List(days) { focus }
        return when (days) {
            2 -> listOf(FocusArea.UPPER_BODY, FocusArea.LOWER_BODY)
            3 -> listOf(FocusArea.FULL_BODY, FocusArea.FULL_BODY, FocusArea.FULL_BODY)
            4 -> listOf(FocusArea.UPPER_BODY, FocusArea.LOWER_BODY, FocusArea.UPPER_BODY, FocusArea.LOWER_BODY)
            5 -> listOf(FocusArea.UPPER_BODY, FocusArea.LOWER_BODY, FocusArea.CORE, FocusArea.UPPER_BODY, FocusArea.LOWER_BODY)
            else -> listOf(
                FocusArea.UPPER_BODY, FocusArea.LOWER_BODY, FocusArea.CORE,
                FocusArea.UPPER_BODY, FocusArea.LOWER_BODY, FocusArea.FULL_BODY,
            )
        }
    }
}

private fun Equipment.isAvailableWith(owned: Equipment): Boolean = when (owned) {
    Equipment.FULL_GYM -> true
    Equipment.DUMBBELLS -> this == Equipment.DUMBBELLS || this == Equipment.BODYWEIGHT
    Equipment.RESISTANCE_BANDS -> this == Equipment.RESISTANCE_BANDS || this == Equipment.BODYWEIGHT
    Equipment.BODYWEIGHT -> this == Equipment.BODYWEIGHT
}

private fun ExperienceLevel.meets(min: ExperienceLevel): Boolean = ordinal >= min.ordinal

private fun FocusArea.label() = when (this) {
    FocusArea.UPPER_BODY -> "Upper"
    FocusArea.LOWER_BODY -> "Lower"
    FocusArea.CORE -> "Core"
    FocusArea.FULL_BODY -> "Full Body"
}
