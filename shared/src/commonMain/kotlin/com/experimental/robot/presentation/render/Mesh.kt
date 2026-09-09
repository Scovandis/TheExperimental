package com.experimental.robot.presentation.render

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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

        /**
         * Bola UV berpusat di titik asal, dipakai untuk kepala/helm bulat dan sendi bahu.
         *
         * Kutub digambar sebagai kipas segitiga (bukan quad degenerate) agar normalnya
         * tidak pernah nol; pita tengah dipetakan sebagai quad per segmen lintang-bujur.
         */
        fun sphere(
            radius: Float,
            color: Color,
            latSegments: Int = 8,
            lonSegments: Int = 14,
        ): Mesh {
            require(latSegments >= 2 && lonSegments >= 3)

            val vertices = mutableListOf<Vec3>()
            val topPole = 0
            vertices += Vec3(0f, radius, 0f)

            val ringStart = IntArray(latSegments - 1)
            for (ring in 1 until latSegments) {
                val theta = PI.toFloat() * ring / latSegments
                val y = radius * cos(theta)
                val ringRadius = radius * sin(theta)
                ringStart[ring - 1] = vertices.size
                for (lon in 0 until lonSegments) {
                    val phi = 2f * PI.toFloat() * lon / lonSegments
                    vertices += Vec3(ringRadius * cos(phi), y, ringRadius * sin(phi))
                }
            }
            val bottomPole = vertices.size
            vertices += Vec3(0f, -radius, 0f)

            val faces = mutableListOf<Face>()

            val firstRing = ringStart.first()
            for (lon in 0 until lonSegments) {
                val a = firstRing + lon
                val b = firstRing + (lon + 1) % lonSegments
                faces += Face(intArrayOf(topPole, b, a), color)
            }

            for (ring in 0 until ringStart.size - 1) {
                val current = ringStart[ring]
                val next = ringStart[ring + 1]
                for (lon in 0 until lonSegments) {
                    val nextLon = (lon + 1) % lonSegments
                    faces += Face(
                        intArrayOf(current + nextLon, next + nextLon, next + lon, current + lon),
                        color,
                    )
                }
            }

            val lastRing = ringStart.last()
            for (lon in 0 until lonSegments) {
                val a = lastRing + lon
                val b = lastRing + (lon + 1) % lonSegments
                faces += Face(intArrayOf(bottomPole, a, b), color)
            }

            return Mesh(vertices, faces)
        }

        /**
         * Ekstrusi poligon 2D cembung sepanjang sumbu Z, dipakai untuk lencana dada
         * berbentuk segitiga/perisai yang tidak bisa dibuat dari balok.
         *
         * @param points2D titik poligon di bidang XY berurutan berlawanan arah jam
         *        dilihat dari +Z (sisi depan), dan berpusat mendekati titik asal.
         */
        fun extrudedPolygon(
            points2D: List<Pair<Float, Float>>,
            depth: Float,
            color: Color,
            emissiveFront: Boolean = false,
        ): Mesh {
            require(points2D.size >= 3)
            val n = points2D.size
            val hz = depth / 2f

            val front = points2D.map { (x, y) -> Vec3(x, y, hz) }
            val back = points2D.map { (x, y) -> Vec3(x, y, -hz) }
            val vertices = front + back

            val faces = mutableListOf<Face>()

            // Sisi depan (+Z): kipas segitiga, urutan asli sudah CCW dilihat dari +Z.
            for (i in 1 until n - 1) {
                faces += Face(intArrayOf(0, i, i + 1), color, emissive = emissiveFront)
            }
            // Sisi belakang (-Z): urutan dibalik agar normal menghadap -Z.
            val backOffset = n
            for (i in 1 until n - 1) {
                faces += Face(
                    intArrayOf(backOffset, backOffset + i + 1, backOffset + i),
                    color,
                )
            }
            // Sisi samping: satu quad per tepi poligon.
            for (i in 0 until n) {
                val next = (i + 1) % n
                faces += Face(
                    intArrayOf(backOffset + i, backOffset + next, next, i),
                    color,
                )
            }

            return Mesh(vertices, faces)
        }

        /** Silinder atau kerucut terpancung (frustum) sepanjang sumbu Y. */
        fun cylinder(
            radiusTop: Float,
            radiusBottom: Float,
            height: Float,
            color: Color,
            segments: Int = 8,
        ): Mesh {
            val vertices = mutableListOf<Vec3>()
            val hy = height / 2f
            val twoPi = (2 * PI).toFloat()

            for (i in 0 until segments) {
                val phi = i * twoPi / segments
                vertices += Vec3(radiusTop * cos(phi), hy, radiusTop * sin(phi))
            }
            for (i in 0 until segments) {
                val phi = i * twoPi / segments
                vertices += Vec3(radiusBottom * cos(phi), -hy, radiusBottom * sin(phi))
            }
            val topCenter = vertices.size
            vertices += Vec3(0f, hy, 0f)
            val bottomCenter = vertices.size
            vertices += Vec3(0f, -hy, 0f)

            val faces = mutableListOf<Face>()
            for (i in 0 until segments) {
                val next = (i + 1) % segments
                val topA = i
                val topB = next
                val botA = segments + i
                val botB = segments + next
                faces += Face(intArrayOf(topA, topB, botB, botA), color)
            }
            for (i in 0 until segments) {
                val next = (i + 1) % segments
                faces += Face(intArrayOf(topCenter, next, i), color)
            }
            for (i in 0 until segments) {
                val next = (i + 1) % segments
                faces += Face(intArrayOf(bottomCenter, segments + i, segments + next), color)
            }
            return Mesh(vertices, faces)
        }
    }
}
