package com.experimental.robot.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.experimental.robot.data.HandLandmarkStream
import com.experimental.robot.domain.model.HandLandmarkIndex
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.presentation.viewmodel.RobotRenderMode
import com.experimental.robot.presentation.viewmodel.RobotUiState

// ── TOP DASHBOARD BAR ────────────────────────────────────────────────────────

@Composable
fun TopDashboardBar(
    state: RobotUiState,
    onSettingsClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Logo & Judul Kiri
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1438))
                    .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                RobotLogoIcon(modifier = Modifier.size(24.dp), color = Color(0xFFA78BFA))
            }

            Column {
                Text(
                    text = "Gesture Robot",
                    color = RobotColors.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                )
                Text(
                    text = "AI Gesture Control",
                    color = RobotColors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Status Pill Tengah
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xEE0D172A))
                .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Status 1: System Ready
                val ready = !state.halt.blocksMovement
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (ready) Color(0xFF10B981) else Color(0xFFEF4444)),
                    )
                    Text(
                        text = if (ready) "System Ready" else state.halt.name,
                        color = if (ready) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Text(text = "|", color = Color(0xFF334155), fontSize = 12.sp)

                // Status 2: Connection
                Text(
                    text = if (state.handDetected) "Connected" else "Waiting Hand",
                    color = if (state.handDetected) RobotColors.textPrimary else RobotColors.textSecondary,
                    fontSize = 12.sp,
                )

                Text(text = "|", color = Color(0xFF334155), fontSize = 12.sp)

                // Status 3: Mode
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    ScreenMonitorIcon(modifier = Modifier.size(13.dp), color = RobotColors.textSecondary)
                    Text(
                        text = "Simulation Mode",
                        color = RobotColors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Tombol Aksi Kanan: Settings & Hamburger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1E36))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
                    .clickable { onSettingsClick() },
                contentAlignment = Alignment.Center,
            ) {
                GearIcon(modifier = Modifier.size(18.dp), color = RobotColors.textSecondary)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F1E36))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp))
                    .clickable { onMenuClick() },
                contentAlignment = Alignment.Center,
            ) {
                HamburgerIcon(modifier = Modifier.size(18.dp), color = RobotColors.textSecondary)
            }
        }
    }
}

// ── LEFT COLUMN: KAMERA PREVIEW CARD ─────────────────────────────────────────

@Composable
fun KameraPreviewCard(
    state: RobotUiState,
    stream: HandLandmarkStream,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Card Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981)),
            )
            Text(
                text = "Kamera Preview",
                color = RobotColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Video Preview Box
            Box(
                modifier = Modifier
                    .weight(1.15f)
                    .height(115.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070D19))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(10.dp)),
            ) {
                CameraFeed(stream = stream, modifier = Modifier.fillMaxSize())
                HandSkeletonOverlay(
                    landmarks = state.landmarks,
                    accent = RobotColors.forAction(state.stableAction),
                    crouchThresholdY = 0.65f,
                    modifier = Modifier.fillMaxSize(),
                )

                // Badge FPS Kamera
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xCC070D19))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "FPS ${state.frameRates.camera.coerceAtLeast(state.frameRates.detection)}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Hand Tracking Status Box
            Column(
                modifier = Modifier.weight(0.85f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Pill Tangan Terdeteksi
                val detected = state.handDetected
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (detected) Color(0x2010B981) else Color(0x2064748B))
                        .border(1.dp, if (detected) Color(0x6010B981) else Color(0x4064748B), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (detected) Color(0xFF10B981) else Color(0xFF64748B)),
                        )
                        Text(
                            text = if (detected) "Tangan Terdeteksi" else "Tidak Ada",
                            color = if (detected) Color(0xFF10B981) else Color(0xFF94A3B8),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                // Ikon Tangan Vektor
                HandSilhouetteIcon(
                    modifier = Modifier.size(32.dp),
                    color = if (detected) Color(0xFF00F5A0) else Color(0xFF475569),
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Jari Terbuka",
                        color = RobotColors.textSecondary,
                        fontSize = 9.sp,
                    )
                    Text(
                        text = if (detected) "${state.fingers.extendedCount}/5" else "0/5",
                        color = RobotColors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Confidence Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = "Confidence", color = RobotColors.textSecondary, fontSize = 9.sp)
                        Text(
                            text = "${(state.confidence * 100).toInt()}%",
                            color = RobotColors.accentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    MiniProgressBar(
                        progress = state.confidence,
                        barColor = Color(0xFF00E5FF),
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                }
            }
        }
    }
}

// ── LEFT COLUMN: TELEMETRY CARD ──────────────────────────────────────────────

