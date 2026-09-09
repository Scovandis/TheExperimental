package com.experimental.robot.presentation.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.experimental.robot.data.HandLandmarkStream
import com.experimental.robot.data.TrackerStatus
import com.experimental.robot.data.camera.HandLandmarkerDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Implementasi Android: CameraX (Preview + ImageAnalysis) -> MediaPipe -> [stream].
 *
 * Izin kamera diminta di dalam composable ini; bila ditolak, status berubah ke
 * [TrackerStatus.PermissionRequired] sehingga UI menampilkan kontrol manual.
 */
@Composable
actual fun CameraFeed(
    stream: HandLandmarkStream,
    modifier: Modifier,
) {
    val context = LocalContext.current
    if (LocalInspectionMode.current) {
        CameraPlaceholder(text = "Preview kamera", modifier = modifier)
        return
    }

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (!granted) stream.publishStatus(TrackerStatus.PermissionRequired)
    }

    LaunchedEffect(hasPermission) {
        if (!hasPermission) {
            stream.publishStatus(TrackerStatus.PermissionRequired)
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasPermission) {
        CameraPipeline(stream = stream, modifier = modifier)
    } else {
        CameraPlaceholder(text = "Izin kamera\nditolak", modifier = modifier)
    }
}

@Composable
private fun CameraPipeline(stream: HandLandmarkStream, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val detector = remember { HandLandmarkerDetector(context.applicationContext, stream) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(Unit) {
        detector.setup()
        onDispose {
            detector.close()
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(previewView) {
        try {
            val provider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }
            val useFrontCamera = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            val selector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            // Hand tracking tidak butuh resolusi penuh. Meminta 640x480 memotong biaya
            // konversi bitmap dan inferensi tanpa mengurangi kualitas landmark, karena
            // koordinat yang dipakai sudah ternormalisasi 0..1 - bukan piksel.
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            )
                        )
                        .build()
                )
                .build()
                .apply {
                    setAnalyzer(analysisExecutor) { imageProxy ->
                        detector.detect(imageProxy, mirrorHorizontally = useFrontCamera)
                    }
                }

            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
        } catch (t: Throwable) {
            Log.e("CameraFeed", "Gagal mengikat kamera", t)
            stream.publishStatus(TrackerStatus.Error("kamera tidak dapat dibuka (${t.message})"))
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@Composable
private fun CameraPlaceholder(text: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RobotColors.surfaceSolid),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = RobotColors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(8.dp),
        )
    }
}

private fun Context.hasCameraPermission(): Boolean =
    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
