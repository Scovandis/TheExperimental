package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.FingerState

/**
 * Mendeteksi gestur darurat: **kepalan dengan jempol terbuka**, ditahan [holdMs].
 *
 * Pose ini dipilih justru karena tidak dipakai gestur lain. Kepalan biasa sudah
 * berarti IDLE dan telapak terbuka sudah berarti MAJU, jadi keduanya tidak bisa
 * dipakai sebagai pemicu darurat - pengguna yang menahan MAJU beberapa detik akan
 * memicu emergency stop tanpa sengaja. Kepalan + jempol saat ini jatuh ke IDLE
 * lewat cabang `else` di [HandGestureClassifier], sehingga bebas dipakai di sini.
 *
 * Menahan penuh [holdMs] disyaratkan supaya pose yang terlewat saat tangan berpindah
 * tidak menghentikan robot.
 */
class EmergencyGestureDetector(private val holdMs: Long = 1_000L) {

    private var heldSinceMs: Long? = null

    /** 0f..1f progres menahan; dipakai UI untuk menunjukkan darurat akan terpicu. */
    var progress: Float = 0f
        private set

    fun update(fingers: FingerState, handDetected: Boolean, nowMs: Long): Boolean {
        val posed = handDetected && fingers.thumb && fingers.extendedCount == 0
        if (!posed) {
            reset()
            return false
        }

        val since = heldSinceMs ?: nowMs.also { heldSinceMs = it }
        val heldMs = nowMs - since
        progress = (heldMs.toFloat() / holdMs).coerceIn(0f, 1f)
        return heldMs >= holdMs
    }

    fun reset() {
        heldSinceMs = null
        progress = 0f
    }
}
