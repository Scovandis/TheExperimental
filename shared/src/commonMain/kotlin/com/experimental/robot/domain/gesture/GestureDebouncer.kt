package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.RobotAction

/**
 * Temporal smoothing / debounce filter.
 *
 * Aksi baru hanya diterima setelah bertahan [framesToConfirm] frame berturut-turut,
 * sehingga transisi tangan (misal saat membuka kepalan) tidak memicu gerakan liar.
 */
class GestureDebouncer(private val framesToConfirm: Int = 4) {

    private var stableAction: RobotAction = RobotAction.IDLE
    private var candidate: RobotAction = RobotAction.IDLE
    private var streak: Int = 0

    /** Progres 0f..1f menuju konfirmasi kandidat; dipakai HUD sebagai indikator. */
    var progress: Float = 1f
        private set

    val current: RobotAction get() = stableAction

    fun submit(raw: RobotAction): RobotAction {
        if (raw == stableAction) {
            candidate = raw
            streak = framesToConfirm
            progress = 1f
            return stableAction
        }

        if (raw == candidate) {
            streak++
        } else {
            candidate = raw
            streak = 1
        }

        progress = (streak.toFloat() / framesToConfirm).coerceIn(0f, 1f)
        if (streak >= framesToConfirm) {
            stableAction = candidate
            progress = 1f
        }
        return stableAction
    }

    fun reset() {
        stableAction = RobotAction.IDLE
        candidate = RobotAction.IDLE
        streak = 0
        progress = 1f
    }
}
