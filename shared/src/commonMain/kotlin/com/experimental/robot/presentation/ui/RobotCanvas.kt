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
    val legLength = 22f * unit * state.scaleY
    val hipY = groundY - legLength
    val torsoHeight = 28f * unit * state.scaleY
    val torsoWidth = 26f * unit * widthFactor
    val torsoTop = hipY - torsoHeight

    val headWidth = 28f * unit * widthFactor
    val headHeight = 18f * unit
    val headCenter = Offset(centerX + facing * 2f * unit, torsoTop - headHeight * 0.72f)

    // Palet warna robot oranye retro
    val orangePrimary = Color(0xFFFA541C)
    val darkCharcoal = Color(0xFF262B34)
    val eyeCyan = if (state.currentAction == RobotAction.CROUCH) Color(0xFFFF4D4D) else Color(0xFF67E8F9)
    val eyeSocketColor = Color(0xFF141820)

    // Kaki: berayun berlawanan fase
    val legSpread = 7.5f * unit * widthFactor
    drawLeg(centerX - legSpread, hipY, groundY, unit, swing, widthFactor)
    drawLeg(centerX + legSpread, hipY, groundY, unit, -swing, widthFactor)

    // Lengan di kedua sisi
    val shoulderY = torsoTop + 6f * unit * state.scaleY
    val armSpread = torsoWidth / 2f + 3f * unit
    drawArm(centerX - armSpread, shoulderY, unit, -swing, state.scaleY, widthFactor, isLeft = true)
    drawArm(centerX + armSpread, shoulderY, unit, swing, state.scaleY, widthFactor, isLeft = false)

    // Pelvis (dark charcoal)
    val pelvisWidth = 18f * unit * widthFactor
    val pelvisHeight = 5.5f * unit * state.scaleY
    drawRoundRect(
        color = darkCharcoal,
        topLeft = Offset(centerX - pelvisWidth / 2f, hipY - pelvisHeight),
        size = Size(pelvisWidth, pelvisHeight),
        cornerRadius = CornerRadius(2.5f * unit, 2.5f * unit),
    )

    // Badan / Torso (Rounded orange capsule/barrel)
    drawRoundRect(
        color = orangePrimary,
        topLeft = Offset(centerX - torsoWidth / 2f, torsoTop),
        size = Size(torsoWidth, torsoHeight),
        cornerRadius = CornerRadius(7f * unit, 7f * unit),
    )
    // Shading/highlight pada torso
    drawRoundRect(
        color = Color.White.copy(alpha = 0.15f),
        topLeft = Offset(centerX - torsoWidth / 2f, torsoTop),
        size = Size(torsoWidth * 0.38f, torsoHeight),
        cornerRadius = CornerRadius(7f * unit, 7f * unit),
    )

    // Panel dada / aksen indikator aksi (subtle glowing pill)
    val badgeWidth = 10f * unit * widthFactor
    val badgeHeight = 2.8f * unit
    drawRoundRect(
        color = accent.copy(alpha = 0.9f),
        topLeft = Offset(centerX - badgeWidth / 2f + facing * 1.5f * unit, torsoTop + torsoHeight * 0.38f),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = CornerRadius(1.4f * unit, 1.4f * unit),
    )

    // Leher (dark charcoal)
    drawRoundRect(
        color = darkCharcoal,
        topLeft = Offset(centerX - 4f * unit * widthFactor, torsoTop - 3.5f * unit),
        size = Size(8f * unit * widthFactor, 4.5f * unit),
        cornerRadius = CornerRadius(1.5f * unit, 1.5f * unit),
    )

    // Ear pods di samping kepala (kiri & kanan)
    val earWidth = 3f * unit * widthFactor
    val earHeight = 9f * unit
    // Ear kiri
    drawRoundRect(
        color = darkCharcoal,
        topLeft = Offset(headCenter.x - headWidth / 2f - earWidth * 0.8f, headCenter.y - earHeight / 2f),
        size = Size(earWidth, earHeight),
        cornerRadius = CornerRadius(1.5f * unit, 1.5f * unit),
    )
    // Ear kanan
    drawRoundRect(
        color = darkCharcoal,
        topLeft = Offset(headCenter.x + headWidth / 2f - earWidth * 0.2f, headCenter.y - earHeight / 2f),
        size = Size(earWidth, earHeight),
        cornerRadius = CornerRadius(1.5f * unit, 1.5f * unit),
    )

    // Kepala TV-Head (Rounded box orange)
    drawRoundRect(
        color = orangePrimary,
        topLeft = Offset(headCenter.x - headWidth / 2f, headCenter.y - headHeight / 2f),
        size = Size(headWidth, headHeight),
        cornerRadius = CornerRadius(4.5f * unit, 4.5f * unit),
    )

    // Highlight lembut di atas kepala
    drawRoundRect(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(headCenter.x - headWidth * 0.44f, headCenter.y - headHeight * 0.44f),
        size = Size(headWidth * 0.88f, headHeight * 0.35f),
        cornerRadius = CornerRadius(3f * unit, 3f * unit),
    )

    // Bingkai layar TV gelap (eye socket)
    val socketWidth = 20f * unit * widthFactor
    val socketHeight = 11f * unit
    val socketX = headCenter.x - socketWidth / 2f + facing * 1.5f * unit
    val socketY = headCenter.y - socketHeight / 2f + 0.5f * unit
    drawRoundRect(
        color = eyeSocketColor,
        topLeft = Offset(socketX, socketY),
        size = Size(socketWidth, socketHeight),
        cornerRadius = CornerRadius(3f * unit, 3f * unit),
    )

    // Mata cyan kembar (dua kotak rounded sejajar dengan celah di tengah)
    val eyeWidth = 5.5f * unit * widthFactor
    val eyeHeight = 6.8f * unit
    val eyeSpacing = 2.2f * unit * widthFactor
    val eyeY = socketY + (socketHeight - eyeHeight) / 2f
    val leftEyeX = headCenter.x - eyeWidth - eyeSpacing / 2f + facing * 1.8f * unit
    val rightEyeX = headCenter.x + eyeSpacing / 2f + facing * 1.8f * unit

    // Mata Kiri
    drawRoundRect(
        color = eyeCyan,
        topLeft = Offset(leftEyeX, eyeY),
        size = Size(eyeWidth, eyeHeight),
        cornerRadius = CornerRadius(1.8f * unit, 1.8f * unit),
    )
    // Mata Kanan
    drawRoundRect(
        color = eyeCyan,
        topLeft = Offset(rightEyeX, eyeY),
        size = Size(eyeWidth, eyeHeight),
        cornerRadius = CornerRadius(1.8f * unit, 1.8f * unit),
    )

    // Refleksi kilau mata
    val glareSize = 1.3f * unit
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = glareSize,
        center = Offset(leftEyeX + eyeWidth * 0.35f, eyeY + eyeHeight * 0.32f),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = glareSize,
        center = Offset(rightEyeX + eyeWidth * 0.35f, eyeY + eyeHeight * 0.32f),
    )
}

