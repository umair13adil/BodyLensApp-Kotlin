package com.bodylens.feature.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodylens.domain.model.BodyFatCategory
import com.bodylens.domain.model.ExercisePlan
import com.bodylens.domain.service.BodyFatCalculator
import com.bodylens.domain.usecase.GeneratePlanUseCase
import com.bodylens.domain.usecase.GetProgressHistoryUseCase
import com.bodylens.domain.usecase.ObserveUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val observeProfile: ObserveUserProfileUseCase,
    private val getHistory: GetProgressHistoryUseCase,
    private val generatePlan: GeneratePlanUseCase,
) : ViewModel() {

    private val _plan = MutableStateFlow<ExercisePlan?>(null)
    val plan = _plan.asStateFlow()

    init { regenerate() }

    fun regenerate() {
        viewModelScope.launch {
            val profile = observeProfile().first()
            _plan.value = generatePlan(profile, latestBodyFatCategory())
        }
    }

    /** Most recent stored body-fat reading mapped to a category, if any. */
    private suspend fun latestBodyFatCategory(): BodyFatCategory? {
        val sex = observeProfile().first().sex ?: return null
        val latestBf = getHistory().first()
            .filter { it.bodyFatPercent != null }
            .maxByOrNull { it.createdAtEpochMs }
            ?.bodyFatPercent ?: return null
        return BodyFatCalculator.categorize(sex, latestBf)
    }
}