@Composable
fun TelemetryCard(
    state: RobotUiState,
    onDetailClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        // Card Header + Detail button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TelemetryHeaderIcon(modifier = Modifier.size(15.dp), color = Color(0xFF38BDF8))
                Text(
                    text = "Telemetry",
                    color = RobotColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1E36))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                    .clickable { onDetailClick() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SlidersIcon(modifier = Modifier.size(11.dp), color = RobotColors.textSecondary)
                    Text(
                        text = "Detail",
                        color = RobotColors.textSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        val wrist = state.landmarks.getOrNull(HandLandmarkIndex.WRIST)
        val posX = wrist?.let { (it.x * 100).toInt() / 100f } ?: 0.50f
        val posY = wrist?.let { (it.y * 100).toInt() / 100f } ?: 0.50f
        val posZ = (state.robot.positionZ / 55f * 100).toInt() / 100f
        val rotY = state.robot.rotationY.toInt()
        val scaleY = (state.robot.scaleY * 100).toInt() / 100f

        TelemetryItemRow(
            iconColor = Color(0xFF0284C7),
            label = "Camera FPS",
            value = "${state.frameRates.camera.coerceAtLeast(state.frameRates.detection)} fps",
            iconType = TelemetryIconType.CAMERA,
        )
        TelemetryItemRow(
            iconColor = Color(0xFF2563EB),
            label = "Detection FPS",
            value = "${state.frameRates.detection} fps",
            iconType = TelemetryIconType.DETECTION,
        )
        TelemetryItemRow(
            iconColor = Color(0xFF8B5CF6),
            label = "Render FPS",
            value = "${state.frameRates.render.coerceAtLeast(60)} fps",
            iconType = TelemetryIconType.RENDER,
        )
        TelemetryItemRow(
            iconColor = Color(0xFF0D9488),
            label = "Hand Position (X,Y)",
            value = "$posX , $posY",
            iconType = TelemetryIconType.POSITION,
        )
        TelemetryItemRow(
            iconColor = Color(0xFFD97706),
            label = "Rotation (Y)",
            value = "$rotY°",
            iconType = TelemetryIconType.ROTATION,
        )
        TelemetryItemRow(
            iconColor = Color(0xFF3B82F6),
            label = "Scale (Y)",
            value = "$scaleY",
            iconType = TelemetryIconType.SCALE,
        )
        TelemetryItemRow(
            iconColor = Color(0xFF06B6D4),
            label = "Position Z",
            value = "$posZ m",
            iconType = TelemetryIconType.DISTANCE,
        )
        TelemetryItemRow(
            iconColor = Color(0xFFEA580C),
            label = "Jumlah Jari",
            value = "${state.fingers.extendedCount}",
            iconType = TelemetryIconType.FINGERS,
        )
        TelemetryItemRow(
            iconColor = Color(0xFF059669),
            label = "Source",
            value = "MEDIAPIPE",
            iconType = TelemetryIconType.SOURCE,
        )
    }
}

enum class TelemetryIconType { CAMERA, DETECTION, RENDER, POSITION, ROTATION, SCALE, DISTANCE, FINGERS, SOURCE }

@Composable
private fun TelemetryItemRow(
    iconColor: Color,
    label: String,
    value: String,
    iconType: TelemetryIconType,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(11.dp)) {
                drawCircle(color = iconColor, radius = size.minDimension / 2.6f)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            color = RobotColors.textSecondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            color = RobotColors.textPrimary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ── LEFT COLUMN: TIPS CARD ───────────────────────────────────────────────────

@Composable
fun TipsCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1E36))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LightbulbIcon(modifier = Modifier.size(20.dp), color = Color(0xFFFBBF24))
        Text(
            text = "Tips: Gunakan pencahayaan yang cukup agar tracking tangan lebih stabil.",
            color = Color(0xFFCBD5E1),
            fontSize = 10.sp,
            lineHeight = 14.sp,
        )
    }
}

// ── CENTER COLUMN: ACTION & STATUS DECK ──────────────────────────────────────

