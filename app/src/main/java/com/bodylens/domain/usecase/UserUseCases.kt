package com.bodylens.domain.usecase

import com.bodylens.domain.model.UserProfile
import com.bodylens.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    operator fun invoke(): Flow<UserProfile> = repository.observeProfile()
}

class UpdateUserProfileUseCase @Inject constructor(
    private val repository: UserRepository,
) {
    suspend operator fun invoke(transform: (UserProfile) -> UserProfile) =
        repository.updateProfile(transform)
}

class GeneratePlanUseCase @Inject constructor(
    private val repository: com.bodylens.domain.repository.ExercisePlanRepository,
) {
    suspend operator fun invoke(profile: UserProfile) = repository.generatePlan(profile)
}
