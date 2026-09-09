package com.experimental.robot

import com.experimental.robot.domain.gesture.ConfidenceConfig
import com.experimental.robot.domain.gesture.ConfidenceTier
import com.experimental.robot.domain.gesture.GestureConfidenceScorer
import com.experimental.robot.domain.gesture.GestureConfig
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.RobotAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Tangan sintetis dengan kendali eksplisit atas margin ekstensi tiap jari. */
private class Hand(private val wristY: Float = 0.4f, private val wristX: Float = 0.5f) {
    private val points = MutableList(L.TOTAL) { HandPoint(wristX, wristY) }

    init {
        points[L.WRIST] = HandPoint(wristX, wristY)
        points[L.INDEX_MCP] = HandPoint(wristX - 0.06f, wristY - 0.10f)
        points[L.PINKY_MCP] = HandPoint(wristX + 0.06f, wristY - 0.10f)
    }

    /** @param margin selisih di luar ambang batas; positif = terbuka, negatif = tertutup. */
    fun finger(pip: Int, tip: Int, margin: Float, offsetX: Float = 0f) = apply {
        val x = wristX + offsetX
        val pipY = wristY - 0.16f
        points[pip] = HandPoint(x, pipY)
        points[tip] = HandPoint(x, pipY - 0.015f - margin)
    }

    fun longFingers(margin: Float) = apply {
        finger(L.INDEX_PIP, L.INDEX_TIP, margin, offsetX = -0.06f)
        finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin, offsetX = -0.02f)
        finger(L.RING_PIP, L.RING_TIP, margin, offsetX = 0.02f)
        finger(L.PINKY_PIP, L.PINKY_TIP, margin, offsetX = 0.06f)
    }

    fun indexTip(x: Float, y: Float) = apply { points[L.INDEX_TIP] = HandPoint(x, y) }

    fun build(confidence: Float = 0f) =
        HandFrame(landmarks = points.toList(), confidence = confidence)
}

class GestureConfidenceScorerTest {

    private val gestureConfig = GestureConfig()
    private val config = ConfidenceConfig()
    private val scorer = GestureConfidenceScorer(gestureConfig, config)

    @Test
    fun tangan_kosong_tidak_punya_keyakinan() {
        assertEquals(0f, scorer.score(null, RobotAction.MOVE_FORWARD))
        assertEquals(0f, scorer.score(HandFrame.EMPTY, RobotAction.MOVE_FORWARD))
    }

    @Test
    fun telapak_terbuka_tegas_di_area_atas_menghasilkan_keyakinan_tinggi() {
        val hand = Hand(wristY = 0.35f).longFingers(margin = 0.08f).build()

        val score = scorer.score(hand, RobotAction.MOVE_FORWARD)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    /**
     * Yang membedakan skor berbasis margin dari sekadar lolos/tidak: pose yang persis
     * di perbatasan ambang jongkok tidak boleh pernah terkunci sebagai MAJU.
     */
    @Test
    fun telapak_tepat_di_ambang_jongkok_hampir_tidak_punya_keyakinan() {
        val hand = Hand(wristY = gestureConfig.crouchWristY - 0.005f)
            .longFingers(margin = 0.08f)
            .build()

        val score = scorer.score(hand, RobotAction.MOVE_FORWARD)

        assertTrue(score < config.unknownCeiling, "pose di perbatasan harus UNKNOWN, dapat $score")
    }

    @Test
    fun satu_jari_yang_ragu_ragu_menurunkan_seluruh_keyakinan() {
        val tegas = Hand(wristY = 0.35f).longFingers(margin = 0.08f).build()
        val ragu = Hand(wristY = 0.35f)
            .longFingers(margin = 0.08f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = 0.002f, offsetX = 0.06f)
            .build()

        val skorTegas = scorer.score(tegas, RobotAction.MOVE_FORWARD)
        val skorRagu = scorer.score(ragu, RobotAction.MOVE_FORWARD)

        assertTrue(skorRagu < skorTegas / 2f, "mata rantai terlemah: $skorRagu vs $skorTegas")
    }

    @Test
    fun kepalan_tegas_menghasilkan_keyakinan_tinggi_untuk_idle() {
        val hand = Hand(wristY = 0.4f).longFingers(margin = -0.08f).build()

        val score = scorer.score(hand, RobotAction.IDLE)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    @Test
    fun jongkok_makin_yakin_saat_tangan_makin_rendah() {
        val tepiAmbang = Hand(wristY = 0.67f).longFingers(margin = 0.08f).build()
        val jauhKeBawah = Hand(wristY = 0.85f).longFingers(margin = 0.08f).build()

        val skorTepi = scorer.score(tepiAmbang, RobotAction.CROUCH)
        val skorJauh = scorer.score(jauhKeBawah, RobotAction.CROUCH)

        assertTrue(skorTepi < skorJauh, "$skorTepi harus lebih kecil dari $skorJauh")
        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(skorJauh, config))
    }

    @Test
    fun putar_di_dalam_dead_zone_tidak_punya_keyakinan() {
        val hand = Hand(wristY = 0.4f)
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.03f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = 0.08f, offsetX = 0.01f)
            .finger(L.RING_PIP, L.RING_TIP, margin = -0.08f, offsetX = 0.03f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)
            .indexTip(x = 0.51f, y = 0.14f)
            .build()

        val score = scorer.score(hand, RobotAction.ROTATE_RIGHT)

        assertTrue(score < config.unknownCeiling, "kemiringan 0.01 masih dead zone, dapat $score")
    }

    @Test
    fun putar_yang_condong_tegas_menghasilkan_keyakinan_tinggi() {
        val hand = Hand(wristY = 0.4f)
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.03f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = 0.08f, offsetX = 0.01f)
            .finger(L.RING_PIP, L.RING_TIP, margin = -0.08f, offsetX = 0.03f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)
            .indexTip(x = 0.70f, y = 0.14f)
            .build()

        val score = scorer.score(hand, RobotAction.ROTATE_RIGHT)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    @Test
    fun skor_kehadiran_dari_mediapipe_ikut_membatasi_hasil() {
        val hand = Hand(wristY = 0.35f).longFingers(margin = 0.08f)

        val yakin = scorer.score(hand.build(confidence = 0.99f), RobotAction.MOVE_FORWARD)
        val raguRagu = scorer.score(hand.build(confidence = 0.55f), RobotAction.MOVE_FORWARD)

        assertTrue(raguRagu < yakin, "$raguRagu harus di bawah $yakin")
        assertTrue(raguRagu < config.candidateFloor, "tangan yang tidak yakin tidak boleh mengunci")
    }
}