@Composable
fun CenterActionDeck(
    state: RobotUiState,
    onEmergencyStop: () -> Unit,
    onReset: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xEE0D172A))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sub-Card 1: AKSI SAAT INI
        Column(
            modifier = Modifier
                .weight(1.1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B1426))
                .border(1.dp, Color(0xFF1A3258), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "AKSI SAAT INI",
                color = RobotColors.textSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val actionColor = if (state.halt.blocksMovement) RobotColors.danger else RobotColors.forAction(state.stableAction)

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(actionColor.copy(alpha = 0.18f))
                        .border(1.dp, actionColor.copy(alpha = 0.60f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    ActionDirectionIcon(action = state.stableAction, color = actionColor, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(
                        text = if (state.halt.blocksMovement) state.halt.name else state.stableAction.label,
                        color = actionColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Speed Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiniProgressBar(
                    progress = if (state.halt.blocksMovement) 0f else state.command.speed,
                    barColor = if (state.halt.blocksMovement) RobotColors.danger else Color(0xFF00F5A0),
                    modifier = Modifier.weight(1f).height(6.dp),
                )
                Text(
                    text = "${((if (state.halt.blocksMovement) 0f else state.command.speed) * 100).toInt()}%",
                    color = RobotColors.textPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Sub-Card 2: STATUS
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B1426))
                .border(1.dp, Color(0xFF1A3258), RoundedCornerShape(12.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "STATUS",
                color = RobotColors.textSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )

            // Status Badge
            val active = state.command.isMoving || state.command.isRotating
            val statusLabel = when {
                state.latched -> state.halt.name
                state.halt == HaltMode.STOP -> state.stopReason.label
                active -> "Command Active"
                else -> "Standby / Idle"
            }
            val statusColor = when {
                state.halt.blocksMovement -> RobotColors.danger
                active -> Color(0xFF10B981)
                else -> Color(0xFF38BDF8)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            // Speedometer Arc
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpeedometerIcon(modifier = Modifier.size(24.dp), color = Color(0xFF00E5FF))
                Column {
                    Text(
                        text = "Kecepatan",
                        color = RobotColors.textSecondary,
                        fontSize = 9.sp,
                    )
                    Text(
                        text = "${(state.command.speed * 100).toInt()}%",
                        color = RobotColors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        // Sub-Card 3: EMERGENCY STOP BUTTON
        Box(
            modifier = Modifier
                .weight(1.05f)
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFDC2626), Color(0xFF991B1B)),
                    ),
                )
                .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                .clickable { onEmergencyStop() }
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WarningTriangleIcon(modifier = Modifier.size(24.dp), color = Color.White)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "EMERGENCY STOP",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                )
                Text(
                    text = if (state.latched) "Sentuh RESET" else "Tekan / Tahan",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                )
            }
        }
    }
}

// ── RIGHT COLUMN: PETA GESTUR GRID (6 KARTU) ─────────────────────────────────

