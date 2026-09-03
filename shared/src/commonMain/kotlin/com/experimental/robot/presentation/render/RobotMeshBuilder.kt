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

    // Palet warna XR-07: badan titanium hampir hitam + aksen ungu (lihat design reference).
    private val shellColor = Color(0xFF17171F)      // panel luar torso/lengan/kaki
    private val shellDark = Color(0xFF0D0D11)        // pinggul & recess, sesuai swatch #0D0D11
    private val helmetColor = Color(0xFF1C1C26)      // kepala, sedikit lebih terang agar terbaca bulat
    private val jointColor = Color(0xFF0A0A0D)       // sendi & telapak kaki, paling gelap
    private val visorColor = Color(0xFF05050A)       // panel wajah gelap di sekitar mata
    private val accentPurple = Color(0xFF7B2CF7)     // #7B2CF7 dari swatch — lencana dada
    private val glowLavender = Color(0xFFC39BFF)     // #C39BFF dari swatch — cincin cahaya & sol kaki

    private const val HIP_Y = 0.62f
    private const val FOOT_HEIGHT = 0.16f
    private const val HEAD_RADIUS = 0.40f

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

    private fun upperBody(state: RobotState, accent: Color, swing: Float): Mesh {
        val hipDrop = HIP_Y * (1f - state.scaleY)
        val lift = -hipDrop
        var mesh = Mesh.EMPTY

        // Pinggul rendah & lebar, dasar dari siluet chibi.
        mesh += Mesh.box(0.62f, 0.24f, 0.46f, shellDark)
            .transformed(Mat4.translation(0f, HIP_Y + lift, 0f))

        // Torso pendek dan tegap (proporsi chibi: badan jauh lebih pendek dari kepala).
        val torsoY = HIP_Y + 0.5f + lift
        mesh += Mesh.box(0.86f, 0.76f, 0.5f, shellColor)
            .transformed(Mat4.translation(0f, torsoY, 0f))

        // Lencana dada segitiga terbalik (logo "V" pada referensi), memancarkan cahaya ungu.
        // Urutan titik CCW dilihat dari +Z (sisi depan) agar sisi emissive menghadap kamera.
        val badgeTriangle = listOf(0.13f to 0.11f, -0.13f to 0.11f, 0f to -0.14f)
        mesh += Mesh.extrudedPolygon(badgeTriangle, depth = 0.05f, color = accentPurple, emissiveFront = true)
            .transformed(Mat4.translation(0f, torsoY + 0.02f, 0.26f))

        // Bahu bulat (ball joint) menonjol di sisi torso.
        val shoulderY = torsoY + 0.3f
        val shoulderX = 0.5f
        mesh += Mesh.sphere(0.19f, shellColor, latSegments = 6, lonSegments = 10)
            .transformed(Mat4.translation(-shoulderX, shoulderY, 0f))
        mesh += Mesh.sphere(0.19f, shellColor, latSegments = 6, lonSegments = 10)
            .transformed(Mat4.translation(shoulderX, shoulderY, 0f))

        // Lengan pendek & tebal, berayun berlawanan fase dengan kaki.
        mesh += arm(-shoulderX, shoulderY, -swing * ARM_SWING_DEGREES)
        mesh += arm(shoulderX, shoulderY, swing * ARM_SWING_DEGREES)

        // Leher pendek menghubungkan torso ke kepala besar.
        val neckY = torsoY + 0.42f
        mesh += Mesh.box(0.22f, 0.12f, 0.22f, jointColor)
            .transformed(Mat4.translation(0f, neckY, 0f))

        // Kepala: bola besar sedikit oval, ciri khas desain (helm bulat penuh).
        val headY = neckY + HEAD_RADIUS * 0.92f
        mesh += Mesh.sphere(HEAD_RADIUS, helmetColor, latSegments = 9, lonSegments = 16)
            .transformed(Mat4.translation(0f, headY, 0f) * Mat4.scale(1.06f, 0.94f, 1.0f))

        // Panel wajah gelap sebagai dudukan mata (efek "visor" pada referensi).
        mesh += Mesh.box(0.44f, 0.24f, 0.06f, visorColor)
            .transformed(Mat4.translation(0f, headY + 0.02f, HEAD_RADIUS * 0.9f))

        // Mata LED kotak membulat, warnanya mengikuti aksi (idle = lavender khas XR-07).
        val eyeColor = if (state.currentAction == RobotAction.CROUCH) Color(0xFFFF5A6E) else accent
        val eyeZ = HEAD_RADIUS * 0.94f
        mesh += Mesh.box(0.13f, 0.13f, 0.05f, eyeColor, emissiveFront = true)
            .transformed(Mat4.translation(-0.15f, headY, eyeZ))
        mesh += Mesh.box(0.13f, 0.13f, 0.05f, eyeColor, emissiveFront = true)
            .transformed(Mat4.translation(0.15f, headY, eyeZ))

        // Lampu pelipis di sisi kiri/kanan kepala (ciri khas referensi: aksen menyala di dekat telinga).
        val templeZ = HEAD_RADIUS * 0.35f
        val templeX = HEAD_RADIUS * 0.98f
        mesh += Mesh.box(0.05f, 0.16f, 0.05f, glowLavender, emissiveFront = true)
            .transformed(Mat4.translation(-templeX, headY, templeZ) * Mat4.rotationY(-90f))
        mesh += Mesh.box(0.05f, 0.16f, 0.05f, glowLavender, emissiveFront = true)
            .transformed(Mat4.translation(templeX, headY, templeZ) * Mat4.rotationY(90f))

        return mesh
    }

    /** Lengan pendek & tebal digantung dari bahu, lalu diputar pada engsel bahu. */
    private fun arm(shoulderX: Float, shoulderY: Float, swingDegrees: Float): Mesh {
        val local = Mesh.box(0.24f, 0.5f, 0.24f, shellColor)
            .transformed(Mat4.translation(0f, -0.26f, 0f)) +
            Mesh.sphere(0.15f, jointColor, latSegments = 5, lonSegments = 8)
                .transformed(Mat4.translation(0f, -0.52f, 0f)) +
            // Tangan kecil membulat di ujung lengan.
            Mesh.box(0.2f, 0.18f, 0.22f, shellDark)
                .transformed(Mat4.translation(0f, -0.66f, 0f))

        return local.transformed(
            Mat4.translation(shoulderX, shoulderY, 0f) * Mat4.rotationX(swingDegrees)
        )
    }

    /**
     * Kaki pendek & besar digantung dari pinggul; panjangnya selalu tepat setinggi
     * pinggul sehingga telapak tetap menapak lantai (y = 0) baik berdiri maupun jongkok.
     */
    private fun legs(hipY: Float, swing: Float): Mesh {
        val shinHeight = (hipY - FOOT_HEIGHT).coerceAtLeast(0.08f)

        fun leg(hipX: Float, swingDegrees: Float): Mesh {
            val local = Mesh.box(0.3f, shinHeight, 0.3f, shellColor)
                .transformed(Mat4.translation(0f, -shinHeight / 2f, 0f)) +
                // Telapak kaki besar dan membulat, ciri khas siluet chibi pada referensi.
                Mesh.box(0.36f, FOOT_HEIGHT, 0.56f, jointColor)
                    .transformed(Mat4.translation(0f, -shinHeight - FOOT_HEIGHT / 2f, 0.08f)) +
                // Strip cahaya di sol kaki (lampu ungu pada referensi).
                Mesh.box(0.3f, 0.02f, 0.42f, glowLavender, emissiveFront = true)
                    .transformed(Mat4.translation(0f, -shinHeight - FOOT_HEIGHT + 0.01f, 0.06f))

            return local.transformed(
                Mat4.translation(hipX, hipY, 0f) * Mat4.rotationX(swingDegrees)
            )
        }

        return leg(-0.22f, swing * LEG_SWING_DEGREES) + leg(0.22f, -swing * LEG_SWING_DEGREES)
    }

    private const val ARM_SWING_DEGREES = 20f
    private const val LEG_SWING_DEGREES = 18f
}
