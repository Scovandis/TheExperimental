package com.experimental.robot

import com.experimental.robot.data.FrameRateMeter
import com.experimental.robot.domain.calibration.CalibrationProfile
import com.experimental.robot.domain.calibration.CalibrationRecorder
import com.experimental.robot.domain.calibration.CalibrationStep
import com.experimental.robot.domain.calibration.HandScaleNormalizer
import com.experimental.robot.domain.command.ControlSpaceConfig
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.HandPoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun handAt(x: Float, y: Float, span: Float = 0.18f): HandFrame {
    val points = MutableList(L.TOTAL) { HandPoint(x, y) }
    points[L.WRIST] = HandPoint(x, y)
    points[L.INDEX_MCP] = HandPoint(x - span / 2f, y - 0.10f)
    points[L.PINKY_MCP] = HandPoint(x + span / 2f, y - 0.10f)
    return HandFrame(landmarks = points.toList())
}

class HandScaleNormalizerTest {

    @Test
    fun lebar_telapak_diukur_dari_mcp_telunjuk_ke_kelingking() {
        assertEquals(0.20f, HandScaleNormalizer.handSpan(handAt(0.5f, 0.5f, span = 0.20f)), 0.0001f)
    }

    @Test
    fun tangan_kosong_tidak_punya_lebar() {
        assertEquals(0f, HandScaleNormalizer.handSpan(HandFrame.EMPTY))
    }

    /**
     * Inti dari normalisasi: tangan yang sama pada jarak berbeda dari kamera terlihat
     * berbeda besar, tapi rasio antar landmarknya tetap.
     */
    @Test
    fun rasio_tidak_berubah_saat_tangan_menjauh_dari_kamera() {
        val dekat = handAt(0.5f, 0.5f, span = 0.30f)
        val jauh = handAt(0.5f, 0.5f, span = 0.10f)

        val rasioDekat = HandScaleNormalizer.ratio(dekat, L.INDEX_MCP, L.PINKY_MCP)
        val rasioJauh = HandScaleNormalizer.ratio(jauh, L.INDEX_MCP, L.PINKY_MCP)

        assertEquals(rasioDekat, rasioJauh, 0.0001f)
    }
}

class CalibrationRecorderTest {

    private fun recorder() = CalibrationRecorder(minSamplesPerStep = 4)

    private fun fill(recorder: CalibrationRecorder, step: CalibrationStep, x: Float, y: Float) {
        repeat(4) { recorder.record(step, handAt(x, y)) }
    }

    @Test
    fun langkah_butuh_cukup_sampel_sebelum_dianggap_selesai() {
        val recorder = recorder()

        repeat(3) { assertTrue(!recorder.record(CalibrationStep.CENTER, handAt(0.5f, 0.5f))) }

        assertTrue(recorder.record(CalibrationStep.CENTER, handAt(0.5f, 0.5f)))
    }

    @Test
    fun frame_tanpa_tangan_tidak_dihitung_sebagai_sampel() {
        val recorder = recorder()

        repeat(10) { recorder.record(CalibrationStep.CENTER, null) }

        assertEquals(0f, recorder.progress(CalibrationStep.CENTER))
    }

    @Test
    fun pusat_diambil_dari_rata_rata_bukan_dari_satu_frame() {
        val recorder = recorder()

        // Getaran tangan di sekitar 0.45: rata-ratanya harus tepat di tengah.
        recorder.record(CalibrationStep.CENTER, handAt(0.40f, 0.55f))
        recorder.record(CalibrationStep.CENTER, handAt(0.50f, 0.45f))
        recorder.record(CalibrationStep.CENTER, handAt(0.42f, 0.53f))
        recorder.record(CalibrationStep.CENTER, handAt(0.48f, 0.47f))

        val profile = recorder.build()

        assertEquals(0.45f, profile.neutralX, 0.01f)
        assertEquals(0.50f, profile.neutralY, 0.01f)
    }

    /** Recorder yang belum menerima sampel sama sekali tetap harus menghasilkan profil bawaan penuh. */
    @Test
    fun tidak_ada_sampel_sama_sekali_jatuh_ke_profil_bawaan() {
        val recorder = recorder()

        val profile = recorder.build()

        assertEquals(CalibrationProfile.DEFAULT, profile)
    }

    @Test
    fun profil_menggeser_ruang_kontrol() {
        val profile = CalibrationProfile(neutralX = 0.45f, neutralY = 0.52f)

        val control = profile.applyTo(ControlSpaceConfig())

        assertEquals(0.45f, control.neutralX)
        assertEquals(0.52f, control.neutralY)
    }

    @Test
    fun langkah_kalibrasi_berurutan_dan_berakhir() {
        assertEquals(CalibrationStep.UP, CalibrationStep.CENTER.next)
        assertEquals(null, CalibrationStep.FIST.next)
    }
}

class FrameRateMeterTest {

    @Test
    fun belum_ada_angka_sebelum_jendela_pertama_selesai() {
        val meter = FrameRateMeter(windowMs = 500L)

        assertEquals(0, meter.tick(0L))
        assertEquals(0, meter.tick(100L))
    }

    @Test
    fun mengukur_laju_setelah_jendela_selesai() {
        val meter = FrameRateMeter(windowMs = 500L)

        // 30 tick berjarak 20 ms: satu jendela 500 ms berisi 25 tick -> 50 FPS.
        var fps = 0
        repeat(30) { index -> fps = meter.tick(index * 20L) }

        assertTrue(fps in 45..55, "laju terukur: $fps")
    }

    @Test
    fun laju_rendah_terbaca_rendah() {
        val meter = FrameRateMeter(windowMs = 500L)

        // Satu frame tiap 80 ms -> sekitar 12 FPS, mirip gejala yang dilaporkan.
        var fps = 0
        repeat(14) { index -> fps = meter.tick(index * 80L) }

        assertTrue(fps in 10..15, "laju terukur: $fps")
    }

    @Test
    fun reset_menghapus_angka_terakhir() {
        val meter = FrameRateMeter(windowMs = 100L)
        repeat(10) { index -> meter.tick(index * 20L) }
        assertTrue(meter.current > 0)

        meter.reset()

        assertEquals(0, meter.current)
    }
}
