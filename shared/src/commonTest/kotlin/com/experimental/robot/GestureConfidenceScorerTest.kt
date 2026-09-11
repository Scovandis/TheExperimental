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

/** Tangan sintetis dengan kendali eksplisit atas margin ekstensi tiap jari (termasuk jempol). */
private class Hand(wristY: Float = 0.4f, wristX: Float = 0.5f) {
    private val points = MutableList(L.TOTAL) { HandPoint(wristX, wristY) }
    private val thumbRatio = GestureConfig().thumbExtensionRatio

    init {
        points[L.WRIST] = HandPoint(wristX, wristY)
        points[L.INDEX_MCP] = HandPoint(wristX - 0.06f, wristY - 0.10f)
        points[L.PINKY_MCP] = HandPoint(wristX + 0.06f, wristY - 0.10f)
        thumb(margin = -0.08f) // Jempol tertutup secara tegas, kecuali di-override.
    }

    /** @param margin selisih di luar ambang batas ekstensi; positif = terbuka, negatif = tertutup. */
    fun finger(pip: Int, tip: Int, margin: Float, offsetX: Float = 0f) = apply {
        val x = points[L.WRIST].x + offsetX
        val pipY = points[L.WRIST].y - 0.16f
        points[pip] = HandPoint(x, pipY)
        points[tip] = HandPoint(x, pipY - 0.015f - margin)
    }

