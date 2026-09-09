package com.experimental.robot.di

import com.experimental.robot.data.HandLandmarkStream
import com.experimental.robot.data.HandTrackingRepository
import com.experimental.robot.domain.calibration.CalibrationProfile
import com.experimental.robot.domain.calibration.CalibrationRecorder
import com.experimental.robot.domain.command.CommandInterpreter
import com.experimental.robot.domain.command.ControlSpace
import com.experimental.robot.domain.command.ControlSpaceConfig
import com.experimental.robot.domain.command.SlewRateLimiter
import com.experimental.robot.domain.gesture.ConfidenceConfig
import com.experimental.robot.domain.gesture.EmergencyGestureDetector
import com.experimental.robot.domain.gesture.GestureConfig
import com.experimental.robot.domain.gesture.GestureStateMachine
import com.experimental.robot.domain.gesture.HandGestureClassifier
import com.experimental.robot.domain.gesture.TemporalStabilizer
import com.experimental.robot.domain.motion.MotionConfig
import com.experimental.robot.domain.motion.RobotMotionEngine
import com.experimental.robot.domain.safety.SafetyConfig
import com.experimental.robot.domain.safety.SafetyController
import com.experimental.robot.presentation.viewmodel.RobotControlViewModel

/**
 * Service locator sederhana (tanpa library DI) yang menyatukan dependensi antar layer.
 *
 * [handLandmarkStream] disengaja singleton: layer kamera pada tiap platform menulis
 * ke instance ini, sementara ViewModel membacanya sebagai [HandTrackingRepository].
 *
 * Seluruh ambang batas yang bisa dikalibrasi berkumpul di sini - [GestureConfig],
 * [ConfidenceConfig], [ControlSpaceConfig], [SafetyConfig], [MotionConfig] - sehingga
 * penyetelan perangkat tidak pernah menyentuh algoritma.
 */
object RobotGraph {

    val handLandmarkStream: HandLandmarkStream by lazy { HandLandmarkStream() }

    private val repository: HandTrackingRepository get() = handLandmarkStream

    /** Ambang batas gestur, sudah digeser profil kalibrasi bila ada. */
    var calibration: CalibrationProfile = CalibrationProfile.DEFAULT

    fun createRobotControlViewModel(): RobotControlViewModel {
        val gestureConfig = calibration.applyTo(GestureConfig())
        val controlSpaceConfig = calibration.applyTo(ControlSpaceConfig())

        return RobotControlViewModel(
            repository = repository,
            classifier = HandGestureClassifier(gestureConfig),
            stabilizer = TemporalStabilizer(holdMs = 300L, config = ConfidenceConfig()),
            stateMachine = GestureStateMachine(),
            interpreter = CommandInterpreter(
                controlSpace = ControlSpace(controlSpaceConfig),
                speedLimiter = SlewRateLimiter(),
            ),
            safety = SafetyController(SafetyConfig()),
            motionEngine = RobotMotionEngine(MotionConfig()),
            emergencyDetector = EmergencyGestureDetector(holdMs = 1_000L),
            calibrationRecorder = CalibrationRecorder(),
        )
    }
}
