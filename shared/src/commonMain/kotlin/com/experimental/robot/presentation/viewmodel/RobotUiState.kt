package com.experimental.robot.presentation.viewmodel

import com.experimental.robot.data.TrackerStatus
import com.experimental.robot.domain.calibration.CalibrationStep
import com.experimental.robot.domain.command.ControlVector
import com.experimental.robot.domain.gesture.ConfidenceTier
import com.experimental.robot.domain.gesture.GesturePhase
import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.Handedness
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotCommand
import com.experimental.robot.domain.model.RobotState
import com.experimental.robot.domain.model.StopReason
import com.experimental.robot.presentation.render.Camera

/** Laju keempat tahap pipeline, diukur terpisah supaya bisa dipakai mendiagnosis. */
data class FrameRates(
    val camera: Int = 0,
    val detection: Int = 0,
    val render: Int = 0,
    val commandHz: Int = 0,
)

/** Kondisi wizard kalibrasi; null saat kalibrasi tidak berjalan. */
data class CalibrationUiState(
    val step: CalibrationStep = CalibrationStep.CENTER,
    val progress: Float = 0f,
    val stepComplete: Boolean = false,
)

/**
 * Satu-satunya sumber kebenaran untuk layer View.
 *
 * Dikelompokkan mengikuti tiga level antarmuka: [command] dan [confidence] untuk mode
 * pengguna, blok persepsi dan [frameRates] untuk mode developer, [halt] dan [stopReason]
 * untuk keduanya.
 *
 * @param command perintah yang benar-benar dieksekusi, sesudah lewat gerbang safety.
 * @param rawAction hasil klasifikasi frame terakhir, belum distabilkan.
 * @param stableAction gestur yang terkunci state machine.
 * @param confidence 0f..1f keyakinan gestur terakhir.
 * @param stabilityMs berapa lama kondisi gestur saat ini bertahan.
 * @param lockProgress 0f..1f progres menuju lock; dipakai HUD sebagai indikator.
 * @param emergencyProgress 0f..1f progres menahan gestur darurat.
 * @param handLostMs berapa lama tangan sudah tidak terlihat.
 * @param cameraDistance jarak kamera orbit panggung 3D; murni state tampilan.
 */
data class RobotUiState(
    val robot: RobotState = RobotState(),
    val command: RobotCommand = RobotCommand.STOP,
    val controlVector: ControlVector = ControlVector(),

    val rawAction: RobotAction = RobotAction.IDLE,
    val stableAction: RobotAction = RobotAction.IDLE,
    val confidence: Float = 0f,
    val confidenceTier: ConfidenceTier = ConfidenceTier.UNKNOWN,
    val stabilityMs: Long = 0L,
    val lockProgress: Float = 0f,

    val phase: GesturePhase = GesturePhase.NO_HAND,
    val halt: HaltMode = HaltMode.STOP,
    val stopReason: StopReason = StopReason.NO_HAND,
    val emergencyProgress: Float = 0f,
    val handLostMs: Long = 0L,

    val fingers: FingerState = FingerState.NONE,
    val handDetected: Boolean = false,
    val handedness: Handedness = Handedness.UNKNOWN,
    val landmarks: List<HandPoint> = emptyList(),

    val frameRates: FrameRates = FrameRates(),
    val trackerStatus: TrackerStatus = TrackerStatus.Idle,
    val manualOverride: Boolean = false,
    val calibration: CalibrationUiState? = null,
    val renderMode: RobotRenderMode = RobotRenderMode.THREE_D,
    val cameraDistance: Float = Camera.DEFAULT_DISTANCE,
) {
    /** Gestur sudah terkunci dan perintah sedang diterbitkan. */
    val locked: Boolean get() = confidenceTier == ConfidenceTier.LOCKED && !halt.blocksMovement

    /** Butuh reset manual; UI menampilkan tombol RESET, bukan sekadar peringatan. */
    val latched: Boolean get() = halt.isLatched

    val detectionFps: Int get() = frameRates.detection
}
