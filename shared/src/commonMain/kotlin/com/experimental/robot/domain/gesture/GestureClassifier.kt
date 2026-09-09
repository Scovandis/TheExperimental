package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.RobotAction

/**
 * Hasil klasifikasi satu frame beserta data antara untuk HUD debug.
 *
 * @param confidence 0f..1f seberapa tegas pose ini memenuhi pola [action]; pose yang
 *        persis di ambang batas bernilai mendekati nol sehingga tidak pernah terkunci.
 */
data class GestureResult(
    val action: RobotAction = RobotAction.IDLE,
    val fingers: FingerState = FingerState.NONE,
    val handDetected: Boolean = false,
    val confidence: Float = 0f,
)

/** Kontrak klasifikasi gestur; memudahkan penggantian implementasi (rule based vs ML). */
interface GestureClassifier {
    fun classify(hand: HandFrame?): GestureResult
}
