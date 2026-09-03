package com.experimental.robot.presentation.viewmodel

import com.experimental.robot.data.TrackerStatus
import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.Handedness
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState

/**
 * Satu-satunya sumber kebenaran untuk layer View.
 *
 * @param robot transformasi robot yang siap digambar.
 * @param rawAction hasil klasifikasi frame terakhir (belum di-debounce).
 * @param stableAction aksi yang sudah lolos debounce dan benar-benar dieksekusi.
 * @param debounceProgress 0f..1f progres konfirmasi gestur baru.
 * @param landmarks 21 titik untuk overlay kerangka tangan.
 * @param manualOverride true bila robot sedang dikendalikan tombol manual.
 * @param renderMode visualisasi robot yang aktif (3D atau siluet 2D).
 */
data class RobotUiState(
    val robot: RobotState = RobotState(),
    val rawAction: RobotAction = RobotAction.IDLE,
    val stableAction: RobotAction = RobotAction.IDLE,
    val debounceProgress: Float = 1f,
    val fingers: FingerState = FingerState.NONE,
    val handDetected: Boolean = false,
    val handedness: Handedness = Handedness.UNKNOWN,
    val landmarks: List<HandPoint> = emptyList(),
    val detectionFps: Int = 0,
    val trackerStatus: TrackerStatus = TrackerStatus.Idle,
    val manualOverride: Boolean = false,
    val renderMode: RobotRenderMode = RobotRenderMode.THREE_D,
)
