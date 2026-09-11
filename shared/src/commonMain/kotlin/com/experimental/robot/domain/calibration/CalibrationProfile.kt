package com.experimental.robot.domain.calibration

import com.experimental.robot.domain.command.ControlSpaceConfig
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.HandPoint
import kotlin.math.sqrt

/**
 * Mengukur jarak antar landmark relatif terhadap **lebar tangan itu sendiri**.
 *
 * Ukuran tangan setiap orang berbeda, dan tangan yang sama terlihat berbeda besar
 * tergantung jaraknya ke kamera. Membandingkan jarak absolut antar landmark karena itu
 * tidak bisa diandalkan; membandingkan rasionya terhadap lebar telapak bisa.
 */
object HandScaleNormalizer {

    /** Lebar telapak: jarak MCP telunjuk ke MCP kelingking. */
    fun handSpan(hand: HandFrame): Float =
        if (!hand.isValid) 0f else distance(hand[L.INDEX_MCP], hand[L.PINKY_MCP])

    /** Jarak antara dua landmark, dinyatakan sebagai kelipatan lebar telapak. */
    fun ratio(hand: HandFrame, from: Int, to: Int): Float {
        val span = handSpan(hand)
        if (span <= 0f) return 0f
        return distance(hand[from], hand[to]) / span
    }

    fun distance(a: HandPoint, b: HandPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }
}

/**
 * Profil gestur satu pengguna, hasil kalibrasi.
 *
 * Nilai bawaan sama dengan konstanta yang dipakai sebelum kalibrasi ada, jadi aplikasi
 * tetap jalan penuh tanpa kalibrasi - profil hanya menggeser ambang batas, bukan
 * mengaktifkan fitur.
 *
 * Tidak lagi menggeser [com.experimental.robot.domain.gesture.GestureConfig]: sejak
 * klasifikasi gestur berbasis identitas jari ([com.experimental.robot.domain.gesture.GesturePattern]),
 * tidak ada lagi ambang posisi/kemiringan tangan (bekas `crouchWristY`/`rotateDeadZoneX`)
 * yang perlu dikalibrasi per pengguna.
 */
data class CalibrationProfile(
    val handSpan: Float = 0.18f,
    val neutralX: Float = 0.5f,
    val neutralY: Float = 0.5f,
) {
    fun applyTo(base: ControlSpaceConfig): ControlSpaceConfig = base.copy(
        neutralX = neutralX,
        neutralY = neutralY,
    )

    companion object {
        val DEFAULT = CalibrationProfile()
    }
}

/** Enam langkah kalibrasi, dijalankan berurutan saat aplikasi pertama dibuka. */
enum class CalibrationStep(val instruction: String) {
    CENTER(instruction = "Letakkan tangan di tengah frame"),
    UP(instruction = "Angkat tangan ke atas"),
    DOWN(instruction = "Turunkan tangan ke bawah"),
    LEFT(instruction = "Gerakkan tangan ke kiri"),
    RIGHT(instruction = "Gerakkan tangan ke kanan"),
    FIST(instruction = "Kepalkan tangan");

    val next: CalibrationStep? get() = entries.getOrNull(ordinal + 1)
}

/**
 * Mengumpulkan sampel per langkah lalu menyusun [CalibrationProfile].
 *
 * Setiap langkah dirata-rata dari banyak frame, bukan diambil dari satu frame, supaya
 * getaran tangan dan jitter deteksi tidak ikut terkalibrasi.
 */
class CalibrationRecorder(private val minSamplesPerStep: Int = 12) {

    private class Accumulator {
        var sumX = 0f
        var sumY = 0f
        var sumSpan = 0f
        var count = 0

        fun add(wrist: HandPoint, span: Float) {
            sumX += wrist.x
            sumY += wrist.y
            sumSpan += span
            count++
        }

        val meanX: Float get() = if (count == 0) 0f else sumX / count
        val meanY: Float get() = if (count == 0) 0f else sumY / count
        val meanSpan: Float get() = if (count == 0) 0f else sumSpan / count
    }

    private val samples = mutableMapOf<CalibrationStep, Accumulator>()

    /** @return true bila langkah ini sudah punya cukup sampel untuk dilanjutkan. */
    fun record(step: CalibrationStep, hand: HandFrame?): Boolean {
        if (hand == null || !hand.isValid) return isComplete(step)
        val accumulator = samples.getOrPut(step) { Accumulator() }
        accumulator.add(hand[L.WRIST], HandScaleNormalizer.handSpan(hand))
        return isComplete(step)
    }

    fun isComplete(step: CalibrationStep): Boolean =
        (samples[step]?.count ?: 0) >= minSamplesPerStep

    fun progress(step: CalibrationStep): Float =
        ((samples[step]?.count ?: 0).toFloat() / minSamplesPerStep).coerceIn(0f, 1f)

    /**
     * Menyusun profil dari sampel yang ada. Langkah yang belum terekam jatuh ke nilai
     * bawaan, jadi kalibrasi yang ditinggalkan separuh jalan tetap menghasilkan profil
     * yang bisa dipakai.
     *
     * Langkah DOWN/LEFT/RIGHT/FIST tetap direkam untuk memandu pengguna menggerakkan
     * tangan ke seluruh area frame (memperkaya rata-rata [CalibrationProfile.handSpan]),
     * walau tidak lagi punya ambang posisi khusus untuk dihasilkan.
     */
    fun build(fallback: CalibrationProfile = CalibrationProfile.DEFAULT): CalibrationProfile {
        val center = samples[CalibrationStep.CENTER]

        val neutralX = center?.takeIf { it.count > 0 }?.meanX ?: fallback.neutralX
        val neutralY = center?.takeIf { it.count > 0 }?.meanY ?: fallback.neutralY

        val spans = samples.values.filter { it.count > 0 }.map { it.meanSpan }
        val handSpan = if (spans.isEmpty()) fallback.handSpan else spans.average().toFloat()

        return CalibrationProfile(
            handSpan = handSpan,
            neutralX = neutralX,
            neutralY = neutralY,
        )
    }

    fun reset() {
        samples.clear()
    }
}
