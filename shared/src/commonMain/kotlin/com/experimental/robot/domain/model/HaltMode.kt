package com.experimental.robot.domain.model

/**
 * Tingkat penghentian robot. Membedakan ketiganya penting untuk robot fisik:
 * satu bisa pulih sendiri, dua lainnya menuntut tindakan manusia.
 *
 * Perhatikan bahwa IDLE **bukan** bagian dari enum ini. IDLE adalah gestur yang
 * berarti "tidak ada perintah gerak" - robot tetap menerima perintah berikutnya.
 * [RUNNING] dengan perintah bernilai nol adalah representasi IDLE.
 */
enum class HaltMode {
    /** Perintah boleh dieksekusi. */
    RUNNING,

    /** Motor diperintahkan berhenti; pulih otomatis begitu penyebabnya hilang. */
    STOP,

    /** Motor berhenti dan perintah dikunci; keluar hanya lewat reset eksplisit. */
    EMERGENCY_STOP,

    /** Sama seperti [EMERGENCY_STOP], tapi dipicu sistem, bukan pengguna. */
    SAFETY_LOCK;

    /** Butuh reset manual untuk keluar. */
    val isLatched: Boolean get() = this == EMERGENCY_STOP || this == SAFETY_LOCK

    val blocksMovement: Boolean get() = this != RUNNING
}

/** Alasan sebuah perintah dihentikan; ditampilkan ke pengguna dan dicatat di log. */
enum class StopReason(val label: String) {
    NONE(label = "-"),
    NO_HAND(label = "Tidak ada tangan"),
    HAND_LOST(label = "Tangan hilang dari frame"),
    LOW_CONFIDENCE(label = "Keyakinan gestur terlalu rendah"),
    LOW_FRAME_RATE(label = "Laju deteksi terlalu rendah"),
    OBSTACLE(label = "Ada penghalang di depan"),
    BATTERY(label = "Baterai robot kritis"),
    LINK_LOST(label = "Koneksi ke robot hilang"),
    COMMAND_TIMEOUT(label = "Robot tidak merespons"),
    EMERGENCY(label = "Emergency stop"),
}
