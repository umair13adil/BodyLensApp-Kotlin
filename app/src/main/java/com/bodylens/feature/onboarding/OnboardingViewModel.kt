package com.bodylens.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodylens.domain.model.*
import com.bodylens.domain.usecase.UpdateUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingState(
    val step: Int = 0,
    val ageConfirmed: Boolean = false,
    val consentGiven: Boolean = false,
    val sex: Sex? = null,
    val heightCm: String = "",
    val goal: FitnessGoal = FitnessGoal.GENERAL_HEALTH,
    val experience: ExperienceLevel = ExperienceLevel.BEGINNER,
    val equipment: Equipment = Equipment.BODYWEIGHT,
) {
    val canProceedFromGate get() = ageConfirmed && consentGiven
    val canFinish get() = sex != null && heightCm.toDoubleOrNull()?.let { it in 100.0..230.0 } == true
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val updateProfile: UpdateUserProfileUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state = _state.asStateFlow()

    fun setAgeConfirmed(v: Boolean) = _state.update { it.copy(ageConfirmed = v) }
    fun setConsent(v: Boolean) = _state.update { it.copy(consentGiven = v) }
    fun setSex(v: Sex) = _state.update { it.copy(sex = v) }
    fun setHeight(v: String) = _state.update { it.copy(heightCm = v.filter { c -> c.isDigit() || c == '.' }) }
    fun setGoal(v: FitnessGoal) = _state.update { it.copy(goal = v) }
    fun setExperience(v: ExperienceLevel) = _state.update { it.copy(experience = v) }
    fun setEquipment(v: Equipment) = _state.update { it.copy(equipment = v) }
    fun next() = _state.update { it.copy(step = it.step + 1) }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canFinish || !s.canProceedFromGate) return
        viewModelScope.launch {
            updateProfile { profile ->
                profile.copy(
                    ageVerified18Plus = true,
                    onboardingComplete = true,
                    sex = s.sex,
                    heightCm = s.heightCm.toDoubleOrNull(),
                    goal = s.goal,
                    experience = s.experience,
                    equipment = s.equipment,
                )
            }
            onDone()
        }
    }
}
