package com.experimental.robot.domain.model

/**
 * Aksi robot hasil klasifikasi gestur tangan.
 *
 * [label] dipakai untuk HUD, [gestureHint] untuk panel panduan gestur.
 */
enum class RobotAction(val label: String, val gestureHint: String) {
    IDLE(label = "IDLE", gestureHint = "Kepalan tangan (0 jari)"),
    MOVE_FORWARD(label = "MAJU", gestureHint = "1 jari (telunjuk)"),
    MOVE_BACKWARD(label = "MUNDUR", gestureHint = "2 jari (telunjuk + tengah)"),
    ROTATE_LEFT(label = "PUTAR KIRI", gestureHint = "3 jari (telunjuk + tengah + manis)"),
    ROTATE_RIGHT(label = "PUTAR KANAN", gestureHint = "4 jari, tanpa jempol"),
    CROUCH(label = "JONGKOK", gestureHint = "5 jari, telapak terbuka penuh");

    val isMoving: Boolean get() = this == MOVE_FORWARD || this == MOVE_BACKWARD
    val isRotating: Boolean get() = this == ROTATE_LEFT || this == ROTATE_RIGHT
}
