package com.experimental.robot.presentation.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.experimental.robot.presentation.viewmodel.CalibrationUiState
import com.experimental.robot.presentation.viewmodel.RobotUiState

/**
 * Tombol emergency stop.
 *
 * Selalu terlihat, tidak pernah disembunyikan di balik menu, dan tidak pernah dinonaktifkan.
 * Sebuah tombol berhenti yang bisa hilang bukan tombol berhenti.
 */
@Composable
fun EmergencyStopButton(
    onEmergencyStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RobotColors.dangerSurface)
            .border(2.dp, RobotColors.danger, RoundedCornerShape(12.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onEmergencyStop() }) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "STOP",
            color = RobotColors.danger,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Banner alasan berhenti, dengan tombol RESET saat kunci darurat aktif.
 *
 * Emergency stop dan safety lock sengaja tidak bisa pulih sendiri, jadi UI harus
 * menyediakan satu-satunya jalan keluarnya secara eksplisit.
 */
@Composable
fun SafetyBanner(
    state: RobotUiState,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.halt.blocksMovement) return

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RobotColors.dangerSurface)
            .border(1.dp, RobotColors.danger.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = state.halt.name,
            color = RobotColors.danger,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = state.stopReason.label,
            color = RobotColors.textPrimary,
            fontSize = 11.sp,
        )
        if (state.latched) {
            Text(
                text = "Butuh reset manual untuk melanjutkan.",
                color = RobotColors.textSecondary,
                fontSize = 10.sp,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(RobotColors.danger)
                    .pointerInput(Unit) { detectTapGestures(onTap = { onReset() }) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = "RESET",
                    color = Color(0xFF2A1113),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Overlay wizard kalibrasi: satu instruksi per langkah, dengan progres pengumpulan sampel.
 *
 * Selama kalibrasi berjalan, robot dipaksa berhenti - snapshot persepsi tidak
 * diterbitkan sama sekali, jadi tidak ada perintah yang bisa lolos.
 */
@Composable
fun CalibrationOverlay(
    calibration: CalibrationUiState,
    onNext: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(RobotColors.surfaceSolid)
            .border(1.dp, RobotColors.accent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "KALIBRASI ${calibration.step.ordinal + 1}/6",
            color = RobotColors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = calibration.step.instruction,
            color = RobotColors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.15f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(calibration.progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(if (calibration.stepComplete) RobotColors.fingerOn else RobotColors.accent),
            )
        }
        Text(
            text = if (calibration.stepComplete) {
                "Sampel cukup - lanjut ke langkah berikutnya."
            } else {
                "Tahan posisi sampai sampel terkumpul."
            },
            color = RobotColors.textSecondary,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalibrationButton(
                label = if (calibration.step.next == null) "SELESAI" else "LANJUT",
                enabled = calibration.stepComplete,
                onTap = onNext,
            )
            CalibrationButton(label = "BATAL", enabled = true, onTap = onCancel)
        }
    }
}

@Composable
private fun CalibrationButton(label: String, enabled: Boolean, onTap: () -> Unit) {
    val tint = if (enabled) RobotColors.accent else RobotColors.fingerOff
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(RobotColors.fingerOff.copy(alpha = 0.35f))
            .border(1.dp, tint.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .pointerInput(enabled) {
                detectTapGestures(onTap = { if (enabled) onTap() })
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text = label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Tombol memulai wizard kalibrasi. */
@Composable
fun CalibrationLauncher(onStart: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(RobotColors.surface)
            .border(1.dp, RobotColors.outline, RoundedCornerShape(10.dp))
            .pointerInput(Unit) { detectTapGestures(onTap = { onStart() }) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = "KALIBRASI",
            color = RobotColors.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
