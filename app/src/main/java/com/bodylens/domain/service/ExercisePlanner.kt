package com.bodylens.domain.service

import com.bodylens.domain.model.*

/**
 * Rules + coverage-scoring weekly plan generator. Pure, deterministic, fully offline.
 *
 * Two stages:
 *  1. [deriveParameters] turns the user's goal, body-fat category, experience and
 *     weekly availability into safe, clamped training parameters (emphasis, weekly
 *     sets-per-muscle, rep/rest scheme, cardio minutes).
 *  2. [generate] distributes movements across the week as a weighted muscle-coverage
 *     problem — each day greedily picks the exercises that best fill the still-unmet
 *     weekly volume for that day's muscles, preferring compounds for time efficiency.
 *
 * Body composition is a calibration MODIFIER (how hard/what emphasis), never the
 * exercise selector. Every output is clamped to evidence-based, body-neutral ranges.
 */
class ExercisePlanner {

    fun generate(
        catalog: List<Exercise>,
        goal: FitnessGoal,
        bodyFatCategory: BodyFatCategory?,
        experience: ExperienceLevel,
        equipment: Equipment,
        focus: FocusArea,
        daysPerWeek: Int,
    ): ExercisePlan {
        val days = daysPerWeek.coerceIn(2, 6)
        val params = deriveParameters(goal, bodyFatCategory, experience, days)

        val eligible = catalog.filter { ex ->
            ex.requiredEquipment.isAvailableWith(equipment) &&
                experience.meets(ex.minLevel) &&
                goal in ex.goals &&
                ex.primaryMuscles.isNotEmpty()
        }

        val split = splitFor(days, focus)
        val targetMuscles = split.flatMap { musclesIn(it) }.toSet()
        val remaining = targetMuscles.associateWith { params.weeklySetsPerMuscle }.toMutableMap()

        val workoutDays = split.mapIndexed { index, area ->
            val sessionMuscles = musclesIn(area)
            val pool = eligible
                .filter { (it.focusArea == area || it.focusArea == FocusArea.FULL_BODY) }
                .filter { it.primaryMuscles.any { m -> m in sessionMuscles } }
                .ifEmpty { eligible }
            val picked = selectForDay(pool, sessionMuscles, remaining, params)
            WorkoutDay(
                dayLabel = "Day ${index + 1} - ${area.label()}",
                focusArea = area,
                prescriptions = picked.map {
                    ExercisePrescription(it, params.setsPerExercise, params.repRange, params.restSeconds)
                },
            )
        }

        return ExercisePlan(
            goal = goal,
            daysPerWeek = days,
            emphasis = params.emphasis,
            cardioZone2Minutes = params.cardioZone2Minutes,
            days = workoutDays,
            weeklyVolume = summarizeVolume(targetMuscles, workoutDays, params.weeklySetsPerMuscle),
        )
    }

    /**
     * Derive clamped training parameters. Body-fat category nudges emphasis, volume
     * and cardio; it never expands them beyond safe limits.
     */
    fun deriveParameters(
        goal: FitnessGoal,
        category: BodyFatCategory?,
        level: ExperienceLevel,
        daysPerWeek: Int,
    ): TrainingParameters {
        val highFat = category == BodyFatCategory.AVERAGE || category == BodyFatCategory.ABOVE_AVERAGE

        val emphasis = when (goal) {
            FitnessGoal.MUSCLE_GAIN -> Emphasis.HYPERTROPHY
            FitnessGoal.FAT_LOSS, FitnessGoal.ENDURANCE -> Emphasis.METABOLIC
            FitnessGoal.RECOMPOSITION -> Emphasis.BALANCED
            FitnessGoal.GENERAL_HEALTH -> if (highFat) Emphasis.METABOLIC else Emphasis.BALANCED
        }

        // Volume landmarks: ~10 sets/muscle/week (beginner) up to ~18-20 (advanced).
        val baseSets = when (level) {
            ExperienceLevel.BEGINNER -> 10
            ExperienceLevel.INTERMEDIATE -> 14
            ExperienceLevel.ADVANCED -> 18
        }
        val emphasisAdj = when (emphasis) {
            Emphasis.HYPERTROPHY -> 2
            Emphasis.METABOLIC -> -2   // preserve lean mass, don't chase maximal volume
            Emphasis.BALANCED -> 0
        }
        val weeklySets = (baseSets + emphasisAdj)
            .coerceIn(8, 20)
            .coerceAtMost(daysPerWeek * 6) // recovery / time ceiling

        val (repRange, rest, setsPerExercise) = when (emphasis) {
            Emphasis.HYPERTROPHY -> Triple("8-12", 90, 4)
            Emphasis.METABOLIC -> Triple("12-20", 45, 3)
            Emphasis.BALANCED -> Triple("10-12", 60, 3)
        }

        // ACSM floor is ~150 min/week moderate activity; fat-loss with higher body
        // fat trends to the upper end. Muscle gain keeps a health-maintenance dose.
        val cardio = when {
            goal == FitnessGoal.FAT_LOSS && highFat -> 180
            goal == FitnessGoal.FAT_LOSS -> 150
            goal == FitnessGoal.ENDURANCE -> 150
            goal == FitnessGoal.GENERAL_HEALTH -> 150
            highFat -> 150
            else -> 90
        }.coerceIn(90, 240)

        return TrainingParameters(
            emphasis = emphasis,
            weeklySetsPerMuscle = weeklySets,
            repRange = repRange,
            restSeconds = rest,
            setsPerExercise = setsPerExercise,
            cardioZone2Minutes = cardio,
        )
    }