private fun DrawScope.drawLeg(
    hipX: Float,
    hipY: Float,
    groundY: Float,
    unit: Float,
    swing: Float,
    widthFactor: Float,
) {
    val lift = max(0f, swing) * 3.5f * unit
    val footX = hipX + swing * 6f * unit
    val kneeX = (hipX + footX) / 2f + swing * 1.5f * unit
    val kneeY = (hipY + groundY) / 2f - lift * 0.4f
    val currentSoleY = groundY - lift

    // Paha atas (strut metal perak)
    drawLine(
        color = Color(0xFF8C96A4),
        start = Offset(hipX, hipY),
        end = Offset(kneeX, kneeY),
        strokeWidth = 5f * unit * widthFactor,
        cap = StrokeCap.Round,
    )

    // Engsel lutut
    drawCircle(
        color = Color(0xFF6B7280),
        radius = 3.2f * unit,
        center = Offset(kneeX, kneeY),
    )

    // Boot mengerucut / flared (trapezoid oranye khas)
    val bootTopHalf = 3.2f * unit * widthFactor
    val bootBottomHalf = 5.8f * unit * widthFactor
    val bootTopY = kneeY + 1f * unit
    val bootBottomY = currentSoleY - 2.5f * unit

    val bootPath = Path().apply {
        moveTo(kneeX - bootTopHalf, bootTopY)
        lineTo(kneeX + bootTopHalf, bootTopY)
        lineTo(footX + bootBottomHalf, bootBottomY)
        lineTo(footX - bootBottomHalf, bootBottomY)
        close()
    }
    drawPath(bootPath, color = Color(0xFFFA541C))

    // Highlight boot
    val highlightPath = Path().apply {
        moveTo(kneeX - bootTopHalf, bootTopY)
        lineTo(kneeX - bootTopHalf * 0.25f, bootTopY)
        lineTo(footX - bootBottomHalf * 0.25f, bootBottomY)
        lineTo(footX - bootBottomHalf, bootBottomY)
        close()
    }
    drawPath(highlightPath, color = Color.White.copy(alpha = 0.18f))

    // Sol telapak kaki (charcoal/soleGray)
    val soleWidth = 13f * unit * widthFactor
    val soleHeight = 3.2f * unit
    drawRoundRect(
        color = Color(0xFF474F5A),
        topLeft = Offset(footX - soleWidth / 2f, currentSoleY - soleHeight),
        size = Size(soleWidth, soleHeight),
        cornerRadius = CornerRadius(1.2f * unit, 1.2f * unit),
    )
}

