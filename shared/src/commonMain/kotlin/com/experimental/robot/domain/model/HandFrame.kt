package com.experimental.robot.domain.model

/** Sisi tangan yang terdeteksi. */
enum class Handedness { LEFT, RIGHT, UNKNOWN }

/**
 * Hasil satu frame deteksi tangan.
 *
 * @param landmarks 21 titik ternormalisasi (kosong bila tidak ada tangan).
 * @param handedness sisi tangan menurut MediaPipe.
 * @param confidence skor keyakinan handedness (0..1).
 * @param timestampMs waktu frame, dipakai untuk perhitungan FPS.
 */
data class HandFrame(
    val landmarks: List<HandPoint>,
    val handedness: Handedness = Handedness.UNKNOWN,
    val confidence: Float = 0f,
    val timestampMs: Long = 0L,
) {
    val isValid: Boolean get() = landmarks.size >= HandLandmarkIndex.TOTAL

    val wrist: HandPoint? get() = landmarks.getOrNull(HandLandmarkIndex.WRIST)

    operator fun get(index: Int): HandPoint = landmarks[index]

    companion object {
        val EMPTY = HandFrame(landmarks = emptyList())
    }
}
