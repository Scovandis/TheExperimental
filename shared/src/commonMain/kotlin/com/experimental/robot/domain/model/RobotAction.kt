package com.experimental.robot.domain.model

/**
 * Aksi robot hasil klasifikasi gestur tangan.
 *
 * [label] dipakai untuk HUD, [gestureHint] untuk panel panduan gestur.
 */
enum class RobotAction(val label: String, val gestureHint: String) {
    IDLE(label = "IDLE", gestureHint = "Kepalan tangan (fist)"),
    MOVE_FORWARD(label = "MAJU", gestureHint = "Telapak terbuka, tangan di area atas"),
    MOVE_BACKWARD(label = "MUNDUR", gestureHint = "Hanya telunjuk, ujung menunjuk ke bawah"),
    ROTATE_LEFT(label = "PUTAR KIRI", gestureHint = "V-sign condong ke kiri"),
    ROTATE_RIGHT(label = "PUTAR KANAN", gestureHint = "V-sign condong ke kanan"),
    CROUCH(label = "JONGKOK", gestureHint = "Telapak terbuka didorong ke bawah frame");

    val isMoving: Boolean get() = this == MOVE_FORWARD || this == MOVE_BACKWARD
    val isRotating: Boolean get() = this == ROTATE_LEFT || this == ROTATE_RIGHT
}
