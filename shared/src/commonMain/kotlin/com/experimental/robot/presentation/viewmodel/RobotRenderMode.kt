package com.experimental.robot.presentation.viewmodel

/** Mode visualisasi robot yang dipilih pengguna. */
enum class RobotRenderMode(val label: String) {
    /** Renderer 3D software (mesh balok + perspektif + shading). */
    THREE_D("3D"),

    /** Renderer siluet 2D (lebih ringan, berguna di perangkat kelas bawah). */
    TWO_D("2D");

    fun toggled(): RobotRenderMode = if (this == THREE_D) TWO_D else THREE_D
}
