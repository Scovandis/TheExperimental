package com.experimental.robot.domain.model

/** Status ekstensi (terbuka/tertutup) tiap jari untuk satu frame. */
data class FingerState(
    val thumb: Boolean = false,
    val index: Boolean = false,
    val middle: Boolean = false,
    val ring: Boolean = false,
    val pinky: Boolean = false,
) {
    /** Jumlah jari terbuka tanpa menghitung jempol (sesuai logika klasifikasi). */
    val extendedCount: Int
        get() = listOf(index, middle, ring, pinky).count { it }

    /** Semua jari (termasuk jempol) tertutup -> kepalan tangan. */
    val isFist: Boolean
        get() = extendedCount == 0 && !thumb

    fun asList(): List<Pair<String, Boolean>> = listOf(
        "Jempol" to thumb,
        "Telunjuk" to index,
        "Tengah" to middle,
        "Manis" to ring,
        "Kelingking" to pinky,
    )

    companion object {
        val NONE = FingerState()
    }
}
