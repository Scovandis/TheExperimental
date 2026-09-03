package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.HandPoint
import kotlin.math.sqrt

/**
 * Menghitung status terbuka/tertutup tiap jari dari 21 landmark.
 *
 * Empat jari panjang memakai perbandingan sumbu Y (tip lebih atas dari PIP) sesuai konsep.
 * Jempol memakai perbandingan jarak euclidean ke pergelangan, karena aturan
 * perbandingan X pada konsep hanya valid untuk tangan kanan yang tidak di-mirror.
 */
class FingerExtensionDetector(private val config: GestureConfig = GestureConfig()) {

    fun detect(hand: HandFrame): FingerState {
        if (!hand.isValid) return FingerState.NONE
        val wrist = hand[L.WRIST]
        return FingerState(
            thumb = isThumbExtended(hand, wrist),
            index = isFingerExtended(hand, L.INDEX_TIP, L.INDEX_PIP),
            middle = isFingerExtended(hand, L.MIDDLE_TIP, L.MIDDLE_PIP),
            ring = isFingerExtended(hand, L.RING_TIP, L.RING_PIP),
            pinky = isFingerExtended(hand, L.PINKY_TIP, L.PINKY_PIP),
        )
    }

    /** Jari terbuka bila ujungnya berada di atas sendi PIP (Y layar mengecil ke atas). */
    private fun isFingerExtended(hand: HandFrame, tip: Int, pip: Int): Boolean =
        hand[tip].y < hand[pip].y - config.fingerExtensionMargin

    /** Jempol terbuka bila ujungnya jauh lebih jauh dari pergelangan dibanding sendi IP. */
    private fun isThumbExtended(hand: HandFrame, wrist: HandPoint): Boolean {
        val tipDistance = distance(hand[L.THUMB_TIP], wrist)
        val ipDistance = distance(hand[L.THUMB_IP], wrist)
        return tipDistance > ipDistance * config.thumbExtensionRatio
    }

    private fun distance(a: HandPoint, b: HandPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}
