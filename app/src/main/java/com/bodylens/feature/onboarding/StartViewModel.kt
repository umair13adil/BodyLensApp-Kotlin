package com.bodylens.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodylens.domain.usecase.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StartViewModel @Inject constructor(
    observeProfile: ObserveUserProfileUseCase,
) : ViewModel() {
    val onboardingComplete = observeProfile()
        .map { it.onboardingComplete }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
