package com.experimental.robot.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.experimental.robot.data.HandLandmarkStream
import com.experimental.robot.data.TrackerStatus

/**
 * Desktop belum memiliki binding webcam + MediaPipe pada build ini.
 * Status `Unsupported` membuat layar otomatis menampilkan kontrol manual,
 * sehingga layer domain & animasi tetap bisa diuji di JVM.
 */
@Composable
actual fun CameraFeed(
    stream: HandLandmarkStream,
    modifier: Modifier,
) {
    LaunchedEffect(Unit) {
        stream.publishStatus(
            TrackerStatus.Unsupported("Kontrol gestur hanya tersedia di Android. Gunakan kontrol manual.")
        )
    }
    Box(
        modifier = modifier.fillMaxSize().background(RobotColors.surfaceSolid),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Kamera tidak\ntersedia",
            color = RobotColors.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp),
        )
    }
}
