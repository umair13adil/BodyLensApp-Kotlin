package com.bodylens.data.ml

import com.bodylens.core.common.AppResult
import com.bodylens.domain.model.BodyMeasurements

/**
 * Abstraction over the on-device vision pipeline.
 *
 * Implementations must run entirely on the device — no network calls — because
 * the input is sensitive body imagery. The contract is intentionally small so the
 * MediaPipe implementation can be swapped (e.g. for a future 3D-mesh approach)
 * without touching the domain or presentation layers.
 */
interface BodyAnalyzer {
    /**
     * @param imagePath local file path to a captured frame.
     * @param heightCm the user's real height, used to convert pixels to centimetres.
     */
    suspend fun measure(imagePath: String, heightCm: Double): AppResult<BodyMeasurements>
}
