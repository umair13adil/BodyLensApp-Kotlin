package com.bodylens.data.catalog

import com.bodylens.domain.model.Equipment
import com.bodylens.domain.model.Exercise
import com.bodylens.domain.model.ExerciseCategory
import com.bodylens.domain.model.ExperienceLevel
import com.bodylens.domain.model.FitnessGoal
import com.bodylens.domain.model.FocusArea
import com.bodylens.domain.model.Force
import com.bodylens.domain.model.Mechanic
import com.bodylens.domain.model.Muscle
import kotlinx.serialization.Serializable

/**
 * Raw shape of an entry in the bundled free-exercise-db `exercises.json`
 * (public domain). Unknown fields (e.g. images) are ignored by the parser.
 */
@Serializable
data class ExerciseDto(
    val id: String,
    val name: String,
    val force: String? = null,
    val level: String? = null,
    val mechanic: String? = null,
    val equipment: String? = null,
    val primaryMuscles: List<String> = emptyList(),
    val secondaryMuscles: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val category: String? = null,
    val image: String? = null,
    val bodyPart: String? = null,
    val target: String? = null,
) {
    fun toDomain(): Exercise {
        val primary = primaryMuscles.mapNotNull { muscle(it) }.toSet()
        val secondary = secondaryMuscles.mapNotNull { muscle(it) }.toSet()
        val cat = category(category)
        val mech = mechanic(mechanic)
        return Exercise(
            id = id,
            name = name,
            focusArea = focusArea(primary),
            requiredEquipment = equipment(equipment),
            minLevel = level(level),
            goals = goals(cat, mech),
            primaryMuscles = primary,
            secondaryMuscles = secondary,
            force = force(force),
            mechanic = mech,
            category = cat,
            instructions = instructions,
            image = image,
            bodyPart = bodyPart,
            target = target,
        )
    }
}

private fun muscle(raw: String): Muscle? = when (raw.lowercase()) {
    "abdominals" -> Muscle.ABDOMINALS
    "abductors" -> Muscle.ABDUCTORS
    "adductors" -> Muscle.ADDUCTORS
    "biceps" -> Muscle.BICEPS
    "calves" -> Muscle.CALVES
    "chest" -> Muscle.CHEST
    "forearms" -> Muscle.FOREARMS
    "glutes" -> Muscle.GLUTES
    "hamstrings" -> Muscle.HAMSTRINGS
    "lats" -> Muscle.LATS
    "lower back" -> Muscle.LOWER_BACK
    "middle back" -> Muscle.MIDDLE_BACK
    "neck" -> Muscle.NECK
    "quadriceps" -> Muscle.QUADRICEPS
    "shoulders" -> Muscle.SHOULDERS
    "traps" -> Muscle.TRAPS
    "triceps" -> Muscle.TRICEPS
    else -> null
}

private fun force(raw: String?): Force? = when (raw?.lowercase()) {
    "push" -> Force.PUSH
    "pull" -> Force.PULL
    "static" -> Force.STATIC
    else -> null
}

private fun mechanic(raw: String?): Mechanic? = when (raw?.lowercase()) {
    "compound" -> Mechanic.COMPOUND
    "isolation" -> Mechanic.ISOLATION
    else -> null
}

private fun level(raw: String?): ExperienceLevel = when (raw?.lowercase()) {
    "intermediate" -> ExperienceLevel.INTERMEDIATE
    "expert" -> ExperienceLevel.ADVANCED
    else -> ExperienceLevel.BEGINNER
}

private fun category(raw: String?): ExerciseCategory = when (raw?.lowercase()) {
    "stretching" -> ExerciseCategory.STRETCHING
    "plyometrics" -> ExerciseCategory.PLYOMETRICS
    "powerlifting" -> ExerciseCategory.POWERLIFTING
    "olympic weightlifting" -> ExerciseCategory.OLYMPIC_WEIGHTLIFTING
    "strongman" -> ExerciseCategory.STRONGMAN
    "cardio" -> ExerciseCategory.CARDIO
    else -> ExerciseCategory.STRENGTH
}

/**
 * Collapse the dataset's fine-grained equipment list onto the four options the
 * user selects in onboarding. Anything that needs a gym/extra kit becomes FULL_GYM.
 */
private fun equipment(raw: String?): Equipment = when (raw?.lowercase()) {
    "body only", "body weight", "none", "assisted", "weighted", null -> Equipment.BODYWEIGHT
    "dumbbell", "kettlebells", "kettlebell" -> Equipment.DUMBBELLS
    "bands", "band", "resistance band" -> Equipment.RESISTANCE_BANDS
    else -> Equipment.FULL_GYM // barbell, cable, machine, e-z curl bar, balls, foam roll, other
}

private fun focusArea(primary: Set<Muscle>): FocusArea {
    if (primary.isEmpty()) return FocusArea.FULL_BODY
    val groups = primary.map { it.group() }.toSet()
    return groups.singleOrNull() ?: FocusArea.FULL_BODY
}

/** Map category + mechanic onto the goals a movement reasonably serves. */
private fun goals(category: ExerciseCategory, mechanic: Mechanic?): Set<FitnessGoal> = when (category) {
    ExerciseCategory.CARDIO, ExerciseCategory.PLYOMETRICS -> setOf(
        FitnessGoal.FAT_LOSS, FitnessGoal.ENDURANCE, FitnessGoal.GENERAL_HEALTH, FitnessGoal.RECOMPOSITION,
    )
    ExerciseCategory.STRETCHING -> setOf(FitnessGoal.GENERAL_HEALTH, FitnessGoal.ENDURANCE)
    else -> if (mechanic == Mechanic.ISOLATION) {
        setOf(FitnessGoal.MUSCLE_GAIN, FitnessGoal.RECOMPOSITION, FitnessGoal.GENERAL_HEALTH)
    } else {
        FitnessGoal.entries.toSet() // compound (or unknown) strength serves every goal
    }
}