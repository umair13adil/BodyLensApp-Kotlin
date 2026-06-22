package com.bodylens.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bodylens.core.common.AnalysisResultBus
import com.bodylens.core.common.AppError
import com.bodylens.core.common.AppResult
import com.bodylens.domain.model.ProgressEntry
import com.bodylens.domain.model.Sex
import com.bodylens.domain.usecase.AnalyzeBodyPhotosUseCase
import com.bodylens.domain.usecase.ObserveUserProfileUseCase
import com.bodylens.domain.usecase.SaveProgressEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Camera modes. [Live] overlays real-time pose + silhouette for framing feedback;
 * the actual measurement always runs on the still captured by the shutter. */
enum class CaptureMode { Manual, Live }

data class CaptureUiState(
    val mode: CaptureMode = CaptureMode.Manual,
    val frontPhotoUri: String? = null,
    val sidePhotoUri: String? = null,
    val isAnalysing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val analyze: AnalyzeBodyPhotosUseCase,
    private val observeProfile: ObserveUserProfileUseCase,
    private val saveProgress: SaveProgressEntryUseCase,
    private val resultBus: AnalysisResultBus,
) : ViewModel() {

    private val _state = MutableStateFlow(CaptureUiState())
    val state = _state.asStateFlow()

    fun setMode(mode: CaptureMode) = _state.update { it.copy(mode = mode) }

    fun onFrontCaptured(uri: String) = _state.update { it.copy(frontPhotoUri = uri.ifEmpty { null }, error = null) }
    fun onSideCaptured(uri: String) = _state.update { it.copy(sidePhotoUri = uri) }

    fun analyseAndContinue(onReady: () -> Unit) {
        val front = _state.value.frontPhotoUri ?: return
        viewModelScope.launch {
            _state.update { it.copy(isAnalysing = true, error = null) }
            val profile = observeProfile().first()
            val height = profile.heightCm
            val sex = profile.sex
            if (height == null || sex == null) {
                _state.update { it.copy(isAnalysing = false, error = "Complete your profile first.") }
                return@launch
            }
            when (val result = analyze(front, _state.value.sidePhotoUri, height, sex)) {
                is AppResult.Success -> {
                    resultBus.latest.value = result.data
                    saveProgress(
                        ProgressEntry(
                            createdAtEpochMs = result.data.capturedAtEpochMs,
                            frontPhotoUri = front,
                            sidePhotoUri = _state.value.sidePhotoUri,
                            measurements = result.data.measurements,
                            bodyFatPercent = result.data.composition?.bodyFatPercent,
                        )
                    )
                    _state.update { it.copy(isAnalysing = false) }
                    onReady()
                }
                is AppResult.Error -> _state.update {
                    it.copy(isAnalysing = false, error = result.cause.toMessage())
                }
            }
        }
    }

    private fun AppError.toMessage() = when (this) {
        AppError.NoPersonDetected -> "Couldn't detect a person. Use even lighting and fit the guide outline."
        AppError.PoseNotUsable -> "Stand straight, arms slightly away from your body, and retake."
        AppError.MissingHeight -> "Your height is missing from your profile."
        is AppError.Unexpected -> message
    }
}
