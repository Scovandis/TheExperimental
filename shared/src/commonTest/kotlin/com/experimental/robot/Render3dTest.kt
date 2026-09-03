package com.experimental.robot

import androidx.compose.ui.graphics.Color
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.RobotState
import com.experimental.robot.presentation.render.Camera
import com.experimental.robot.presentation.render.Mat4
import com.experimental.robot.presentation.render.Mesh
import com.experimental.robot.presentation.render.RobotMeshBuilder
import com.experimental.robot.presentation.render.Vec3
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Render3dTest {

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 1e-4f) {
        assertTrue(
            abs(expected - actual) <= tolerance,
            "diharapkan $expected, ternyata $actual",
        )
    }

    @Test
    fun rotasi_y_90_derajat_memutar_sumbu_x_ke_minus_z() {
        val rotated = Mat4.rotationY(90f).transform(Vec3(1f, 0f, 0f))
        assertClose(0f, rotated.x)
        assertClose(0f, rotated.y)
        assertClose(-1f, rotated.z)
    }

    @Test
    fun komposisi_matriks_menerapkan_operan_kanan_lebih_dulu() {
        // Geser dulu ke (1,0,0), baru diputar 90 derajat -> (0,0,-1).
        val transform = Mat4.rotationY(90f) * Mat4.translation(1f, 0f, 0f)
        val result = transform.transform(Vec3.ZERO)
        assertClose(0f, result.x)
        assertClose(-1f, result.z)
    }

    @Test
    fun target_kamera_selalu_jatuh_di_pusat_layar() {
        // Berlaku untuk kombinasi azimut/pitch apa pun, bukan hanya kamera tegak.
        listOf(
            Camera(),
            Camera(azimuthDegrees = 0f, pitchDegrees = 0f),
            Camera(azimuthDegrees = 65f, pitchDegrees = 30f, distance = 4f),
            Camera(azimuthDegrees = -140f, pitchDegrees = -18f, distance = 9f),
        ).forEach { camera ->
            val projected = camera.viewMatrix().transform(camera.target)
            assertClose(0f, projected.x, tolerance = 1e-3f)
            assertClose(0f, projected.y, tolerance = 1e-3f)
            assertClose(-camera.distance, projected.z, tolerance = 1e-3f)
        }
    }

    @Test
    fun titik_di_depan_kamera_punya_z_negatif() {
        val origin = Camera().viewMatrix().transform(Vec3.ZERO)
        assertTrue(origin.z < 0f, "objek di depan kamera harus berada di -Z ruang view")
    }

    @Test
    fun posisi_kamera_berjarak_tepat_dari_target() {
        val camera = Camera(distance = 7f, azimuthDegrees = -24f, pitchDegrees = 12f)
        assertClose(7f, (camera.position - camera.target).length, tolerance = 1e-3f)
        assertTrue(camera.position.y > camera.target.y, "pitch positif menaruh kamera di atas target")
        assertTrue(camera.position.x < 0f, "azimut negatif menaruh kamera di sisi kiri")
    }

    @Test
    fun setiap_sisi_balok_menghadap_keluar() {
        val box = Mesh.box(2f, 2f, 2f, Color.White)
        assertEquals(8, box.vertices.size)
        assertEquals(6, box.faces.size)

        box.faces.forEach { face ->
            val corners = face.indices.map { box.vertices[it] }
            val normal = (corners[1] - corners[0]) cross (corners[2] - corners[1])
            val centroid = corners.fold(Vec3.ZERO) { acc, v -> acc + v } * (1f / corners.size)
            // Untuk balok berpusat di titik asal, normal luar harus searah dengan centroid.
            assertTrue(
                (normal dot centroid) > 0f,
                "winding sisi tidak konsisten ke luar: ${face.indices.toList()}",
            )
        }
    }

    @Test
    fun setiap_sisi_bola_menghadap_keluar() {
        val sphere = Mesh.sphere(radius = 1.5f, color = Color.White, latSegments = 6, lonSegments = 8)

        sphere.faces.forEach { face ->
            val corners = face.indices.map { sphere.vertices[it] }
            val normal = (corners[1] - corners[0]) cross (corners[2] - corners[1])
            val centroid = corners.fold(Vec3.ZERO) { acc, v -> acc + v } * (1f / corners.size)
            assertTrue(
                (normal dot centroid) > 0f,
                "winding sisi bola tidak konsisten ke luar: ${face.indices.toList()}",
            )
        }
    }

    @Test
    fun bola_memiliki_jumlah_sisi_sesuai_segmentasi() {
        val sphere = Mesh.sphere(radius = 1f, color = Color.White, latSegments = 4, lonSegments = 6)
        // 2 kutub (kipas 6 segitiga tiap kutub) + (4-2) pita tengah x 6 quad.
        assertEquals(6 + 6 + 2 * 6, sphere.faces.size)
    }

    @Test
    fun ekstrusi_segitiga_menghasilkan_delapan_sisi_menghadap_keluar() {
        val triangle = listOf(-0.5f to -0.4f, 0.5f to -0.4f, 0f to 0.6f)
        val badge = Mesh.extrudedPolygon(triangle, depth = 0.1f, color = Color.White)

        // 1 depan + 1 belakang + 3 samping = 5 sisi untuk poligon 3 titik.
        assertEquals(5, badge.faces.size)

        badge.faces.forEach { face ->
            val corners = face.indices.map { badge.vertices[it] }
            val normal = (corners[1] - corners[0]) cross (corners[2] - corners[1])
            val centroid = corners.fold(Vec3.ZERO) { acc, v -> acc + v } * (1f / corners.size)
            assertTrue(
                (normal dot centroid) > 0f,
                "winding sisi ekstrusi tidak konsisten ke luar: ${face.indices.toList()}",
            )
        }
    }

    @Test
    fun penggabungan_mesh_menggeser_indeks_vertex() {
        val merged = Mesh.box(1f, 1f, 1f, Color.White) + Mesh.box(1f, 1f, 1f, Color.Red)
        assertEquals(16, merged.vertices.size)
        assertEquals(12, merged.faces.size)
        assertTrue(merged.faces.all { face -> face.indices.all { it in merged.vertices.indices } })
    }

    @Test
    fun rotasi_gestur_memutar_model_robot_secara_nyata() {
        val idle = RobotMeshBuilder.build(RobotState(), Color.Cyan)
        val turned = RobotMeshBuilder.build(RobotState(rotationY = 90f), Color.Cyan)

        val idleWidth = idle.vertices.maxOf { it.x } - idle.vertices.minOf { it.x }
        val turnedWidth = turned.vertices.maxOf { it.x } - turned.vertices.minOf { it.x }
        val turnedDepth = turned.vertices.maxOf { it.z } - turned.vertices.minOf { it.z }

        // Setelah diputar 90 derajat, lebar bahu berpindah menjadi kedalaman.
        assertTrue(turnedWidth < idleWidth, "lebar seharusnya menyusut saat menyamping")
        assertClose(idleWidth, turnedDepth, tolerance = 0.01f)
    }

    @Test
    fun maju_menggeser_model_mendekat_ke_kamera() {
        val idle = RobotMeshBuilder.build(RobotState(), Color.Cyan)
        val forward = RobotMeshBuilder.build(RobotState(positionZ = 55f), Color.Cyan)
        val idleZ = idle.vertices.map { it.z }.average()
        val forwardZ = forward.vertices.map { it.z }.average()
        assertTrue(forwardZ > idleZ, "positionZ positif harus mendekat ke kamera (+Z)")
    }

    @Test
    fun jongkok_menurunkan_kepala_tapi_kaki_tetap_menapak() {
        val standing = RobotMeshBuilder.build(RobotState(scaleY = 1f), Color.Cyan)
        val crouching = RobotMeshBuilder.build(
            RobotState(scaleY = 0.6f, currentAction = RobotAction.CROUCH),
            Color.Cyan,
        )

        assertTrue(crouching.vertices.maxOf { it.y } < standing.vertices.maxOf { it.y })
        assertClose(
            standing.vertices.minOf { it.y },
            crouching.vertices.minOf { it.y },
            tolerance = 0.02f,
        )
    }
}
