package com.experimental.robot.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.experimental.robot.di.RobotGraph
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.presentation.viewmodel.RobotControlViewModel
import com.experimental.robot.presentation.viewmodel.RobotRenderMode
import com.experimental.robot.presentation.viewmodel.RobotUiState

/**
 * Layar Utama Gesture Robot Controller: Desain Sci-Fi Cyber Dashboard.
 *
 * Mengadopsi arsitektur 3-Kolom yang identik dengan desain referensi:
 * - Header: Logo, Status Pill (System Ready, Connected, Sim Mode), Actions
 * - Kolom Kiri: Kamera Preview Card, Telemetry Card, Tips Card
 * - Kolom Tengah: Panggung 3D Robot XR-07 dengan Platform Hologram & Action/Status Deck
 * - Kolom Kanan: Peta Gestur 6 Kartu, Gesture Info Card, Mode & View Switcher
 * - Bottom Dock: Navigation Bar (Home, Calibration, Record, Replay, Settings, Robot Status)
 */
@Preview(device = "spec:width=1280dp,height=800dp,dpi=240")
@Composable
fun RobotControlScreen(
    modifier: Modifier = Modifier,
    viewModel: RobotControlViewModel = viewModel { RobotGraph.createRobotControlViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf("Home") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(RobotColors.background)
            .safeContentPadding(),
    ) {
        val isWideScreen = maxWidth >= 860.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Top Dashboard Header Bar
            // TODO(AUDIT_INCOMPLETE.md #6): ikon Settings & Menu belum ada aksi nyata di baliknya.
            TopDashboardBar(
                state = state,
                onSettingsClick = { /* Settings dialog/action */ },
                onMenuClick = { /* Drawer/Menu action */ },
            )

            // 2. Area Konten Utama
            if (isWideScreen) {
                // Tata Letak Landscape / Tablet Widescreen (3 Kolom Sesuai Desain Target)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // KOLOM KIRI: Kamera Preview + Telemetri + Tips
                    Column(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        KameraPreviewCard(
                            state = state,
                            stream = RobotGraph.handLandmarkStream,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TelemetryCard(
                            state = state,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        TipsCard(modifier = Modifier.fillMaxWidth())
                    }

                    // KOLOM TENGAH: Panggung Robot 3D + Action & Status Deck
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Panggung Robot 3D / 2D
                        RobotStage(
                            state = state,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF070D19))
                                .border(1.dp, Color(0xFF162A48), RoundedCornerShape(16.dp)),
                        )

                        // Deck Kontrol: Aksi Saat Ini + Status Kecepatan + Tombol Emergency Stop
                        CenterActionDeck(
                            state = state,
                            onEmergencyStop = viewModel::onEmergencyStop,
                            onReset = viewModel::resetRobot,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // KOLOM KANAN: Peta Gestur + Gesture & Hand Info + Mode & View
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // TODO(AUDIT_INCOMPLETE.md #4): hanya onManualActionPressed yang dipanggil;
                        // viewModel.onManualActionReleased() tidak pernah terpanggil dari sini,
                        // jadi satu tap mengunci override manual sampai Emergency Stop/Reset.
                        PetaGesturGrid(
                            activeAction = state.stableAction,
                            onGestureClick = { action ->
                                viewModel.onManualActionPressed(action)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        GestureAndHandInfoCard(
                            state = state,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        // TODO(AUDIT_INCOMPLETE.md #3): onSelectSimulation/onSelectRealRobot tidak
                        // diisi di sini — pill "Simulation/Real Robot" jadi murni kosmetik.
                        ModeAndViewCard(
                            state = state,
                            onToggleRenderMode = viewModel::toggleRenderMode,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                // Tata Letak Responsif untuk Layar Portrait / Sempit (Vertically Scrollable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RobotStage(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF070D19))
                            .border(1.dp, Color(0xFF162A48), RoundedCornerShape(16.dp)),
                    )

                    CenterActionDeck(
                        state = state,
                        onEmergencyStop = viewModel::onEmergencyStop,
                        onReset = viewModel::resetRobot,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    KameraPreviewCard(
                        state = state,
                        stream = RobotGraph.handLandmarkStream,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // TODO(AUDIT_INCOMPLETE.md #4): sama seperti versi wide-screen di atas — tap
                    // sekali mengunci override manual karena onManualActionReleased() tak dipanggil.
                    PetaGesturGrid(
                        activeAction = state.stableAction,
                        onGestureClick = { action -> viewModel.onManualActionPressed(action) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    GestureAndHandInfoCard(
                        state = state,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    TelemetryCard(
                        state = state,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // TODO(AUDIT_INCOMPLETE.md #3): sama seperti versi wide-screen — pill
                    // "Simulation/Real Robot" tidak tersambung ke ViewModel, murni kosmetik.
                    ModeAndViewCard(
                        state = state,
                        onToggleRenderMode = viewModel::toggleRenderMode,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    TipsCard(modifier = Modifier.fillMaxWidth())
                }
            }

            // 3. Bottom Navigation Dock
            // TODO(AUDIT_INCOMPLETE.md #7): hanya tab "Calibration" yang memicu aksi nyata di
            // sini — Home/Record/Replay/Settings cuma mengganti label currentTab.
            BottomNavigationDock(
                currentTab = currentTab,
                onTabSelect = { tab ->
                    currentTab = tab
                    if (tab == "Calibration") {
                        viewModel.startCalibration()
                    }
                },
            )
        }

        // 4. Alert Banner saat Kunci Darurat / Safety Lock Aktif
        if (state.latched) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xEE7F1D1D))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        Text(
                            text = "${state.halt.name} AKTIF",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = state.stopReason.label,
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.sp,
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { viewModel.resetRobot() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "RESET ROBOT",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // 5. Modal Overlay Kalibrasi saat Wizard Berjalan
        state.calibration?.let { calibration ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.70f)),
                contentAlignment = Alignment.Center,
            ) {
                CalibrationOverlay(
                    calibration = calibration,
                    onNext = viewModel::advanceCalibration,
                    onCancel = viewModel::cancelCalibration,
                )
            }
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
