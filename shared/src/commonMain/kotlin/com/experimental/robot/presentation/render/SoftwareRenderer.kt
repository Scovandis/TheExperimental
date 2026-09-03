package com.experimental.robot.presentation.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Kamera orbit pinhole: mengelilingi [target] pada jarak, azimut, dan pitch tertentu.
 *
 * Karena kamera selalu diarahkan ke [target], titik tersebut jatuh tepat di tengah
 * canvas berapa pun pitch/azimutnya — jadi robot tidak pernah lari ke tepi layar.
 *
 * @param azimuthDegrees sudut mendatar; nilai negatif menggeser kamera ke kiri
 *        sehingga robot terlihat dari sudut 3/4 (kesan 3D lebih terasa).
 * @param pitchDegrees sudut pandang dari atas.
 * @param focalLength panjang fokus relatif terhadap dimensi acuan canvas.
 */
data class Camera(
    val target: Vec3 = Vec3(0f, 1.62f, 0f),
    val distance: Float = 6.4f,
    val azimuthDegrees: Float = -24f,
    val pitchDegrees: Float = 12f,
    val focalLength: Float = 1.3f,
) {
    val position: Vec3
        get() {
            val az = azimuthDegrees.toRadians()
            val pitch = pitchDegrees.toRadians()
            return Vec3(
                x = target.x + distance * sin(az) * cos(pitch),
                y = target.y + distance * sin(pitch),
                z = target.z + distance * cos(az) * cos(pitch),
            )
        }

    /**
     * Matriks world -> view: geser ke posisi kamera, lalu batalkan azimut dan pitch.
     * Hasilnya [target] berada tepat di (0, 0, -[distance]).
     */
    fun viewMatrix(): Mat4 {
        val pos = position
        return Mat4.rotationX(pitchDegrees) *
            Mat4.rotationY(-azimuthDegrees) *
            Mat4.translation(-pos.x, -pos.y, -pos.z)
    }

    private fun Float.toRadians(): Float = this * PI.toFloat() / 180f
}

/**
 * Renderer 3D software: proyeksi perspektif + back-face culling + painter's algorithm
 * + flat shading Lambert. Berjalan penuh di commonMain sehingga robot 3D tampil sama
 * di Android, Desktop, dan iOS tanpa engine grafis tambahan.
 */
class SoftwareRenderer(
    private val camera: Camera = Camera(),
    private val lightDirection: Vec3 = Vec3(-0.45f, 0.8f, 0.6f).normalized(),
    private val ambient: Float = 0.34f,
) {

    /** Sisi yang sudah siap digambar, beserta kedalaman untuk pengurutan. */
    private class Fragment(
        val path: Path,
        val color: Color,
        val depth: Float,
    )

    fun render(scope: DrawScope, mesh: Mesh, canvasSize: Size) {
        val view = camera.viewMatrix()
        val viewVertices = mesh.vertices.map(view::transform)
        val fragments = ArrayList<Fragment>(mesh.faces.size)

        mesh.faces.forEach { face ->
            val corners = face.indices.map { viewVertices[it] }

            // Buang sisi yang berada di belakang / terlalu dekat bidang kamera.
            if (corners.any { it.z > -NEAR_PLANE }) return@forEach

            val normal = faceNormal(corners)
            val centroid = corners.centroid()

            // Back-face culling: sisi terlihat bila normalnya mengarah ke kamera (titik asal view).
            if (normal dot -centroid <= 0f) return@forEach

            val shade = if (face.emissive) {
                1f
            } else {
                val lambert = normal.normalized() dot lightDirection
                (ambient + (1f - ambient) * lambert.coerceAtLeast(0f)).coerceIn(0f, 1f)
            }

            val path = Path()
            corners.forEachIndexed { index, corner ->
                val screen = project(corner, canvasSize)
                if (index == 0) path.moveTo(screen.x, screen.y) else path.lineTo(screen.x, screen.y)
            }
            path.close()

            fragments += Fragment(
                path = path,
                color = face.color.shaded(shade),
                depth = centroid.z,
            )
        }

        // Gambar dari yang terjauh (z paling negatif) ke yang terdekat.
        fragments.sortBy { it.depth }
        fragments.forEach { scope.drawPath(it.path, it.color) }
    }

    /** Gambar garis 3D (dipakai untuk grid lantai) dengan proyeksi yang sama. */
    fun renderLine(
        scope: DrawScope,
        from: Vec3,
        to: Vec3,
        color: Color,
        canvasSize: Size,
        strokeWidth: Float = 1.5f,
    ) {
        val view = camera.viewMatrix()
        val a = view.transform(from)
        val b = view.transform(to)
        if (a.z > -NEAR_PLANE || b.z > -NEAR_PLANE) return
        scope.drawLine(
            color = color,
            start = project(a, canvasSize),
            end = project(b, canvasSize),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }

    /** Proyeksi view-space -> koordinat layar (Y layar dibalik). */
    private fun project(point: Vec3, canvasSize: Size): Offset {
        // Dimensi acuan berbasis tinggi (dibatasi lebar) agar robot mengisi panggung
        // secara wajar baik di layar ponsel yang tinggi maupun jendela desktop yang lebar.
        val reference = min(canvasSize.height, canvasSize.width * 1.6f)
        val scale = camera.focalLength * reference / -point.z
        return Offset(
            x = canvasSize.width / 2f + point.x * scale,
            y = canvasSize.height / 2f - point.y * scale,
        )
    }

    private fun faceNormal(corners: List<Vec3>): Vec3 =
        (corners[1] - corners[0]) cross (corners[2] - corners[1])

    private fun List<Vec3>.centroid(): Vec3 {
        var acc = Vec3.ZERO
        forEach { acc += it }
        return acc * (1f / size)
    }

    private fun Color.shaded(factor: Float): Color =
        Color(red = red * factor, green = green * factor, blue = blue * factor, alpha = alpha)

    private companion object {
        const val NEAR_PLANE = 0.15f
    }
}