@Composable
fun PetaGesturGrid(
    activeAction: RobotAction,
    onGestureClick: (RobotAction) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                HandWaveHeaderIcon(modifier = Modifier.size(15.dp), color = Color(0xFF38BDF8))
                Text(
                    text = "PETA GESTUR",
                    color = RobotColors.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1E36))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp))
                    .clickable { }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    PencilIcon(modifier = Modifier.size(11.dp), color = RobotColors.textSecondary)
                    Text(
                        text = "Edit",
                        color = RobotColors.textSecondary,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        // Baris 1: 3 Kartu (IDLE, MAJU, MUNDUR)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GestureCardItem(
                number = "1",
                action = RobotAction.IDLE,
                label = "IDLE",
                subtitle = "(Kepalan tangan)",
                isActive = activeAction == RobotAction.IDLE,
                onClick = { onGestureClick(RobotAction.IDLE) },
                modifier = Modifier.weight(1f),
            )
            GestureCardItem(
                number = "2",
                action = RobotAction.MOVE_FORWARD,
                label = "MAJU",
                subtitle = "(Telapak terbuka)",
                isActive = activeAction == RobotAction.MOVE_FORWARD,
                onClick = { onGestureClick(RobotAction.MOVE_FORWARD) },
                modifier = Modifier.weight(1f),
            )
            GestureCardItem(
                number = "3",
                action = RobotAction.MOVE_BACKWARD,
                label = "MUNDUR",
                subtitle = "(Telunjuk ke bawah)",
                isActive = activeAction == RobotAction.MOVE_BACKWARD,
                onClick = { onGestureClick(RobotAction.MOVE_BACKWARD) },
                modifier = Modifier.weight(1f),
            )
        }

        // Baris 2: 3 Kartu (PUTAR KIRI, PUTAR KANAN, JONGKOK)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GestureCardItem(
                number = "4",
                action = RobotAction.ROTATE_LEFT,
                label = "PUTAR KIRI",
                subtitle = "(V-sign kiri)",
                isActive = activeAction == RobotAction.ROTATE_LEFT,
                onClick = { onGestureClick(RobotAction.ROTATE_LEFT) },
                modifier = Modifier.weight(1f),
            )
            GestureCardItem(
                number = "5",
                action = RobotAction.ROTATE_RIGHT,
                label = "PUTAR KANAN",
                subtitle = "(V-sign kanan)",
                isActive = activeAction == RobotAction.ROTATE_RIGHT,
                onClick = { onGestureClick(RobotAction.ROTATE_RIGHT) },
                modifier = Modifier.weight(1f),
            )
            GestureCardItem(
                number = "6",
                action = RobotAction.CROUCH,
                label = "JONGKOK",
                subtitle = "(Telapak ke bawah)",
                isActive = activeAction == RobotAction.CROUCH,
                onClick = { onGestureClick(RobotAction.CROUCH) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GestureCardItem(
    number: String,
    action: RobotAction,
    label: String,
    subtitle: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) Color(0xFF00F5A0) else Color(0xFF1E3A5F)
    val bgColor = if (isActive) Color(0xFF0A2624) else Color(0xFF0B1426)

    Column(
        modifier = modifier
            .height(118.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top row: Number badge
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isActive) Color(0xFF10B981) else Color(0xFF1E293B)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // Gesture Illustration Vector
        HandGestureSilhouette(
            action = action,
            color = if (isActive) Color(0xFF00F5A0) else Color(0xFFFDBA74),
            modifier = Modifier.size(36.dp),
        )

        // Labels
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (isActive) Color(0xFF00F5A0) else RobotColors.textPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                color = RobotColors.textSecondary,
                fontSize = 7.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

// ── RIGHT COLUMN: GESTURE & HAND INFO CARD ───────────────────────────────────

@Composable
fun GestureAndHandInfoCard(
    state: RobotUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            EyeHeaderIcon(modifier = Modifier.size(15.dp), color = Color(0xFF38BDF8))
            Text(
                text = "Gesture & Hand Info",
                color = RobotColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Left: Green Glowing Hand Outline Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0B182B))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                HandSilhouetteIcon(modifier = Modifier.size(32.dp), color = Color(0xFF00F5A0))
            }

            // Middle: Gesture name & Bars
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(text = "Gesture", color = RobotColors.textSecondary, fontSize = 10.sp)
                    Text(
                        text = state.stableAction.label,
                        color = Color(0xFF00F5A0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                // Confidence
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Confidence", color = RobotColors.textSecondary, fontSize = 9.sp)
                    Text(
                        text = "${(state.confidence * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                MiniProgressBar(
                    progress = state.confidence,
                    barColor = Color(0xFF00E5FF),
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )

                // Stability
                val stabilitySec = (state.stabilityMs / 1000f * 100).toInt() / 100f
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "Stability", color = RobotColors.textSecondary, fontSize = 9.sp)
                    Text(
                        text = "$stabilitySec s",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                MiniProgressBar(
                    progress = (state.stabilityMs / 1000f).coerceIn(0f, 1f),
                    barColor = Color(0xFF10B981),
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }

            // Right: LOCKED Green Badge
            val locked = state.locked
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (locked) Color(0xFF064E3B) else Color(0xFF1E293B))
                    .border(1.dp, if (locked) Color(0xFF10B981) else Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Text(
                    text = if (locked) "LOCKED" else state.confidenceTier.label,
                    color = if (locked) Color(0xFF10B981) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

// ── RIGHT COLUMN: MODE & VIEW CARD ───────────────────────────────────────────

@Composable
fun ModeAndViewCard(
    state: RobotUiState,
    onToggleRenderMode: () -> Unit,
    onSelectSimulation: () -> Unit = {},
    onSelectRealRobot: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.cardBorder, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ModeHeaderIcon(modifier = Modifier.size(15.dp), color = Color(0xFF38BDF8))
            Text(
                text = "Mode & View",
                color = RobotColors.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        // Row 1: Simulation vs Real Robot
        var isSimMode by remember { mutableStateOf(true) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeTogglePill(
                icon = { GamepadIcon(modifier = Modifier.size(14.dp), color = if (isSimMode) Color.White else RobotColors.textSecondary) },
                title = "Simulation",
                isSelected = isSimMode,
                onClick = {
                    isSimMode = true
                    onSelectSimulation()
                },
                modifier = Modifier.weight(1f),
            )
            ModeTogglePill(
                icon = { RobotLogoIcon(modifier = Modifier.size(14.dp), color = if (!isSimMode) Color.White else RobotColors.textSecondary) },
                title = "Real Robot",
                isSelected = !isSimMode,
                onClick = {
                    isSimMode = false
                    onSelectRealRobot()
                },
                modifier = Modifier.weight(1f),
            )
        }

        // Row 2: 2D View vs 3D View
        val is3D = state.renderMode == RobotRenderMode.THREE_D
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeTogglePill(
                icon = { Grid2dIcon(modifier = Modifier.size(14.dp), color = if (!is3D) Color.White else RobotColors.textSecondary) },
                title = "2D View",
                isSelected = !is3D,
                onClick = { if (is3D) onToggleRenderMode() },
                modifier = Modifier.weight(1f),
            )
            ModeTogglePill(
                icon = { Cube3dIcon(modifier = Modifier.size(14.dp), color = if (is3D) Color.White else RobotColors.textSecondary) },
                title = "3D View",
                isSelected = is3D,
                onClick = { if (!is3D) onToggleRenderMode() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeTogglePill(
    icon: @Composable () -> Unit,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (isSelected) Color(0xFF1D4ED8) else Color(0xFF0F1E36)
    val border = if (isSelected) Color(0xFF3B82F6) else Color(0xFF1E3A5F)

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            icon()
            Text(
                text = title,
                color = if (isSelected) Color.White else RobotColors.textSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}

// ── BOTTOM NAVIGATION DOCK ───────────────────────────────────────────────────

@Composable
fun BottomNavigationDock(
    currentTab: String = "Home",
    onTabSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(Color(0xFF070D19))
            .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Tab Navigasi Kiri & Tengah
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Home (Active)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1D4ED8))
                    .border(1.dp, Color(0xFF3B82F6), RoundedCornerShape(20.dp))
                    .clickable { onTabSelect("Home") }
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HomeNavIcon(modifier = Modifier.size(15.dp), color = Color.White)
                    Text(text = "Home", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Calibration
            Row(
                modifier = Modifier
                    .clickable { onTabSelect("Calibration") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CrosshairNavIcon(modifier = Modifier.size(15.dp), color = RobotColors.textSecondary)
                Text(text = "Calibration", color = RobotColors.textSecondary, fontSize = 12.sp)
            }

            // Record
            Row(
                modifier = Modifier
                    .clickable { onTabSelect("Record") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                )
                Text(text = "Record", color = RobotColors.textSecondary, fontSize = 12.sp)
            }

            // Replay
            Row(
                modifier = Modifier
                    .clickable { onTabSelect("Replay") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PlayNavIcon(modifier = Modifier.size(15.dp), color = RobotColors.textSecondary)
                Text(text = "Replay", color = RobotColors.textSecondary, fontSize = 12.sp)
            }

            // Settings
            Row(
                modifier = Modifier
                    .clickable { onTabSelect("Settings") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                GearIcon(modifier = Modifier.size(15.dp), color = RobotColors.textSecondary)
                Text(text = "Settings", color = RobotColors.textSecondary, fontSize = 12.sp)
            }
        }

        // Status Robot Kanan
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F1E36))
                    .border(1.dp, Color(0xFF1E3A5F), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                RobotLogoIcon(modifier = Modifier.size(18.dp), color = RobotColors.textSecondary)
            }
            Column {
                Text(text = "Robot", color = RobotColors.textSecondary, fontSize = 10.sp)
                Text(text = "Not Connected", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── REUSABLE UI HELPERS ──────────────────────────────────────────────────────

@Composable
fun MiniProgressBar(
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF1E293B)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .background(barColor),
        )
    }
}

// ── STANDALONE CANVAS VECTOR ICONS ───────────────────────────────────────────

@Composable
fun RobotLogoIcon(modifier: Modifier = Modifier, color: Color = Color(0xFFFA541C)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Ear pods on sides
        drawRoundRect(
            color = Color(0xFF383E4B),
            topLeft = Offset(w * 0.04f, h * 0.36f),
            size = Size(w * 0.12f, h * 0.28f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )
        drawRoundRect(
            color = Color(0xFF383E4B),
            topLeft = Offset(w * 0.84f, h * 0.36f),
            size = Size(w * 0.12f, h * 0.28f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
        )

        // Rounded Box Head (Orange)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.22f),
            size = Size(w * 0.76f, h * 0.56f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f, h * 0.12f),
        )

        // Left Eye (Cyan with dark socket)
        drawRoundRect(
            color = Color(0xFF141820),
            topLeft = Offset(w * 0.24f, h * 0.34f),
            size = Size(w * 0.22f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
        )
        drawRoundRect(
            color = Color(0xFF67E8F9),
            topLeft = Offset(w * 0.26f, h * 0.36f),
            size = Size(w * 0.18f, h * 0.28f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f),
        )

        // Right Eye (Cyan with dark socket)
        drawRoundRect(
            color = Color(0xFF141820),
            topLeft = Offset(w * 0.54f, h * 0.34f),
            size = Size(w * 0.22f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
        )
        drawRoundRect(
            color = Color(0xFF67E8F9),
            topLeft = Offset(w * 0.56f, h * 0.36f),
            size = Size(w * 0.18f, h * 0.28f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f),
        )
    }
}

@Composable
fun ScreenMonitorIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.1f, h * 0.15f),
            size = Size(w * 0.8f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            style = Stroke(width = 1.5f),
        )
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.70f), end = Offset(w * 0.5f, h * 0.85f), strokeWidth = 1.5f)
        drawLine(color = color, start = Offset(w * 0.3f, h * 0.85f), end = Offset(w * 0.7f, h * 0.85f), strokeWidth = 1.5f)
    }
}

@Composable
fun GearIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val r = w * 0.32f
        drawCircle(color = color, radius = r, center = c, style = Stroke(width = 2f))
        drawCircle(color = color, radius = r * 0.4f, center = c)
    }
}

@Composable
fun HamburgerIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val x1 = w * 0.2f
        val x2 = w * 0.8f
        drawLine(color = color, start = Offset(x1, h * 0.30f), end = Offset(x2, h * 0.30f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(x1, h * 0.50f), end = Offset(x2, h * 0.50f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(x1, h * 0.70f), end = Offset(x2, h * 0.70f), strokeWidth = 2f, cap = StrokeCap.Round)
    }
}

@Composable
fun SlidersIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.3f, h * 0.15f), end = Offset(w * 0.3f, h * 0.85f), strokeWidth = 1.5f)
        drawLine(color = color, start = Offset(w * 0.7f, h * 0.15f), end = Offset(w * 0.7f, h * 0.85f), strokeWidth = 1.5f)
        drawCircle(color = color, radius = 2f, center = Offset(w * 0.3f, h * 0.40f))
        drawCircle(color = color, radius = 2f, center = Offset(w * 0.7f, h * 0.65f))
    }
}

