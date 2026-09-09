package com.experimental.robot

import com.experimental.robot.domain.command.CommandInterpreter
import com.experimental.robot.domain.command.CommandInterpreterConfig
import com.experimental.robot.domain.command.ControlSpace
import com.experimental.robot.domain.command.SlewRateLimiter
import com.experimental.robot.domain.gesture.GestureMachineState
import com.experimental.robot.domain.gesture.GesturePhase
import com.experimental.robot.domain.model.Direction
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotCommand
import com.experimental.robot.domain.model.StopReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommandInterpreterTest {

    private val dt = 0.016f

    private fun interpreter() = CommandInterpreter(
        controlSpace = ControlSpace(),
        speedLimiter = SlewRateLimiter(),
        config = CommandInterpreterConfig(),
    )

    private fun active(action: RobotAction) = GestureMachineState(
        phase = GesturePhase.COMMAND_ACTIVE,
        halt = HaltMode.RUNNING,
        reason = StopReason.NONE,
        action = action,
    )

    /** Tangan jauh di atas pusat: keluar dead zone, mendekati kecepatan penuh. */
    private val far = HandPoint(x = 0.5f, y = 0.10f)
    private val center = HandPoint(x = 0.5f, y = 0.5f)

    private fun settle(
        interpreter: CommandInterpreter,
        machine: GestureMachineState,
        point: HandPoint?,
        ticks: Int = 60,
    ): RobotCommand {
        var command = RobotCommand.STOP
        repeat(ticks) { index ->
            command = interpreter.interpret(machine, point, dt, nowMs = index * 16L)
        }
        return command
    }

    @Test
    fun arah_datang_dari_gestur_bukan_dari_posisi() {
        val forward = settle(interpreter(), active(RobotAction.MOVE_FORWARD), far)
        val backward = settle(interpreter(), active(RobotAction.MOVE_BACKWARD), far)

        assertEquals(Direction.FORWARD, forward.direction)
        assertEquals(Direction.BACKWARD, backward.direction, "posisi tangan sama, arah tetap dari gestur")
    }

    @Test
    fun kecepatan_datang_dari_posisi_bukan_dari_gestur() {
        val fast = settle(interpreter(), active(RobotAction.MOVE_FORWARD), far)
        val still = settle(interpreter(), active(RobotAction.MOVE_FORWARD), center)

        assertTrue(fast.speed > 0.8f, "tangan jauh dari pusat: ${fast.speed}")
        assertEquals(0f, still.speed, "tangan di dead zone: tidak bergerak walau gestur MAJU")
    }

    /** Inti dari jalur kecepatan: perubahan dibatasi, tidak melompat dalam satu tick. */
    @Test
    fun kecepatan_tidak_melompat_dalam_satu_tick() {
        val interpreter = interpreter()

        val first = interpreter.interpret(active(RobotAction.MOVE_FORWARD), far, dt, nowMs = 0L)

        assertTrue(first.speed > 0f, "sudah mulai bergerak")
        assertTrue(first.speed < 0.1f, "tapi belum penuh: ${first.speed}")
    }

    @Test
    fun fase_release_menurunkan_kecepatan_menuju_nol() {
        val interpreter = interpreter()
        val moving = settle(interpreter, active(RobotAction.MOVE_FORWARD), far)
        assertTrue(moving.speed > 0.8f)

        val releasing = active(RobotAction.MOVE_FORWARD).copy(phase = GesturePhase.COMMAND_RELEASE)
        val released = settle(interpreter, releasing, far, ticks = 40)

        assertEquals(0f, released.speed, "release harus melambat sampai berhenti")
    }

    @Test
    fun halt_menghasilkan_perintah_kosong() {
        val interpreter = interpreter()
        settle(interpreter, active(RobotAction.MOVE_FORWARD), far)

        val stopped = active(RobotAction.MOVE_FORWARD)
            .copy(halt = HaltMode.STOP, reason = StopReason.HAND_LOST)
        val command = settle(interpreter, stopped, far, ticks = 40)

        assertEquals(Direction.NONE, command.direction)
        assertEquals(0f, command.speed)
        assertTrue(command.isIdle)
    }

    @Test
    fun putar_menghasilkan_rotasi_bertanda_bukan_arah() {
        val left = settle(interpreter(), active(RobotAction.ROTATE_LEFT), far)
        val right = settle(interpreter(), active(RobotAction.ROTATE_RIGHT), far)

        assertTrue(left.rotation < 0f, "putar kiri: ${left.rotation}")
        assertTrue(right.rotation > 0f, "putar kanan: ${right.rotation}")
        assertEquals(Direction.NONE, left.direction, "rotasi bukan arah gerak")
    }

    /** Putar punya arah dari gesturnya sendiri, jadi dead zone tidak boleh mematikannya. */
    @Test
    fun putar_tetap_berjalan_walau_tangan_di_dead_zone() {
        val command = settle(interpreter(), active(RobotAction.ROTATE_LEFT), center)

        assertTrue(command.rotation < 0f, "rotasi tetap jalan: ${command.rotation}")
    }

    @Test
    fun jongkok_adalah_postur_bukan_arah() {
        val command = settle(interpreter(), active(RobotAction.CROUCH), far)

        assertTrue(command.crouch)
        assertEquals(Direction.NONE, command.direction)
        assertEquals(0f, command.speed)
    }

    @Test
    fun tanpa_titik_kontrol_dipakai_magnitude_bawaan() {
        val command = settle(interpreter(), active(RobotAction.MOVE_FORWARD), point = null)

        assertTrue(command.speed > 0.6f, "pad manual tetap bisa menggerakkan robot: ${command.speed}")
    }

    @Test
    fun nomor_urut_selalu_naik() {
        val interpreter = interpreter()

        val first = interpreter.interpret(active(RobotAction.IDLE), center, dt, nowMs = 0L)
        val second = interpreter.interpret(active(RobotAction.IDLE), center, dt, nowMs = 16L)

        assertEquals(first.sequence + 1, second.sequence)
        assertEquals(16L, second.timestampMs)
    }

    @Test
    fun idle_menghasilkan_perintah_diam_tapi_bukan_halt() {
        val command = settle(interpreter(), active(RobotAction.IDLE), far)

        assertTrue(command.isIdle)
        assertEquals(Direction.NONE, command.direction)
    }

    @Test
    fun reset_menolkan_kecepatan_seketika() {
        val interpreter = interpreter()
        settle(interpreter, active(RobotAction.MOVE_FORWARD), far)

        interpreter.reset()
        val afterReset = interpreter.interpret(active(RobotAction.MOVE_FORWARD), far, dt, nowMs = 0L)

        assertTrue(afterReset.speed < 0.1f, "kecepatan mulai dari nol lagi: ${afterReset.speed}")
    }
}
