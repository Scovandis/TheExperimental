package com.experimental.robot.domain.command

import com.experimental.robot.domain.gesture.GestureMachineState
import com.experimental.robot.domain.gesture.GesturePhase
import com.experimental.robot.domain.model.Direction
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotCommand

data class CommandInterpreterConfig(
    /** Kecepatan rotasi pada magnitude penuh, derajat per detik. */
    val rotationDegreesPerSecond: Float = 90f,
    /**
     * Magnitude minimum untuk gestur putar. Putar sudah punya arah dari gesturnya
     * sendiri, jadi tangan yang berada di dead zone tetap boleh memutar - pelan.
     */
    val minRotationMagnitude: Float = 0.35f,
    /** Magnitude yang dipakai bila tidak ada titik kontrol, mis. dari pad manual. */
    val fallbackMagnitude: Float = 0.7f,
)

/**
 * Menerjemahkan gestur yang sudah stabil menjadi [RobotCommand].
 *
 * Di sinilah gestur berhenti dan perintah dimulai. Aturannya:
 * - **arah** berasal dari gestur yang terkunci state machine,
 * - **kecepatan** berasal dari posisi tangan lewat [ControlSpace], lalu dihaluskan
 *   [SlewRateLimiter],
 * - fase [GesturePhase.COMMAND_RELEASE] dan setiap [com.experimental.robot.domain.model.HaltMode]
 *   selain RUNNING menargetkan kecepatan nol.
 *
 * Zona sumbu dari [ControlSpace] ikut dikembalikan untuk telemetri, tapi belum
 * menentukan arah - joystick penuh (arah murni dari posisi) masuk tahap lanjutan.
 */
class CommandInterpreter(
    private val controlSpace: ControlSpace = ControlSpace(),
    private val speedLimiter: SlewRateLimiter = SlewRateLimiter(),
    private val config: CommandInterpreterConfig = CommandInterpreterConfig(),
) {

    private var sequence: Long = 0L

    var lastVector: ControlVector = ControlVector()
        private set

    fun interpret(
        machine: GestureMachineState,
        controlPoint: HandPoint?,
        deltaSeconds: Float,
        nowMs: Long,
    ): RobotCommand {
        val vector = controlPoint
            ?.let { controlSpace.resolve(it.x, it.y) }
            ?: ControlVector(magnitude = config.fallbackMagnitude)
        lastVector = vector

        val halted = machine.halt.blocksMovement
        val releasing = machine.phase == GesturePhase.COMMAND_RELEASE
        val action = if (halted) RobotAction.IDLE else machine.action

        val targetMagnitude = when {
            halted || releasing -> 0f
            action.isRotating -> maxOf(vector.magnitude, config.minRotationMagnitude)
            action.isMoving -> vector.magnitude
            else -> 0f
        }

        val magnitude = speedLimiter.advance(targetMagnitude, deltaSeconds)
        sequence++

        val direction = when (action) {
            RobotAction.MOVE_FORWARD -> Direction.FORWARD
            RobotAction.MOVE_BACKWARD -> Direction.BACKWARD
            else -> Direction.NONE
        }

        val rotation = when (action) {
            RobotAction.ROTATE_LEFT -> -config.rotationDegreesPerSecond * magnitude
            RobotAction.ROTATE_RIGHT -> config.rotationDegreesPerSecond * magnitude
            else -> 0f
        }

        return RobotCommand(
            direction = direction,
            speed = if (direction == Direction.NONE) 0f else magnitude,
            rotation = rotation,
            crouch = !halted && action == RobotAction.CROUCH,
            sequence = sequence,
            timestampMs = nowMs,
        )
    }

    fun reset() {
        controlSpace.reset()
        speedLimiter.forceZero()
        lastVector = ControlVector()
    }
}
