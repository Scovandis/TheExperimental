package com.experimental.robot.data

/** Status pipeline pelacakan tangan yang ditampilkan ke UI. */
sealed interface TrackerStatus {
    /** Belum diinisialisasi. */
    data object Idle : TrackerStatus

    /** Izin kamera belum diberikan. */
    data object PermissionRequired : TrackerStatus

    /** Model sedang dimuat / kamera sedang dibuka. */
    data object Initializing : TrackerStatus

    /** Pipeline berjalan normal. */
    data object Running : TrackerStatus

    /** Platform ini tidak menyediakan pelacakan tangan (mis. desktop/iOS pada build ini). */
    data class Unsupported(val reason: String) : TrackerStatus

    /** Terjadi kegagalan; [message] aman ditampilkan ke pengguna. */
    data class Error(val message: String) : TrackerStatus
}
