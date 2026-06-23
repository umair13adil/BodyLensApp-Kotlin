package com.bodylens.domain.service

import com.bodylens.domain.model.BodyComposition
import com.bodylens.domain.model.BodyFatCategory
import com.bodylens.domain.model.BodyMeasurements
import com.bodylens.domain.model.Sex
import kotlin.math.log10

/**
 * US-Navy body-fat estimation (metric). Pure, deterministic, fully offline.
 *
 * Reported error band is ~±3.5 percentage points, comparable to skinfold calipers.
 * This is an ESTIMATE for trend tracking, not a clinical measurement.
 */
object BodyFatCalculator {

    private const val NAVY_MARGIN = 3.5

    fun estimate(sex: Sex, m: BodyMeasurements): BodyComposition? {
        val height = m.heightCm
        val neck = m.neckCm ?: return null
        val waist = m.waistCm ?: return null

        val bf = when (sex) {
            Sex.MALE -> {
                if (waist - neck <= 0) return null
                495.0 / (1.0324 - 0.19077 * log10(waist - neck) + 0.15456 * log10(height)) - 450.0
            }
            Sex.FEMALE -> {
                val hip = m.hipCm ?: return null
                if (waist + hip - neck <= 0) return null
                495.0 / (1.29579 - 0.35004 * log10(waist + hip - neck) + 0.22100 * log10(height)) - 450.0
            }
        }
        val clamped = bf.coerceIn(3.0, 60.0)
        return BodyComposition(
            bodyFatPercent = (clamped * 10).toInt() / 10.0,
            confidenceMargin = NAVY_MARGIN,
            category = categorize(sex, clamped),
        )
    }

    /** Ranges follow American Council on Exercise (ACE) standards. */
    fun categorize(sex: Sex, bf: Double): BodyFatCategory = when (sex) {
        Sex.MALE -> when {
            bf < 6 -> BodyFatCategory.ESSENTIAL
            bf < 14 -> BodyFatCategory.ATHLETE
            bf < 18 -> BodyFatCategory.FITNESS
            bf < 25 -> BodyFatCategory.AVERAGE
            else -> BodyFatCategory.ABOVE_AVERAGE
        }
        Sex.FEMALE -> when {
            bf < 14 -> BodyFatCategory.ESSENTIAL
            bf < 21 -> BodyFatCategory.ATHLETE
            bf < 25 -> BodyFatCategory.FITNESS
            bf < 32 -> BodyFatCategory.AVERAGE
            else -> BodyFatCategory.ABOVE_AVERAGE
        }
    }
}
