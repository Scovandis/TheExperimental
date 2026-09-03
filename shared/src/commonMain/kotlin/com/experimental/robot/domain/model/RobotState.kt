package com.experimental.robot.domain.model

/**
 * Transformasi robot yang dirender. Semua nilai sudah hasil interpolasi
 * sehingga layer UI hanya perlu menggambar apa adanya.
 *
 * @param positionZ maju (+) / mundur (-), rentang dibatasi engine.
 * @param rotationY derajat yaw, akumulatif.
 * @param scaleY 1.0 = berdiri normal, 0.6 = jongkok.
 * @param walkPhase fase siklus langkah (radian) untuk animasi kaki/tangan.
 * @param currentAction aksi stabil yang sedang dieksekusi.
 */
data class RobotState(
    val positionZ: Float = 0f,
    val rotationY: Float = 0f,
    val scaleY: Float = 1f,
    val walkPhase: Float = 0f,
    val currentAction: RobotAction = RobotAction.IDLE,
)