@Composable
fun PencilIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.8f, h * 0.2f), end = Offset(w * 0.25f, h * 0.75f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.25f, h * 0.75f), end = Offset(w * 0.20f, h * 0.80f), strokeWidth = 2f)
    }
}

@Composable
fun LightbulbIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h * 0.42f)
        drawCircle(color = color, radius = w * 0.30f, center = c, style = Stroke(width = 1.8f))
        drawLine(color = color, start = Offset(w * 0.38f, h * 0.74f), end = Offset(w * 0.62f, h * 0.74f), strokeWidth = 1.8f)
        drawLine(color = color, start = Offset(w * 0.42f, h * 0.84f), end = Offset(w * 0.58f, h * 0.84f), strokeWidth = 1.8f)
    }
}

@Composable
fun SpeedometerIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h * 0.60f)
        drawArc(
            color = color,
            startAngle = 160f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = Offset(w * 0.15f, h * 0.15f),
            size = Size(w * 0.7f, h * 0.7f),
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
        drawLine(
            color = color,
            start = c,
            end = Offset(w * 0.72f, h * 0.32f),
            strokeWidth = 2f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun WarningTriangleIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.85f)
            lineTo(w * 0.12f, h * 0.85f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 2f, join = StrokeJoin.Round))
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.5f, h * 0.60f), strokeWidth = 2f, cap = StrokeCap.Round)
        drawCircle(color = color, radius = 1.5f, center = Offset(w * 0.5f, h * 0.72f))
    }
}

