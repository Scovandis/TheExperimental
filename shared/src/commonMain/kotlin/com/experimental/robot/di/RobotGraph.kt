package com.experimental.robot.di

import com.experimental.robot.data.HandLandmarkStream
import com.experimental.robot.data.HandTrackingRepository
import com.experimental.robot.domain.gesture.GestureConfig
import com.experimental.robot.domain.gesture.GestureDebouncer
import com.experimental.robot.domain.gesture.HandGestureClassifier
import com.experimental.robot.domain.motion.MotionConfig
import com.experimental.robot.domain.motion.RobotMotionEngine
import com.experimental.robot.presentation.viewmodel.RobotControlViewModel

/**
 * Service locator sederhana (tanpa library DI) yang menyatukan dependensi antar layer.
 *
 * [handLandmarkStream] disengaja singleton: layer kamera pada tiap platform menulis
 * ke instance ini, sementara ViewModel membacanya sebagai [HandTrackingRepository].
 */
object RobotGraph {

    val handLandmarkStream: HandLandmarkStream by lazy { HandLandmarkStream() }

    private val repository: HandTrackingRepository get() = handLandmarkStream

    fun createRobotControlViewModel(): RobotControlViewModel = RobotControlViewModel(
        repository = repository,
        classifier = HandGestureClassifier(GestureConfig()),
        debouncer = GestureDebouncer(framesToConfirm = 4),
        motionEngine = RobotMotionEngine(MotionConfig()),
    )
}