    fun longFingers(margin: Float) = apply {
        finger(L.INDEX_PIP, L.INDEX_TIP, margin, offsetX = -0.06f)
        finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin, offsetX = -0.02f)
        finger(L.RING_PIP, L.RING_TIP, margin, offsetX = 0.02f)
        finger(L.PINKY_PIP, L.PINKY_TIP, margin, offsetX = 0.06f)
    }

    /** @param margin selisih jarak tip-vs-IP*rasio terhadap pergelangan; positif = jempol terbuka. */
    fun thumb(margin: Float) = apply {
        val wrist = points[L.WRIST]
        val ipDistance = 0.08f
        points[L.THUMB_IP] = HandPoint(wrist.x + ipDistance, wrist.y)
        points[L.THUMB_TIP] = HandPoint(wrist.x + ipDistance * thumbRatio + margin, wrist.y)
    }

    fun build(confidence: Float = 0f) = HandFrame(landmarks = points.toList(), confidence = confidence)
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
    fun satu_jari_telunjuk_tegas_menghasilkan_keyakinan_tinggi_untuk_maju() {
        val hand = Hand()
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.06f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = -0.08f, offsetX = -0.02f)
            .finger(L.RING_PIP, L.RING_TIP, margin = -0.08f, offsetX = 0.02f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)
            .build()

        val score = scorer.score(hand, RobotAction.MOVE_FORWARD)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    @Test
    fun dua_jari_tegas_menghasilkan_keyakinan_tinggi_untuk_mundur() {
        val hand = Hand()
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.06f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = 0.08f, offsetX = -0.02f)
            .finger(L.RING_PIP, L.RING_TIP, margin = -0.08f, offsetX = 0.02f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)
            .build()

        val score = scorer.score(hand, RobotAction.MOVE_BACKWARD)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    @Test
    fun kepalan_tegas_menghasilkan_keyakinan_tinggi_untuk_idle() {
        val hand = Hand().longFingers(margin = -0.08f).build()

        val score = scorer.score(hand, RobotAction.IDLE)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    @Test
    fun lima_jari_termasuk_jempol_tegas_menghasilkan_keyakinan_tinggi_untuk_jongkok() {
        val hand = Hand().longFingers(margin = 0.08f).thumb(margin = 0.08f).build()

        val score = scorer.score(hand, RobotAction.CROUCH)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    /**
     * Yang membedakan skor berbasis margin dari sekadar lolos/tidak: jempol yang persis
     * di perbatasan ambang ekstensi tidak boleh membuat JONGKOK terkunci.
     */
    @Test
    fun jempol_tepat_di_ambang_ekstensi_membuat_jongkok_tidak_yakin() {
        val hand = Hand().longFingers(margin = 0.08f).thumb(margin = 0.001f).build()

        val score = scorer.score(hand, RobotAction.CROUCH)

        assertTrue(score < config.unknownCeiling, "jempol di perbatasan harus UNKNOWN, dapat $score")
    }

    @Test
    fun satu_jari_yang_ragu_ragu_menurunkan_seluruh_keyakinan() {
        val tegas = Hand().longFingers(margin = 0.08f).build()
        val ragu = Hand()
            .longFingers(margin = 0.08f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = 0.002f, offsetX = 0.06f)
            .build()

        val skorTegas = scorer.score(tegas, RobotAction.ROTATE_RIGHT)
        val skorRagu = scorer.score(ragu, RobotAction.ROTATE_RIGHT)

        assertTrue(skorRagu < skorTegas / 2f, "mata rantai terlemah: $skorRagu vs $skorTegas")
    }

    /**
     * Jempol tidak diperhitungkan untuk IDLE (lihat KDoc [GesturePattern]) - deteksi gestur
     * darurat "kepalan + jempol" ditangani terpisah oleh `EmergencyGestureDetector`, bukan
     * lewat skor IDLE ini. Yang harus tetap membuat IDLE tidak yakin adalah kombinasi 4 jari
     * panjang yang tidak cocok pola IDLE manapun.
     */
    @Test
    fun kombinasi_jari_panjang_yang_tidak_cocok_pola_manapun_tidak_yakin_sebagai_idle() {
        val hand = Hand()
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.06f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = -0.08f, offsetX = -0.02f)
            .finger(L.RING_PIP, L.RING_TIP, margin = 0.08f, offsetX = 0.02f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)
            .build()

        val score = scorer.score(hand, RobotAction.IDLE)

        assertTrue(score < config.unknownCeiling, "kombinasi jari acak harus UNKNOWN untuk IDLE, dapat $score")
    }

    /** Jempol yang salah baca tidak boleh menjatuhkan keyakinan MAJU/MUNDUR/PUTAR KIRI - lihat KDoc [GesturePattern]. */
    @Test
    fun jempol_yang_salah_baca_tidak_menurunkan_keyakinan_maju() {
        val hand = Hand()
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.06f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = -0.08f, offsetX = -0.02f)
            .finger(L.RING_PIP, L.RING_TIP, margin = -0.08f, offsetX = 0.02f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)
            .thumb(margin = 0.08f) // Jempol "terbuka" walau pengguna hanya bermaksud 1 jari.
            .build()

        val score = scorer.score(hand, RobotAction.MOVE_FORWARD)

        assertEquals(ConfidenceTier.LOCKED, ConfidenceTier.of(score, config), "skor $score")
    }

    @Test
    fun skor_kehadiran_dari_mediapipe_ikut_membatasi_hasil() {
        val hand = Hand()
            .finger(L.INDEX_PIP, L.INDEX_TIP, margin = 0.08f, offsetX = -0.06f)
            .finger(L.MIDDLE_PIP, L.MIDDLE_TIP, margin = -0.08f, offsetX = -0.02f)
            .finger(L.RING_PIP, L.RING_TIP, margin = -0.08f, offsetX = 0.02f)
            .finger(L.PINKY_PIP, L.PINKY_TIP, margin = -0.08f, offsetX = 0.06f)

        val yakin = scorer.score(hand.build(confidence = 0.99f), RobotAction.MOVE_FORWARD)
        val raguRagu = scorer.score(hand.build(confidence = 0.55f), RobotAction.MOVE_FORWARD)

        assertTrue(raguRagu < yakin, "$raguRagu harus di bawah $yakin")
        assertTrue(raguRagu < config.candidateFloor, "tangan yang tidak yakin tidak boleh mengunci")
    }
}
