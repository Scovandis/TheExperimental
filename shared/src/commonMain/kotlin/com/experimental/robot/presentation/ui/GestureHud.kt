package com.experimental.robot.presentation.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.experimental.robot.data.TrackerStatus
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.presentation.viewmodel.RobotRenderMode
import com.experimental.robot.presentation.viewmodel.RobotUiState

/** Badge besar berisi aksi aktif + progres debounce gestur berikutnya. */
@Composable
fun ActionBadge(state: RobotUiState, modifier: Modifier = Modifier) {
    val accent = RobotColors.forAction(state.stableAction)
    val animatedAccent by animateColorAsState(accent, label = "actionAccent")

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surface)
            .border(1.dp, animatedAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "AKSI: ${state.stableAction.label}",
            color = animatedAccent,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = state.stableAction.gestureHint,
            color = RobotColors.textSecondary,
            fontSize = 11.sp,
        )
        if (state.rawAction != state.stableAction) {
            Text(
                text = "Mengonfirmasi ${state.rawAction.label}...",
                color = RobotColors.textPrimary,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            DebounceMeter(
                progress = state.debounceProgress,
                color = RobotColors.forAction(state.rawAction),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Indikator seberapa lama gestur kandidat sudah bertahan. */
@Composable
private fun DebounceMeter(progress: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(140.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(4.dp)
                .background(color),
        )
    }
}

/** Panel telemetri: status jari, FPS deteksi, dan state robot. */
@Composable
fun TelemetryPanel(state: RobotUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RobotColors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "TELEMETRI",
            color = RobotColors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            state.fingers.asList().forEach { (name, extended) ->
                FingerChip(name = name.take(3).uppercase(), extended = extended)
            }
        }
        TelemetryRow("Tangan", if (state.handDetected) "TERDETEKSI (${state.handedness.name})" else "TIDAK ADA")
        TelemetryRow("Jari terbuka", state.fingers.extendedCount.toString())
        TelemetryRow("FPS deteksi", state.detectionFps.toString())
        TelemetryRow("Posisi Z", state.robot.positionZ.toIntString())
        TelemetryRow("Rotasi Y", "${state.robot.rotationY.toIntString()} deg")
        TelemetryRow("Skala Y", state.robot.scaleY.toFixed2())
        TelemetryRow("Sumber", if (state.manualOverride) "MANUAL" else "GESTUR")
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
        )
    }
}

@Composable
private fun FingerChip(name: String, extended: Boolean) {
    Box(
        modifier = Modifier
            .size(width = 26.dp, height = 20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (extended) RobotColors.fingerOn else RobotColors.fingerOff),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name,
            color = if (extended) Color(0xFF06281A) else RobotColors.textSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Daftar pemetaan gestur -> aksi, sekaligus penanda aksi yang sedang aktif. */
@Composable
fun GestureLegend(activeAction: RobotAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RobotColors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = "PETA GESTUR",
            color = RobotColors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        RobotAction.entries.forEach { action ->
            val active = action == activeAction
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (active) RobotColors.forAction(action)
                            else RobotColors.forAction(action).copy(alpha = 0.28f)
                        ),
                )
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = action.label,
                        color = if (active) RobotColors.textPrimary else RobotColors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                    Text(
                        text = action.gestureHint,
                        color = RobotColors.textSecondary.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

/** Tombol pindah visualisasi 3D <-> 2D. */
@Composable
fun RenderModeToggle(
    mode: RobotRenderMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.accent.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggle() }) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "TAMPILAN: ${mode.label}",
            color = RobotColors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Banner status pipeline (izin kamera, error model, platform tanpa kamera). */
@Composable
fun StatusBanner(status: TrackerStatus, modifier: Modifier = Modifier) {
    val message = when (status) {
        TrackerStatus.Idle -> "Menyiapkan pipeline..."
        TrackerStatus.Initializing -> "Memuat model hand_landmarker.task..."
        TrackerStatus.PermissionRequired -> "Izin kamera diperlukan untuk kontrol gestur."
        TrackerStatus.Running -> null
        is TrackerStatus.Unsupported -> status.reason
        is TrackerStatus.Error -> "Gagal: ${status.message}"
    } ?: return

    Text(
        text = message,
        color = RobotColors.textPrimary,
        fontSize = 12.sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RobotColors.surfaceSolid.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private fun Float.toIntString(): String = toInt().toString()

private fun Float.toFixed2(): String {
    val scaled = (this * 100).toInt()
    return "${scaled / 100}.${(scaled % 100).toString().padStart(2, '0')}"
}
