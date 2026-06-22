package com.bodylens.domain.usecase

import com.bodylens.core.common.AppResult
import com.bodylens.domain.model.AnalysisResult
import com.bodylens.domain.model.Sex
import com.bodylens.domain.repository.AnalysisRepository
import javax.inject.Inject

class AnalyzeBodyPhotosUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository,
) {
    suspend operator fun invoke(
        frontPhotoUri: String,
        sidePhotoUri: String?,
        heightCm: Double,
        sex: Sex,
    ): AppResult<AnalysisResult> =
        analysisRepository.analyze(frontPhotoUri, sidePhotoUri, heightCm, sex)
}
