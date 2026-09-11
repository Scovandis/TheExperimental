package com.experimental.robot.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.experimental.robot.data.FrameRateMeter
import com.experimental.robot.data.HandTrackingRepository
import com.experimental.robot.domain.calibration.CalibrationProfile
import com.experimental.robot.domain.calibration.CalibrationRecorder
import com.experimental.robot.domain.calibration.CalibrationStep
import com.experimental.robot.domain.command.CommandInterpreter
import com.experimental.robot.domain.gesture.ConfidenceTier
import com.experimental.robot.domain.gesture.EmergencyGestureDetector
import com.experimental.robot.domain.gesture.GestureClassifier
import com.experimental.robot.domain.gesture.GestureStateMachine
import com.experimental.robot.domain.gesture.HandGestureClassifier
import com.experimental.robot.domain.gesture.PerceptionSnapshot
import com.experimental.robot.domain.gesture.TemporalStabilizer
import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex
import com.experimental.robot.domain.model.Handedness
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotCommand
import com.experimental.robot.domain.model.RobotState
import com.experimental.robot.domain.motion.RobotMotionEngine
import com.experimental.robot.domain.safety.SafetyController
import com.experimental.robot.domain.safety.SafetySignals
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
 * ViewModel MVVM: menyatukan persepsi, keputusan, dan safety menjadi satu [RobotUiState].
 *
 * Ada dua clock yang berjalan terpisah, dan pemisahan itu disengaja:
 *
 * - **Jalur persepsi** ([onHandFrame]) berdenyut mengikuti FPS kamera. Tugasnya hanya
 *   mengubah frame menjadi [PerceptionSnapshot]; ia tidak memutuskan apa pun.
 * - **Loop kendali** ([startControlLoop]) berdenyut tetap 60 FPS. Di sinilah state machine,
 *   interpreter, safety, dan motion engine dijalankan.
 *
 * Kenapa keputusan tidak diambil di callback kamera: kalau kamera berhenti mengirim frame,
 * tidak akan ada yang pernah menyimpulkan bahwa tangan hilang - dan robot terus bergerak
 * dengan perintah terakhirnya. Loop kendali yang berdiri sendiri membuat tangan yang keluar
 * dari frame dan kamera yang macet tertangani mekanisme yang sama.
 */
