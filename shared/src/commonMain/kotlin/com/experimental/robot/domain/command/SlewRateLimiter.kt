package com.experimental.robot.domain.command

/**
 * Membatasi seberapa cepat sebuah nilai boleh berubah, dalam satuan per detik.
 *
 * Inilah yang membuat kecepatan bisa mengalir kontinu tanpa perlu dikunci seperti arah.
 * Arah lewat state machine dan lock; kecepatan lewat pembatas ini. Kalau kecepatan juga
 * harus menunggu lock, kontrol proporsional akan terasa patah-patah - masalah yang justru
 * ingin dihilangkan.
 *
 * Turun dibuat lebih cepat daripada naik: melambat adalah tindakan aman, jadi tidak ada
 * alasan menahannya.
 */
class SlewRateLimiter(
    private val risePerSecond: Float = 2.5f,
    private val fallPerSecond: Float = 6f,
) {

    var value: Float = 0f
        private set

    fun advance(target: Float, deltaSeconds: Float): Float {
        val dt = deltaSeconds.coerceIn(0f, 0.1f)
        val limit = if (target > value) risePerSecond * dt else fallPerSecond * dt
        val delta = target - value
        value = if (delta > 0f) {
            (value + minOf(delta, limit))
        } else {
            (value + maxOf(delta, -limit))
        }
        return value
    }

    /** Nol seketika, melewati pembatas - hanya untuk stop keras dan reset. */
    fun forceZero() {
        value = 0f
    }
}
