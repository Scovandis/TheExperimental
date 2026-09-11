// TODO(AUDIT_INCOMPLETE.md #4, #5): seluruh file ini tidak dipanggil dari layar manapun.
// RobotControlScreen.kt memakai PetaGesturGrid (DashboardComponents.kt) sebagai gantinya, yang
// hanya tap-sekali (.clickable) sehingga onManualActionReleased() tidak pernah terpanggil dan
// override manual mengunci permanen. Pad di file ini sudah punya pola press-and-hold yang benar
// (onPress/tryAwaitRelease di bawah) — solusi siap pakai untuk bug tersebut, tinggal disambungkan.
package com.experimental.robot.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.experimental.robot.domain.model.RobotAction

/**
 * Kontrol manual (tahan tombol) sebagai cadangan bila kamera tidak tersedia
 * atau untuk menguji layer motion tanpa gestur.
 */
@Composable
fun ManualControlPad(
    onPressed: (RobotAction) -> Unit,
    onReleased: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(RobotColors.surface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("KONTROL MANUAL", color = RobotColors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        HoldButton("MAJU", RobotAction.MOVE_FORWARD, onPressed, onReleased)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HoldButton("KIRI", RobotAction.ROTATE_LEFT, onPressed, onReleased)
            HoldButton("KANAN", RobotAction.ROTATE_RIGHT, onPressed, onReleased)
        }
        HoldButton("MUNDUR", RobotAction.MOVE_BACKWARD, onPressed, onReleased)
        HoldButton("JONGKOK", RobotAction.CROUCH, onPressed, onReleased)
        Box(
            modifier = Modifier
                .size(width = 156.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(RobotColors.fingerOff)
                .pointerInput(Unit) { detectTapGestures(onTap = { onReset() }) },
            contentAlignment = Alignment.Center,
        ) {
            Text("RESET POSISI", color = RobotColors.textPrimary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun HoldButton(
    label: String,
    action: RobotAction,
    onPressed: (RobotAction) -> Unit,
    onReleased: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(width = 75.dp, height = 34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(RobotColors.forAction(action).copy(alpha = 0.85f))
            .pointerInput(action) {
                detectTapGestures(
                    onPress = {
                        onPressed(action)
                        tryAwaitRelease()
                        onReleased()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = RobotColors.surfaceSolid, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
