package com.experimental.robot.data

import com.experimental.robot.domain.gesture.LandmarkSmoother
import com.experimental.robot.domain.model.HandFrame
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Jembatan antara detektor spesifik platform (MediaPipe di Android) dan layer shared.
 *
 * Layer platform hanya memanggil [publish] / [publishStatus]; ViewModel cukup
 * mengonsumsi [handFrames] sehingga logika gestur tetap 100% di commonMain.
 * Frame lama dibuang bila konsumen tertinggal, agar kontrol selalu real time.
 */
class HandLandmarkStream(
    private val smoother: LandmarkSmoother = LandmarkSmoother(alpha = 0.55f),
) : HandTrackingRepository {

    private val _frames = MutableSharedFlow<HandFrame?>(
        replay = 1,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val _status = MutableStateFlow<TrackerStatus>(TrackerStatus.Idle)

    private val _cameraFps = MutableStateFlow(0)

    override val handFrames: Flow<HandFrame?> = _frames.asSharedFlow().map(smoother::smooth)

    override val status: StateFlow<TrackerStatus> = _status.asStateFlow()

    override val cameraFps: StateFlow<Int> = _cameraFps.asStateFlow()

    /** Dipanggil dari thread analisis kamera; non-blocking. */
    fun publish(frame: HandFrame?) {
        _frames.tryEmit(frame)
    }

    fun publishStatus(status: TrackerStatus) {
        _status.value = status
    }

    /** Dipanggil layer kamera setiap kali jendela pengukuran selesai. */
    fun publishCameraFps(fps: Int) {
        _cameraFps.value = fps
    }

    fun reset() {
        smoother.reset()
        _frames.tryEmit(null)
        _cameraFps.value = 0
    }
}
