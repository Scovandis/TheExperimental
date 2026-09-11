package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.FingerState
import com.experimental.robot.domain.model.RobotAction

/**
 * Pemetaan identitas jari yang terbuka ke aksi robot - sama seperti berhitung
 * dengan tangan, bukan posisi/kemiringan tangan di frame:
 *
 * - 0 jari (kepalan)                       -> IDLE
 * - 1 jari (telunjuk)                      -> MAJU
 * - 2 jari (telunjuk + tengah)              -> MUNDUR
 * - 3 jari (+ manis)                        -> PUTAR KIRI
 * - 4 jari (+ kelingking, tanpa jempol)      -> PUTAR KANAN
 * - 5 jari (+ jempol, telapak penuh)         -> JONGKOK
 *
 * [thumb] bernilai `null` berarti status jempol **tidak diperhitungkan** untuk pola itu.
 * Deteksi jempol jauh lebih rapuh daripada 4 jari panjang (dihitung dari rasio jarak ke
 * pergelangan, bukan sekadar tip-vs-PIP - lihat [FingerExtensionDetector]), dan secara alami
 * orang tidak menekuk jempol rapat saat menunjukkan 1-3 jari. Mewajibkan jempol tertutup
 * persis untuk pola itu membuat gestur nyaris tidak pernah cocok di dunia nyata dan selalu
 * jatuh ke IDLE. Jempol baru benar-benar dibutuhkan untuk membedakan PUTAR KANAN (4 jari)
 * dari JONGKOK (5 jari), yang identitas 4 jari panjangnya sama persis.
 *
 * Dipakai sebagai satu-satunya sumber kebenaran oleh [HandGestureClassifier] (menentukan
 * aksi) dan [GestureConfidenceScorer] (menilai seberapa yakin pose cocok dengan aksi itu),
 * supaya keduanya tidak pernah bisa saling melenceng.
 */
internal data class GesturePattern(
    val action: RobotAction,
    val thumb: Boolean?,
    val index: Boolean,
    val middle: Boolean,
    val ring: Boolean,
    val pinky: Boolean,
) {
    fun matches(fingers: FingerState): Boolean =
        (thumb == null || fingers.thumb == thumb) &&
            fingers.index == index &&
            fingers.middle == middle &&
            fingers.ring == ring &&
            fingers.pinky == pinky

    companion object {
        val ALL: List<GesturePattern> = listOf(
            GesturePattern(RobotAction.CROUCH, thumb = true, index = true, middle = true, ring = true, pinky = true),
            GesturePattern(RobotAction.ROTATE_RIGHT, thumb = false, index = true, middle = true, ring = true, pinky = true),
            GesturePattern(RobotAction.ROTATE_LEFT, thumb = null, index = true, middle = true, ring = true, pinky = false),
            GesturePattern(RobotAction.MOVE_BACKWARD, thumb = null, index = true, middle = true, ring = false, pinky = false),
            GesturePattern(RobotAction.MOVE_FORWARD, thumb = null, index = true, middle = false, ring = false, pinky = false),
            GesturePattern(RobotAction.IDLE, thumb = null, index = false, middle = false, ring = false, pinky = false),
        )

        /** Pola persis untuk [action]. Aman dipakai tanpa null-check: setiap [RobotAction] ada di [ALL]. */
        fun of(action: RobotAction): GesturePattern = ALL.first { it.action == action }
    }
}
