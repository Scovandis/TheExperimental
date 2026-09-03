package com.experimental.robot.presentation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Render robot secara prosedural dengan Compose Canvas.
 *
 * Pemetaan state -> visual:
 * - positionZ  : skala perspektif (maju = mendekat/membesar)
 * - rotationY  : lebar badan mengikuti cos(yaw) + arah wajah mengikuti sin(yaw)
 * - scaleY     : tinggi badan & kaki (efek jongkok)
 * - walkPhase  : ayunan kaki dan tangan
 */
@Composable
fun RobotCanvas(state: RobotState, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) { drawRobotScene(state) }
}

private fun DrawScope.drawRobotScene(state: RobotState) {
    val accent = RobotColors.forAction(state.currentAction)
    val depth = (1f + state.positionZ / 180f).coerceIn(0.55f, 1.6f)
    val unit = (size.minDimension / 100f) * depth
    val groundY = size.height * 0.80f
    val centerX = size.width / 2f

    val yaw = state.rotationY * PI.toFloat() / 180f
    val facing = sin(yaw)
    val widthFactor = max(0.24f, abs(cos(yaw)))

    drawFloorGrid(groundY)
    drawHeadingArrow(centerX, groundY, unit, facing, cos(yaw), accent)
    drawShadow(centerX, groundY, unit, widthFactor, state.scaleY)
    drawRobotBody(state, centerX, groundY, unit, widthFactor, facing, accent)
}

/** Garis lantai untuk memberi rasa kedalaman saat robot maju/mundur. */
private fun DrawScope.drawFloorGrid(groundY: Float) {
    val rows = 6
    repeat(rows) { i ->
        val t = i / (rows - 1f)
        val y = groundY + (size.height - groundY) * t * t
        val inset = size.width * 0.5f * (1f - t) * 0.55f
        drawLine(
            color = Color.White.copy(alpha = 0.10f - 0.012f * i),
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 1.5f,
        )
    }
}

/** Panah di lantai yang menunjukkan arah hadap robot (hasil rotationY). */
private fun DrawScope.drawHeadingArrow(
    centerX: Float,
    groundY: Float,
    unit: Float,
    facing: Float,
    forward: Float,
    accent: Color,
) {
    val tipX = centerX + facing * 26f * unit
    val tipY = groundY + 9f * unit - forward * 5f * unit
    val baseX = centerX
    val baseY = groundY + 9f * unit

    drawLine(
        color = accent.copy(alpha = 0.55f),
        start = Offset(baseX, baseY),
        end = Offset(tipX, tipY),
        strokeWidth = 2.5f * unit * 0.6f,
        cap = StrokeCap.Round,
    )
    val head = Path().apply {
        moveTo(tipX, tipY)
        lineTo(tipX - 3.5f * unit, tipY + 3.5f * unit)
        lineTo(tipX + 3.5f * unit, tipY + 3.5f * unit)
        close()
    }
    drawPath(head, accent.copy(alpha = 0.55f))
}

private fun DrawScope.drawShadow(
    centerX: Float,
    groundY: Float,
    unit: Float,
    widthFactor: Float,
    scaleY: Float,
) {
    val shadowWidth = 34f * unit * widthFactor + 8f * unit
    drawOval(
        color = Color.Black.copy(alpha = 0.35f * (2f - scaleY)),
        topLeft = Offset(centerX - shadowWidth / 2f, groundY - 2.5f * unit),
        size = Size(shadowWidth, 6f * unit),
    )
}

