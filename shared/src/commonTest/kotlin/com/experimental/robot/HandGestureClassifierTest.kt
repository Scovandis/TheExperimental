package com.experimental.robot

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
        thumb(extended = false)
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

    fun thumb(extended: Boolean) = apply {
        points[L.THUMB_IP] = HandPoint(wristX + 0.08f, wristY - 0.02f)
        points[L.THUMB_TIP] = if (extended) {
            // Jauh dari pergelangan dibanding IP -> rasio jarak melewati ambang ekstensi.
            HandPoint(wristX + 0.20f, wristY - 0.02f)
        } else {
            HandPoint(wristX + 0.04f, wristY - 0.01f)
        }
    }

    fun build() = HandFrame(landmarks = points.toList())
}

/** Tangan dengan jari terbuka sesuai [index]/[middle]/[ring]/[pinky]/[thumb] - identitas persis, tanpa posisi. */
private fun handWithFingers(
    index: Boolean,
    middle: Boolean,
    ring: Boolean,
    pinky: Boolean,
    thumb: Boolean,
): HandFrame = HandBuilder()
    .finger(L.INDEX_MCP, L.INDEX_PIP, L.INDEX_TIP, extended = index, offsetX = -0.06f)
    .finger(L.MIDDLE_MCP, L.MIDDLE_PIP, L.MIDDLE_TIP, extended = middle, offsetX = -0.02f)
    .finger(L.RING_MCP, L.RING_PIP, L.RING_TIP, extended = ring, offsetX = 0.02f)
    .finger(L.PINKY_MCP, L.PINKY_PIP, L.PINKY_TIP, extended = pinky, offsetX = 0.06f)
    .thumb(extended = thumb)
    .build()

class HandGestureClassifierTest {

    private val classifier = HandGestureClassifier()

    @Test
    fun kepalan_tangan_menghasilkan_idle() {
        val hand = handWithFingers(index = false, middle = false, ring = false, pinky = false, thumb = false)
        assertEquals(RobotAction.IDLE, classifier.classify(hand).action)
    }

    @Test
    fun satu_jari_telunjuk_menghasilkan_maju() {
        val hand = handWithFingers(index = true, middle = false, ring = false, pinky = false, thumb = false)
        val result = classifier.classify(hand)
        assertEquals(RobotAction.MOVE_FORWARD, result.action)
        assertEquals(1, result.fingers.extendedCount)
    }

    @Test
    fun dua_jari_telunjuk_tengah_menghasilkan_mundur() {
        val hand = handWithFingers(index = true, middle = true, ring = false, pinky = false, thumb = false)
        assertEquals(RobotAction.MOVE_BACKWARD, classifier.classify(hand).action)
    }

    @Test
    fun tiga_jari_menghasilkan_putar_kiri() {
        val hand = handWithFingers(index = true, middle = true, ring = true, pinky = false, thumb = false)
        assertEquals(RobotAction.ROTATE_LEFT, classifier.classify(hand).action)
    }

    /**
     * Regresi: deteksi jempol jauh lebih rapuh daripada 4 jari panjang (rasio jarak ke
     * pergelangan, bukan tip-vs-PIP), dan orang secara alami tidak menekuk jempol rapat saat
     * menunjukkan 1-3 jari. Mewajibkan jempol tertutup persis di sini membuat gestur nyaris
     * tidak pernah cocok di kamera nyata - robot tidak bergerak sama sekali walau gestur
     * diganti-ganti. 1-3 jari harus tetap terbaca terlepas dari status jempol.
     */
    @Test
    fun satu_dua_tiga_jari_tetap_terbaca_walau_jempol_ikut_terbaca_terbuka() {
        val maju = handWithFingers(index = true, middle = false, ring = false, pinky = false, thumb = true)
        val mundur = handWithFingers(index = true, middle = true, ring = false, pinky = false, thumb = true)
        val putarKiri = handWithFingers(index = true, middle = true, ring = true, pinky = false, thumb = true)

        assertEquals(RobotAction.MOVE_FORWARD, classifier.classify(maju).action)
        assertEquals(RobotAction.MOVE_BACKWARD, classifier.classify(mundur).action)
        assertEquals(RobotAction.ROTATE_LEFT, classifier.classify(putarKiri).action)
    }

    @Test
    fun empat_jari_tanpa_jempol_menghasilkan_putar_kanan() {
        val hand = handWithFingers(index = true, middle = true, ring = true, pinky = true, thumb = false)
        assertEquals(RobotAction.ROTATE_RIGHT, classifier.classify(hand).action)
    }

    @Test
    fun lima_jari_termasuk_jempol_menghasilkan_jongkok() {
        val hand = handWithFingers(index = true, middle = true, ring = true, pinky = true, thumb = true)
        assertEquals(RobotAction.CROUCH, classifier.classify(hand).action)
    }

    @Test
    fun kombinasi_jari_yang_tidak_dikenal_jatuh_ke_idle() {
        // Telunjuk + manis terbuka, tengah tertutup: bukan pola berhitung manapun.
        val hand = handWithFingers(index = true, middle = false, ring = true, pinky = false, thumb = false)
        assertEquals(RobotAction.IDLE, classifier.classify(hand).action)
    }

    @Test
    fun kepalan_dengan_jempol_terentang_tetap_idle() {
        // Disengaja: ini pola gestur darurat (EmergencyGestureDetector), harus tetap IDLE di sini.
        val hand = handWithFingers(index = false, middle = false, ring = false, pinky = false, thumb = true)
        assertEquals(RobotAction.IDLE, classifier.classify(hand).action)
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