@Composable
fun TelemetryHeaderIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawLine(color = color, start = Offset(w * 0.2f, h * 0.85f), end = Offset(w * 0.2f, h * 0.55f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.85f), end = Offset(w * 0.5f, h * 0.25f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.8f, h * 0.85f), end = Offset(w * 0.8f, h * 0.40f), strokeWidth = 2.5f, cap = StrokeCap.Round)
    }
}

@Composable
fun HandWaveHeaderIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    HandSilhouetteIcon(modifier = modifier, color = color)
}

@Composable
fun EyeHeaderIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        val path = Path().apply {
            moveTo(w * 0.1f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.15f, w * 0.9f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.85f, w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.8f))
        drawCircle(color = color, radius = w * 0.18f, center = c)
    }
}

@Composable
fun ModeHeaderIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.15f, h * 0.2f),
            size = Size(w * 0.7f, h * 0.6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f),
            style = Stroke(width = 1.5f),
        )
        drawLine(color = color, start = Offset(w * 0.15f, h * 0.5f), end = Offset(w * 0.85f, h * 0.5f), strokeWidth = 1.5f)
    }
}

@Composable
fun GamepadIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(w * 0.76f, h * 0.44f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f, h * 0.15f),
            style = Stroke(width = 1.5f),
        )
        // D-pad cross
        drawLine(color = color, start = Offset(w * 0.28f, h * 0.5f), end = Offset(w * 0.40f, h * 0.5f), strokeWidth = 1.5f)
        drawLine(color = color, start = Offset(w * 0.34f, h * 0.42f), end = Offset(w * 0.34f, h * 0.58f), strokeWidth = 1.5f)
        // Buttons
        drawCircle(color = color, radius = 1.5f, center = Offset(w * 0.65f, h * 0.45f))
        drawCircle(color = color, radius = 1.5f, center = Offset(w * 0.72f, h * 0.55f))
    }
}

@Composable
fun Grid2dIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(color = color, topLeft = Offset(w * 0.15f, h * 0.15f), size = Size(w * 0.7f, h * 0.7f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f), style = Stroke(width = 1.5f))
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.15f), end = Offset(w * 0.5f, h * 0.85f), strokeWidth = 1.5f)
        drawLine(color = color, start = Offset(w * 0.15f, h * 0.5f), end = Offset(w * 0.85f, h * 0.5f), strokeWidth = 1.5f)
    }
}

