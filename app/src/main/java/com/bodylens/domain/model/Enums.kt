package com.bodylens.domain.model

enum class Sex { MALE, FEMALE }

enum class FitnessGoal { FAT_LOSS, MUSCLE_GAIN, RECOMPOSITION, ENDURANCE, GENERAL_HEALTH }

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

enum class Equipment { BODYWEIGHT, DUMBBELLS, FULL_GYM, RESISTANCE_BANDS }

enum class FocusArea { UPPER_BODY, LOWER_BODY, CORE, FULL_BODY }

/** Camera pose required for a complete capture session. */
enum class CaptureAngle { FRONT, SIDE }

/**
 * Individual muscle groups, matching the free-exercise-db taxonomy so the bundled
 * catalog maps 1:1. Each muscle rolls up into a [FocusArea] via [group] for splits.
 */
enum class Muscle {
    ABDOMINALS, ABDUCTORS, ADDUCTORS, BICEPS, CALVES, CHEST, FOREARMS, GLUTES,
    HAMSTRINGS, LATS, LOWER_BACK, MIDDLE_BACK, NECK, QUADRICEPS, SHOULDERS, TRAPS, TRICEPS;

    /** Which split bucket this muscle belongs to. */
    fun group(): FocusArea = when (this) {
        CHEST, SHOULDERS, TRICEPS, BICEPS, LATS, MIDDLE_BACK, TRAPS, FOREARMS, NECK -> FocusArea.UPPER_BODY
        QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, ADDUCTORS, ABDUCTORS -> FocusArea.LOWER_BODY
        ABDOMINALS, LOWER_BACK -> FocusArea.CORE
    }
}

/** Direction of resistance for a movement (free-exercise-db `force`). May be unknown. */
enum class Force { PUSH, PULL, STATIC }

/** Whether a movement trains one joint or many (free-exercise-db `mechanic`). May be unknown. */
enum class Mechanic { COMPOUND, ISOLATION }

/** Movement category from free-exercise-db. */
enum class ExerciseCategory {
    STRENGTH, STRETCHING, PLYOMETRICS, POWERLIFTING, OLYMPIC_WEIGHTLIFTING, STRONGMAN, CARDIO
}

/** How the weekly program is biased, derived from goal + body-fat category. */
enum class Emphasis { HYPERTROPHY, METABOLIC, BALANCED }
