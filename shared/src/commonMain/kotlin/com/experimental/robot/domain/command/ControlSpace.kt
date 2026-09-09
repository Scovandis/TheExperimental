package com.experimental.robot.domain.command

import kotlin.math.sqrt

/**
 * Ambang batas ruang kontrol, dalam koordinat ternormalisasi 0..1 dari MediaPipe.
 *
 * Karena semuanya ternormalisasi, kontrol tidak berubah ketika tangan mendekat atau
 * menjauh dari kamera.
 *
 * @param neutralX pusat ruang kontrol; diganti hasil kalibrasi per pengguna.
 * @param hysteresis lebar tambahan untuk keluar dari sebuah zona. Masuk di 0.35,
 *        keluar di 0.38 - tanpa ini tangan yang berhenti tepat di ambang akan
 *        menghasilkan zona yang berkedip-kedip.
 * @param deadRadius jarak radial dari pusat yang masih dianggap diam.
 * @param fullSpeedRadius jarak radial yang menghasilkan kecepatan penuh.
 * @param minSpeed kecepatan minimum begitu keluar dari dead zone, supaya gerakan
 *        pertama terasa - bukan merambat dari nol.
 */
data class ControlSpaceConfig(
    val neutralX: Float = 0.5f,
    val neutralY: Float = 0.5f,
    val lowThreshold: Float = 0.35f,
    val highThreshold: Float = 0.65f,
    val hysteresis: Float = 0.03f,
    val deadRadius: Float = 0.15f,
    val fullSpeedRadius: Float = 0.42f,
    val minSpeed: Float = 0.15f,
)

/** Posisi tangan pada satu sumbu, setelah histeresis. */
enum class AxisZone { LOW, CENTER, HIGH }

/**
 * @param magnitude 0f..1f dari jarak radial ke pusat; inilah kecepatan yang diminta.
 */
data class ControlVector(
    val horizontal: AxisZone = AxisZone.CENTER,
    val vertical: AxisZone = AxisZone.CENTER,
    val magnitude: Float = 0f,
) {
    val inDeadZone: Boolean get() = magnitude <= 0f
}

/**
 * Memetakan posisi tangan ke zona arah dan magnitude kecepatan.
 *
 * Arah datang dari zona kotak (dua sumbu independen), kecepatan dari jarak radial ke
 * pusat. Keduanya sengaja tidak memakai geometri yang sama: zona kotak memberi batas
 * yang jelas dan mudah dihafal, sedangkan jarak radial memberi kecepatan yang mulus
 * ke segala arah.
 *
 * Menyimpan state karena histeresis butuh tahu zona sebelumnya.
 */
class ControlSpace(private val config: ControlSpaceConfig = ControlSpaceConfig()) {

    private var horizontal: AxisZone = AxisZone.CENTER
    private var vertical: AxisZone = AxisZone.CENTER

    fun resolve(x: Float, y: Float): ControlVector {
        horizontal = gate(x, horizontal)
        vertical = gate(y, vertical)
        return ControlVector(
            horizontal = horizontal,
            vertical = vertical,
            magnitude = magnitude(x, y),
        )
    }

    fun reset() {
        horizontal = AxisZone.CENTER
        vertical = AxisZone.CENTER
    }

    /** Zona baru dengan histeresis: keluar butuh melewati ambang plus [ControlSpaceConfig.hysteresis]. */
    private fun gate(value: Float, current: AxisZone): AxisZone = when (current) {
        AxisZone.CENTER -> fromCenter(value)
        AxisZone.LOW ->
            if (value <= config.lowThreshold + config.hysteresis) AxisZone.LOW else fromCenter(value)
        AxisZone.HIGH ->
            if (value >= config.highThreshold - config.hysteresis) AxisZone.HIGH else fromCenter(value)
    }

    private fun fromCenter(value: Float): AxisZone = when {
        value < config.lowThreshold -> AxisZone.LOW
        value > config.highThreshold -> AxisZone.HIGH
        else -> AxisZone.CENTER
    }

    private fun magnitude(x: Float, y: Float): Float {
        val dx = x - config.neutralX
        val dy = y - config.neutralY
        val radius = sqrt(dx * dx + dy * dy)
        if (radius <= config.deadRadius) return 0f

        val span = config.fullSpeedRadius - config.deadRadius
        if (span <= 0f) return 1f
        val t = ((radius - config.deadRadius) / span).coerceIn(0f, 1f)
        return (config.minSpeed + (1f - config.minSpeed) * t).coerceIn(0f, 1f)
    }
}
