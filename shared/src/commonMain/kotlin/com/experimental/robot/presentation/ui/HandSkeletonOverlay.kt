package com.experimental.robot.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.experimental.robot.domain.model.HandLandmarkIndex
import com.experimental.robot.domain.model.HandPoint

/**
 * Overlay kerangka tangan di atas preview kamera.
 *
 * Landmark ternormalisasi (0..1) dipetakan langsung ke ukuran canvas, dan garis
 * horizontal ambang JONGKOK digambar sebagai bantuan kalibrasi.
 */
@Composable
fun HandSkeletonOverlay(
    landmarks: List<HandPoint>,
    accent: Color,
    crouchThresholdY: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        // Ambang JONGKOK.
        val thresholdY = size.height * crouchThresholdY
        drawLine(
            color = Color(0xFFF87171).copy(alpha = 0.5f),
            start = Offset(0f, thresholdY),
            end = Offset(size.width, thresholdY),
            strokeWidth = 2f,
        )

        if (landmarks.size < HandLandmarkIndex.TOTAL) return@Canvas

        fun point(index: Int) = Offset(
            x = landmarks[index].x * size.width,
            y = landmarks[index].y * size.height,
        )

        HandLandmarkIndex.CONNECTIONS.forEach { (from, to) ->
            drawLine(
                color = accent.copy(alpha = 0.85f),
                start = point(from),
                end = point(to),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
        }

        landmarks.indices.forEach { i ->
            val isTip = i in TIP_INDICES
            drawCircle(
                color = if (isTip) accent else Color.White.copy(alpha = 0.9f),
                radius = if (isTip) 7f else 4.5f,
                center = point(i),
            )
        }

        // Tandai pergelangan sebagai titik acuan perhitungan.
        drawCircle(color = Color(0xFFFBBF24), radius = 9f, center = point(HandLandmarkIndex.WRIST))
    }
}

private val TIP_INDICES = setOf(
    HandLandmarkIndex.THUMB_TIP,
    HandLandmarkIndex.INDEX_TIP,
    HandLandmarkIndex.MIDDLE_TIP,
    HandLandmarkIndex.RING_TIP,
    HandLandmarkIndex.PINKY_TIP,
)
