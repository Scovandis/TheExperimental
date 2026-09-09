package com.experimental.robot.presentation.ui

import androidx.compose.ui.graphics.Color
import com.experimental.robot.domain.model.RobotAction

/** Palet warna tunggal berorientasi Sci-Fi Cyberpunk / High-Tech Dashboard. */
object RobotColors {
    // Latar belakang utama bernuansa biru malam dalam (Sci-Fi Deep Navy)
    val background = Color(0xFF070D19)
    val surface = Color(0xEE0D172A)
    val surfaceSolid = Color(0xFF0D172A)
    val surfaceCard = Color(0xFF0F1E36)
    val surfaceElevated = Color(0xFF142442)
    val surfaceHover = Color(0xFF1A3056)

    // Garis tepi / batas kartu
    val outline = Color(0x331E3A5F)
    val cardBorder = Color(0xFF1A3258)
    val cardBorderLight = Color(0xFF224476)
    val cardBorderActive = Color(0xFF00E5FF)

    // Aksen warna futuristik
    val accent = Color(0xFF00E5FF)
    val accentBlue = Color(0xFF00A3FF)
    val accentCyan = Color(0xFF00E5FF)
    val accentGreen = Color(0xFF10B981)
    val accentGreenNeon = Color(0xFF00F5A0)
    val accentYellow = Color(0xFFF59E0B)
    val accentPurple = Color(0xFF8B5CF6)
    val chrome = Color(0xFFEEEEEE)

    // Tipografi
    val textPrimary = Color(0xFFFFFFFF)
    val textSecondary = Color(0xFF94A3B8)
    val textMuted = Color(0xFF64748B)

    // Status Jari & Indikator
    val fingerOn = Color(0xFF10B981)
    val fingerOff = Color(0xFF334155)

    // Status Peringatan & Berhenti Darurat
    val danger = Color(0xFFEF4444)
    val dangerSurface = Color(0x28EF4444)
    val dangerGlow = Color(0x55EF4444)
    val dangerDark = Color(0xFF7F1D1D)

    // Efek Cahaya / Glow
    val glowCyan = Color(0x4000E5FF)
    val glowBlue = Color(0x3300A3FF)
    val glowGreen = Color(0x4010B981)

    fun forAction(action: RobotAction): Color = when (action) {
        RobotAction.IDLE -> Color(0xFFC39BFF)
        RobotAction.MOVE_FORWARD -> Color(0xFF00F5A0)
        RobotAction.MOVE_BACKWARD -> Color(0xFFFBBF24)
        RobotAction.ROTATE_LEFT -> Color(0xFF38BDF8)
        RobotAction.ROTATE_RIGHT -> Color(0xFFA78BFA)
        RobotAction.CROUCH -> Color(0xFFF87171)
    }
}
