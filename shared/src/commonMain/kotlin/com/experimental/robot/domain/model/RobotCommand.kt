package com.experimental.robot.domain.model

/**
 * Arah gerak yang diminta sebuah perintah.
 *
 * Sengaja dipisah dari [RobotAction]: [RobotAction] adalah *gestur* yang dikenali,
 * sedangkan [Direction] adalah *perintah* yang dieksekusi. Pemisahan inilah yang
 * memungkinkan satu gestur menghasilkan perintah berbeda tergantung posisi tangan.
 */
enum class Direction { NONE, FORWARD, BACKWARD, LEFT, RIGHT }

/**
 * Kontrak tunggal antara layer persepsi (kamera, gestur) dan layer eksekusi
 * (robot 3D, robot fisik).
 *
 * Semua yang di atas kontrak ini boleh diganti tanpa menyentuh yang di bawah, dan
 * sebaliknya. Robot 3D dan robot fisik membaca objek yang sama.
 *
 * @param speed 0f..1f, kontinu. Arah menentukan *mode*, speed menentukan *magnitude*.
 * @param rotation derajat per detik; negatif = kiri, positif = kanan.
 * @param crouch postur, bukan arah - bisa aktif bersamaan dengan [direction].
 * @param sequence nomor urut monoton; penerima wajib menolak paket yang mundur.
 * @param timestampMs waktu terbit, dipakai penerima untuk mendeteksi timeout.
 */
data class RobotCommand(
    val direction: Direction = Direction.NONE,
    val speed: Float = 0f,
    val rotation: Float = 0f,
    val crouch: Boolean = false,
    val durationMs: Long = 0L,
    val sequence: Long = 0L,
    val timestampMs: Long = 0L,
) {
    val isMoving: Boolean get() = direction != Direction.NONE && speed > 0f

    val isRotating: Boolean get() = rotation != 0f

    /** Tidak ada gerak sama sekali; crouch tidak dihitung sebagai gerak. */
    val isIdle: Boolean get() = !isMoving && !isRotating

    /**
     * Versi berhenti dari perintah ini, dengan [sequence] dan [timestampMs] dipertahankan
     * supaya penerima tetap melihat aliran nomor urut yang utuh saat safety memblokir.
     */
    fun halted(): RobotCommand = copy(
        direction = Direction.NONE,
        speed = 0f,
        rotation = 0f,
        crouch = false,
        durationMs = 0L,
    )

    companion object {
        val STOP = RobotCommand()
    }
}