private fun DrawScope.drawArm(
    shoulderX: Float,
    shoulderY: Float,
    unit: Float,
    swing: Float,
    scaleY: Float,
    widthFactor: Float,
    isLeft: Boolean,
) {
    // Soket bahu (dark charcoal)
    drawCircle(
        color = Color(0xFF262B34),
        radius = 3.5f * unit,
        center = Offset(shoulderX, shoulderY),
    )
    // Penutup bahu (oranye)
    drawCircle(
        color = Color(0xFFFA541C),
        radius = 2.4f * unit,
        center = Offset(shoulderX, shoulderY),
    )

    val elbowX = shoulderX + swing * 3f * unit
    val elbowY = shoulderY + 8f * unit * scaleY

    // Lengan atas (strut metal perak)
    drawLine(
        color = Color(0xFF8C96A4),
        start = Offset(shoulderX, shoulderY),
        end = Offset(elbowX, elbowY),
        strokeWidth = 3.5f * unit * widthFactor,
        cap = StrokeCap.Round,
    )

    // Engsel siku
    drawCircle(
        color = Color(0xFF8C96A4),
        radius = 2.6f * unit,
        center = Offset(elbowX, elbowY),
    )

    val handX = elbowX + swing * 3f * unit
    val handY = elbowY + 9f * unit * scaleY

    // Lengan bawah (oranye)
    drawLine(
        color = Color(0xFFFA541C),
        start = Offset(elbowX, elbowY),
        end = Offset(handX, handY),
        strokeWidth = 4.2f * unit * widthFactor,
        cap = StrokeCap.Round,
    )

    // Dudukan capit pincer
    drawCircle(
        color = Color(0xFF262B34),
        radius = 2.2f * unit,
        center = Offset(handX, handY),
    )

    // Capit pincer 3 cabang (metalik perak)
    val clawColor = Color(0xFF8C96A4)
    val clawStroke = 1.8f * unit
    val clawLen = 4f * unit
    val dir = if (isLeft) -1f else 1f

    // Jempol / cabang dalam
    drawLine(
        color = clawColor,
        start = Offset(handX, handY),
        end = Offset(handX - dir * 2f * unit, handY + clawLen),
        strokeWidth = clawStroke,
        cap = StrokeCap.Round,
    )
    // Cabang luar atas
    drawLine(
        color = clawColor,
        start = Offset(handX, handY),
        end = Offset(handX + dir * 2.5f * unit, handY + clawLen * 0.9f),
        strokeWidth = clawStroke,
        cap = StrokeCap.Round,
    )
    // Cabang luar bawah
    drawLine(
        color = clawColor,
        start = Offset(handX, handY),
        end = Offset(handX + dir * 1f * unit, handY + clawLen * 1.15f),
        strokeWidth = clawStroke,
        cap = StrokeCap.Round,
    )
}
