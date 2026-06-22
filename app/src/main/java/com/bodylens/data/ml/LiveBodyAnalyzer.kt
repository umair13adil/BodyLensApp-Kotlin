package com.bodylens.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.Segmenter
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions

/**
 * Real-time per-frame pose + selfie-segmentation pipeline used by the camera's
 * "Live" mode. Unlike [MediaPipeBodyAnalyzer] (which runs the IMAGE-mode models
 * on a saved still for the final measurement) this runs the *streaming* variants
 * of the same two models so it can keep up with the camera feed:
 *
 *   - Pose Landmarker in [RunningMode.VIDEO] — `detectForVideo` is synchronous and
 *     returns the result inline, so the whole frame can be processed on one worker
 *     thread without async listener plumbing.
 *   - ML Kit Selfie Segmentation in [SelfieSegmenterOptions.STREAM_MODE], which is
 *     tuned for consecutive video frames.
 *
 * Both calls block on the analysis executor; CameraX's KEEP_ONLY_LATEST back-pressure
 * means slow frames are simply dropped rather than queued. The output is purely for
 * the on-screen overlay (framing feedback) — it never feeds the measurement pipeline,
 * which stays with the higher-fidelity still capture.
 *
 * A [PoseLandmarker] instance is bound to a single [RunningMode], so this class owns
 * its own instances and must not share them with [MediaPipeBodyAnalyzer].
 *
 * Not thread-safe: call [analyze] from a single analysis thread, and [close] once the
 * camera is torn down.
 */
class LiveBodyAnalyzer(private val context: Context) {

    private var poseLandmarker: PoseLandmarker? = null
    private var segmenter: Segmenter? = null

    /** A single sampled horizontal slice of the silhouette, in normalized [0,1] coords. */
    data class EdgeSpan(val y: Float, val left: Float, val right: Float)

    /**
     * One processed frame. All coordinates are normalized to the *upright* image
     * (after rotation) so the overlay can map them onto the preview independently of
     * sensor orientation. [landmarks] are `(x, y)` pairs indexed by BlazePose topology.
     */
    data class LiveFrame(
        val imageWidth: Int,
        val imageHeight: Int,
        val landmarks: List<Pair<Float, Float>>,
        val edges: List<EdgeSpan>,
    )

    private fun ensureInitialised() {
        if (poseLandmarker == null) {
            poseLandmarker = PoseLandmarker.createFromOptions(
                context,
                PoseLandmarker.PoseLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath(POSE_MODEL).build())
                    .setRunningMode(RunningMode.VIDEO)
                    .setNumPoses(1)
                    .build(),
            )
        }
        if (segmenter == null) {
            segmenter = Segmentation.getClient(
                SelfieSegmenterOptions.Builder()
                    .setDetectorMode(SelfieSegmenterOptions.STREAM_MODE)
                    .build(),
            )
        }
    }

    /**
     * Processes one camera frame and returns overlay geometry, or null when no person
     * is detected. Always closes [proxy] before returning.
     */
    fun analyze(proxy: ImageProxy): LiveFrame? {
        ensureInitialised()
        val pose = poseLandmarker ?: return null.also { proxy.close() }
        val seg = segmenter ?: return null.also { proxy.close() }

        // Decode + rotate to upright once, then release the camera buffer so neither
        // detector has to hold the ImageProxy open.
        val bitmap = try {
            proxy.toBitmap().rotate(proxy.imageInfo.rotationDegrees)
        } catch (_: Exception) {
            return null
        } finally {
            proxy.close()
        }

        val timestampMs = SystemClock.uptimeMillis()

        val landmarks = pose
            .detectForVideo(BitmapImageBuilder(bitmap).build(), timestampMs)
            .landmarks().firstOrNull()
            ?.map { it.x() to it.y() }
            ?: return null

        val edges = try {
            val mask = Tasks.await(seg.process(InputImage.fromBitmap(bitmap, 0)))
            silhouetteEdges(mask.buffer.asFloatBuffer(), mask.width, mask.height, landmarks)
        } catch (_: Exception) {
            emptyList()
        }

        return LiveFrame(bitmap.width, bitmap.height, landmarks, edges)
    }

    /**
     * Samples the foreground confidence mask at evenly spaced rows between the head top
     * and the ankles, returning the left/right silhouette edges per row (normalized).
     */
    private fun silhouetteEdges(
        mask: java.nio.FloatBuffer,
        maskW: Int,
        maskH: Int,
        landmarks: List<Pair<Float, Float>>,
    ): List<EdgeSpan> {
        val topNorm = landmarks.minOf { it.second }.coerceIn(0f, 1f)
        val ankleNorm = ((landmarks[LM_LEFT_ANKLE].second + landmarks[LM_RIGHT_ANKLE].second) / 2f)
            .coerceIn(0f, 1f)
        if (ankleNorm <= topNorm) return emptyList()

        val out = ArrayList<EdgeSpan>(EDGE_ROWS)
        for (i in 0 until EDGE_ROWS) {
            val normY = topNorm + (ankleNorm - topNorm) * i / (EDGE_ROWS - 1f)
            val row = (normY * maskH).toInt().coerceIn(0, maskH - 1)
            val rowStart = row * maskW
            var left = -1
            var right = -1
            for (x in 0 until maskW) {
                if (mask.get(rowStart + x) >= FOREGROUND_THRESHOLD) {
                    if (left < 0) left = x
                    right = x
                }
            }
            if (left >= 0) {
                out += EdgeSpan(normY, left / maskW.toFloat(), right / maskW.toFloat())
            }
        }
        return out
    }

    private fun Bitmap.rotate(degrees: Int): Bitmap {
        if (degrees == 0) return this
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        segmenter?.close()
        segmenter = null
    }

    private companion object {
        const val POSE_MODEL = "pose_landmarker_full.task"
        const val FOREGROUND_THRESHOLD = 0.5f
        const val EDGE_ROWS = 24

        const val LM_LEFT_ANKLE = 27
        const val LM_RIGHT_ANKLE = 28
    }
}