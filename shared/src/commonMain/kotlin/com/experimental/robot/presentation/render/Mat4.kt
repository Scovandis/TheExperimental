package com.experimental.robot.presentation.render

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Matriks affine 4x4 row-major untuk transformasi model/view.
 *
 * Proyeksi perspektif tidak memakai matriks ini; pembagian perspektif dilakukan
 * di [SoftwareRenderer] agar mudah dibaca dan diuji.
 */
class Mat4(val m: FloatArray) {

    init {
        require(m.size == 16) { "Mat4 membutuhkan 16 elemen" }
    }

    /** Komposisi: `this * other` berarti [other] diterapkan lebih dulu. */
    operator fun times(other: Mat4): Mat4 {
        val r = FloatArray(16)
        for (row in 0 until 4) {
            for (col in 0 until 4) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += m[row * 4 + k] * other.m[k * 4 + col]
                }
                r[row * 4 + col] = sum
            }
        }
        return Mat4(r)
    }

    /** Transformasi titik (w = 1). */
    fun transform(v: Vec3): Vec3 = Vec3(
        x = m[0] * v.x + m[1] * v.y + m[2] * v.z + m[3],
        y = m[4] * v.x + m[5] * v.y + m[6] * v.z + m[7],
        z = m[8] * v.x + m[9] * v.y + m[10] * v.z + m[11],
    )

    companion object {
        fun identity() = Mat4(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            )
        )

        fun translation(x: Float, y: Float, z: Float) = Mat4(
            floatArrayOf(
                1f, 0f, 0f, x,
                0f, 1f, 0f, y,
                0f, 0f, 1f, z,
                0f, 0f, 0f, 1f,
            )
        )

        fun scale(x: Float, y: Float, z: Float) = Mat4(
            floatArrayOf(
                x, 0f, 0f, 0f,
                0f, y, 0f, 0f,
                0f, 0f, z, 0f,
                0f, 0f, 0f, 1f,
            )
        )

        fun rotationX(degrees: Float): Mat4 {
            val a = degrees.toRadians()
            val c = cos(a)
            val s = sin(a)
            return Mat4(
                floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, -s, 0f,
                    0f, s, c, 0f,
                    0f, 0f, 0f, 1f,
                )
            )
        }

        fun rotationY(degrees: Float): Mat4 {
            val a = degrees.toRadians()
            val c = cos(a)
            val s = sin(a)
            return Mat4(
                floatArrayOf(
                    c, 0f, s, 0f,
                    0f, 1f, 0f, 0f,
                    -s, 0f, c, 0f,
                    0f, 0f, 0f, 1f,
                )
            )
        }

        fun rotationZ(degrees: Float): Mat4 {
            val a = degrees.toRadians()
            val c = cos(a)
            val s = sin(a)
            return Mat4(
                floatArrayOf(
                    c, -s, 0f, 0f,
                    s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f,
                )
            )
        }

        private fun Float.toRadians(): Float = this * PI.toFloat() / 180f
    }
}
