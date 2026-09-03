package com.experimental.robot.domain.motion

import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp

/** Parameter gerak robot; semua kecepatan dalam satuan per detik agar bebas dari FPS. */
data class MotionConfig(
    val forwardSpeed: Float = 26f,
    val backwardSpeed: Float = 18f,
    val rotationSpeed: Float = 90f,
    val positionRange: ClosedFloatingPointRange<Float> = -90f..90f,
    val crouchScaleY: Float = 0.6f,
    val standScaleY: Float = 1.0f,
    /** Konstanta kekakuan interpolasi (makin besar makin cepat menyusul target). */
    val scaleStiffness: Float = 9f,
    /** Kecepatan siklus langkah (radian per detik). */
    val walkCadence: Float = 7.5f,
)

/**
 * Mesin gerak robot: fungsi murni yang memajukan [RobotState] satu tick.
 *
 * Perubahan posisi/rotasi memakai integrasi berbasis waktu, sedangkan tinggi badan
 * (jongkok/berdiri) memakai interpolasi eksponensial (lerp) agar transisinya halus
 * dan tidak bergantung pada frame rate.
 */
class RobotMotionEngine(private val config: MotionConfig = MotionConfig()) {

    fun step(state: RobotState, action: RobotAction, deltaSeconds: Float): RobotState {
        val dt = deltaSeconds.coerceIn(0f, 0.1f)

        val positionZ = when (action) {
            RobotAction.MOVE_FORWARD -> state.positionZ + config.forwardSpeed * dt
            RobotAction.MOVE_BACKWARD -> state.positionZ - config.backwardSpeed * dt
            else -> state.positionZ
        }.coerceIn(config.positionRange)

        val rotationY = when (action) {
            RobotAction.ROTATE_LEFT -> state.rotationY - config.rotationSpeed * dt
            RobotAction.ROTATE_RIGHT -> state.rotationY + config.rotationSpeed * dt
            else -> state.rotationY
        }

        val targetScaleY = if (action == RobotAction.CROUCH) config.crouchScaleY else config.standScaleY
        val scaleY = lerp(state.scaleY, targetScaleY, smoothingFactor(config.scaleStiffness, dt))

        val walkPhase = advanceWalkPhase(state, action, dt)

        return state.copy(
            positionZ = positionZ,
            rotationY = rotationY,
            scaleY = scaleY,
            walkPhase = walkPhase,
            currentAction = action,
        )
    }

    /** Fase langkah berjalan saat robot bergerak/berputar, dan kembali ke 0 saat diam. */
    private fun advanceWalkPhase(state: RobotState, action: RobotAction, dt: Float): Float =
        if (action.isMoving || action.isRotating) {
            (state.walkPhase + config.walkCadence * dt) % TWO_PI
        } else {
            val decayed = lerp(state.walkPhase, 0f, smoothingFactor(config.scaleStiffness, dt))
            if (abs(decayed) < 0.001f) 0f else decayed
        }

    companion object {
        private const val TWO_PI = (2 * PI).toFloat()

        /** Faktor lerp yang stabil terhadap variasi dt. */
        fun smoothingFactor(stiffness: Float, dt: Float): Float = 1f - exp(-stiffness * dt)

        fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t.coerceIn(0f, 1f)
    }
}
