package com.experimental.robot.presentation.render

import kotlin.math.sqrt

/** Vektor 3D untuk pipeline render (sistem tangan kanan: X kanan, Y atas, Z ke arah kamera). */
data class Vec3(val x: Float, val y: Float, val z: Float) {

    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)

    operator fun unaryMinus() = Vec3(-x, -y, -z)

    infix fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vec3) = Vec3(
        x = y * other.z - z * other.y,
        y = z * other.x - x * other.z,
        z = x * other.y - y * other.x,
    )

    val length: Float get() = sqrt(x * x + y * y + z * z)

    fun normalized(): Vec3 {
        val len = length
        return if (len < 1e-6f) ZERO else Vec3(x / len, y / len, z / len)
    }

    companion object {
        val ZERO = Vec3(0f, 0f, 0f)
    }
}
