package com.experimental.robot.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.experimental.robot.data.HandLandmarkStream

/**
 * Permukaan kamera spesifik platform.
 *
 * Implementasi Android menyalakan CameraX + MediaPipe Hand Landmarker dan menulis
 * hasilnya ke [stream]; platform lain menampilkan placeholder dan menandai
 * status `Unsupported` sehingga UI otomatis menawarkan kontrol manual.
 */
@Composable
expect fun CameraFeed(
    stream: HandLandmarkStream,
    modifier: Modifier = Modifier,
)
