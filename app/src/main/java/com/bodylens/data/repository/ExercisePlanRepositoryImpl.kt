package com.bodylens.data.repository

import com.bodylens.domain.model.*
import com.bodylens.domain.repository.ExercisePlanRepository
import com.bodylens.domain.service.ExercisePlanner
import javax.inject.Inject

class ExercisePlanRepositoryImpl @Inject constructor(
    private val planner: ExercisePlanner,
) : ExercisePlanRepository {

    override suspend fun catalog(): List<Exercise> = DefaultCatalog.exercises

    override suspend fun generatePlan(profile: UserProfile): ExercisePlan = planner.generate(
        catalog = DefaultCatalog.exercises,
        goal = profile.goal,
        experience = profile.experience,
        equipment = profile.equipment,
        focus = profile.focusArea,
        daysPerWeek = profile.daysPerWeek,
    )
}

/** A small starter catalog. Extend or move to a bundled JSON/Room table later. */
private object DefaultCatalog {
    private val allGoals = FitnessGoal.entries.toSet()
    val exercises = listOf(
        Exercise("pushup", "Push-up", FocusArea.UPPER_BODY, Equipment.BODYWEIGHT, ExperienceLevel.BEGINNER, allGoals),
        Exercise("squat", "Bodyweight Squat", FocusArea.LOWER_BODY, Equipment.BODYWEIGHT, ExperienceLevel.BEGINNER, allGoals),
        Exercise("plank", "Plank", FocusArea.CORE, Equipment.BODYWEIGHT, ExperienceLevel.BEGINNER, allGoals),
        Exercise("lunge", "Walking Lunge", FocusArea.LOWER_BODY, Equipment.BODYWEIGHT, ExperienceLevel.BEGINNER, allGoals),
        Exercise("db_press", "Dumbbell Bench Press", FocusArea.UPPER_BODY, Equipment.DUMBBELLS, ExperienceLevel.INTERMEDIATE, setOf(FitnessGoal.MUSCLE_GAIN, FitnessGoal.RECOMPOSITION)),
        Exercise("db_row", "Dumbbell Row", FocusArea.UPPER_BODY, Equipment.DUMBBELLS, ExperienceLevel.BEGINNER, allGoals),
        Exercise("db_rdl", "Dumbbell Romanian Deadlift", FocusArea.LOWER_BODY, Equipment.DUMBBELLS, ExperienceLevel.INTERMEDIATE, setOf(FitnessGoal.MUSCLE_GAIN, FitnessGoal.RECOMPOSITION)),
        Exercise("band_pull", "Band Pull-apart", FocusArea.UPPER_BODY, Equipment.RESISTANCE_BANDS, ExperienceLevel.BEGINNER, allGoals),
        Exercise("barbell_squat", "Barbell Back Squat", FocusArea.LOWER_BODY, Equipment.FULL_GYM, ExperienceLevel.INTERMEDIATE, setOf(FitnessGoal.MUSCLE_GAIN, FitnessGoal.RECOMPOSITION)),
        Exercise("deadlift", "Barbell Deadlift", FocusArea.FULL_BODY, Equipment.FULL_GYM, ExperienceLevel.ADVANCED, setOf(FitnessGoal.MUSCLE_GAIN, FitnessGoal.RECOMPOSITION)),
        Exercise("burpee", "Burpee", FocusArea.FULL_BODY, Equipment.BODYWEIGHT, ExperienceLevel.INTERMEDIATE, setOf(FitnessGoal.FAT_LOSS, FitnessGoal.ENDURANCE)),
        Exercise("mountain_climber", "Mountain Climber", FocusArea.CORE, Equipment.BODYWEIGHT, ExperienceLevel.BEGINNER, setOf(FitnessGoal.FAT_LOSS, FitnessGoal.ENDURANCE, FitnessGoal.GENERAL_HEALTH)),
    )
}
