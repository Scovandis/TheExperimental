package com.experimental.robot

import androidx.compose.ui.graphics.Color
import com.experimental.robot.presentation.render.ObjMeshLoader
import com.experimental.robot.presentation.render.Vec3
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val SAMPLE_MTL = """
newmtl body
Kd 0.0 0.678 0.710
newmtl eye
Kd 0.0 1.0 1.0
"""

// Segitiga sederhana + satu kotak (4 vertex, 1 sisi quad) menggunakan dua material berbeda.
private const val SAMPLE_OBJ = """
# komentar harus diabaikan
o RobotPart
v 0.0 0.0 0.0
v 1.0 0.0 0.0
v 0.0 1.0 0.0
usemtl body
f 1 2 3
v -0.5 -0.5 0.0
v 0.5 -0.5 0.0
v 0.5 0.5 0.0
v -0.5 0.5 0.0
usemtl eye
f 4/1/1 5/2/1 6/3/1 7/4/1
"""

class ObjMeshLoaderTest {

    @Test
    fun parseMaterials_membaca_warna_kd_per_nama() {
        val materials = ObjMeshLoader.parseMaterials(SAMPLE_MTL)
        assertEquals(Color(0.0f, 0.678f, 0.710f), materials["body"])
        assertEquals(Color(0.0f, 1.0f, 1.0f), materials["eye"])
    }

    @Test
    fun parse_menghasilkan_vertex_dan_sisi_sesuai_jumlah_di_file() {
        val materials = ObjMeshLoader.parseMaterials(SAMPLE_MTL)
        val mesh = ObjMeshLoader.parse(SAMPLE_OBJ, materials)

        assertEquals(7, mesh.vertices.size)
        assertEquals(2, mesh.faces.size)
        assertEquals(3, mesh.faces[0].indices.size)
        assertEquals(4, mesh.faces[1].indices.size)
    }

    @Test
    fun parse_menerapkan_warna_material_terakhir_yang_aktif() {
        val materials = ObjMeshLoader.parseMaterials(SAMPLE_MTL)
        val mesh = ObjMeshLoader.parse(SAMPLE_OBJ, materials)

        assertEquals(materials.getValue("body"), mesh.faces[0].color)
        assertEquals(materials.getValue("eye"), mesh.faces[1].color)
    }

    @Test
    fun parse_menandai_material_emissive_sesuai_daftar() {
        val materials = ObjMeshLoader.parseMaterials(SAMPLE_MTL)
        val mesh = ObjMeshLoader.parse(
            SAMPLE_OBJ,
            materials,
            emissiveMaterials = setOf("eye"),
        )

        assertTrue(!mesh.faces[0].emissive)
        assertTrue(mesh.faces[1].emissive)
    }

    @Test
    fun parse_indeks_face_berbasis_1_dikonversi_ke_0_based() {
        val obj = """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 1 2 3
        """.trimIndent()
        val mesh = ObjMeshLoader.parse(obj)
        assertEquals(intArrayOf(0, 1, 2).toList(), mesh.faces[0].indices.toList())
    }

    @Test
    fun parse_indeks_negatif_relatif_terhadap_vertex_terakhir() {
        val obj = """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f -3 -2 -1
        """.trimIndent()
        val mesh = ObjMeshLoader.parse(obj)
        assertEquals(intArrayOf(0, 1, 2).toList(), mesh.faces[0].indices.toList())
    }

    @Test
    fun parse_menerapkan_skala_dan_translasi() {
        val obj = "v 1 1 1"
        val mesh = ObjMeshLoader.parse(
            objText = obj + "\nf 1 1 1", // sisi dummy tidak relevan, hanya untuk menghindari parse kosong
            scale = 2f,
            translate = Vec3(1f, 0f, -1f),
        )
        val v = mesh.vertices.single()
        assertEquals(Vec3(3f, 2f, 1f), v)
    }

    @Test
    fun face_dengan_kurang_dari_tiga_indeks_valid_diabaikan() {
        val obj = """
            v 0 0 0
            v 1 0 0
            f 1 2
        """.trimIndent()
        val mesh = ObjMeshLoader.parse(obj)
        assertTrue(mesh.faces.isEmpty())
    }

    @Test
    fun material_tak_dikenal_jatuh_ke_fallback_color() {
        val obj = """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            usemtl tidak_ada
            f 1 2 3
        """.trimIndent()
        val mesh = ObjMeshLoader.parse(obj, fallbackColor = Color.Magenta)
        assertEquals(Color.Magenta, mesh.faces[0].color)
    }
}
