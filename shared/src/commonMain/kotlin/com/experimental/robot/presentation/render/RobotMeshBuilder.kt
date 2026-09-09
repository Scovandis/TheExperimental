package com.experimental.robot.presentation.render

import androidx.compose.ui.graphics.Color
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import kotlin.math.sin

/**
 * Menyusun model robot 3D bergaya "XR-07 Assistant Robot": proporsi chibi (kepala
 * besar bulat seperti helm), badan titanium gelap, dan aksen LED ungu — lalu
 * menerapkan transformasi hasil gestur.
 *
 * Pemetaan state -> transformasi 3D:
 * - `positionZ` : translasi sumbu Z dunia (maju = mendekat ke kamera, perspektif nyata)
 * - `rotationY` : rotasi yaw sesungguhnya di sekitar sumbu Y
 * - `scaleY`    : pinggul turun & kaki memendek (jongkok, kaki tetap menapak lantai)
 * - `walkPhase` : rotasi engsel pinggul dan bahu (ayunan langkah)
 */
object RobotMeshBuilder {

    // Palet warna robot retro oranye (sesuai referensi 3D model):
    private val orangePrimary = Color(0xFFFA541C)  // cangkang oranye cerah utama
    private val orangeDark = Color(0xFFD4380D)     // lekukan oranye
    private val darkCharcoal = Color(0xFF262B34)   // pelvis, leher, ear pods, soket bahu
    private val metalSilver = Color(0xFF8C96A4)    // engsel siku, lutut, capit pincer
    private val eyeCyan = Color(0xFF67E8F9)        // layar TV-mata cyan bercahaya
    private val eyeSocket = Color(0xFF141820)      // bingkai mata gelap
    private val soleGray = Color(0xFF474F5A)       // sol telapak kaki

    private const val HIP_Y = 0.68f
    private const val FOOT_HEIGHT = 0.16f
    private const val HEAD_WIDTH = 0.88f
    private const val HEAD_HEIGHT = 0.58f
    private const val HEAD_DEPTH = 0.52f

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
            width = 1.3f * squash,
            depth = 0.9f * squash,
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

    data class HologramLine(val from: Vec3, val to: Vec3, val color: Color, val strokeWidth: Float)

    /** Cincin holografik sirkular futuristik di lantai mengikuti posisi robot. */
    fun hologramPlatformLines(state: RobotState): List<HologramLine> {
        val lines = mutableListOf<HologramLine>()
        val centerZ = state.positionZ / DEPTH_DIVISOR
        val y = 0.006f
        val twoPi = (2 * kotlin.math.PI).toFloat()

        fun addRing(radius: Float, segments: Int, color: Color, strokeWidth: Float) {
            val step = twoPi / segments
            for (i in 0 until segments) {
                val a1 = i * step
                val a2 = (i + 1) * step
                lines += HologramLine(
                    from = Vec3(kotlin.math.sin(a1) * radius, y, centerZ + kotlin.math.cos(a1) * radius),
                    to = Vec3(kotlin.math.sin(a2) * radius, y, centerZ + kotlin.math.cos(a2) * radius),
                    color = color,
                    strokeWidth = strokeWidth,
                )
            }
        }

        // Cincin luar bercahaya biru-cyan tebal
        addRing(radius = 1.38f, segments = 36, color = Color(0xFF00E5FF).copy(alpha = 0.85f), strokeWidth = 2.4f)
        // Cincin kedua
        addRing(radius = 1.15f, segments = 32, color = Color(0xFF00A3FF).copy(alpha = 0.60f), strokeWidth = 1.6f)
        // Cincin dalam
        addRing(radius = 0.72f, segments = 24, color = Color(0xFF38BDF8).copy(alpha = 0.40f), strokeWidth = 1.2f)

        // Radial ticks & crosshairs di sekitar platform
        val tickCount = 16
        val tickStep = twoPi / tickCount
        for (i in 0 until tickCount) {
            val angle = i * tickStep
            val isMajor = i % 4 == 0
            val rInner = 1.38f
            val rOuter = if (isMajor) 1.58f else 1.48f
            val alpha = if (isMajor) 0.90f else 0.50f
            lines += HologramLine(
                from = Vec3(kotlin.math.sin(angle) * rInner, y, centerZ + kotlin.math.cos(angle) * rInner),
                to = Vec3(kotlin.math.sin(angle) * rOuter, y, centerZ + kotlin.math.cos(angle) * rOuter),
                color = Color(0xFF00E5FF).copy(alpha = alpha),
                strokeWidth = if (isMajor) 2.2f else 1.4f,
            )
        }
        return lines
    }