@Composable
fun Cube3dIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        // Iso cube outline
        val p1 = Offset(c.x, h * 0.15f)
        val p2 = Offset(w * 0.85f, h * 0.35f)
        val p3 = Offset(w * 0.85f, h * 0.75f)
        val p4 = Offset(c.x, h * 0.92f)
        val p5 = Offset(w * 0.15f, h * 0.75f)
        val p6 = Offset(w * 0.15f, h * 0.35f)

        val path = Path().apply {
            moveTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(p4.x, p4.y)
            lineTo(p5.x, p5.y)
            lineTo(p6.x, p6.y)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.5f, join = StrokeJoin.Round))
        drawLine(color = color, start = c, end = p1, strokeWidth = 1.5f)
        drawLine(color = color, start = c, end = p3, strokeWidth = 1.5f)
        drawLine(color = color, start = c, end = p5, strokeWidth = 1.5f)
    }
}

@Composable
fun HomeNavIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.48f)
            lineTo(w * 0.78f, h * 0.48f)
            lineTo(w * 0.78f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.88f)
            lineTo(w * 0.22f, h * 0.48f)
            lineTo(w * 0.12f, h * 0.48f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.8f, join = StrokeJoin.Round))
    }
}

@Composable
fun CrosshairNavIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        drawCircle(color = color, radius = w * 0.35f, center = c, style = Stroke(width = 1.5f))
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.05f), end = Offset(w * 0.5f, h * 0.95f), strokeWidth = 1.5f)
        drawLine(color = color, start = Offset(w * 0.05f, h * 0.5f), end = Offset(w * 0.95f, h * 0.5f), strokeWidth = 1.5f)
    }
}

@Composable
fun PlayNavIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2f, h / 2f)
        drawCircle(color = color, radius = w * 0.42f, center = c, style = Stroke(width = 1.5f))
        val path = Path().apply {
            moveTo(w * 0.42f, h * 0.32f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.68f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun HandSilhouetteIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Palm + 5 fingers stylized outline
        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.85f)
            lineTo(w * 0.20f, h * 0.55f) // Thumb
            lineTo(w * 0.35f, h * 0.45f)
            lineTo(w * 0.38f, h * 0.15f) // Index
            lineTo(w * 0.46f, h * 0.15f)
            lineTo(w * 0.50f, h * 0.10f) // Middle
            lineTo(w * 0.58f, h * 0.10f)
            lineTo(w * 0.62f, h * 0.18f) // Ring
            lineTo(w * 0.70f, h * 0.18f)
            lineTo(w * 0.74f, h * 0.32f) // Pinky
            lineTo(w * 0.82f, h * 0.32f)
            lineTo(w * 0.80f, h * 0.65f)
            lineTo(w * 0.70f, h * 0.85f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
    }
}

