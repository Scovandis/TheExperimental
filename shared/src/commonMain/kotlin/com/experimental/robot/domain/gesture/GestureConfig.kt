package com.experimental.robot.domain.gesture

/**
 * Ambang batas klasifikasi gestur. Dipisah dari algoritma agar mudah dikalibrasi
 * per perangkat tanpa menyentuh logika.
 */
data class GestureConfig(
    /** Y pergelangan minimal agar telapak terbuka dibaca sebagai JONGKOK. */
    val crouchWristY: Float = 0.65f,
    /** Selisih X minimal ujung telunjuk vs pergelangan agar arah putar valid (dead zone). */
    val rotateDeadZoneX: Float = 0.05f,
    /** Toleransi ekstensi jari: tip harus lebih tinggi dari PIP minimal sebesar nilai ini. */
    val fingerExtensionMargin: Float = 0.015f,
    /** Rasio jarak tip-vs-IP terhadap pergelangan agar jempol dianggap terbuka. */
    val thumbExtensionRatio: Float = 1.15f,
    /** Selisih Y minimal ujung telunjuk di bawah pergelangan agar dibaca MUNDUR. */
    val backwardPointMargin: Float = 0.03f,
    /** Jumlah jari panjang terbuka minimal agar dianggap telapak terbuka. */
    val openPalmMinFingers: Int = 4,
)
