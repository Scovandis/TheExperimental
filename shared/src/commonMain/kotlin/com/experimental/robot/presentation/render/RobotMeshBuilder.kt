package com.experimental.robot.presentation.render

import androidx.compose.ui.graphics.Color
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import kotlin.math.sin

/**
 * Menyusun model robot 3D dari balok-balok, lengkap dengan transformasi hasil gestur.
 *
 * Pemetaan state -> transformasi 3D:
 * - `positionZ` : translasi sumbu Z dunia (maju = mendekat ke kamera, perspektif nyata)
 * - `rotationY` : rotasi yaw sesungguhnya di sekitar sumbu Y
 * - `scaleY`    : pinggul turun & kaki memendek (jongkok, kaki tetap menapak lantai)
 * - `walkPhase` : rotasi engsel pinggul dan bahu (ayunan langkah)
 */
object RobotMeshBuilder {

    private val bodyColor = Color(0xFF00ADB5)
    private val bodyDark = Color(0xFF12707A)
    private val chromeColor = Color(0xFFE4E7EC)
    private val limbColor = Color(0xFF8A93A5)
    private val jointColor = Color(0xFF4B5566)
    private val darkPanel = Color(0xFF2A303C)

    private const val HIP_Y = 0.94f
    private const val FOOT_HEIGHT = 0.16f

    /** Skala pemetaan positionZ (satuan engine) ke satuan dunia render. */
    private const val DEPTH_DIVISOR = 55f

    fun build(state: RobotState, accent: Color): Mesh {
        val swing = sin(state.walkPhase)
        val hipDrop = HIP_Y * (1f - state.scaleY)
        val hipY = HIP_Y - hipDrop

        val body = upperBody(state, accent, swing) + legs(hipY, swing)

        // Transformasi dunia: yaw lalu geser kedalaman.
        val world = Mat4.translation(0f, 0f, state.positionZ / DEPTH_DIVISOR) *
            Mat4.rotationY(state.rotationY)
        return body.transformed(world)
    }

    /** Bayangan sebagai bidang datar di lantai, mengikuti kedalaman & lebar badan. */
    fun shadow(state: RobotState): Mesh {
        val squash = 1f + (1f - state.scaleY) * 0.6f
        return Mesh.groundQuad(
            width = 1.5f * squash,
            depth = 1.0f * squash,
            y = 0.004f,
            color = Color.Black.copy(alpha = 0.34f),
        ).transformed(Mat4.translation(0f, 0f, state.positionZ / DEPTH_DIVISOR))
    }

    /** Garis-garis lantai 3D (pasangan titik) untuk memperkuat kesan kedalaman. */
    fun groundGrid(): List<Pair<Vec3, Vec3>> {
        val lines = mutableListOf<Pair<Vec3, Vec3>>()
        val halfX = 4f
        for (i in -6..3) {
            val z = i.toFloat()
            lines += Vec3(-halfX, 0f, z) to Vec3(halfX, 0f, z)
        }
        for (i in -4..4) {
            val x = i.toFloat()
            lines += Vec3(x, 0f, -6f) to Vec3(x, 0f, 3f)
        }
        return lines
    }

