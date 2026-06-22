package com.bodylens.di

import com.bodylens.data.repository.*
import com.bodylens.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun analysisRepository(impl: AnalysisRepositoryImpl): AnalysisRepository

    @Binds @Singleton
    abstract fun progressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds @Singleton
    abstract fun userRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun exercisePlanRepository(impl: ExercisePlanRepositoryImpl): ExercisePlanRepository
}
