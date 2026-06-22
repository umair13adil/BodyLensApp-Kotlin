package com.bodylens.domain.model

enum class Sex { MALE, FEMALE }

enum class FitnessGoal { FAT_LOSS, MUSCLE_GAIN, RECOMPOSITION, ENDURANCE, GENERAL_HEALTH }

enum class ExperienceLevel { BEGINNER, INTERMEDIATE, ADVANCED }

enum class Equipment { BODYWEIGHT, DUMBBELLS, FULL_GYM, RESISTANCE_BANDS }

enum class FocusArea { UPPER_BODY, LOWER_BODY, CORE, FULL_BODY }

/** Camera pose required for a complete capture session. */
enum class CaptureAngle { FRONT, SIDE }
