package com.bodylens.domain.model

/** Onboarding/profile state. Age verification is mandatory before any capture. */
data class UserProfile(
    val ageVerified18Plus: Boolean,
    val onboardingComplete: Boolean,
    val sex: Sex?,
    val heightCm: Double?,
    val goal: FitnessGoal,
    val experience: ExperienceLevel,
    val equipment: Equipment,
    val focusArea: FocusArea,
    val daysPerWeek: Int,
) {
    companion object {
        val EMPTY = UserProfile(
            ageVerified18Plus = false,
            onboardingComplete = false,
            sex = null,
            heightCm = null,
            goal = FitnessGoal.GENERAL_HEALTH,
            experience = ExperienceLevel.BEGINNER,
            equipment = Equipment.BODYWEIGHT,
            focusArea = FocusArea.FULL_BODY,
            daysPerWeek = 3,
        )
    }
}

/** A stored progress point. Photos are referenced by a local-only URI, never uploaded. */
data class ProgressEntry(
    val id: Long = 0,
    val createdAtEpochMs: Long,
    val frontPhotoUri: String?,
    val sidePhotoUri: String?,
    val measurements: com.bodylens.domain.model.BodyMeasurements,
    val bodyFatPercent: Double?,
)