    private fun upperBody(state: RobotState, accent: Color, swing: Float): Mesh {
        val hipDrop = HIP_Y * (1f - state.scaleY)
        val lift = -hipDrop
        var mesh = Mesh.EMPTY

        // Pinggul
        mesh += Mesh.box(0.72f, 0.28f, 0.52f, bodyDark)
            .transformed(Mat4.translation(0f, HIP_Y + lift, 0f))

        // Torso + panel dada (indikator aksi)
        mesh += Mesh.box(1.02f, 1.05f, 0.6f, bodyColor)
            .transformed(Mat4.translation(0f, 1.62f + lift, 0f))
        mesh += Mesh.box(0.44f, 0.2f, 0.04f, accent, emissiveFront = true)
            .transformed(Mat4.translation(0f, 1.62f + lift, 0.31f))

        // Bahu
        mesh += Mesh.box(0.24f, 0.28f, 0.32f, jointColor)
            .transformed(Mat4.translation(-0.63f, 2.0f + lift, 0f))
        mesh += Mesh.box(0.24f, 0.28f, 0.32f, jointColor)
            .transformed(Mat4.translation(0.63f, 2.0f + lift, 0f))

        // Lengan berayun berlawanan fase dengan kaki
        mesh += arm(-0.63f, 1.96f + lift, -swing * ARM_SWING_DEGREES)
        mesh += arm(0.63f, 1.96f + lift, swing * ARM_SWING_DEGREES)

        // Leher & kepala
        mesh += Mesh.box(0.2f, 0.16f, 0.2f, jointColor)
            .transformed(Mat4.translation(0f, 2.22f + lift, 0f))
        mesh += Mesh.box(0.72f, 0.62f, 0.66f, chromeColor)
            .transformed(Mat4.translation(0f, 2.6f + lift, 0f))

        // Mata LED (emissive) + grill mulut
        val eyeColor = if (state.currentAction == RobotAction.CROUCH) Color(0xFFFF4D4D) else accent
        mesh += Mesh.box(0.16f, 0.14f, 0.05f, eyeColor, emissiveFront = true)
            .transformed(Mat4.translation(-0.17f, 2.66f + lift, 0.34f))
        mesh += Mesh.box(0.16f, 0.14f, 0.05f, eyeColor, emissiveFront = true)
            .transformed(Mat4.translation(0.17f, 2.66f + lift, 0.34f))
        mesh += Mesh.box(0.34f, 0.06f, 0.04f, darkPanel)
            .transformed(Mat4.translation(0f, 2.44f + lift, 0.34f))

        // Antena
        mesh += Mesh.box(0.06f, 0.3f, 0.06f, limbColor)
            .transformed(Mat4.translation(0f, 3.05f + lift, 0f))
        mesh += Mesh.box(0.14f, 0.14f, 0.14f, accent, emissiveFront = true)
            .transformed(Mat4.translation(0f, 3.26f + lift, 0f))

        return mesh
    }

    /** Lengan + tangan digantung dari bahu, lalu diputar pada engsel bahu. */
    private fun arm(shoulderX: Float, shoulderY: Float, swingDegrees: Float): Mesh {
        val local = Mesh.box(0.2f, 0.8f, 0.2f, limbColor)
            .transformed(Mat4.translation(0f, -0.4f, 0f)) +
            Mesh.box(0.24f, 0.22f, 0.24f, jointColor)
                .transformed(Mat4.translation(0f, -0.9f, 0f))

        return local.transformed(
            Mat4.translation(shoulderX, shoulderY, 0f) * Mat4.rotationX(swingDegrees)
        )
    }

    /**
     * Kaki + telapak digantung dari pinggul; panjangnya selalu tepat setinggi pinggul
     * sehingga telapak tetap menapak lantai (y = 0) baik berdiri maupun jongkok.
     */
    private fun legs(hipY: Float, swing: Float): Mesh {
        val shinHeight = (hipY - FOOT_HEIGHT).coerceAtLeast(0.1f)

        fun leg(hipX: Float, swingDegrees: Float): Mesh {
            val local = Mesh.box(0.28f, shinHeight, 0.28f, limbColor)
                .transformed(Mat4.translation(0f, -shinHeight / 2f, 0f)) +
                Mesh.box(0.34f, FOOT_HEIGHT, 0.52f, jointColor)
                    .transformed(Mat4.translation(0f, -shinHeight - FOOT_HEIGHT / 2f, 0.1f))

            return local.transformed(
                Mat4.translation(hipX, hipY, 0f) * Mat4.rotationX(swingDegrees)
            )
        }

        return leg(-0.24f, swing * LEG_SWING_DEGREES) + leg(0.24f, -swing * LEG_SWING_DEGREES)
    }

    private const val ARM_SWING_DEGREES = 26f
    private const val LEG_SWING_DEGREES = 22f
}