class RobotControlViewModel(
    private val repository: HandTrackingRepository,
    private val classifier: GestureClassifier = HandGestureClassifier(),
    private val stabilizer: TemporalStabilizer = TemporalStabilizer(),
    private val stateMachine: GestureStateMachine = GestureStateMachine(),
    private val interpreter: CommandInterpreter = CommandInterpreter(),
    private val safety: SafetyController = SafetyController(),
    private val motionEngine: RobotMotionEngine = RobotMotionEngine(),
    private val emergencyDetector: EmergencyGestureDetector = EmergencyGestureDetector(),
    private val calibrationRecorder: CalibrationRecorder = CalibrationRecorder(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(RobotUiState())
    val uiState: StateFlow<RobotUiState> = _uiState.asStateFlow()

    /**
     * Profil kalibrasi aktif; diterapkan ke ambang batas lewat [RobotGraph].
     *
     * TODO(AUDIT_INCOMPLETE.md #2): "diterapkan lewat RobotGraph" ini belum benar terjadi —
     * [finishCalibration] hanya menulis ke variabel lokal ini, tidak pernah menulis balik ke
     * [RobotGraph.calibration] maupun membangun ulang [classifier]/[interpreter] yang sudah
     * dikonstruksi dengan config lama. Hasil wizard kalibrasi saat ini tidak berefek sama sekali
     * pada pipeline gestur yang sedang berjalan.
     */
    var calibrationProfile: CalibrationProfile = CalibrationProfile.DEFAULT
        private set

    private val startMark = TimeSource.Monotonic.markNow()
    private fun nowMs(): Long = startMark.elapsedNow().inWholeMilliseconds

    private val detectionRate = FrameRateMeter()
    private val renderRate = FrameRateMeter()
    private val commandRate = FrameRateMeter()

    /** Hasil persepsi terakhir; dibaca loop kendali, ditulis jalur kamera. */
    private var snapshot = PerceptionSnapshot()

    /** Aksi dari pad manual; masuk ke pipeline yang sama, termasuk gerbang safety. */
    private var manualAction: RobotAction? = null

    /**
     * Permintaan emergency stop dari tombol, menunggu dikonsumsi loop kendali.
     *
     * Disimpan terpisah dari [snapshot] karena snapshot ditimpa setiap frame kamera:
     * kalau penanda darurat ikut di sana, frame yang datang sebelum tick berikutnya
     * bisa menghapusnya - dan tombol berhentinya tidak berfungsi.
     */
    private var emergencyRequested: Boolean = false

    private var calibrationStep: CalibrationStep? = null

    init {
        observeHandTracking()
        observeTrackerStatus()
        observeCameraFps()
        startControlLoop()
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

    private fun observeCameraFps() {
        viewModelScope.launch {
            repository.cameraFps.collect { fps ->
                _uiState.update { it.copy(frameRates = it.frameRates.copy(camera = fps)) }
            }
        }
    }

    /**
     * Titik masuk tunggal untuk setiap hasil deteksi 21 landmark.
     *
     * Hanya menghasilkan snapshot dan angka telemetri - tidak ada keputusan gerak di sini.
     */
    fun onHandFrame(frame: HandFrame?) {
        val now = nowMs()
        val detectionFps = detectionRate.tick(now)
        val result = classifier.classify(frame)

        calibrationStep?.let { step ->
            recordCalibration(step, frame)
            _uiState.update {
                it.copy(
                    handDetected = result.handDetected,
                    landmarks = frame?.takeIf { f -> f.isValid }?.landmarks ?: emptyList(),
                    frameRates = it.frameRates.copy(detection = detectionFps),
                )
            }
            return
        }

        val stabilized = stabilizer.submit(result.action, result.confidence, now)
        val emergency = emergencyDetector.update(result.fingers, result.handDetected, now)

        snapshot = PerceptionSnapshot(
            handDetected = result.handDetected,
            action = stabilized.action,
            tier = stabilized.tier,
            confidence = stabilized.confidence,
            emergencyTriggered = emergency,
            frameAtMs = now,
        )

        _uiState.update { current ->
            current.copy(
                rawAction = result.action,
                stableAction = stabilized.action,
                confidence = stabilized.confidence,
                confidenceTier = stabilized.tier,
                stabilityMs = stabilized.stableMs,
                lockProgress = stabilized.progress,
                emergencyProgress = emergencyDetector.progress,
                fingers = if (result.handDetected) result.fingers else FingerState.NONE,
                handDetected = result.handDetected,
                handedness = frame?.handedness ?: Handedness.UNKNOWN,
                landmarks = frame?.takeIf { it.isValid }?.landmarks ?: emptyList(),
                frameRates = current.frameRates.copy(detection = detectionFps),
            )
        }
    }

    /**
     * Loop kendali 60 FPS: state machine, interpreter, safety, lalu animasi.
     *
     * Urutannya tidak boleh ditukar - safety harus menjadi hal terakhir yang menyentuh
     * perintah sebelum perintah itu dipakai.
     */
    private fun startControlLoop() {
        viewModelScope.launch {
            var lastTick = nowMs()
            while (isActive) {
                delay(FRAME_INTERVAL_MS)
                val now = nowMs()
                val deltaSeconds = (now - lastTick) / 1000f
                lastTick = now
                tick(now, deltaSeconds)
            }
        }
    }

    private fun tick(now: Long, deltaSeconds: Float) {
        val perception = manualAction?.let { action ->
            // Pad manual meniru gestur yang sudah terkunci penuh, sehingga tetap
            // melewati state machine dan gerbang safety seperti gestur asli.
            PerceptionSnapshot(
                handDetected = true,
                action = action,
                tier = ConfidenceTier.LOCKED,
                confidence = 1f,
                frameAtMs = now,
            )
        } ?: snapshot

        val requested = emergencyRequested
        emergencyRequested = false
        val machine = stateMachine.update(
            perception.copy(emergencyTriggered = perception.emergencyTriggered || requested),
            now,
        )
        val controlPoint = if (manualAction != null) {
            null
        } else {
            _uiState.value.landmarks.getOrNull(HandLandmarkIndex.WRIST)
        }

        val proposed = interpreter.interpret(machine, controlPoint, deltaSeconds, now)
        val verdict = safety.evaluate(
            command = proposed,
            machine = machine,
            signals = SafetySignals(
                confidence = perception.confidence,
                detectionFps = _uiState.value.frameRates.detection,
                // TODO(AUDIT_INCOMPLETE.md #3): hardcoded false — tidak ada jalur kode yang
                // pernah mengubahnya jadi true, jadi gerbang link/baterai/timeout/obstacle di
                // SafetyController selalu di-short-circuit ke RUNNING. Toggle "Real Robot" di UI
                // tidak sampai ke sini.
                requireRobotLink = false,
            ),
            nowMs = now,
        )

        val commandHz = commandRate.tick(now)
        val renderFps = renderRate.tick(now)

        _uiState.update { current ->
            current.copy(
                robot = motionEngine.step(current.robot, verdict.command, deltaSeconds),
                command = verdict.command,
                controlVector = interpreter.lastVector,
                phase = machine.phase,
                halt = verdict.halt,
                stopReason = verdict.reason,
                handLostMs = machine.handLostMs,
                frameRates = current.frameRates.copy(render = renderFps, commandHz = commandHz),
            )
        }
    }

    /** Dipakai pad kontrol manual (desktop/iOS atau saat izin kamera ditolak). */
    fun onManualActionPressed(action: RobotAction) {
        manualAction = action
        _uiState.update { it.copy(manualOverride = true, stableAction = action) }
    }

    // TODO(AUDIT_INCOMPLETE.md #4): fungsi ini tidak pernah dipanggil dari UI manapun
    // (PetaGesturGrid hanya memanggil onManualActionPressed lewat .clickable tap-sekali).
    // Akibatnya manualAction tidak pernah null lagi lewat jalur normal — override manual
    // mengunci permanen sampai Emergency Stop/Reset. Sambungkan ke pola press-and-hold
    // (bandingkan ManualControlPad.kt yang sudah punya onPress/tryAwaitRelease yang benar).
    fun onManualActionReleased() {
        manualAction = null
        _uiState.update { it.copy(manualOverride = false, stableAction = RobotAction.IDLE) }
    }

    /** Emergency stop dari tombol; setara dengan gestur darurat yang ditahan penuh. */
    fun onEmergencyStop() {
        manualAction = null
        emergencyRequested = true
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

    /**
     * Melepas kunci darurat dan mengembalikan robot ke posisi awal.
     *
     * Satu-satunya jalan keluar dari [com.experimental.robot.domain.model.HaltMode.EMERGENCY_STOP]
     * dan SAFETY_LOCK - keduanya sengaja tidak bisa pulih sendiri.
     */
    fun resetRobot() {
        emergencyRequested = false
        stabilizer.reset()
        stateMachine.reset()
        interpreter.reset()
        emergencyDetector.reset()
        manualAction = null
        snapshot = PerceptionSnapshot()
        _uiState.update {
            it.copy(
                robot = RobotState(),
                command = RobotCommand.STOP,
                stableAction = RobotAction.IDLE,
                confidence = 0f,
                confidenceTier = ConfidenceTier.UNKNOWN,
                lockProgress = 0f,
                emergencyProgress = 0f,
                manualOverride = false,
                cameraDistance = Camera.DEFAULT_DISTANCE,
            )
        }
    }

    // ── Kalibrasi ────────────────────────────────────────────────────────────

    fun startCalibration() {
        calibrationRecorder.reset()
        calibrationStep = CalibrationStep.CENTER
        manualAction = null
        snapshot = PerceptionSnapshot()
        _uiState.update {
            it.copy(calibration = CalibrationUiState(step = CalibrationStep.CENTER))
        }
    }

    /** Lanjut ke langkah berikutnya; menyimpan profil begitu langkah terakhir selesai. */
    fun advanceCalibration() {
        val step = calibrationStep ?: return
        val next = step.next
        if (next == null) {
            finishCalibration()
            return
        }
        calibrationStep = next
        _uiState.update { it.copy(calibration = CalibrationUiState(step = next)) }
    }

    // TODO(AUDIT_INCOMPLETE.md #2): hasil build() di sini hanya menimpa calibrationProfile lokal.
    // Untuk benar-benar berefek, ini perlu ditulis balik ke RobotGraph.calibration DAN
    // classifier/interpreter yang dipakai ViewModel perlu dibangun ulang dengan config baru —
    // keduanya belum terjadi sama sekali di kode saat ini.
    fun finishCalibration() {
        calibrationProfile = calibrationRecorder.build(calibrationProfile)
        calibrationStep = null
        _uiState.update { it.copy(calibration = null) }
    }

    fun cancelCalibration() {
        calibrationRecorder.reset()
        calibrationStep = null
        _uiState.update { it.copy(calibration = null) }
    }

    private fun recordCalibration(step: CalibrationStep, frame: HandFrame?) {
        val complete = calibrationRecorder.record(step, frame)
        _uiState.update {
            it.copy(
                calibration = CalibrationUiState(
                    step = step,
                    progress = calibrationRecorder.progress(step),
                    stepComplete = complete,
                ),
            )
        }
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 16L
    }
}
