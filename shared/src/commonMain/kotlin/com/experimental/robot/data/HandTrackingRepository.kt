package com.experimental.robot.data

import com.experimental.robot.domain.model.HandFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sumber data landmark tangan untuk layer presentation.
 *
 * Emisi bernilai `null` berarti tidak ada tangan pada frame tersebut.
 */
interface HandTrackingRepository {
    val handFrames: Flow<HandFrame?>
    val status: StateFlow<TrackerStatus>

    /**
     * Laju frame yang masuk dari kamera, diukur di sisi platform.
     *
     * Dipisah dari laju deteksi karena keduanya bisa berbeda jauh: kamera 30 FPS dengan
     * inferensi 13 FPS berarti dua pertiga frame dibuang. Tanpa dua angka terpisah,
     * penurunan performa tidak bisa dilacak ke penyebabnya.
     */
    val cameraFps: StateFlow<Int>
}