@Composable
fun ActionDirectionIcon(action: RobotAction, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (action) {
            RobotAction.MOVE_FORWARD -> {
                // Arrow UP
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    lineTo(w * 0.82f, h * 0.52f)
                    lineTo(w * 0.62f, h * 0.52f)
                    lineTo(w * 0.62f, h * 0.88f)
                    lineTo(w * 0.38f, h * 0.88f)
                    lineTo(w * 0.38f, h * 0.52f)
                    lineTo(w * 0.18f, h * 0.52f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.MOVE_BACKWARD -> {
                // Arrow DOWN
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.85f)
                    lineTo(w * 0.82f, h * 0.48f)
                    lineTo(w * 0.62f, h * 0.48f)
                    lineTo(w * 0.62f, h * 0.12f)
                    lineTo(w * 0.38f, h * 0.12f)
                    lineTo(w * 0.38f, h * 0.48f)
                    lineTo(w * 0.18f, h * 0.48f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.ROTATE_LEFT -> {
                // Curved Arrow LEFT
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.2f),
                    size = Size(w * 0.7f, h * 0.6f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
                val arrow = Path().apply {
                    moveTo(w * 0.15f, h * 0.5f)
                    lineTo(w * 0.05f, h * 0.35f)
                    lineTo(w * 0.28f, h * 0.35f)
                    close()
                }
                drawPath(arrow, color = color)
            }
            RobotAction.ROTATE_RIGHT -> {
                // Curved Arrow RIGHT
                drawArc(
                    color = color,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.15f, h * 0.2f),
                    size = Size(w * 0.7f, h * 0.6f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
                val arrow = Path().apply {
                    moveTo(w * 0.85f, h * 0.5f)
                    lineTo(w * 0.72f, h * 0.35f)
                    lineTo(w * 0.95f, h * 0.35f)
                    close()
                }
                drawPath(arrow, color = color)
            }
            RobotAction.CROUCH -> {
                // Arrow Down to floor bar
                drawLine(color = color, start = Offset(w * 0.15f, h * 0.88f), end = Offset(w * 0.85f, h * 0.88f), strokeWidth = 3f)
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.75f)
                    lineTo(w * 0.75f, h * 0.45f)
                    lineTo(w * 0.25f, h * 0.45f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.IDLE -> {
                // Fist outline
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.2f, h * 0.25f),
                    size = Size(w * 0.6f, h * 0.55f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.15f, h * 0.15f),
                )
            }
        }
    }
}

@Composable
fun HandGestureSilhouette(action: RobotAction, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (action) {
            RobotAction.IDLE -> {
                // 1: Kepalan tangan (Fist)
                val path = Path().apply {
                    moveTo(w * 0.3f, h * 0.85f)
                    lineTo(w * 0.3f, h * 0.45f)
                    quadraticTo(w * 0.3f, h * 0.25f, w * 0.5f, h * 0.25f)
                    quadraticTo(w * 0.75f, h * 0.25f, w * 0.75f, h * 0.48f)
                    lineTo(w * 0.75f, h * 0.85f)
                    close()
                }
                drawPath(path, color = color)
                // Finger roll lines
                drawLine(color = Color.Black.copy(alpha = 0.3f), start = Offset(w * 0.35f, h * 0.45f), end = Offset(w * 0.70f, h * 0.45f), strokeWidth = 2f)
                drawLine(color = Color.Black.copy(alpha = 0.3f), start = Offset(w * 0.35f, h * 0.60f), end = Offset(w * 0.70f, h * 0.60f), strokeWidth = 2f)
            }
            RobotAction.MOVE_FORWARD -> {
                // 2: Telapak terbuka (Open Palm facing forward)
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.90f)
                    lineTo(w * 0.18f, h * 0.60f) // Thumb
                    lineTo(w * 0.32f, h * 0.48f)
                    lineTo(w * 0.35f, h * 0.15f) // Index
                    lineTo(w * 0.44f, h * 0.15f)
                    lineTo(w * 0.48f, h * 0.08f) // Middle
                    lineTo(w * 0.56f, h * 0.08f)
                    lineTo(w * 0.60f, h * 0.18f) // Ring
                    lineTo(w * 0.68f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.32f) // Pinky
                    lineTo(w * 0.80f, h * 0.32f)
                    lineTo(w * 0.78f, h * 0.68f)
                    lineTo(w * 0.68f, h * 0.90f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.MOVE_BACKWARD -> {
                // 3: Telunjuk ke bawah (Index pointing downward)
                val path = Path().apply {
                    moveTo(w * 0.3f, h * 0.15f)
                    lineTo(w * 0.7f, h * 0.15f)
                    lineTo(w * 0.7f, h * 0.55f)
                    lineTo(w * 0.55f, h * 0.55f)
                    lineTo(w * 0.55f, h * 0.92f) // Pointing finger tip
                    lineTo(w * 0.42f, h * 0.92f)
                    lineTo(w * 0.42f, h * 0.55f)
                    lineTo(w * 0.3f, h * 0.55f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.ROTATE_LEFT -> {
                // 4: V-sign tilted left
                val path = Path().apply {
                    moveTo(w * 0.32f, h * 0.90f)
                    lineTo(w * 0.70f, h * 0.90f)
                    lineTo(w * 0.70f, h * 0.55f)
                    lineTo(w * 0.65f, h * 0.15f) // Middle finger
                    lineTo(w * 0.52f, h * 0.15f)
                    lineTo(w * 0.48f, h * 0.42f)
                    lineTo(w * 0.30f, h * 0.18f) // Index finger tilted left
                    lineTo(w * 0.20f, h * 0.24f)
                    lineTo(w * 0.32f, h * 0.55f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.ROTATE_RIGHT -> {
                // 5: V-sign tilted right
                val path = Path().apply {
                    moveTo(w * 0.30f, h * 0.90f)
                    lineTo(w * 0.68f, h * 0.90f)
                    lineTo(w * 0.68f, h * 0.55f)
                    lineTo(w * 0.80f, h * 0.24f) // Index finger tilted right
                    lineTo(w * 0.70f, h * 0.18f)
                    lineTo(w * 0.52f, h * 0.42f)
                    lineTo(w * 0.48f, h * 0.15f) // Middle finger
                    lineTo(w * 0.35f, h * 0.15f)
                    lineTo(w * 0.30f, h * 0.55f)
                    close()
                }
                drawPath(path, color = color)
            }
            RobotAction.CROUCH -> {
                // 6: Telapak ke bawah (Palm pushing down)
                val path = Path().apply {
                    moveTo(w * 0.15f, h * 0.35f)
                    lineTo(w * 0.85f, h * 0.35f)
                    lineTo(w * 0.78f, h * 0.65f)
                    lineTo(w * 0.22f, h * 0.65f)
                    close()
                }
                drawPath(path, color = color)
                drawLine(color = color, start = Offset(w * 0.15f, h * 0.78f), end = Offset(w * 0.85f, h * 0.78f), strokeWidth = 2.5f, cap = StrokeCap.Round)
            }
        }
    }
}
