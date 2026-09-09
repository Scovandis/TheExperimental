package com.experimental.robot.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimental.robot.data.TrackerStatus
import com.experimental.robot.di.RobotGraph
import com.experimental.robot.domain.gesture.GestureConfig
import com.experimental.robot.presentation.viewmodel.RobotControlViewModel
import com.experimental.robot.presentation.viewmodel.RobotRenderMode
import com.experimental.robot.presentation.viewmodel.RobotUiState

/**
 * View utama (MVVM): hanya membaca [RobotUiState] dan meneruskan event pengguna ke
 * ViewModel. Tidak ada logika gestur di sini.
 *
 * Tata letak menyesuaikan tinggi jendela: pada layar tinggi (ponsel portrait) panel
 * telemetri & peta gestur diletakkan di bawah panggung, sedangkan pada jendela pendek
 * (desktop / landscape) keduanya menjadi overlay agar panggung robot tetap lega.
 */
@Composable
fun RobotControlScreen(
    modifier: Modifier = Modifier,
    viewModel: RobotControlViewModel = viewModel { RobotGraph.createRobotControlViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val trackerActive = state.trackerStatus == TrackerStatus.Running ||
        state.trackerStatus == TrackerStatus.Initializing

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(RobotColors.background)
            .safeContentPadding(),
    ) {
        val compact = maxHeight < 620.dp

        if (compact) {
            RobotStage(state = state, modifier = Modifier.fillMaxSize())
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                RobotStage(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TelemetryPanel(state = state, modifier = Modifier.weight(1f))
                    GestureLegend(activeAction = state.stableAction, modifier = Modifier.weight(1f))
                }
            }
        }

        ActionBadge(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
        )

        // Kolom kanan atas: jendela kamera + overlay kerangka tangan, lalu tombol zoom di bawahnya.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 116.dp, height = 155.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(RobotColors.surfaceSolid)
                    .border(
                        width = 1.dp,
                        color = RobotColors.forAction(state.stableAction).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            ) {
                CameraFeed(stream = RobotGraph.handLandmarkStream, modifier = Modifier.fillMaxSize())
                HandSkeletonOverlay(
                    landmarks = state.landmarks,
                    accent = RobotColors.forAction(state.stableAction),
                    crouchThresholdY = GestureConfig().crouchWristY,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            // Zoom hanya relevan untuk panggung 3D (2D punya pseudo-depth sendiri).
            if (state.renderMode == RobotRenderMode.THREE_D) {
                ZoomControls(onZoomIn = viewModel::zoomIn, onZoomOut = viewModel::zoomOut)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .width(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RenderModeToggle(mode = state.renderMode, onToggle = viewModel::toggleRenderMode)
            CalibrationLauncher(onStart = viewModel::startCalibration)
            StatusBanner(status = state.trackerStatus)
            SafetyBanner(state = state, onReset = viewModel::resetRobot)
        }

        if (compact) {
            TelemetryPanel(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .width(190.dp),
            )
            GestureLegend(
                activeAction = state.stableAction,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .width(190.dp),
            )
        }

        // Kontrol manual muncul saat pipeline gestur tidak aktif.
        if (!trackerActive) {
            ManualControlPad(
                onPressed = viewModel::onManualActionPressed,
                onReleased = viewModel::onManualActionReleased,
                onReset = viewModel::resetRobot,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(10.dp),
            )
        }

        // Emergency stop selalu terlihat, di posisi yang sama, tanpa pernah dinonaktifkan.
        EmergencyStopButton(
            onEmergencyStop = viewModel::onEmergencyStop,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )

        // Kalibrasi mengambil alih layar: selama berjalan, robot dipaksa berhenti.
        state.calibration?.let { calibration ->
            CalibrationOverlay(
                calibration = calibration,
                onNext = viewModel::advanceCalibration,
                onCancel = viewModel::cancelCalibration,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

/** Panggung robot: memilih renderer 3D atau siluet 2D sesuai mode. */
@Composable
private fun RobotStage(state: RobotUiState, modifier: Modifier = Modifier) {
    when (state.renderMode) {
        RobotRenderMode.THREE_D -> Robot3dCanvas(
            state = state.robot,
            accent = RobotColors.forAction(state.stableAction),
            cameraDistance = state.cameraDistance,
            modifier = modifier,
        )

        RobotRenderMode.TWO_D -> RobotCanvas(
            state = state.robot,
            modifier = modifier,
        )
    }
}
