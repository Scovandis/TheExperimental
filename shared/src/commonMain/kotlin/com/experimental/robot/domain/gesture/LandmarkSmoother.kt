package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandPoint

/**
 * Exponential moving average per landmark untuk menekan jitter deteksi
 * sebelum data masuk ke classifier.
 *
 * @param alpha 0f..1f; makin kecil makin halus tapi makin lambat merespons.
 */
class LandmarkSmoother(private val alpha: Float = 0.5f) {

    private var previous: List<HandPoint>? = null

    fun smooth(frame: HandFrame?): HandFrame? {
        if (frame == null || !frame.isValid) {
            previous = null
            return frame
        }
        val prev = previous
        val smoothed = if (prev == null || prev.size != frame.landmarks.size) {
            frame.landmarks
        } else {
            frame.landmarks.mapIndexed { i, point ->
                HandPoint(
                    x = prev[i].x + alpha * (point.x - prev[i].x),
                    y = prev[i].y + alpha * (point.y - prev[i].y),
                    z = prev[i].z + alpha * (point.z - prev[i].z),
                )
            }
        }
        previous = smoothed
        return frame.copy(landmarks = smoothed)
    }

    fun reset() {
        previous = null
    }
}
