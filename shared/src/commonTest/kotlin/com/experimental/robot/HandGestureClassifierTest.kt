package com.experimental.robot

import com.experimental.robot.domain.gesture.GestureConfig
import com.experimental.robot.domain.gesture.HandGestureClassifier
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.RobotAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pembangun landmark sintetis: semua titik default di posisi pergelangan,
 * lalu tiap jari di-set eksplisit terbuka/tertutup.
 */
private class HandBuilder(
    private val wristX: Float = 0.5f,
    private val wristY: Float = 0.5f,
) {
    private val points = MutableList(L.TOTAL) { HandPoint(wristX, wristY) }

    init {
        points[L.WRIST] = HandPoint(wristX, wristY)
        // Jempol default tertutup: tip lebih dekat ke pergelangan daripada IP.
        points[L.THUMB_IP] = HandPoint(wristX + 0.08f, wristY - 0.02f)
        points[L.THUMB_TIP] = HandPoint(wristX + 0.04f, wristY - 0.01f)
    }

    fun finger(mcp: Int, pip: Int, tip: Int, extended: Boolean, offsetX: Float = 0f) = apply {
        val x = wristX + offsetX
        points[mcp] = HandPoint(x, wristY - 0.10f)
        if (extended) {
            points[pip] = HandPoint(x, wristY - 0.16f)
            points[tip] = HandPoint(x, wristY - 0.26f)
        } else {
            // Tertutup: ujung jari melipat ke bawah PIP.
            points[pip] = HandPoint(x, wristY - 0.16f)
            points[tip] = HandPoint(x, wristY - 0.08f)
        }
    }

    fun pointDown(offsetX: Float = 0f) = apply {
        val x = wristX + offsetX
        points[L.INDEX_MCP] = HandPoint(x, wristY + 0.08f)
        points[L.INDEX_PIP] = HandPoint(x, wristY + 0.14f)
        points[L.INDEX_TIP] = HandPoint(x, wristY + 0.10f)
    }

    fun moveIndexTip(x: Float, y: Float) = apply { points[L.INDEX_TIP] = HandPoint(x, y) }

    fun build() = HandFrame(landmarks = points.toList())
}

private fun openPalm(wristY: Float = 0.4f): HandFrame = HandBuilder(wristY = wristY)
    .finger(L.INDEX_MCP, L.INDEX_PIP, L.INDEX_TIP, extended = true, offsetX = -0.06f)
    .finger(L.MIDDLE_MCP, L.MIDDLE_PIP, L.MIDDLE_TIP, extended = true, offsetX = -0.02f)
    .finger(L.RING_MCP, L.RING_PIP, L.RING_TIP, extended = true, offsetX = 0.02f)
    .finger(L.PINKY_MCP, L.PINKY_PIP, L.PINKY_TIP, extended = true, offsetX = 0.06f)
    .build()

private fun fist(): HandFrame = HandBuilder()
    .finger(L.INDEX_MCP, L.INDEX_PIP, L.INDEX_TIP, extended = false, offsetX = -0.06f)
    .finger(L.MIDDLE_MCP, L.MIDDLE_PIP, L.MIDDLE_TIP, extended = false, offsetX = -0.02f)
    .finger(L.RING_MCP, L.RING_PIP, L.RING_TIP, extended = false, offsetX = 0.02f)
    .finger(L.PINKY_MCP, L.PINKY_PIP, L.PINKY_TIP, extended = false, offsetX = 0.06f)
    .build()

private fun vSign(indexTipX: Float): HandFrame = HandBuilder()
    .finger(L.INDEX_MCP, L.INDEX_PIP, L.INDEX_TIP, extended = true, offsetX = -0.04f)
    .finger(L.MIDDLE_MCP, L.MIDDLE_PIP, L.MIDDLE_TIP, extended = true, offsetX = 0.0f)
    .finger(L.RING_MCP, L.RING_PIP, L.RING_TIP, extended = false, offsetX = 0.04f)
    .finger(L.PINKY_MCP, L.PINKY_PIP, L.PINKY_TIP, extended = false, offsetX = 0.08f)
    .moveIndexTip(indexTipX, 0.24f)
    .build()

class HandGestureClassifierTest {

    private val classifier = HandGestureClassifier()
    private val config = GestureConfig()

    @Test
    fun fist_menghasilkan_idle() {
        assertEquals(RobotAction.IDLE, classifier.classify(fist()).action)
    }

    @Test
    fun telapak_terbuka_di_atas_menghasilkan_maju() {
        val result = classifier.classify(openPalm(wristY = 0.35f))
        assertEquals(RobotAction.MOVE_FORWARD, result.action)
        assertEquals(4, result.fingers.extendedCount)
    }

    @Test
    fun telapak_terbuka_di_bawah_frame_menghasilkan_jongkok() {
        val wristY = config.crouchWristY + 0.1f
        assertEquals(RobotAction.CROUCH, classifier.classify(openPalm(wristY = wristY)).action)
    }

    @Test
    fun telunjuk_menunjuk_bawah_menghasilkan_mundur() {
        val hand = HandBuilder()
            .finger(L.MIDDLE_MCP, L.MIDDLE_PIP, L.MIDDLE_TIP, extended = false, offsetX = -0.02f)
            .finger(L.RING_MCP, L.RING_PIP, L.RING_TIP, extended = false, offsetX = 0.02f)
            .finger(L.PINKY_MCP, L.PINKY_PIP, L.PINKY_TIP, extended = false, offsetX = 0.06f)
            .pointDown(offsetX = -0.05f)
            .build()
        assertEquals(RobotAction.MOVE_BACKWARD, classifier.classify(hand).action)
    }

    @Test
    fun v_sign_condong_kanan_menghasilkan_putar_kanan() {
        val hand = vSign(indexTipX = 0.5f + config.rotateDeadZoneX + 0.05f)
        assertEquals(RobotAction.ROTATE_RIGHT, classifier.classify(hand).action)
    }

    @Test
    fun v_sign_condong_kiri_menghasilkan_putar_kiri() {
        val hand = vSign(indexTipX = 0.5f - config.rotateDeadZoneX - 0.05f)
        assertEquals(RobotAction.ROTATE_LEFT, classifier.classify(hand).action)
    }

    @Test
    fun v_sign_tegak_di_dead_zone_tetap_idle() {
        assertEquals(RobotAction.IDLE, classifier.classify(vSign(indexTipX = 0.5f)).action)
    }

    @Test
    fun landmark_tidak_lengkap_diabaikan() {
        val partial = HandFrame(landmarks = listOf(HandPoint(0.5f, 0.5f)))
        val result = classifier.classify(partial)
        assertEquals(RobotAction.IDLE, result.action)
        assertTrue(!result.handDetected)
    }

    @Test
    fun frame_null_diabaikan() {
        assertEquals(RobotAction.IDLE, classifier.classify(null).action)
    }
}
