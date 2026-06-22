package com.bodylens.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.bodylens.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val AGE_VERIFIED = booleanPreferencesKey("age_verified")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val SEX = stringPreferencesKey("sex")
        val HEIGHT = doublePreferencesKey("height_cm")
        val GOAL = stringPreferencesKey("goal")
        val EXPERIENCE = stringPreferencesKey("experience")
        val EQUIPMENT = stringPreferencesKey("equipment")
        val FOCUS = stringPreferencesKey("focus")
        val DAYS = intPreferencesKey("days_per_week")
    }

    val profile: Flow<UserProfile> = context.dataStore.data.map { p ->
        UserProfile(
            ageVerified18Plus = p[Keys.AGE_VERIFIED] ?: false,
            onboardingComplete = p[Keys.ONBOARDED] ?: false,
            sex = p[Keys.SEX]?.let { runCatching { Sex.valueOf(it) }.getOrNull() },
            heightCm = p[Keys.HEIGHT],
            goal = p[Keys.GOAL]?.let { runCatching { FitnessGoal.valueOf(it) }.getOrNull() } ?: FitnessGoal.GENERAL_HEALTH,
            experience = p[Keys.EXPERIENCE]?.let { runCatching { ExperienceLevel.valueOf(it) }.getOrNull() } ?: ExperienceLevel.BEGINNER,
            equipment = p[Keys.EQUIPMENT]?.let { runCatching { Equipment.valueOf(it) }.getOrNull() } ?: Equipment.BODYWEIGHT,
            focusArea = p[Keys.FOCUS]?.let { runCatching { FocusArea.valueOf(it) }.getOrNull() } ?: FocusArea.FULL_BODY,
            daysPerWeek = p[Keys.DAYS] ?: 3,
        )
    }

    suspend fun update(profile: UserProfile) {
        context.dataStore.edit { p ->
            p[Keys.AGE_VERIFIED] = profile.ageVerified18Plus
            p[Keys.ONBOARDED] = profile.onboardingComplete
            profile.sex?.let { p[Keys.SEX] = it.name }
            profile.heightCm?.let { p[Keys.HEIGHT] = it }
            p[Keys.GOAL] = profile.goal.name
            p[Keys.EXPERIENCE] = profile.experience.name
            p[Keys.EQUIPMENT] = profile.equipment.name
            p[Keys.FOCUS] = profile.focusArea.name
            p[Keys.DAYS] = profile.daysPerWeek
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
