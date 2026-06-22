package com.bodylens.data.repository

import com.bodylens.core.common.AppResult
import com.bodylens.core.common.map
import com.bodylens.data.ml.BodyAnalyzer
import com.bodylens.domain.model.AnalysisResult
import com.bodylens.domain.model.Sex
import com.bodylens.domain.repository.AnalysisRepository
import com.bodylens.domain.service.BodyFatCalculator
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val analyzer: BodyAnalyzer,
) : AnalysisRepository {

    override suspend fun analyze(
        frontPhotoUri: String,
        sidePhotoUri: String?,
        heightCm: Double,
        sex: Sex,
    ): AppResult<AnalysisResult> {
        // Front view drives circumferences; a side view (if present) sharpens depth.
        return analyzer.measure(frontPhotoUri, heightCm).map { measurements ->
            AnalysisResult(
                measurements = measurements,
                composition = BodyFatCalculator.estimate(sex, measurements),
                capturedAtEpochMs = System.currentTimeMillis(),
            )
        }
    }
}
