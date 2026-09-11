package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.RobotAction

/**
 * Pemetaan gestur tangan ke aksi robot, murni Kotlin tanpa dependensi platform.
 *
 * Aksi ditentukan dari identitas jari mana yang terbuka (lihat [GesturePattern]), bukan
 * dari posisi atau kemiringan tangan di frame. Kombinasi jari yang tidak cocok dengan
 * pola manapun (mis. campuran acak saat tangan sedang berpindah pose) jatuh ke IDLE
 * sebagai default aman.
 */
class HandGestureClassifier(
    private val config: GestureConfig = GestureConfig(),
    private val fingerDetector: FingerExtensionDetector = FingerExtensionDetector(config),
    private val confidenceScorer: GestureConfidenceScorer = GestureConfidenceScorer(config),
) : GestureClassifier {

    override fun classify(hand: HandFrame?): GestureResult {
        if (hand == null || !hand.isValid) return GestureResult()

        val fingers = fingerDetector.detect(hand)
        val action = GesturePattern.ALL.firstOrNull { it.matches(fingers) }?.action ?: RobotAction.IDLE

        return GestureResult(
            action = action,
            fingers = fingers,
            handDetected = true,
            confidence = confidenceScorer.score(hand, action),
        )
    }
}