    /**
     * Greedy weighted set-cover for one day: repeatedly take the exercise that best
     * fills the still-unmet weekly volume of this day's muscles, decrementing the
     * weekly remainder as it goes. Compounds are favoured for time efficiency.
     */
    private fun selectForDay(
        pool: List<Exercise>,
        sessionMuscles: Set<Muscle>,
        remaining: MutableMap<Muscle, Int>,
        params: TrainingParameters,
    ): List<Exercise> {
        val picked = mutableListOf<Exercise>()
        val pickedIds = mutableSetOf<String>()
        val secondarySets = params.setsPerExercise / 2

        while (picked.size < MAX_PER_DAY) {
            var best: Exercise? = null
            var bestScore = 0.0
            for (ex in pool) {
                if (ex.id in pickedIds) continue
                val score = coverageScore(ex, sessionMuscles, remaining, params.setsPerExercise) +
                    if (ex.mechanic == Mechanic.COMPOUND) COMPOUND_BONUS else 0.0
                if (score > bestScore) {
                    bestScore = score
                    best = ex
                }
            }
            // Stop once nothing still meaningfully serves the day's unmet volume.
            if (best == null || bestScore <= STOP_THRESHOLD) {
                if (picked.size >= MIN_PER_DAY) break
                // Below the minimum: take the single best remaining movement regardless.
                best = pool.firstOrNull { it.id !in pickedIds } ?: break
            }
            picked += best
            pickedIds += best.id
            for (m in best.primaryMuscles) {
                if (m in remaining) remaining[m] = (remaining[m]!! - params.setsPerExercise).coerceAtLeast(0)
            }
            for (m in best.secondaryMuscles) {
                if (m in remaining) remaining[m] = (remaining[m]!! - secondarySets).coerceAtLeast(0)
            }
        }
        return picked
    }

    /** Unmet-volume value an exercise delivers for the current day's muscles. */
    private fun coverageScore(
        ex: Exercise,
        sessionMuscles: Set<Muscle>,
        remaining: Map<Muscle, Int>,
        setsPerExercise: Int,
    ): Double {
        var score = 0.0
        for (m in ex.primaryMuscles) {
            if (m in sessionMuscles) score += minOf(remaining[m] ?: 0, setsPerExercise).toDouble()
        }
        for (m in ex.secondaryMuscles) {
            if (m in sessionMuscles) score += 0.5 * minOf(remaining[m] ?: 0, setsPerExercise / 2).toDouble()
        }
        return score
    }

    private fun summarizeVolume(
        targetMuscles: Set<Muscle>,
        days: List<WorkoutDay>,
        target: Int,
    ): List<MuscleVolume> {
        val planned = targetMuscles.associateWith { 0 }.toMutableMap()
        for (day in days) {
            for (rx in day.prescriptions) {
                for (m in rx.exercise.primaryMuscles) {
                    if (m in planned) planned[m] = planned[m]!! + rx.sets
                }
                for (m in rx.exercise.secondaryMuscles) {
                    if (m in planned) planned[m] = planned[m]!! + rx.sets / 2
                }
            }
        }
        return targetMuscles
            .map { MuscleVolume(it, planned[it] ?: 0, target) }
            .sortedWith(compareBy({ it.muscle.group().ordinal }, { it.muscle.name }))
    }

    private fun musclesIn(area: FocusArea): Set<Muscle> = when (area) {
        FocusArea.FULL_BODY -> Muscle.entries.toSet()
        else -> Muscle.entries.filter { it.group() == area }.toSet()
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

    private companion object {
        const val MIN_PER_DAY = 3
        const val MAX_PER_DAY = 6
        const val COMPOUND_BONUS = 1.0
        const val STOP_THRESHOLD = 0.5
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