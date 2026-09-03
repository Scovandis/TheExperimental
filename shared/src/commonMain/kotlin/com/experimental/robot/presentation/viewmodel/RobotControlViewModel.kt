package com.experimental.robot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimental.robot.data.HandTrackingRepository
import com.experimental.robot.domain.gesture.GestureClassifier
import com.experimental.robot.domain.gesture.GestureDebouncer
import com.experimental.robot.domain.gesture.HandGestureClassifier
import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.Handedness
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import com.experimental.robot.domain.motion.RobotMotionEngine
import com.experimental.robot.presentation.render.Camera
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/**
 * ViewModel MVVM: menghubungkan [HandTrackingRepository] (Model) dengan state UI (View).
 *
 * Alur:
 * 1. `handFrames` -> [GestureClassifier] -> [GestureDebouncer] -> aksi stabil
 * 2. Loop gerak ~60 FPS memajukan [RobotState] via [RobotMotionEngine] selama aksi bertahan,
 *    sehingga robot bergerak kontinu, bukan sekali lompat per frame kamera.
 */
class RobotControlViewModel(
    private val repository: HandTrackingRepository,
    private val classifier: GestureClassifier = HandGestureClassifier(),
    private val debouncer: GestureDebouncer = GestureDebouncer(framesToConfirm = 4),
    private val motionEngine: RobotMotionEngine = RobotMotionEngine(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(RobotUiState())
    val uiState: StateFlow<RobotUiState> = _uiState.asStateFlow()

    /** Aksi dari tombol manual; menimpa gestur selama tombol ditahan. */
    private var manualAction: RobotAction? = null

    private var framesInWindow = 0
    private var fpsWindowStart = TimeSource.Monotonic.markNow()

    init {
        observeHandTracking()
        observeTrackerStatus()
        startMotionLoop()
    }

    private fun observeHandTracking() {
        viewModelScope.launch {
            repository.handFrames.collect { frame -> onHandFrame(frame) }
        }
    }

    private fun observeTrackerStatus() {
        viewModelScope.launch {
            repository.status.collect { status ->
                _uiState.update { it.copy(trackerStatus = status) }
            }
        }
    }

    /** Titik masuk tunggal untuk setiap hasil deteksi 21 landmark. */
    fun onHandFrame(frame: HandFrame?) {
        val result = classifier.classify(frame)
        val stable = debouncer.submit(result.action)

        _uiState.update { current ->
            current.copy(
                rawAction = result.action,
                stableAction = stable,
                debounceProgress = debouncer.progress,
                fingers = if (result.handDetected) result.fingers else FingerState.NONE,
                handDetected = result.handDetected,
                handedness = frame?.handedness ?: Handedness.UNKNOWN,
                landmarks = frame?.takeIf { it.isValid }?.landmarks ?: emptyList(),
                detectionFps = measureFps(),
            )
        }
    }

    private fun startMotionLoop() {
        viewModelScope.launch {
            var lastTick = TimeSource.Monotonic.markNow()
            while (isActive) {
                delay(FRAME_INTERVAL_MS)
                val now = TimeSource.Monotonic.markNow()
                val deltaSeconds = (now - lastTick).inWholeMicroseconds / 1_000_000f
                lastTick = now

                val action = manualAction ?: _uiState.value.stableAction
                _uiState.update { current ->
                    current.copy(robot = motionEngine.step(current.robot, action, deltaSeconds))
                }
            }
        }
    }

    /** Dipakai panel kontrol manual (desktop/iOS atau saat izin kamera ditolak). */
    fun onManualActionPressed(action: RobotAction) {
        manualAction = action
        _uiState.update { it.copy(manualOverride = true, stableAction = action) }
    }

    fun onManualActionReleased() {
        manualAction = null
        _uiState.update { it.copy(manualOverride = false, stableAction = RobotAction.IDLE) }
    }

    /** Ganti visualisasi 3D <-> 2D. */
    fun toggleRenderMode() {
        _uiState.update { it.copy(renderMode = it.renderMode.toggled()) }
    }

    /** Dekatkan kamera panggung 3D satu langkah (dibatasi [Camera.MIN_DISTANCE]). */
    fun zoomIn() {
        _uiState.update {
            it.copy(cameraDistance = (it.cameraDistance - Camera.ZOOM_STEP).coerceAtLeast(Camera.MIN_DISTANCE))
        }
    }

    /** Jauhkan kamera panggung 3D satu langkah (dibatasi [Camera.MAX_DISTANCE]). */
    fun zoomOut() {
        _uiState.update {
            it.copy(cameraDistance = (it.cameraDistance + Camera.ZOOM_STEP).coerceAtMost(Camera.MAX_DISTANCE))
        }
    }

    /** Kembalikan robot ke posisi awal & kamera ke jarak bawaan tanpa mengubah pipeline kamera fisik. */
    fun resetRobot() {
        debouncer.reset()
        manualAction = null
        _uiState.update {
            it.copy(
                robot = RobotState(),
                stableAction = RobotAction.IDLE,
                manualOverride = false,
                cameraDistance = Camera.DEFAULT_DISTANCE,
            )
        }
    }

    private fun measureFps(): Int {
        framesInWindow++
        val elapsed = fpsWindowStart.elapsedNow().inWholeMilliseconds
        if (elapsed < FPS_WINDOW_MS) return _uiState.value.detectionFps
        val fps = (framesInWindow * 1000f / elapsed).toInt()
        framesInWindow = 0
        fpsWindowStart = TimeSource.Monotonic.markNow()
        return fps
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 16L
        const val FPS_WINDOW_MS = 500L
    }
}
