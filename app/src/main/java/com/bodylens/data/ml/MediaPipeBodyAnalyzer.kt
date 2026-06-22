package com.bodylens.data.ml

import android.content.Context
import android.graphics.BitmapFactory
import com.bodylens.core.common.AppError
import com.bodylens.core.common.AppResult
import com.bodylens.domain.model.BodyMeasurements
import com.google.android.gms.tasks.Tasks
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * On-device implementation using MediaPipe Pose Landmarker + ML Kit Selfie Segmentation.
 *
 *   - [POSE_MODEL]: MediaPipe BlazePose bundle in app/src/main/assets/ (kept
 *     uncompressed via the `androidResources { noCompress }` rule). Locates the
 *     neck/waist/hip rows.
 *   - ML Kit Selfie Segmentation: ships its own bundled model (no asset file).
 *     Produces a per-pixel foreground confidence mask used to find the
 *     silhouette's left/right edges on each of those rows.
 *
 * The pixel widths are handed to [SilhouetteMeasurer], which holds the real
 * (unit-tested) px->cm calibration and circumference geometry.
 */
class MediaPipeBodyAnalyzer @Inject constructor(
    private val context: Context,
    private val measurer: SilhouetteMeasurer,
    private val ioDispatcher: CoroutineDispatcher,
) : BodyAnalyzer {

    @Volatile private var poseLandmarker: PoseLandmarker? = null
    @Volatile private var segmenter: Segmenter? = null

    override suspend fun measure(imagePath: String, heightCm: Double): AppResult<BodyMeasurements> =
        withContext(ioDispatcher) {
            try {
                val extracted = extractWidths(imagePath)
                    ?: return@withContext AppResult.Error(AppError.NoPersonDetected)
                AppResult.Success(measurer.toMeasurements(extracted, heightCm))
            } catch (e: Exception) {
                AppResult.Error(AppError.Unexpected(e.message ?: "Body analysis failed"))
            }
        }

    /** Builds both detectors lazily on first use; both are reused across calls. */
    @Synchronized
    private fun ensureInitialised() {
        if (poseLandmarker == null) {
            poseLandmarker = PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(POSE_MODEL).build())
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumPoses(1)
                    .build(),
            )
        }
        if (segmenter == null) {
            segmenter = Segmentation.getClient(
                SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                    .build(),
            )
        }
    }

    /**
     * Runs pose + segmentation and extracts silhouette widths in image pixels.
     * Returns null when no usable person is detected (-> [AppError.NoPersonDetected]).
     */
    private fun extractWidths(imagePath: String): SilhouetteMeasurer.WidthSample? {
        ensureInitialised()
        val pose = poseLandmarker ?: return null
        val seg = segmenter ?: return null

        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

        val landmarks = pose.detect(BitmapImageBuilder(bitmap).build())
            .landmarks().firstOrNull() ?: return null

        // BlazePose 33-point topology. Use min-y of all landmarks as the head top.
        val leftShoulder = landmarks[LM_LEFT_SHOULDER]
        val rightShoulder = landmarks[LM_RIGHT_SHOULDER]
        val leftHip = landmarks[LM_LEFT_HIP]
        val rightHip = landmarks[LM_RIGHT_HIP]
        val leftAnkle = landmarks[LM_LEFT_ANKLE]
        val rightAnkle = landmarks[LM_RIGHT_ANKLE]

        val headTopNorm = landmarks.minOf { it.y() }
        val ankleNorm = (leftAnkle.y() + rightAnkle.y()) / 2f
        val pixelHeight = (ankleNorm - headTopNorm) * bitmap.height
        if (pixelHeight <= 0f) return null

        // ML Kit foreground confidence mask. Default size matches the input image,
        // so mask coordinates == image coordinates (no scaling needed).
        val mask = Tasks.await(seg.process(InputImage.fromBitmap(bitmap, 0)))
        val maskBuf = mask.buffer.asFloatBuffer()
        val maskW = mask.width
        val maskH = mask.height

        fun widthAt(normY: Float): Float? {
            val row = (normY * maskH).toInt().coerceIn(0, maskH - 1)
            val rowStart = row * maskW
            var left = -1
            var right = -1
            for (x in 0 until maskW) {
                if (maskBuf.get(rowStart + x) >= FOREGROUND_THRESHOLD) {
                    if (left < 0) left = x
                    right = x
                }
            }
            return if (left < 0) null else (right - left).toFloat()
        }

        val shoulderY = (leftShoulder.y() + rightShoulder.y()) / 2f
        val hipY = (leftHip.y() + rightHip.y()) / 2f

        return SilhouetteMeasurer.WidthSample(
            pixelHeightHeadToAnkle = pixelHeight,
            neckWidthPx = widthAt(shoulderY),               // shoulder line approximates the neck row
            waistWidthPx = widthAt((shoulderY + hipY) / 2f), // midway between shoulders and hips
            hipWidthPx = widthAt(hipY),
        )
    }

    private companion object {
        const val POSE_MODEL = "pose_landmarker_full.task"
        const val FOREGROUND_THRESHOLD = 0.5f

        // BlazePose landmark indices.
        const val LM_LEFT_SHOULDER = 11
        const val LM_RIGHT_SHOULDER = 12
        const val LM_LEFT_HIP = 23
        const val LM_RIGHT_HIP = 24
        const val LM_LEFT_ANKLE = 27
        const val LM_RIGHT_ANKLE = 28
    }
}