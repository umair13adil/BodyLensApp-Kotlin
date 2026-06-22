package com.bodylens.domain.model

/**
 * Circumferences in centimetres, derived on-device from the silhouette.
 * Null fields mean a measurement could not be estimated from the capture.
 */
data class BodyMeasurements(
    val heightCm: Double,
    val neckCm: Double?,
    val waistCm: Double?,
    val hipCm: Double?,
    val chestCm: Double? = null,
    val thighCm: Double? = null,
)

/**
 * Body-fat estimate expressed as a range, never a single authoritative number.
 * [confidenceMargin] is the ± band (e.g. 3.5 for the US-Navy method).
 */
data class BodyComposition(
    val bodyFatPercent: Double,
    val confidenceMargin: Double,
    val category: BodyFatCategory,
) {
    val lowerBound: Double get() = (bodyFatPercent - confidenceMargin).coerceAtLeast(0.0)
    val upperBound: Double get() = bodyFatPercent + confidenceMargin
}

enum class BodyFatCategory { ESSENTIAL, ATHLETE, FITNESS, AVERAGE, ABOVE_AVERAGE }

/** The outcome of analysing one capture session. */
data class AnalysisResult(
    val measurements: BodyMeasurements,
    val composition: BodyComposition?,
    val capturedAtEpochMs: Long,
)
