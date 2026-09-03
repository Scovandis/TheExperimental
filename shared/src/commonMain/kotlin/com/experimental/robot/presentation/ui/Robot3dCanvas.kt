package com.experimental.robot.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.experimental.robot.domain.model.RobotState
import com.experimental.robot.presentation.render.Camera
import com.experimental.robot.presentation.render.RobotMeshBuilder
import com.experimental.robot.presentation.render.SoftwareRenderer

/**
 * Menggambar robot 3D: grid lantai -> bayangan -> badan robot.
 *
 * Seluruh pipeline (proyeksi, culling, sorting, shading) berjalan di
 * [SoftwareRenderer] sehingga tampilannya identik di semua platform.
 *
 * @param cameraDistance jarak kamera orbit saat ini; berubah lewat tombol zoom
 *        di [RobotControlScreen] tanpa mengubah state robot itu sendiri.
 */
@Composable
fun Robot3dCanvas(
    state: RobotState,
    accent: Color,
    cameraDistance: Float = Camera.DEFAULT_DISTANCE,
    modifier: Modifier = Modifier,
) {
    val renderer = remember { SoftwareRenderer() }
    val grid = remember { RobotMeshBuilder.groundGrid() }
    val camera = Camera(distance = cameraDistance)

    Canvas(modifier = modifier) {
        grid.forEach { (from, to) ->
            renderer.renderLine(
                scope = this,
                from = from,
                to = to,
                color = Color.White.copy(alpha = 0.10f),
                camera = camera,
                canvasSize = size,
            )
        }
        renderer.render(this, RobotMeshBuilder.shadow(state), camera, size)
        renderer.render(this, RobotMeshBuilder.build(state, accent), camera, size)
    }
}
