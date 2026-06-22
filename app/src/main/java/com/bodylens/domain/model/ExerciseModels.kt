package com.bodylens.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val focusArea: FocusArea,
    val requiredEquipment: Equipment,
    val minLevel: ExperienceLevel,
    /** Goals this movement primarily serves. */
    val goals: Set<FitnessGoal>,
)

data class ExercisePrescription(
    val exercise: Exercise,
    val sets: Int,
    val reps: String,        // e.g. "8-12" or "30s"
    val restSeconds: Int,
)

data class WorkoutDay(
    val dayLabel: String,    // e.g. "Day 1 - Upper"
    val focusArea: FocusArea,
    val prescriptions: List<ExercisePrescription>,
)

data class ExercisePlan(
    val goal: FitnessGoal,
    val daysPerWeek: Int,
    val days: List<WorkoutDay>,
)
