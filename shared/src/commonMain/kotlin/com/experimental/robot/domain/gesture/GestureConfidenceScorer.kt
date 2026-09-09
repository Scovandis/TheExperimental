package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.RobotAction
import kotlin.math.abs

/**
 * Menghitung keyakinan gestur dari *margin* - seberapa jauh sebuah pose melewati
 * ambang batasnya, bukan sekadar lolos atau tidak.
 *
 * Pose yang persis di perbatasan menghasilkan skor mendekati nol, sehingga tidak
 * pernah terkunci; pose yang tegas menghasilkan skor mendekati satu. Inilah yang
 * membuat frame ambigu (tangan sedang berpindah pose) tersaring dengan sendirinya.
 *
 * Kejelasan jari dihitung sebagai **mata rantai terlemah** (`minOf`), bukan rata-rata:
 * satu jari yang ragu-ragu sudah cukup untuk membuat seluruh gestur tidak yakin.
 */
class GestureConfidenceScorer(
    private val gestureConfig: GestureConfig = GestureConfig(),
    private val config: ConfidenceConfig = ConfidenceConfig(),
) {

    fun score(hand: HandFrame?, action: RobotAction): Float {
        if (hand == null || !hand.isValid) return 0f

        val presence = if (hand.confidence > 0f) {
            hand.confidence.coerceIn(0f, 1f)
        } else {
            config.neutralPresence
        }

        val wrist = hand[L.WRIST]
        val indexTip = hand[L.INDEX_TIP]

        val pattern = when (action) {
            RobotAction.CROUCH ->
                palmOpenness(hand) * ramp(wrist.y - gestureConfig.crouchWristY, config.crouchSpan)

            RobotAction.MOVE_FORWARD ->
                palmOpenness(hand) * ramp(gestureConfig.crouchWristY - wrist.y, config.crouchSpan)

            RobotAction.MOVE_BACKWARD ->
                minOf(
                    openness(hand, L.INDEX_TIP, L.INDEX_PIP),
                    closedness(hand, L.MIDDLE_TIP, L.MIDDLE_PIP),
                    closedness(hand, L.RING_TIP, L.RING_PIP),
                    closedness(hand, L.PINKY_TIP, L.PINKY_PIP),
                ) * ramp(indexTip.y - wrist.y - gestureConfig.backwardPointMargin, config.pointSpan)

            RobotAction.ROTATE_LEFT, RobotAction.ROTATE_RIGHT ->
                minOf(
                    openness(hand, L.INDEX_TIP, L.INDEX_PIP),
                    openness(hand, L.MIDDLE_TIP, L.MIDDLE_PIP),
                    closedness(hand, L.RING_TIP, L.RING_PIP),
                    closedness(hand, L.PINKY_TIP, L.PINKY_PIP),
                ) * ramp(abs(indexTip.x - wrist.x) - gestureConfig.rotateDeadZoneX, config.rotateSpan)

            RobotAction.IDLE -> fistClarity(hand)
        }

        return (presence * pattern).coerceIn(0f, 1f)
    }

    /** Telapak terbuka hanya seyakin jari panjang yang paling ragu-ragu. */
    private fun palmOpenness(hand: HandFrame): Float = minOf(
        openness(hand, L.INDEX_TIP, L.INDEX_PIP),
        openness(hand, L.MIDDLE_TIP, L.MIDDLE_PIP),
        openness(hand, L.RING_TIP, L.RING_PIP),
        openness(hand, L.PINKY_TIP, L.PINKY_PIP),
    )

    private fun fistClarity(hand: HandFrame): Float = minOf(
        closedness(hand, L.INDEX_TIP, L.INDEX_PIP),
        closedness(hand, L.MIDDLE_TIP, L.MIDDLE_PIP),
        closedness(hand, L.RING_TIP, L.RING_PIP),
        closedness(hand, L.PINKY_TIP, L.PINKY_PIP),
    )

    /** Nol tepat di ambang batas ekstensi, satu bila ujung jari jauh di atas PIP. */
    private fun openness(hand: HandFrame, tip: Int, pip: Int): Float =
        ramp((hand[pip].y - gestureConfig.fingerExtensionMargin) - hand[tip].y, config.extensionSpan)

    /** Cermin dari [openness]; keduanya bernilai nol di ambang batas yang sama. */
    private fun closedness(hand: HandFrame, tip: Int, pip: Int): Float =
        ramp(hand[tip].y - (hand[pip].y - gestureConfig.fingerExtensionMargin), config.extensionSpan)

    private fun ramp(value: Float, span: Float): Float =
        if (span <= 0f) 0f else (value / span).coerceIn(0f, 1f)
}
