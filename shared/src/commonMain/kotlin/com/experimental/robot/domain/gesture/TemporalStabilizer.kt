package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.RobotAction

/** Keluaran satu langkah stabilisasi; menggantikan pasangan properti mutable. */
data class StabilizedGesture(
    val action: RobotAction = RobotAction.IDLE,
    val tier: ConfidenceTier = ConfidenceTier.UNKNOWN,
    val confidence: Float = 0f,
    /** Berapa lama kondisi saat ini bertahan (kandidat menunggu, atau lock berjalan). */
    val stableMs: Long = 0L,
    /** 0f..1f progres menuju lock; dipakai HUD sebagai indikator. */
    val progress: Float = 0f,
)

/**
 * Stabilisasi temporal berbasis **waktu**, bukan jumlah frame.
 *
 * Pendahulunya (`GestureDebouncer`) menghitung 4 frame beruntun, yang membuat latensi
 * konfirmasi bergantung pada FPS kamera: 133 ms di perangkat 30 FPS, tapi 400 ms di
 * perangkat 10 FPS. Ambang dalam milidetik membuat perilakunya sama di semua perangkat.
 *
 * Waktu disuntikkan lewat parameter [submit], bukan dibaca dari jam sistem, supaya
 * seluruh perilakunya bisa diuji tanpa menunggu waktu nyata.
 *
 * Histeresis: lock dipertahankan selama keyakinan masih di atas [ConfidenceConfig.unknownCeiling],
 * walau sudah turun di bawah syarat untuk mengunci gestur baru. Tanpa ini, keyakinan
 * yang berosilasi di sekitar ambang akan melepas dan mengambil lock berulang kali.
 */
class TemporalStabilizer(
    private val holdMs: Long = 300L,
    private val config: ConfidenceConfig = ConfidenceConfig(),
) {

    private var candidate: RobotAction = RobotAction.IDLE
    private var candidateSinceMs: Long = 0L
    private var locked: RobotAction = RobotAction.IDLE
    private var lockedSinceMs: Long = 0L

    val current: RobotAction get() = locked

    fun submit(raw: RobotAction, confidence: Float, nowMs: Long): StabilizedGesture {
        when (ConfidenceTier.of(confidence, config)) {
            // Terlalu tidak yakin untuk apa pun: lepas lock, robot dihentikan.
            ConfidenceTier.UNKNOWN -> {
                candidate = RobotAction.IDLE
                candidateSinceMs = nowMs
                locked = RobotAction.IDLE
                lockedSinceMs = nowMs
                return StabilizedGesture(
                    action = RobotAction.IDLE,
                    tier = ConfidenceTier.UNKNOWN,
                    confidence = confidence,
                )
            }

            // Cukup untuk menahan lock yang ada, belum cukup untuk mencari lock baru.
            ConfidenceTier.DETECTING -> {
                candidate = locked
                candidateSinceMs = nowMs
                return StabilizedGesture(
                    action = locked,
                    tier = ConfidenceTier.DETECTING,
                    confidence = confidence,
                    stableMs = nowMs - lockedSinceMs,
                    progress = 0f,
                )
            }

            ConfidenceTier.CANDIDATE, ConfidenceTier.LOCKED -> Unit
        }

        if (raw != candidate) {
            candidate = raw
            candidateSinceMs = nowMs
        }

        if (raw == locked) {
            return StabilizedGesture(
                action = locked,
                tier = ConfidenceTier.LOCKED,
                confidence = confidence,
                stableMs = nowMs - lockedSinceMs,
                progress = 1f,
            )
        }

        val heldMs = nowMs - candidateSinceMs
        val mayLock = heldMs >= holdMs && confidence >= config.lockFloor

        return if (mayLock) {
            locked = candidate
            lockedSinceMs = nowMs
            StabilizedGesture(
                action = locked,
                tier = ConfidenceTier.LOCKED,
                confidence = confidence,
                stableMs = 0L,
                progress = 1f,
            )
        } else {
            StabilizedGesture(
                action = locked,
                tier = ConfidenceTier.CANDIDATE,
                confidence = confidence,
                stableMs = heldMs,
                progress = (heldMs.toFloat() / holdMs).coerceIn(0f, 1f),
            )
        }
    }

    fun reset() {
        candidate = RobotAction.IDLE
        candidateSinceMs = 0L
        locked = RobotAction.IDLE
        lockedSinceMs = 0L
    }
}
