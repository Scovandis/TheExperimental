package com.experimental.robot.presentation.ui

import androidx.compose.ui.graphics.Color
import com.experimental.robot.domain.model.RobotAction

/** Palet warna tunggal supaya HUD, overlay, dan robot tetap konsisten. */
object RobotColors {
    val background = Color(0xFF12131A)
    val surface = Color(0xCC1E1E24)
    val surfaceSolid = Color(0xFF1E1E24)
    val outline = Color(0x33FFFFFF)
    val accent = Color(0xFF00ADB5)
    val chrome = Color(0xFFEEEEEE)
    val textPrimary = Color(0xFFF2F4F8)
    val textSecondary = Color(0xFF9BA3B4)
    val fingerOn = Color(0xFF4ADE80)
    val fingerOff = Color(0xFF4B5563)

    fun forAction(action: RobotAction): Color = when (action) {
        // Idle memakai lavender khas XR-07 (#C39BFF); aksi lain tetap berkode warna fungsional.
        RobotAction.IDLE -> Color(0xFFC39BFF)
        RobotAction.MOVE_FORWARD -> Color(0xFF4ADE80)
        RobotAction.MOVE_BACKWARD -> Color(0xFFFBBF24)
        RobotAction.ROTATE_LEFT -> Color(0xFF60A5FA)
        RobotAction.ROTATE_RIGHT -> Color(0xFFA78BFA)
        RobotAction.CROUCH -> Color(0xFFF87171)
    }
}