    private fun upperBody(state: RobotState, accent: Color, swing: Float): Mesh {
        val hipDrop = HIP_Y * (1f - state.scaleY)
        val lift = -hipDrop
        var mesh = Mesh.EMPTY

        // 1. Pelvis / Pinggul tengah (Dark Charcoal)
        mesh += Mesh.box(0.46f, 0.22f, 0.38f, darkCharcoal)
            .transformed(Mat4.translation(0f, HIP_Y + lift, 0f))

        // Bola sendi pinggul kiri dan kanan (Orange)
        mesh += Mesh.sphere(0.15f, orangePrimary, latSegments = 5, lonSegments = 8)
            .transformed(Mat4.translation(-0.25f, HIP_Y + lift, 0f))
        mesh += Mesh.sphere(0.15f, orangePrimary, latSegments = 5, lonSegments = 8)
            .transformed(Mat4.translation(0.25f, HIP_Y + lift, 0f))

        // 2. Torso: Badan oranye membulat
        val torsoY = HIP_Y + 0.48f + lift
        mesh += Mesh.box(0.84f, 0.72f, 0.58f, orangePrimary)
            .transformed(Mat4.translation(0f, torsoY, 0f))
        mesh += Mesh.box(0.78f, 0.78f, 0.52f, orangePrimary)
            .transformed(Mat4.translation(0f, torsoY, 0f))

        // 3. Bahu bulat di kiri dan kanan
        val shoulderY = torsoY + 0.25f
        val shoulderX = 0.48f
        // Soket bahu dalam gelap
        mesh += Mesh.sphere(0.14f, darkCharcoal, latSegments = 5, lonSegments = 8)
            .transformed(Mat4.translation(-shoulderX, shoulderY, 0f))
        mesh += Mesh.sphere(0.14f, darkCharcoal, latSegments = 5, lonSegments = 8)
            .transformed(Mat4.translation(shoulderX, shoulderY, 0f))
        // Topi bahu luar oranye
        mesh += Mesh.sphere(0.17f, orangePrimary, latSegments = 5, lonSegments = 8)
            .transformed(Mat4.translation(-shoulderX - 0.05f, shoulderY, 0f))
        mesh += Mesh.sphere(0.17f, orangePrimary, latSegments = 5, lonSegments = 8)
            .transformed(Mat4.translation(shoulderX + 0.05f, shoulderY, 0f))

        // 4. Lengan berengsel dengan capit pincer 3-jari
        mesh += arm(-shoulderX - 0.05f, shoulderY, -swing * ARM_SWING_DEGREES)
        mesh += arm(shoulderX + 0.05f, shoulderY, swing * ARM_SWING_DEGREES)

        // 5. Leher silinder gelap pendek
        val neckY = torsoY + 0.42f
        mesh += Mesh.cylinder(0.12f, 0.12f, 0.12f, darkCharcoal, segments = 8)
            .transformed(Mat4.translation(0f, neckY, 0f))

        // 6. Kepala Kotak Membulat (TV-Head) Oranye
        val headY = neckY + 0.06f + HEAD_HEIGHT / 2f
        mesh += Mesh.box(HEAD_WIDTH, HEAD_HEIGHT, HEAD_DEPTH, orangePrimary)
            .transformed(Mat4.translation(0f, headY, 0f))
        mesh += Mesh.box(HEAD_WIDTH * 0.94f, HEAD_HEIGHT * 0.94f, HEAD_DEPTH * 1.04f, orangePrimary)
            .transformed(Mat4.translation(0f, headY, 0f))

        // Ear Muffs / Silinder telinga gelap di kiri & kanan kepala
        val earX = HEAD_WIDTH / 2f + 0.04f
        mesh += Mesh.cylinder(0.15f, 0.15f, 0.08f, darkCharcoal, segments = 8)
            .transformed(Mat4.translation(-earX, headY, 0f) * Mat4.rotationZ(90f))
        mesh += Mesh.cylinder(0.15f, 0.15f, 0.08f, darkCharcoal, segments = 8)
            .transformed(Mat4.translation(earX, headY, 0f) * Mat4.rotationZ(90f))

        // Dua Layar Mata TV rounded square cyan berdampingan
        val eyeCol = if (state.currentAction == RobotAction.CROUCH) Color(0xFFFF5A6E) else eyeCyan
        val eyeZ = HEAD_DEPTH / 2f + 0.02f
        val eyeSpacing = 0.20f

        // Mata kiri
        mesh += Mesh.box(0.26f, 0.25f, 0.04f, eyeSocket)
            .transformed(Mat4.translation(-eyeSpacing, headY + 0.02f, eyeZ))
        mesh += Mesh.box(0.22f, 0.21f, 0.05f, eyeCol, emissiveFront = true)
            .transformed(Mat4.translation(-eyeSpacing, headY + 0.02f, eyeZ + 0.01f))

        // Mata kanan
        mesh += Mesh.box(0.26f, 0.25f, 0.04f, eyeSocket)
            .transformed(Mat4.translation(eyeSpacing, headY + 0.02f, eyeZ))
        mesh += Mesh.box(0.22f, 0.21f, 0.05f, eyeCol, emissiveFront = true)
            .transformed(Mat4.translation(eyeSpacing, headY + 0.02f, eyeZ + 0.01f))

        return mesh
    }

