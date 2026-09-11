package com.experimental.robot.domain.gesture

import com.experimental.robot.domain.model.HandFrame
import com.experimental.robot.domain.model.HandLandmarkIndex as L
import com.experimental.robot.domain.model.HandPoint
import com.experimental.robot.domain.model.RobotAction
import kotlin.math.sqrt

/**
 * Menghitung keyakinan gestur dari *margin* - seberapa jauh tiap jari melewati ambang
 * ekstensinya menuju status yang diminta pola aksi tersebut (lihat [GesturePattern]),
 * bukan sekadar lolos atau tidak.
 *
 * Pose yang persis di perbatasan menghasilkan skor mendekati nol, sehingga tidak
 * pernah terkunci; pose yang tegas menghasilkan skor mendekati satu. Inilah yang
 * membuat frame ambigu (tangan sedang berpindah pose) tersaring dengan sendirinya -
 * termasuk saat [HandGestureClassifier] jatuh ke IDLE karena kombinasi jari tidak
 * cocok pola manapun: jari yang menyimpang dari pola IDLE (kepalan) ikut menurunkan
 * skornya.
 *
 * Kejelasan dihitung sebagai **mata rantai terlemah** (`minOf`), bukan rata-rata: satu jari
 * yang ragu-ragu sudah cukup membuat seluruh gestur tidak yakin. Jempol ikut dihitung hanya
 * untuk pola yang benar-benar mensyaratkannya (lihat [GesturePattern.thumb]).
 */
class GestureConfidenceScorer(
    private val gestureConfig: GestureConfig = GestureConfig(),
    private val config: ConfidenceConfig = ConfidenceConfig(),
) {

    fun score(hand: HandFrame?, action: RobotAction): Float {
        if (hand == null || !hand.isValid) return 0f

        val presence = if (hand.confidence > 0f) {
            hand.confidence.coerceIn(0f, 1f)
        } else {
            config.neutralPresence
        }

        val pattern = GesturePattern.of(action)
        val parts = buildList {
            pattern.thumb?.let { add(thumbClarity(hand, it)) }
            add(fingerClarity(hand, L.INDEX_TIP, L.INDEX_PIP, pattern.index))
            add(fingerClarity(hand, L.MIDDLE_TIP, L.MIDDLE_PIP, pattern.middle))
            add(fingerClarity(hand, L.RING_TIP, L.RING_PIP, pattern.ring))
            add(fingerClarity(hand, L.PINKY_TIP, L.PINKY_PIP, pattern.pinky))
        }

        return (presence * parts.min()).coerceIn(0f, 1f)
    }

    /** Nol tepat di ambang ekstensi, satu bila margin jauh melewatinya ke arah [open] yang diminta. */
    private fun fingerClarity(hand: HandFrame, tip: Int, pip: Int, open: Boolean): Float {
        val margin = (hand[pip].y - gestureConfig.fingerExtensionMargin) - hand[tip].y
        return ramp(if (open) margin else -margin, config.extensionSpan)
    }

    /** Setara [fingerClarity], tapi untuk jempol yang dideteksi lewat rasio jarak ke pergelangan. */
    private fun thumbClarity(hand: HandFrame, open: Boolean): Float {
        val wrist = hand[L.WRIST]
        val tipDistance = distance(hand[L.THUMB_TIP], wrist)
        val ipThreshold = distance(hand[L.THUMB_IP], wrist) * gestureConfig.thumbExtensionRatio
        val margin = tipDistance - ipThreshold
        return ramp(if (open) margin else -margin, config.thumbSpan)
    }

    private fun distance(a: HandPoint, b: HandPoint): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun ramp(value: Float, span: Float): Float =
        if (span <= 0f) 0f else (value / span).coerceIn(0f, 1f)
}
