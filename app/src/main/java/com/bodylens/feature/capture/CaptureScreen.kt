package com.bodylens.feature.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bodylens.data.ml.LiveBodyAnalyzer
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    onAnalysisReady: () -> Unit,
    onBack: () -> Unit,
    viewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    // Live-mode pipeline. The analyzer + its single worker thread live for the whole
    // screen and are torn down on dispose; the ImageAnalysis use case only exists while
    // Live mode is active so Manual mode pays no per-frame cost.
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val liveAnalyzer = remember { LiveBodyAnalyzer(context) }
    var liveFrame by remember { mutableStateOf<LiveBodyAnalyzer.LiveFrame?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            liveAnalyzer.close()
        }
    }
    val imageAnalysis = remember(state.mode) {
        if (state.mode == CaptureMode.Live) {
            ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { ia ->
                    ia.setAnalyzer(analysisExecutor) { proxy -> liveFrame = liveAnalyzer.analyze(proxy) }
                }
        } else {
            null
        }
    }
    LaunchedEffect(state.mode) { if (state.mode == CaptureMode.Manual) liveFrame = null }

    Scaffold(topBar = { TopAppBar(title = { Text("Capture") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (hasPermission) {
                CameraPreview(imageCapture = imageCapture, imageAnalysis = imageAnalysis)
                if (state.mode == CaptureMode.Live) {
                    LiveOverlay(liveFrame)
                } else {
                    BodyGuideOverlay()
                }
            } else {
                Text(
                    "Camera permission is required to capture.",
                    Modifier.align(Alignment.Center).padding(24.dp),
                )
            }

            // Mode toggle (Manual / Live).
            SingleChoiceSegmentedButtonRow(
                Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            ) {
                CaptureMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = state.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, CaptureMode.entries.size),
                    ) { Text(mode.name) }
                }
            }

            Column(
                Modifier.align(Alignment.BottomCenter).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.frontPhotoUri != null) {
                    Text("Front captured ✓", color = MaterialTheme.colorScheme.primary)
                }

                if (state.isAnalysing) {
                    CircularProgressIndicator()
                } else if (state.frontPhotoUri == null) {
                    // Shutter
                    Surface(
                        modifier = Modifier.size(72.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = {
                            takePhoto(context, imageCapture) { path -> viewModel.onFrontCaptured(path) }
                        },
                    ) { Box(Modifier.fillMaxSize()) }
                } else {
                    Button(onClick = { viewModel.analyseAndContinue(onAnalysisReady) }) {
                        Text("Analyse")
                    }
                    TextButton(onClick = { viewModel.onFrontCaptured("") }) { Text("Retake") }
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(imageCapture: ImageCapture, imageAnalysis: ImageAnalysis?) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    // Re-bind whenever the optional ImageAnalysis use case appears/disappears (mode switch).
    LaunchedEffect(imageAnalysis) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val useCases = listOfNotNull(preview, imageCapture, imageAnalysis).toTypedArray()
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases,
            )
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { previewView })
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, onCaptured: (String) -> Unit) {
    val file = File(context.filesDir, "capture_${System.currentTimeMillis()}.jpg")
    val output = ImageCapture.OutputFileOptions.Builder(file).build()
    val executor: Executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(output, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(results: ImageCapture.OutputFileResults) = onCaptured(file.absolutePath)
        override fun onError(exception: ImageCaptureException) { /* surface via state in production */ }
    })
}

/** Front-facing body outline to enforce consistent framing across sessions. */
@Composable
private fun BodyGuideOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val boxW = size.width * 0.5f
        val boxH = size.height * 0.8f
        drawRect(
            color = Color.White.copy(alpha = 0.6f),
            topLeft = Offset((size.width - boxW) / 2f, (size.height - boxH) / 2f),
            size = Size(boxW, boxH),
            style = Stroke(width = 4f),
        )
    }
}

/**
 * Real-time pose skeleton + selfie silhouette overlay for Live mode. Normalized
 * landmark/edge coordinates are mapped onto the canvas with the same FILL_CENTER
 * (center-crop) transform [PreviewView] uses by default, so the overlay tracks the
 * preview regardless of the analyzed frame's aspect ratio.
 */
@Composable
private fun LiveOverlay(frame: LiveBodyAnalyzer.LiveFrame?) {
    frame ?: return
    val skeleton = MaterialTheme.colorScheme.primary
    val silhouette = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
    Canvas(Modifier.fillMaxSize()) {
        if (frame.imageWidth == 0 || frame.imageHeight == 0) return@Canvas
        val scale = max(size.width / frame.imageWidth, size.height / frame.imageHeight)
        val dx = (size.width - frame.imageWidth * scale) / 2f
        val dy = (size.height - frame.imageHeight * scale) / 2f
        fun map(nx: Float, ny: Float) =
            Offset(nx * frame.imageWidth * scale + dx, ny * frame.imageHeight * scale + dy)

        // Skeleton bones.
        for ((a, b) in POSE_CONNECTIONS) {
            if (a >= frame.landmarks.size || b >= frame.landmarks.size) continue
            val (ax, ay) = frame.landmarks[a]
            val (bx, by) = frame.landmarks[b]
            drawLine(skeleton, map(ax, ay), map(bx, by), strokeWidth = 6f)
        }
        // Joints.
        for ((x, y) in frame.landmarks) {
            drawCircle(skeleton, radius = 7f, center = map(x, y))
        }
        // Silhouette outline from the selfie-segmentation edges.
        if (frame.edges.size >= 2) {
            val outline = Path()
            frame.edges.forEachIndexed { i, e ->
                val p = map(e.left, e.y)
                if (i == 0) outline.moveTo(p.x, p.y) else outline.lineTo(p.x, p.y)
            }
            frame.edges.asReversed().forEach { e ->
                val p = map(e.right, e.y)
                outline.lineTo(p.x, p.y)
            }
            outline.close()
            drawPath(outline, silhouette, style = Stroke(width = 4f))
        }
    }
}

/** BlazePose 33-point skeleton bones (subset covering torso and limbs). */
private val POSE_CONNECTIONS = listOf(
    11 to 12, // shoulders
    11 to 13, 13 to 15, // left arm
    12 to 14, 14 to 16, // right arm
    11 to 23, 12 to 24, // torso sides
    23 to 24, // hips
    23 to 25, 25 to 27, // left leg
    24 to 26, 26 to 28, // right leg
)
