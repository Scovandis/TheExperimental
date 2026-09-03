package com.experimental.robot.presentation.render

import androidx.compose.ui.graphics.Color

/**
 * Satu sisi poligon.
 *
 * Urutan [indices] selalu berlawanan arah jam (CCW) dilihat dari luar objek,
 * sehingga back-face culling di [SoftwareRenderer] dapat mengandalkan arah normal.
 *
 * @param emissive true untuk permukaan yang tidak terkena pencahayaan (LED mata, panel).
 */
data class Face(
    val indices: IntArray,
    val color: Color,
    val emissive: Boolean = false,
) {
    override fun equals(other: Any?): Boolean =
        other is Face && indices.contentEquals(other.indices) &&
            color == other.color && emissive == other.emissive

    override fun hashCode(): Int =
        (indices.contentHashCode() * 31 + color.hashCode()) * 31 + emissive.hashCode()
}

/** Kumpulan vertex + sisi dalam satu ruang koordinat. */
data class Mesh(
    val vertices: List<Vec3>,
    val faces: List<Face>,
) {
    /** Salinan mesh yang seluruh vertexnya ditransformasi oleh [matrix]. */
    fun transformed(matrix: Mat4): Mesh =
        copy(vertices = vertices.map(matrix::transform))

    /** Gabungkan mesh lain sambil menggeser indeks vertexnya. */
    operator fun plus(other: Mesh): Mesh {
        val offset = vertices.size
        return Mesh(
            vertices = vertices + other.vertices,
            faces = faces + other.faces.map { face ->
                face.copy(indices = IntArray(face.indices.size) { face.indices[it] + offset })
            },
        )
    }

    companion object {
        val EMPTY = Mesh(emptyList(), emptyList())

        /**
         * Balok berpusat di titik asal.
         *
         * @param emissiveFront true agar sisi depan (+Z) tidak terkena shading,
         *        dipakai untuk mata dan panel dada.
         */
        fun box(
            width: Float,
            height: Float,
            depth: Float,
            color: Color,
            emissiveFront: Boolean = false,
        ): Mesh {
            val hx = width / 2f
            val hy = height / 2f
            val hz = depth / 2f
            val vertices = listOf(
                Vec3(-hx, -hy, -hz), // 0
                Vec3(hx, -hy, -hz),  // 1
                Vec3(hx, hy, -hz),   // 2
                Vec3(-hx, hy, -hz),  // 3
                Vec3(-hx, -hy, hz),  // 4
                Vec3(hx, -hy, hz),   // 5
                Vec3(hx, hy, hz),    // 6
                Vec3(-hx, hy, hz),   // 7
            )
            val faces = listOf(
                Face(intArrayOf(4, 5, 6, 7), color, emissive = emissiveFront), // depan  (+Z)
                Face(intArrayOf(1, 0, 3, 2), color), // belakang (-Z)
                Face(intArrayOf(5, 1, 2, 6), color), // kanan    (+X)
                Face(intArrayOf(0, 4, 7, 3), color), // kiri     (-X)
                Face(intArrayOf(3, 7, 6, 2), color), // atas     (+Y)
                Face(intArrayOf(0, 1, 5, 4), color), // bawah    (-Y)
            )
            return Mesh(vertices, faces)
        }

        /** Quad horizontal di ketinggian [y], normal menghadap ke atas. */
        fun groundQuad(width: Float, depth: Float, y: Float, color: Color): Mesh {
            val hx = width / 2f
            val hz = depth / 2f
            return Mesh(
                vertices = listOf(
                    Vec3(-hx, y, -hz),
                    Vec3(-hx, y, hz),
                    Vec3(hx, y, hz),
                    Vec3(hx, y, -hz),
                ),
                faces = listOf(Face(intArrayOf(0, 1, 2, 3), color, emissive = true)),
            )
        }
    }
}