    /** Lengan pendek & berengsel dengan capit mekanis 3-jari di ujungnya. */
    private fun arm(shoulderX: Float, shoulderY: Float, swingDegrees: Float): Mesh {
        val local = Mesh.box(0.14f, 0.16f, 0.14f, darkCharcoal)
            .transformed(Mat4.translation(0f, -0.10f, 0f)) +
            // Engsel siku perak
            Mesh.box(0.14f, 0.10f, 0.14f, metalSilver)
                .transformed(Mat4.translation(0f, -0.22f, 0f)) +
            // Lengan bawah oranye melengkung/tapered
            Mesh.cylinder(0.15f, 0.11f, 0.26f, orangePrimary, segments = 8)
                .transformed(Mat4.translation(0f, -0.38f, 0f)) +
            // Gelang pergelangan tangan
            Mesh.box(0.15f, 0.06f, 0.15f, darkCharcoal)
                .transformed(Mat4.translation(0f, -0.53f, 0f)) +
            // Capit tangan mekanis 3-jari (Pincer claws)
            Mesh.box(0.04f, 0.14f, 0.04f, metalSilver)
                .transformed(Mat4.translation(-0.06f, -0.62f, 0f) * Mat4.rotationZ(28f)) +
            Mesh.box(0.04f, 0.14f, 0.04f, metalSilver)
                .transformed(Mat4.translation(0.05f, -0.62f, 0.04f) * Mat4.rotationZ(-18f)) +
            Mesh.box(0.04f, 0.14f, 0.04f, metalSilver)
                .transformed(Mat4.translation(0.05f, -0.62f, -0.04f) * Mat4.rotationZ(-18f))

        return local.transformed(
            Mat4.translation(shoulderX, shoulderY, 0f) * Mat4.rotationX(swingDegrees)
        )
    }

    /**
     * Kaki robot berengsel dengan sepatu bot oranye melebar ke bawah (flared boots),
     * telapak tetap menapak lantai (y = 0) baik berdiri maupun jongkok.
     */
    private fun legs(hipY: Float, swing: Float): Mesh {
        val shinHeight = (hipY - FOOT_HEIGHT).coerceAtLeast(0.08f)
        val bootHeight = (shinHeight - 0.14f).coerceAtLeast(0.10f)

        fun leg(hipX: Float, swingDegrees: Float): Mesh {
            val local = Mesh.box(0.14f, 0.12f, 0.14f, metalSilver)
                .transformed(Mat4.translation(0f, -0.06f, 0f)) +
                // Engsel lutut perak
                Mesh.box(0.15f, 0.10f, 0.15f, metalSilver)
                    .transformed(Mat4.translation(0f, -0.15f, 0f)) +
                // Sepatu bot oranye khas mengembang ke bawah (flared boots)
                Mesh.cylinder(0.15f, 0.28f, bootHeight, orangePrimary, segments = 8)
                    .transformed(Mat4.translation(0f, -0.15f - bootHeight / 2f, 0.02f)) +
                // Sol telapak kaki abu-abu bulat menapak lantai y = 0
                Mesh.box(0.38f, FOOT_HEIGHT, 0.52f, soleGray)
                    .transformed(Mat4.translation(0f, -shinHeight - FOOT_HEIGHT / 2f, 0.08f))

            return local.transformed(
                Mat4.translation(hipX, hipY, 0f) * Mat4.rotationX(swingDegrees)
            )
        }

        return leg(-0.24f, swing * LEG_SWING_DEGREES) + leg(0.24f, -swing * LEG_SWING_DEGREES)
    }

    private const val ARM_SWING_DEGREES = 20f
    private const val LEG_SWING_DEGREES = 18f
}
