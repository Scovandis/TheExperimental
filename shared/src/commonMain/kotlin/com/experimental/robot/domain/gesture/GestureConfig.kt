package com.experimental.robot.domain.gesture

/**
 * Ambang batas deteksi ekstensi jari. Dipisah dari algoritma agar mudah dikalibrasi
 * per perangkat tanpa menyentuh logika.
 */
data class GestureConfig(
    /** Toleransi ekstensi jari: tip harus lebih tinggi dari PIP minimal sebesar nilai ini. */
    val fingerExtensionMargin: Float = 0.015f,
    /** Rasio jarak tip-vs-IP terhadap pergelangan agar jempol dianggap terbuka. */
    val thumbExtensionRatio: Float = 1.15f,
)
