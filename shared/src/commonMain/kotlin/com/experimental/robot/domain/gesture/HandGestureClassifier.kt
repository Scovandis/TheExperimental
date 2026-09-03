package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.RobotAction

/**
 * Pemetaan gestur tangan ke aksi robot, murni Kotlin tanpa dependensi platform.
 *
 * Urutan pemeriksaan dari pola paling spesifik ke paling umum supaya satu frame
 * hanya bisa jatuh ke satu aksi:
 *
 * 1. JONGKOK : telapak terbuka & pergelangan di area bawah frame (Y > crouchWristY)
 * 2. MAJU    : telapak terbuka & pergelangan di area atas frame
 * 3. MUNDUR  : hanya telunjuk terbuka, ujungnya di bawah pergelangan
 * 4. PUTAR   : telunjuk + tengah terbuka (V-sign), arah dari selisih X tip vs pergelangan
 * 5. IDLE    : sisanya, termasuk kepalan tangan
 */
class HandGestureClassifier(
    private val config: GestureConfig = GestureConfig(),
    private val fingerDetector: FingerExtensionDetector = FingerExtensionDetector(config),
) : GestureClassifier {

    override fun classify(hand: HandFrame?): GestureResult {
        if (hand == null || !hand.isValid) return GestureResult()

        val fingers = fingerDetector.detect(hand)
        val wrist = hand[L.WRIST]
        val indexTip = hand[L.INDEX_TIP]
        val openPalm = fingers.extendedCount >= config.openPalmMinFingers

        val action = when {
            // 1 & 2: telapak terbuka, dibedakan oleh posisi vertikal pergelangan.
            openPalm && wrist.y > config.crouchWristY -> RobotAction.CROUCH
            openPalm -> RobotAction.MOVE_FORWARD

            // 3: hanya telunjuk terbuka dan ujungnya menunjuk ke bawah.
            fingers.index && fingers.extendedCount == 1 &&
                indexTip.y > wrist.y + config.backwardPointMargin -> RobotAction.MOVE_BACKWARD

            // 4: V-sign; arah ditentukan kemiringan tangan, dengan dead zone di tengah.
            fingers.index && fingers.middle && !fingers.ring && !fingers.pinky -> when {
                indexTip.x < wrist.x - config.rotateDeadZoneX -> RobotAction.ROTATE_LEFT
                indexTip.x > wrist.x + config.rotateDeadZoneX -> RobotAction.ROTATE_RIGHT
                else -> RobotAction.IDLE
            }

            else -> RobotAction.IDLE
        }

        return GestureResult(action = action, fingers = fingers, handDetected = true)
    }
}
