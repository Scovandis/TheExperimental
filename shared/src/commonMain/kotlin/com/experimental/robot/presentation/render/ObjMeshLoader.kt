package com.experimental.robot.presentation.render

import androidx.compose.ui.graphics.Color

/**
 * Parser Wavefront OBJ/MTL minimal untuk mengisi [Mesh] dari aset low-poly.
 *
 * Batasan yang disengaja (selaras dengan [SoftwareRenderer] yang flat-shading
 * tanpa tekstur/z-buffer):
 * - UV (`vt`) dan normal per-vertex (`vn`) dibaca tapi tidak dipakai; shading
 *   memakai normal per-sisi hasil cross product, sama seperti mesh prosedural.
 * - Warna hanya dari `Kd` di MTL per grup `usemtl`; tekstur (`map_Kd`) diabaikan.
 * - Baris `o`/`g`/`s`/komentar diabaikan; `mtllib` diabaikan (materialnya
 *   diteruskan terpisah lewat [materials] karena pembacaan file lintas
 *   platform ditangani pemanggil, bukan parser ini).
 */
object ObjMeshLoader {

    /** Hasil parse `.mtl`: nama material -> warna diffuse (Kd). */
    fun parseMaterials(mtlText: String): Map<String, Color> {
        val materials = mutableMapOf<String, Color>()
        var currentName: String? = null

        mtlText.lineSequence().forEach { rawLine ->
            val tokens = rawLine.substringBefore('#').trim().split(WHITESPACE)
            if (tokens.isEmpty() || tokens[0].isBlank()) return@forEach

            when (tokens[0]) {
                "newmtl" -> currentName = tokens.getOrNull(1)
                "Kd" -> {
                    val name = currentName ?: return@forEach
                    val r = tokens.getOrNull(1)?.toFloatOrNull() ?: return@forEach
                    val g = tokens.getOrNull(2)?.toFloatOrNull() ?: return@forEach
                    val b = tokens.getOrNull(3)?.toFloatOrNull() ?: return@forEach
                    materials[name] = Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f))
                }
            }
        }
        return materials
    }

    /**
     * Parse `.obj` menjadi [Mesh].
     *
     * @param materials nama material (dari `usemtl`) -> warna, biasanya hasil [parseMaterials].
     * @param fallbackColor dipakai bila sisi tidak pernah diberi `usemtl`.
     * @param scale faktor skala seragam, diterapkan sebelum [translate].
     * @param translate pergeseran setelah skala, untuk menaruh pivot di lokasi yang diinginkan.
     * @param emissiveMaterials nama material yang harus digambar tanpa shading (LED, panel).
     */
    fun parse(
        objText: String,
        materials: Map<String, Color> = emptyMap(),
        fallbackColor: Color = Color.Gray,
        scale: Float = 1f,
        translate: Vec3 = Vec3.ZERO,
        emissiveMaterials: Set<String> = emptySet(),
    ): Mesh {
        val vertices = mutableListOf<Vec3>()
        val faces = mutableListOf<Face>()
        var currentColor = fallbackColor
        var currentEmissive = false

        objText.lineSequence().forEach { rawLine ->
            val tokens = rawLine.substringBefore('#').trim().split(WHITESPACE)
            if (tokens.isEmpty() || tokens[0].isBlank()) return@forEach

            when (tokens[0]) {
                "v" -> {
                    val x = tokens.getOrNull(1)?.toFloatOrNull() ?: return@forEach
                    val y = tokens.getOrNull(2)?.toFloatOrNull() ?: return@forEach
                    val z = tokens.getOrNull(3)?.toFloatOrNull() ?: return@forEach
                    vertices += Vec3(x, y, z) * scale + translate
                }

                "usemtl" -> {
                    val name = tokens.getOrNull(1)
                    currentColor = materials[name] ?: fallbackColor
                    currentEmissive = name in emissiveMaterials
                }

                "f" -> {
                    val indices = tokens.drop(1).mapNotNull { it.toObjVertexIndex(vertices.size) }
                    if (indices.size >= 3) {
                        faces += Face(indices.toIntArray(), currentColor, currentEmissive)
                    }
                }

                // "o", "g", "s", "mtllib", "vt", "vn" — tidak relevan untuk flat shading.
            }
        }

        return Mesh(vertices, faces)
    }

    /** "12", "12/4", "12/4/7", "12//7" -> indeks 0-based; negatif = relatif dari akhir list. */
    private fun String.toObjVertexIndex(vertexCountSoFar: Int): Int? {
        val raw = substringBefore('/').toIntOrNull() ?: return null
        return if (raw < 0) vertexCountSoFar + raw else raw - 1
    }

    private val WHITESPACE = Regex("\\s+")
}
