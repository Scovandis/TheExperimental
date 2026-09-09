package com.experimental.robot.domain.safety

import com.experimental.robot.domain.gesture.GestureMachineState
import com.experimental.robot.domain.model.Direction
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.RobotCommand
import com.experimental.robot.domain.model.StopReason

data class SafetyConfig(
    val minConfidence: Float = 0.50f,
    val minDetectionFps: Int = 15,
    val obstacleStopCm: Float = 25f,
    val batteryStopPercent: Int = 15,
    val commandTimeoutMs: Long = 500L,
)

/**
 * Semua sinyal yang dipertimbangkan gerbang safety.
 *
 * Empat sinyal robot bertipe nullable dengan sengaja: `null` berarti **belum diketahui**,
 * dan pada mode robot fisik itu diperlakukan sebagai gagal, bukan sebagai lolos. Kalau
 * status penghalang belum pernah sampai, robot tidak boleh dianggap aman untuk maju.
 *
 * @param requireRobotLink true hanya pada mode robot fisik. Pada mode simulasi keempat
 *        gerbang robot dilewati, tapi kodenya tetap satu - jadi bug di gerbang lain
 *        ketemu sebelum ada motor yang bisa menabrak.
 */
data class SafetySignals(
    val confidence: Float = 0f,
    val detectionFps: Int = 0,
    val requireRobotLink: Boolean = false,
    val linkConnected: Boolean? = null,
    val obstacleDistanceCm: Float? = null,
    val batteryPercent: Int? = null,
    val lastAckAtMs: Long? = null,
)

data class SafetyVerdict(
    val command: RobotCommand,
    val halt: HaltMode,
    val reason: StopReason,
) {
    val blocked: Boolean get() = halt.blocksMovement
}

/**
 * Gerbang antara perintah dan motor. Tujuh pemeriksaan, urutan menentukan prioritas:
 * kunci darurat menang atas segalanya, lalu keputusan state machine, lalu kualitas
 * persepsi, lalu kondisi robot fisik.
 *
 * **Fail-closed**: kondisi yang tidak diketahui diperlakukan sebagai gagal. Fungsi ini
 * murni - tidak menyimpan state - supaya seluruh tabel keputusannya bisa diuji langsung.
 */
class SafetyController(private val config: SafetyConfig = SafetyConfig()) {

    fun evaluate(
        command: RobotCommand,
        machine: GestureMachineState,
        signals: SafetySignals,
        nowMs: Long,
    ): SafetyVerdict {
        // 1-3: keputusan state machine (darurat, tangan hilang, keyakinan nol).
        if (machine.halt.blocksMovement) {
            return block(command, machine.halt, machine.reason)
        }

        // 4: keyakinan gestur di bawah ambang layak-pakai.
        if (signals.confidence < config.minConfidence) {
            return block(command, HaltMode.STOP, StopReason.LOW_CONFIDENCE)
        }

        // 5: laju deteksi terlalu rendah untuk kontrol yang layak. Nol berarti belum
        // pernah diukur, jadi hanya dinilai ketika robot benar-benar diminta bergerak.
        if (command.isMoving && signals.detectionFps in 1 until config.minDetectionFps) {
            return block(command, HaltMode.STOP, StopReason.LOW_FRAME_RATE)
        }

        if (!signals.requireRobotLink) {
            return SafetyVerdict(command, HaltMode.RUNNING, StopReason.NONE)
        }

        // 6a: tautan ke robot. Tidak diketahui = terputus.
        if (signals.linkConnected != true) {
            return block(command, HaltMode.STOP, StopReason.LINK_LOST)
        }

        // 6b: robot masih merespons. Tidak ada ACK = timeout.
        val lastAck = signals.lastAckAtMs
        if (lastAck == null || nowMs - lastAck > config.commandTimeoutMs) {
            return block(command, HaltMode.STOP, StopReason.COMMAND_TIMEOUT)
        }

        // 6c: baterai. Tidak diketahui = kritis.
        val battery = signals.batteryPercent
        if (battery == null || battery < config.batteryStopPercent) {
            return block(command, HaltMode.STOP, StopReason.BATTERY)
        }

        // 7: penghalang - hanya relevan saat diminta maju, tapi tidak diketahui = ada.
        if (command.direction == Direction.FORWARD) {
            val distance = signals.obstacleDistanceCm
            if (distance == null || distance < config.obstacleStopCm) {
                return block(command, HaltMode.STOP, StopReason.OBSTACLE)
            }
        }

        return SafetyVerdict(command, HaltMode.RUNNING, StopReason.NONE)
    }

    private fun block(command: RobotCommand, halt: HaltMode, reason: StopReason) =
        SafetyVerdict(command.halted(), halt, reason)
}
