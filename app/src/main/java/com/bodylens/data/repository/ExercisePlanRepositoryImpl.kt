package com.bodylens.data.repository

import com.bodylens.data.catalog.ExerciseCatalogLoader
import com.bodylens.domain.model.*
import com.bodylens.domain.repository.ExercisePlanRepository
import com.bodylens.domain.service.ExercisePlanner
import javax.inject.Inject

class ExercisePlanRepositoryImpl @Inject constructor(
    private val loader: ExerciseCatalogLoader,
    private val planner: ExercisePlanner,
) : ExercisePlanRepository {

    override suspend fun catalog(): List<Exercise> = loader.catalog()

    override suspend fun generatePlan(
        profile: UserProfile,
        bodyFatCategory: BodyFatCategory?,
    ): ExercisePlan = planner.generate(
        catalog = loader.catalog(),
        goal = profile.goal,
        bodyFatCategory = bodyFatCategory,
        experience = profile.experience,
        equipment = profile.equipment,
        focus = profile.focusArea,
        daysPerWeek = profile.daysPerWeek,
    )
}