private fun DrawScope.drawRobotBody(
    state: RobotState,
    centerX: Float,
    groundY: Float,
    unit: Float,
    widthFactor: Float,
    facing: Float,
    accent: Color,
) {
    val swing = sin(state.walkPhase)
    val legLength = 20f * unit * state.scaleY
    val hipY = groundY - legLength
    val torsoHeight = 30f * unit * state.scaleY
    val torsoWidth = 26f * unit * widthFactor
    val torsoTop = hipY - torsoHeight
    val headRadius = 11f * unit
    val headCenter = Offset(centerX + facing * 1.5f * unit, torsoTop - headRadius * 1.15f)

    // Kaki: garis tebal dari pinggul ke telapak, berayun berlawanan fase.
    val legSpread = 7f * unit * widthFactor
    drawLeg(centerX - legSpread, hipY, groundY, unit, swing)
    drawLeg(centerX + legSpread, hipY, groundY, unit, -swing)

    // Lengan di belakang badan supaya siluet badan tetap bersih.
    val shoulderY = torsoTop + 7f * unit
    val armSpread = torsoWidth / 2f + 2.5f * unit
    drawArm(centerX - armSpread, shoulderY, unit, -swing, state.scaleY)
    drawArm(centerX + armSpread, shoulderY, unit, swing, state.scaleY)

    // Badan
    drawRoundRect(
        color = RobotColors.accent,
        topLeft = Offset(centerX - torsoWidth / 2f, torsoTop),
        size = Size(torsoWidth, torsoHeight),
        cornerRadius = CornerRadius(6f * unit, 6f * unit),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset(centerX - torsoWidth / 2f, torsoTop),
        size = Size(torsoWidth * 0.45f, torsoHeight),
        cornerRadius = CornerRadius(6f * unit, 6f * unit),
    )

    // Panel dada sebagai indikator aksi.
    drawRoundRect(
        color = accent.copy(alpha = 0.9f),
        topLeft = Offset(centerX - torsoWidth * 0.22f, torsoTop + torsoHeight * 0.34f),
        size = Size(torsoWidth * 0.44f, torsoHeight * 0.2f),
        cornerRadius = CornerRadius(2f * unit, 2f * unit),
    )

    // Leher
    drawRect(
        color = RobotColors.chrome.copy(alpha = 0.65f),
        topLeft = Offset(centerX - 3f * unit, torsoTop - 3f * unit),
        size = Size(6f * unit, 4f * unit),
    )

    // Antena
    drawLine(
        color = RobotColors.chrome,
        start = Offset(headCenter.x, headCenter.y - headRadius),
        end = Offset(headCenter.x, headCenter.y - headRadius - 6f * unit),
        strokeWidth = 1.6f * unit,
        cap = StrokeCap.Round,
    )
    drawCircle(color = accent, radius = 2f * unit, center = Offset(headCenter.x, headCenter.y - headRadius - 6.5f * unit))

    // Kepala
    drawCircle(color = RobotColors.chrome, radius = headRadius, center = headCenter)
    drawCircle(
        color = Color.Black.copy(alpha = 0.18f),
        radius = headRadius,
        center = headCenter,
        style = Stroke(width = 1.2f * unit),
    )

    // Mata (LED) mengikuti arah hadap; merah saat jongkok.
    val eyeColor = if (state.currentAction == RobotAction.CROUCH) Color(0xFFFF4D4D) else accent
    val eyeOffset = 4.2f * unit * widthFactor
    val eyeShift = facing * 2.5f * unit
    drawCircle(eyeColor, 2.1f * unit, Offset(headCenter.x - eyeOffset + eyeShift, headCenter.y))
    drawCircle(eyeColor, 2.1f * unit, Offset(headCenter.x + eyeOffset + eyeShift, headCenter.y))

    // Mulut / speaker grill
    drawRoundRect(
        color = Color(0xFF3B4252),
        topLeft = Offset(headCenter.x - 3.5f * unit + eyeShift, headCenter.y + 4f * unit),
        size = Size(7f * unit, 1.8f * unit),
        cornerRadius = CornerRadius(1f * unit, 1f * unit),
    )
}

private fun DrawScope.drawLeg(
    hipX: Float,
    hipY: Float,
    groundY: Float,
    unit: Float,
    swing: Float,
) {
    val footX = hipX + swing * 6f * unit
    val lift = max(0f, swing) * 3.5f * unit
    val kneeX = (hipX + footX) / 2f + swing * 1.5f * unit
    val kneeY = (hipY + groundY) / 2f - lift * 0.4f

    drawLine(
        color = Color(0xFF6B7280),
        start = Offset(hipX, hipY),
        end = Offset(kneeX, kneeY),
        strokeWidth = 5.5f * unit,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF6B7280),
        start = Offset(kneeX, kneeY),
        end = Offset(footX, groundY - lift),
        strokeWidth = 5.5f * unit,
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = Color(0xFF374151),
        topLeft = Offset(footX - 4.5f * unit, groundY - lift - 1.5f * unit),
        size = Size(9f * unit, 3f * unit),
        cornerRadius = CornerRadius(1.5f * unit, 1.5f * unit),
    )
}

private fun DrawScope.drawArm(
    shoulderX: Float,
    shoulderY: Float,
    unit: Float,
    swing: Float,
    scaleY: Float,
) {
    val handX = shoulderX + swing * 4.5f * unit
    val handY = shoulderY + 18f * unit * scaleY
    drawLine(
        color = Color(0xFF9CA3AF),
        start = Offset(shoulderX, shoulderY),
        end = Offset(handX, handY),
        strokeWidth = 4f * unit,
        cap = StrokeCap.Round,
    )
    drawCircle(color = Color(0xFF6B7280), radius = 2.6f * unit, center = Offset(handX, handY))
}
