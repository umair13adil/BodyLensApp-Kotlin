package com.bodylens.data.ml

import com.bodylens.domain.model.BodyMeasurements
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * Pure geometry that turns a body silhouette + pose landmarks into circumferences.
 *
 * This is the real algorithm and is fully unit-testable in isolation from MediaPipe:
 *   1. The person's pixel height (head-top to ankle) calibrates a px -> cm scale.
 *   2. At each landmark row (neck, waist, hip), the silhouette's horizontal width
 *      gives a body *width* in cm.
 *   3. Front width + (optional) side depth are combined into a circumference using
 *      the Ramanujan ellipse-perimeter approximation. Without a side view we fall
 *      back to an anthropometric width:depth ratio per region.
 */
class SilhouetteMeasurer {

    data class WidthSample(
        val pixelHeightHeadToAnkle: Float,
        val neckWidthPx: Float?,
        val waistWidthPx: Float?,
        val hipWidthPx: Float?,
        val sideDepthRatio: Float? = null, // depth/width if a side view exists
    )

    fun toMeasurements(sample: WidthSample, heightCm: Double): BodyMeasurements {
        val cmPerPx = heightCm / sample.pixelHeightHeadToAnkle

        fun circumference(widthPx: Float?, defaultDepthRatio: Float): Double? {
            val w = widthPx ?: return null
            val widthCm = w * cmPerPx
            val depthCm = widthCm * (sample.sideDepthRatio ?: defaultDepthRatio)
            return ellipsePerimeter(widthCm / 2.0, depthCm / 2.0)
        }

        // Default width:depth ratios from anthropometric tables when no side view.
        return BodyMeasurements(
            heightCm = heightCm,
            neckCm = circumference(sample.neckWidthPx, defaultDepthRatio = 0.90f),
            waistCm = circumference(sample.waistWidthPx, defaultDepthRatio = 0.72f),
            hipCm = circumference(sample.hipWidthPx, defaultDepthRatio = 0.78f),
        )
    }

    /** Ramanujan's first approximation of an ellipse perimeter. */
    private fun ellipsePerimeter(a: Double, b: Double): Double {
        if (a <= 0 || b <= 0) return 0.0
        val h = ((a - b) * (a - b)) / ((a + b) * (a + b))
        return PI * (a + b) * (1 + (3 * h) / (10 + sqrt(4 - 3 * h)))
    }
}
