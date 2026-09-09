package com.experimental.robot.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.experimental.robot.data.FrameRateMeter
import com.experimental.robot.data.HandLandmarkStream
import com.experimental.robot.data.TrackerStatus
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.Handedness
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult

/**
 * Adapter MediaPipe Hand Landmarker (mode LIVE_STREAM) untuk pipeline CameraX.
 *
 * Tanggung jawabnya hanya konversi frame -> 21 landmark ternormalisasi lalu
 * menerbitkannya ke [HandLandmarkStream]. Semua logika gestur tetap di commonMain.
 */
class HandLandmarkerDetector(
    private val context: Context,
    private val stream: HandLandmarkStream,
) {

    private var landmarker: HandLandmarker? = null
    private var lastTimestampMs = 0L

    /** Laju frame yang masuk dari kamera - beda dari laju hasil deteksi. */
    private val cameraRate = FrameRateMeter()

    /**
     * Bitmap perantara yang dipakai ulang antar frame.
     *
     * Sebelumnya setiap frame mengalokasikan bitmap seukuran `rowStride` dari nol.
     * Pada 30 FPS itu berarti puluhan megabita per detik yang harus dikumpulkan GC,
     * dan tekanan GC itulah yang ikut menahan laju deteksi.
     */
    private var scratch: Bitmap? = null

    /** LIVE_STREAM menolak timestamp yang tidak naik, jadi dipaksa monoton. */
    private fun nextTimestampMs(): Long {
        val now = SystemClock.uptimeMillis()
        lastTimestampMs = if (now > lastTimestampMs) now else lastTimestampMs + 1
        return lastTimestampMs
    }

    fun setup() {
        if (landmarker != null) return
        stream.publishStatus(TrackerStatus.Initializing)
        try {
            val baseOptions = BaseOptions.builder()
                .setDelegate(Delegate.CPU)
                .setModelAssetPath(MODEL_ASSET)
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ -> publishResult(result) }
                .setErrorListener { error ->
                    Log.e(TAG, "HandLandmarker error", error)
                    stream.publishStatus(TrackerStatus.Error(error.message ?: "kesalahan deteksi"))
                }
                .build()

            landmarker = HandLandmarker.createFromOptions(context, options)
            stream.publishStatus(TrackerStatus.Running)
        } catch (t: Throwable) {
            Log.e(TAG, "Gagal memuat $MODEL_ASSET", t)
            stream.publishStatus(TrackerStatus.Error("model tidak dapat dimuat (${t.message})"))
        }
    }

    /**
     * Kirim satu frame kamera ke detektor secara asinkron.
     *
     * @param mirrorHorizontally true untuk kamera depan agar koordinat X sesuai
     *        dengan preview yang dilihat pengguna (preview kamera depan di-mirror).
     */
    fun detect(imageProxy: ImageProxy, mirrorHorizontally: Boolean) {
        // Diukur sebelum apa pun dibuang, supaya angkanya benar-benar laju kamera.
        stream.publishCameraFps(cameraRate.tick(SystemClock.uptimeMillis()))

        val detector = landmarker
        if (detector == null) {
            imageProxy.close()
            return
        }
        val bitmap = try {
            imageProxy.toUprightBitmap(mirrorHorizontally)
        } catch (t: Throwable) {
            Log.w(TAG, "Konversi frame gagal", t)
            null
        } finally {
            imageProxy.close()
        }
        if (bitmap == null) return

        val mpImage: MPImage = BitmapImageBuilder(bitmap).build()
        detector.detectAsync(mpImage, nextTimestampMs())
    }

    fun close() {
        landmarker?.close()
        landmarker = null
        scratch?.recycle()
        scratch = null
        cameraRate.reset()
        stream.reset()
        stream.publishStatus(TrackerStatus.Idle)
    }

    /**
     * Ubah [ImageProxy] (RGBA_8888) menjadi bitmap tegak sesuai orientasi perangkat.
     *
     * Lebar buffer dihitung dari rowStride agar padding baris tidak menggeser piksel,
     * lalu hasilnya dipotong kembali ke lebar asli sebelum dirotasi/di-mirror.
     *
     * Bitmap perantara dipakai ulang; hanya hasil rotasinya yang masih dialokasikan
     * tiap frame karena dimensinya bergantung sudut rotasi.
     */
    private fun ImageProxy.toUprightBitmap(mirrorHorizontally: Boolean): Bitmap {
        val plane = planes[0]
        val bufferWidth = plane.rowStride / plane.pixelStride

        val reusable = scratch?.takeIf { it.width == bufferWidth && it.height == height }
            ?: Bitmap.createBitmap(bufferWidth, height, Bitmap.Config.ARGB_8888).also {
                scratch?.recycle()
                scratch = it
            }
        reusable.copyPixelsFromBuffer(plane.buffer)

        val matrix = Matrix().apply {
            postRotate(imageInfo.rotationDegrees.toFloat())
            if (mirrorHorizontally) postScale(-1f, 1f)
        }
        return Bitmap.createBitmap(reusable, 0, 0, width, height, matrix, true)
    }

    private fun publishResult(result: HandLandmarkerResult) {
        val hands = result.landmarks()
        if (hands.isEmpty() || hands[0].isEmpty()) {
            stream.publish(null)
            return
        }
        val points = hands[0].map { HandPoint(x = it.x(), y = it.y(), z = it.z()) }
        val category = result.handedness().firstOrNull()?.firstOrNull()
        stream.publish(
            HandFrame(
                landmarks = points,
                handedness = when (category?.categoryName()) {
                    "Left" -> Handedness.LEFT
                    "Right" -> Handedness.RIGHT
                    else -> Handedness.UNKNOWN
                },
                confidence = category?.score() ?: 0f,
                timestampMs = result.timestampMs(),
            )
        )
    }

    private companion object {
        const val TAG = "HandLandmarkerDetector"
        const val MODEL_ASSET = "hand_landmarker.task"
    }
}
