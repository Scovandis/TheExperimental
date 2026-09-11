package com.experimental.robot.domain.gesture

/**
 * Ambang batas keyakinan gestur. Dipakai bersama oleh [GestureConfidenceScorer]
 * (menghitung skor) dan [TemporalStabilizer] (memutuskan kapan mengunci), supaya
 * hanya ada satu sumber angka.
 */
data class ConfidenceConfig(
    /** Di bawah ini gestur diabaikan sepenuhnya dan robot dihentikan. */
    val unknownCeiling: Float = 0.50f,
    /** Di bawah ini gestur ditampilkan tapi timer stabilitas tidak berjalan. */
    val candidateFloor: Float = 0.75f,
    /** Syarat keyakinan untuk boleh mengunci gestur baru. */
    val lockFloor: Float = 0.85f,
    /** Rentang margin ekstensi jari (telunjuk-kelingking) yang memberi skor penuh. */
    val extensionSpan: Float = 0.055f,
    /** Rentang margin ekstensi jempol (rasio jarak tip-vs-IP ke pergelangan) yang memberi skor penuh. */
    val thumbSpan: Float = 0.05f,
    /** Dipakai bila detektor tidak menyediakan skor kehadiran tangan. */
    val neutralPresence: Float = 0.90f,
)

/** Tingkat keyakinan gestur, seperti yang ditampilkan di HUD. */
enum class ConfidenceTier(val label: String) {
    /** Di bawah 50% - gestur diabaikan, robot dihentikan. */
    UNKNOWN(label = "UNKNOWN"),

    /** 50-75% - ditampilkan, lock yang ada dipertahankan, kandidat baru belum dihitung. */
    DETECTING(label = "DETECTING"),

    /** Di atas 75% - timer stabilitas berjalan. */
    CANDIDATE(label = "CANDIDATE"),

    /** Di atas 85% dan sudah stabil - perintah diterbitkan. */
    LOCKED(label = "LOCKED");

    val isActionable: Boolean get() = this == CANDIDATE || this == LOCKED

    companion object {
        fun of(confidence: Float, config: ConfidenceConfig = ConfidenceConfig()): ConfidenceTier = when {
            confidence < config.unknownCeiling -> UNKNOWN
            confidence < config.candidateFloor -> DETECTING
            confidence < config.lockFloor -> CANDIDATE
            else -> LOCKED
        }
    }
}
