package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.RobotAction

/** Hasil klasifikasi satu frame beserta data antara untuk HUD debug. */
data class GestureResult(
    val action: RobotAction = RobotAction.IDLE,
    val fingers: FingerState = FingerState.NONE,
    val handDetected: Boolean = false,
)

/** Kontrak klasifikasi gestur; memudahkan penggantian implementasi (rule based vs ML). */
interface GestureClassifier {
    fun classify(hand: HandFrame?): GestureResult
}
