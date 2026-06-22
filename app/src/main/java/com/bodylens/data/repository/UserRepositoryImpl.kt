package com.bodylens.data.repository

import com.bodylens.data.local.datastore.UserPreferencesDataStore
import com.bodylens.domain.model.UserProfile
import com.bodylens.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dataStore: UserPreferencesDataStore,
) : UserRepository {
    override fun observeProfile(): Flow<UserProfile> = dataStore.profile

    override suspend fun updateProfile(transform: (UserProfile) -> UserProfile) {
        val current = dataStore.profile.first()
        dataStore.update(transform(current))
    }

    override suspend fun clear() = dataStore.clear()
}
