package com.experimental

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.experimental.robot.presentation.ui.RobotColors
import com.experimental.robot.presentation.ui.RobotControlScreen

/**
 * Titik masuk UI bersama: tema gelap + layar kontrol robot berbasis gestur tangan.
 */
@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RobotColors.accent,
            background = RobotColors.background,
            surface = RobotColors.surfaceSolid,
        ),
    ) {
        RobotControlScreen(modifier = Modifier.fillMaxSize())
    }
}
