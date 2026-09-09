package com.experimental.robot.data

/**
 * Mengukur laju kejadian dalam jendela waktu bergulir.
 *
 * Dipakai empat kali dengan instance terpisah - kamera, deteksi, render, dan laju
 * perintah - karena satu angka gabungan tidak bisa dipakai mendiagnosis apa pun:
 * ketika angkanya turun, tidak ada cara membedakan kamera yang lambat dari inferensi
 * yang lambat.
 *
 * Waktu disuntikkan lewat [tick] supaya bisa diuji tanpa menunggu waktu nyata.
 */
class FrameRateMeter(private val windowMs: Long = 500L) {

    private var count = 0
    private var windowStartMs = -1L

    /** Nilai terukur terakhir; 0 berarti belum ada jendela yang selesai. */
    var current: Int = 0
        private set

    fun tick(nowMs: Long): Int {
        if (windowStartMs < 0L) {
            windowStartMs = nowMs
            count = 0
        }
        count++

        val elapsed = nowMs - windowStartMs
        if (elapsed >= windowMs) {
            current = ((count * 1000L) / elapsed).toInt()
            count = 0
            windowStartMs = nowMs
        }
        return current
    }

    fun reset() {
        count = 0
        windowStartMs = -1L
        current = 0
    }
}
