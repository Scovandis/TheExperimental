package com.experimental.robot

import com.experimental.robot.domain.gesture.GestureMachineState
import com.experimental.robot.domain.gesture.GesturePhase
import com.experimental.robot.domain.model.Direction
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.RobotCommand
import com.experimental.robot.domain.model.StopReason
import com.experimental.robot.domain.safety.SafetyConfig
import com.experimental.robot.domain.safety.SafetyController
import com.experimental.robot.domain.safety.SafetySignals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafetyControllerTest {

    private val controller = SafetyController(SafetyConfig())

    private val running = GestureMachineState(
        phase = GesturePhase.COMMAND_ACTIVE,
        halt = HaltMode.RUNNING,
        reason = StopReason.NONE,
    )

    private val forward = RobotCommand(
        direction = Direction.FORWARD,
        speed = 0.8f,
        sequence = 42L,
        timestampMs = 1_000L,
    )

    /** Semua sinyal robot sehat; dipakai sebagai dasar lalu dirusak satu per satu. */
    private val healthyRobot = SafetySignals(
        confidence = 0.95f,
        detectionFps = 28,
        requireRobotLink = true,
        linkConnected = true,
        obstacleDistanceCm = 120f,
        batteryPercent = 82,
        lastAckAtMs = 950L,
    )

    private val simulation = SafetySignals(confidence = 0.95f, detectionFps = 28, requireRobotLink = false)

    @Test
    fun mode_simulasi_lolos_tanpa_sinyal_robot_apa_pun() {
        val verdict = controller.evaluate(forward, running, simulation, nowMs = 1_000L)

        assertEquals(HaltMode.RUNNING, verdict.halt)
        assertEquals(forward, verdict.command)
    }

    @Test
    fun keputusan_state_machine_selalu_diteruskan() {
        val halted = running.copy(halt = HaltMode.EMERGENCY_STOP, reason = StopReason.EMERGENCY)

        val verdict = controller.evaluate(forward, halted, simulation, nowMs = 1_000L)

        assertEquals(HaltMode.EMERGENCY_STOP, verdict.halt)
        assertEquals(StopReason.EMERGENCY, verdict.reason)
        assertTrue(verdict.command.isIdle)
    }

    /** Nomor urut tetap mengalir walau perintah diblokir, supaya penerima tidak bingung. */
    @Test
    fun perintah_yang_diblokir_mempertahankan_nomor_urut() {
        val halted = running.copy(halt = HaltMode.STOP, reason = StopReason.HAND_LOST)

        val verdict = controller.evaluate(forward, halted, simulation, nowMs = 1_000L)

        assertEquals(42L, verdict.command.sequence)
        assertEquals(1_000L, verdict.command.timestampMs)
        assertEquals(0f, verdict.command.speed)
        assertEquals(Direction.NONE, verdict.command.direction)
    }

    @Test
    fun keyakinan_di_bawah_50_persen_menghentikan_robot() {
        val verdict = controller.evaluate(
            forward,
            running,
            simulation.copy(confidence = 0.4f),
            nowMs = 1_000L,
        )

        assertEquals(StopReason.LOW_CONFIDENCE, verdict.reason)
        assertTrue(verdict.blocked)
    }

    @Test
    fun laju_deteksi_rendah_hanya_memblokir_saat_robot_diminta_bergerak() {
        val slow = simulation.copy(detectionFps = 8)

        val moving = controller.evaluate(forward, running, slow, nowMs = 1_000L)
        val idle = controller.evaluate(RobotCommand.STOP, running, slow, nowMs = 1_000L)

        assertEquals(StopReason.LOW_FRAME_RATE, moving.reason)
        assertFalse(idle.blocked, "robot yang sedang diam tidak perlu diblokir")
    }

    @Test
    fun laju_deteksi_nol_dianggap_belum_terukur_bukan_gagal() {
        val verdict = controller.evaluate(forward, running, simulation.copy(detectionFps = 0), nowMs = 1_000L)

        assertFalse(verdict.blocked, "0 FPS berarti jendela pengukuran pertama belum selesai")
    }

    @Test
    fun semua_sinyal_robot_sehat_meloloskan_perintah() {
        val verdict = controller.evaluate(forward, running, healthyRobot, nowMs = 1_000L)

        assertEquals(HaltMode.RUNNING, verdict.halt)
        assertEquals(0.8f, verdict.command.speed)
    }

    @Test
    fun tautan_yang_belum_diketahui_diperlakukan_sebagai_terputus() {
        val verdict = controller.evaluate(
            forward,
            running,
            healthyRobot.copy(linkConnected = null),
            nowMs = 1_000L,
        )

        assertEquals(StopReason.LINK_LOST, verdict.reason)
    }

    @Test
    fun ack_yang_kedaluwarsa_memicu_command_timeout() {
        // ACK terakhir 600 ms lalu, ambangnya 500 ms.
        val verdict = controller.evaluate(forward, running, healthyRobot, nowMs = 1_550L)

        assertEquals(StopReason.COMMAND_TIMEOUT, verdict.reason)
    }

    @Test
    fun baterai_yang_belum_diketahui_diperlakukan_sebagai_kritis() {
        val verdict = controller.evaluate(
            forward,
            running,
            healthyRobot.copy(batteryPercent = null),
            nowMs = 1_000L,
        )

        assertEquals(StopReason.BATTERY, verdict.reason)
    }

    /** Inti dari fail-closed: sensor yang belum pernah melapor bukan berarti jalan bebas. */
    @Test
    fun penghalang_yang_belum_diketahui_memblokir_gerakan_maju() {
        val verdict = controller.evaluate(
            forward,
            running,
            healthyRobot.copy(obstacleDistanceCm = null),
            nowMs = 1_000L,
        )

        assertEquals(StopReason.OBSTACLE, verdict.reason)
    }

    @Test
    fun penghalang_dekat_memblokir_maju_meski_gestur_masih_maju() {
        val verdict = controller.evaluate(
            forward,
            running,
            healthyRobot.copy(obstacleDistanceCm = 20f),
            nowMs = 1_000L,
        )

        assertEquals(StopReason.OBSTACLE, verdict.reason)
        assertTrue(verdict.command.isIdle)
    }

    @Test
    fun penghalang_tidak_memblokir_gerakan_mundur() {
        val backward = forward.copy(direction = Direction.BACKWARD)

        val verdict = controller.evaluate(
            backward,
            running,
            healthyRobot.copy(obstacleDistanceCm = 5f),
            nowMs = 1_000L,
        )

        assertFalse(verdict.blocked, "sensor depan tidak relevan saat robot menjauh")
    }
}
