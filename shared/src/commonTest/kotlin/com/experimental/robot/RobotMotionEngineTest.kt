package com.experimental.robot

import com.experimental.robot.domain.model.Direction
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotCommand
import com.experimental.robot.domain.model.RobotState
import com.experimental.robot.domain.motion.MotionConfig
import com.experimental.robot.domain.motion.RobotMotionEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RobotMotionEngineTest {

    private val config = MotionConfig()
    private val engine = RobotMotionEngine(config)

    private fun run(action: RobotAction, ticks: Int, dt: Float = 0.016f): RobotState {
        var state = RobotState()
        repeat(ticks) { state = engine.step(state, action, dt) }
        return state
    }

    @Test
    fun maju_menambah_posisi_z_dan_mundur_menguranginya() {
        assertTrue(run(RobotAction.MOVE_FORWARD, ticks = 10).positionZ > 0f)
        assertTrue(run(RobotAction.MOVE_BACKWARD, ticks = 10).positionZ < 0f)
    }

    @Test
    fun posisi_z_dibatasi_rentang_konfigurasi() {
        val state = run(RobotAction.MOVE_FORWARD, ticks = 2000)
        assertEquals(config.positionRange.endInclusive, state.positionZ)
    }

    @Test
    fun rotasi_mengikuti_arah_gestur() {
        assertTrue(run(RobotAction.ROTATE_LEFT, ticks = 10).rotationY < 0f)
        assertTrue(run(RobotAction.ROTATE_RIGHT, ticks = 10).rotationY > 0f)
    }

    @Test
    fun jongkok_menginterpolasi_skala_menuju_target_tanpa_melompat() {
        val afterOneTick = engine.step(RobotState(), RobotAction.CROUCH, 0.016f)
        assertTrue(afterOneTick.scaleY < config.standScaleY)
        assertTrue(afterOneTick.scaleY > config.crouchScaleY, "skala tidak boleh langsung melompat")

        val settled = run(RobotAction.CROUCH, ticks = 120)
        assertTrue(settled.scaleY - config.crouchScaleY < 0.01f)
    }

    @Test
    fun berdiri_kembali_setelah_jongkok_dilepas() {
        var state = run(RobotAction.CROUCH, ticks = 120)
        repeat(120) { state = engine.step(state, RobotAction.IDLE, 0.016f) }
        assertTrue(config.standScaleY - state.scaleY < 0.01f)
    }

    @Test
    fun fase_langkah_hanya_berjalan_saat_bergerak() {
        assertTrue(run(RobotAction.MOVE_FORWARD, ticks = 5).walkPhase > 0f)

        var state = run(RobotAction.MOVE_FORWARD, ticks = 5)
        repeat(200) { state = engine.step(state, RobotAction.IDLE, 0.016f) }
        assertEquals(0f, state.walkPhase)
    }

    @Test
    fun delta_waktu_besar_dibatasi_agar_tidak_teleport() {
        val jump = engine.step(RobotState(), RobotAction.MOVE_FORWARD, deltaSeconds = 5f)
        assertTrue(jump.positionZ <= config.forwardSpeed * 0.1f + 0.001f)
    }

    // ── Normalisasi rotasi ───────────────────────────────────────────────────

    @Test
    fun derajat_dibungkus_ke_rentang_minus_180_sampai_180() {
        assertEquals(0f, RobotMotionEngine.normalizeDegrees(0f))
        assertEquals(90f, RobotMotionEngine.normalizeDegrees(90f))
        assertEquals(-90f, RobotMotionEngine.normalizeDegrees(270f))
        assertEquals(0f, RobotMotionEngine.normalizeDegrees(360f))
        // Angka yang sebelumnya bisa muncul di telemetri.
        assertEquals(-125f, RobotMotionEngine.normalizeDegrees(-485f), 0.001f)
    }

    @Test
    fun rotasi_tidak_pernah_lepas_dari_rentang_walau_diputar_terus() {
        var state = RobotState()
        repeat(500) { state = engine.step(state, RobotAction.ROTATE_RIGHT, 0.016f) }

        assertTrue(
            state.rotationY in -180f..180f,
            "rotasi setelah beberapa putaran penuh: ${state.rotationY}",
        )
    }

    // ── Jalur berbasis RobotCommand ──────────────────────────────────────────

    private fun runCommand(command: RobotCommand, ticks: Int, dt: Float = 0.016f): RobotState {
        var state = RobotState()
        repeat(ticks) { state = engine.step(state, command, dt) }
        return state
    }

    @Test
    fun kecepatan_perintah_bersifat_proporsional() {
        val penuh = runCommand(RobotCommand(direction = Direction.FORWARD, speed = 1f), ticks = 30)
        val separuh = runCommand(RobotCommand(direction = Direction.FORWARD, speed = 0.5f), ticks = 30)

        assertTrue(penuh.positionZ > 0f)
        assertEquals(penuh.positionZ / 2f, separuh.positionZ, 0.01f)
    }

    @Test
    fun perintah_berhenti_tidak_menggerakkan_robot() {
        val state = runCommand(RobotCommand.STOP, ticks = 30)

        assertEquals(0f, state.positionZ)
        assertEquals(0f, state.rotationY)
        assertEquals(RobotAction.IDLE, state.currentAction)
    }

    @Test
    fun rotasi_perintah_dinyatakan_dalam_derajat_per_detik() {
        val state = runCommand(RobotCommand(rotation = -90f), ticks = 1, dt = 0.1f)

        assertEquals(-9f, state.rotationY, 0.001f)
        assertEquals(RobotAction.ROTATE_LEFT, state.currentAction)
    }

    @Test
    fun jongkok_dari_perintah_menginterpolasi_seperti_jalur_gestur() {
        val state = runCommand(RobotCommand(crouch = true), ticks = 120)

        assertTrue(state.scaleY - config.crouchScaleY < 0.01f)
        assertEquals(RobotAction.CROUCH, state.currentAction)
    }

    @Test
    fun fase_langkah_lebih_cepat_saat_kecepatan_lebih_tinggi() {
        val lambat = runCommand(RobotCommand(direction = Direction.FORWARD, speed = 0.3f), ticks = 10)
        val cepat = runCommand(RobotCommand(direction = Direction.FORWARD, speed = 1f), ticks = 10)

        assertTrue(cepat.walkPhase > lambat.walkPhase, "${cepat.walkPhase} vs ${lambat.walkPhase}")
    }

    @Test
    fun perintah_diam_mengembalikan_fase_langkah_ke_nol() {
        var state = runCommand(RobotCommand(direction = Direction.FORWARD, speed = 1f), ticks = 5)
        repeat(200) { state = engine.step(state, RobotCommand.STOP, 0.016f) }

        assertEquals(0f, state.walkPhase)
    }
}
