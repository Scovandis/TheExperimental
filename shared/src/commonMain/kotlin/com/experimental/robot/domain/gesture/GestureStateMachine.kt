package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.StopReason

/** Fase siklus hidup sebuah gestur, dari tidak ada tangan sampai perintah dilepas. */
enum class GesturePhase(val label: String) {
    NO_HAND(label = "NO HAND"),
    HAND_DETECTED(label = "HAND DETECTED"),
    GESTURE_CANDIDATE(label = "CANDIDATE"),
    GESTURE_STABLE(label = "STABLE"),
    COMMAND_ACTIVE(label = "ACTIVE"),
    COMMAND_RELEASE(label = "RELEASE"),
}

/**
 * Ringkasan hasil persepsi dari frame kamera terakhir.
 *
 * [frameAtMs] adalah kuncinya: state machine memakainya untuk menghitung sudah berapa
 * lama tangan tidak terlihat. Karena angka itu berasal dari frame - bukan dari saat
 * [GestureStateMachine.update] dipanggil - kamera yang macet total tertangani oleh
 * mekanisme yang sama dengan tangan yang keluar dari frame.
 */
data class PerceptionSnapshot(
    val handDetected: Boolean = false,
    val action: RobotAction = RobotAction.IDLE,
    val tier: ConfidenceTier = ConfidenceTier.UNKNOWN,
    val confidence: Float = 0f,
    val emergencyTriggered: Boolean = false,
    val frameAtMs: Long = 0L,
)

data class GestureMachineState(
    val phase: GesturePhase = GesturePhase.NO_HAND,
    val halt: HaltMode = HaltMode.STOP,
    val reason: StopReason = StopReason.NO_HAND,
    val action: RobotAction = RobotAction.IDLE,
    val handLostMs: Long = 0L,
)

data class GestureMachineConfig(
    /** Tangan hilang lebih lama dari ini -> STOP. */
    val handLostStopMs: Long = 300L,
    /** Tangan hilang lebih lama dari ini -> SAFETY_LOCK, butuh reset manual. */
    val handLostLockMs: Long = 2_000L,
)

/**
 * State machine gestur: satu-satunya tempat transisi diizinkan terjadi.
 *
 * Dijalankan dari loop gerak 60 FPS, **bukan** dari callback kamera. Ini disengaja:
 * kalau state machine hanya berjalan saat ada frame baru, kamera yang berhenti
 * mengirim frame berarti tidak ada yang pernah menyimpulkan bahwa tangan hilang -
 * dan robot akan terus bergerak dengan perintah terakhirnya.
 *
 * Tiga jalur berhenti bisa memotong dari fase mana pun. Dua di antaranya
 * ([HaltMode.EMERGENCY_STOP] dan [HaltMode.SAFETY_LOCK]) mengunci diri dan hanya
 * bisa dilepas lewat [reset].
 */
class GestureStateMachine(private val config: GestureMachineConfig = GestureMachineConfig()) {

    private var latched: HaltMode = HaltMode.RUNNING
    private var latchedReason: StopReason = StopReason.NONE
    private var lastHandSeenMs: Long? = null
    private var wasLocked: Boolean = false

    fun update(snapshot: PerceptionSnapshot, nowMs: Long): GestureMachineState {
        if (snapshot.handDetected) lastHandSeenMs = snapshot.frameAtMs

        val handLostMs = lastHandSeenMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L

        // Kunci yang sudah aktif menang atas segalanya, termasuk tangan yang kembali.
        if (latched.isLatched) {
            wasLocked = false
            return GestureMachineState(
                phase = GesturePhase.NO_HAND,
                halt = latched,
                reason = latchedReason,
                action = RobotAction.IDLE,
                handLostMs = handLostMs,
            )
        }

        if (snapshot.emergencyTriggered) {
            return latch(HaltMode.EMERGENCY_STOP, StopReason.EMERGENCY, handLostMs)
        }

        if (lastHandSeenMs != null && handLostMs >= config.handLostLockMs) {
            return latch(HaltMode.SAFETY_LOCK, StopReason.HAND_LOST, handLostMs)
        }

        val handPresent = snapshot.handDetected && handLostMs < config.handLostStopMs
        if (!handPresent) {
            wasLocked = false
            // Jendela pendek sebelum STOP: arah dipertahankan, kecepatan diturunkan halus.
            val releasing = lastHandSeenMs != null && handLostMs < config.handLostStopMs
            return GestureMachineState(
                phase = if (releasing) GesturePhase.COMMAND_RELEASE else GesturePhase.NO_HAND,
                halt = if (releasing) HaltMode.RUNNING else HaltMode.STOP,
                reason = if (lastHandSeenMs == null) StopReason.NO_HAND else StopReason.HAND_LOST,
                action = if (releasing) snapshot.action else RobotAction.IDLE,
                handLostMs = handLostMs,
            )
        }

        return when (snapshot.tier) {
            ConfidenceTier.UNKNOWN -> {
                wasLocked = false
                GestureMachineState(
                    phase = GesturePhase.HAND_DETECTED,
                    halt = HaltMode.STOP,
                    reason = StopReason.LOW_CONFIDENCE,
                    action = RobotAction.IDLE,
                    handLostMs = 0L,
                )
            }

            // 50-75%: lock yang ada dipertahankan (histeresis), tapi tidak ada lock baru.
            ConfidenceTier.DETECTING -> GestureMachineState(
                phase = GesturePhase.HAND_DETECTED,
                halt = HaltMode.RUNNING,
                reason = StopReason.NONE,
                action = snapshot.action,
                handLostMs = 0L,
            )

            ConfidenceTier.CANDIDATE -> GestureMachineState(
                phase = GesturePhase.GESTURE_CANDIDATE,
                halt = HaltMode.RUNNING,
                reason = StopReason.NONE,
                action = snapshot.action,
                handLostMs = 0L,
            )

            ConfidenceTier.LOCKED -> {
                val justLocked = !wasLocked
                wasLocked = true
                GestureMachineState(
                    phase = if (justLocked) GesturePhase.GESTURE_STABLE else GesturePhase.COMMAND_ACTIVE,
                    halt = HaltMode.RUNNING,
                    reason = StopReason.NONE,
                    action = snapshot.action,
                    handLostMs = 0L,
                )
            }
        }
    }

    /** Melepas kunci darurat dan mengembalikan mesin ke kondisi awal. */
    fun reset() {
        latched = HaltMode.RUNNING
        latchedReason = StopReason.NONE
        lastHandSeenMs = null
        wasLocked = false
    }

    private fun latch(mode: HaltMode, reason: StopReason, handLostMs: Long): GestureMachineState {
        latched = mode
        latchedReason = reason
        wasLocked = false
        return GestureMachineState(
            phase = GesturePhase.NO_HAND,
            halt = mode,
            reason = reason,
            action = RobotAction.IDLE,
            handLostMs = handLostMs,
        )
    }
}
