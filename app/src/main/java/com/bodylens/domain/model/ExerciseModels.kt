package com.bodylens.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val focusArea: FocusArea,
    val requiredEquipment: Equipment,
    val minLevel: ExperienceLevel,
    /** Goals this movement primarily serves. */
    val goals: Set<FitnessGoal>,
    /** Muscles trained directly. Drives weekly-volume coverage. */
    val primaryMuscles: Set<Muscle> = emptySet(),
    /** Muscles assisting the movement; counted at half weight toward volume. */
    val secondaryMuscles: Set<Muscle> = emptySet(),
    val force: Force? = null,
    val mechanic: Mechanic? = null,
    val category: ExerciseCategory = ExerciseCategory.STRENGTH,
    /** Step-by-step cues, available for the UI. */
    val instructions: List<String> = emptyList(),
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

/** Planned vs. recommended weekly working sets for one muscle. */
data class MuscleVolume(
    val muscle: Muscle,
    val plannedSets: Int,
    val targetSets: Int,
) {
    val meetsTarget: Boolean get() = plannedSets >= targetSets
}

/**
 * Tunable training parameters derived from the user's goal, body-fat category,
 * experience and weekly availability. All values are pre-clamped to safe ranges.
 */
data class TrainingParameters(
    val emphasis: Emphasis,
    val weeklySetsPerMuscle: Int,
    val repRange: String,
    val restSeconds: Int,
    val setsPerExercise: Int,
    val cardioZone2Minutes: Int,
)

data class ExercisePlan(
    val goal: FitnessGoal,
    val daysPerWeek: Int,
    val emphasis: Emphasis,
    /** Weekly Zone-2 cardio recommendation in minutes. */
    val cardioZone2Minutes: Int,
    val days: List<WorkoutDay>,
    /** Per-muscle weekly volume the plan actually delivers vs. its target. */
    val weeklyVolume: List<MuscleVolume> = emptyList(),
)