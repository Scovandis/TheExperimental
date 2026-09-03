package com.experimental.robot.domain.model

/**
 * Satu titik landmark tangan dalam koordinat ternormalisasi MediaPipe.
 *
 * x: 0.0 (kiri frame) .. 1.0 (kanan frame)
 * y: 0.0 (atas frame) .. 1.0 (bawah frame)
 * z: kedalaman relatif terhadap pergelangan (negatif = lebih dekat kamera)
 */
data class HandPoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
)

/** Indeks 21 landmark tangan sesuai spesifikasi MediaPipe Hand Landmarker. */
object HandLandmarkIndex {
    const val WRIST = 0

    const val THUMB_CMC = 1
    const val THUMB_MCP = 2
    const val THUMB_IP = 3
    const val THUMB_TIP = 4

    const val INDEX_MCP = 5
    const val INDEX_PIP = 6
    const val INDEX_DIP = 7
    const val INDEX_TIP = 8

    const val MIDDLE_MCP = 9
    const val MIDDLE_PIP = 10
    const val MIDDLE_DIP = 11
    const val MIDDLE_TIP = 12

    const val RING_MCP = 13
    const val RING_PIP = 14
    const val RING_DIP = 15
    const val RING_TIP = 16

    const val PINKY_MCP = 17
    const val PINKY_PIP = 18
    const val PINKY_DIP = 19
    const val PINKY_TIP = 20

    const val TOTAL = 21

    /** Pasangan titik untuk menggambar kerangka tangan di overlay. */
    val CONNECTIONS: List<Pair<Int, Int>> = listOf(
        WRIST to THUMB_CMC, THUMB_CMC to THUMB_MCP, THUMB_MCP to THUMB_IP, THUMB_IP to THUMB_TIP,
        WRIST to INDEX_MCP, INDEX_MCP to INDEX_PIP, INDEX_PIP to INDEX_DIP, INDEX_DIP to INDEX_TIP,
        INDEX_MCP to MIDDLE_MCP, MIDDLE_MCP to MIDDLE_PIP, MIDDLE_PIP to MIDDLE_DIP, MIDDLE_DIP to MIDDLE_TIP,
        MIDDLE_MCP to RING_MCP, RING_MCP to RING_PIP, RING_PIP to RING_DIP, RING_DIP to RING_TIP,
        RING_MCP to PINKY_MCP, PINKY_MCP to PINKY_PIP, PINKY_PIP to PINKY_DIP, PINKY_DIP to PINKY_TIP,
        WRIST to PINKY_MCP,
    )
}
