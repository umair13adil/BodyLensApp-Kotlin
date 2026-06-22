package com.bodylens.domain.repository

import com.bodylens.core.common.AppResult
import com.bodylens.domain.model.*
import kotlinx.coroutines.flow.Flow

/** Turns captured photos into measurements + composition, entirely on-device. */
interface AnalysisRepository {
    suspend fun analyze(
        frontPhotoUri: String,
        sidePhotoUri: String?,
        heightCm: Double,
        sex: Sex,
    ): AppResult<AnalysisResult>
}

/** Local-only persistence of progress entries (Room). */
interface ProgressRepository {
    fun observeHistory(): Flow<List<ProgressEntry>>
    suspend fun save(entry: ProgressEntry): Long
    suspend fun deleteAll()
}

/** Source of the exercise catalog + plan generation. */
interface ExercisePlanRepository {
    suspend fun catalog(): List<Exercise>
    suspend fun generatePlan(profile: UserProfile): ExercisePlan
}

/** Profile + onboarding state (DataStore). */
interface UserRepository {
    fun observeProfile(): Flow<UserProfile>
    suspend fun updateProfile(transform: (UserProfile) -> UserProfile)
    suspend fun clear()
}
