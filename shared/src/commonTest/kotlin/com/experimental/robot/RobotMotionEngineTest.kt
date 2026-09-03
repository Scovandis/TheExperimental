package com.experimental.robot

import com.experimental.robot.domain.model.RobotAction
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
}
