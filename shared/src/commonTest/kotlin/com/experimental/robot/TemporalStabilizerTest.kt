package com.experimental.robot

import com.experimental.robot.domain.gesture.ConfidenceConfig
import com.experimental.robot.domain.gesture.ConfidenceTier
import com.experimental.robot.domain.gesture.TemporalStabilizer
import com.experimental.robot.domain.model.RobotAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemporalStabilizerTest {

    private val config = ConfidenceConfig()
    private fun stabilizer(holdMs: Long = 300L) = TemporalStabilizer(holdMs, config)

    @Test
    fun gestur_baru_belum_dipakai_sebelum_ambang_waktu_terlewati() {
        val stabilizer = stabilizer(holdMs = 300L)

        assertEquals(RobotAction.IDLE, stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 0L).action)
        assertEquals(RobotAction.IDLE, stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 200L).action)
        assertEquals(RobotAction.MOVE_FORWARD, stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 300L).action)
    }

    /**
     * Regresi terhadap pendahulunya yang berbasis jumlah frame: dua frame yang berjarak
     * 300 ms sudah cukup untuk mengunci. Perangkat lambat tidak lagi berarti latensi lambat.
     */
    @Test
    fun latensi_lock_tidak_bergantung_jumlah_frame() {
        val stabilizer = stabilizer(holdMs = 300L)

        stabilizer.submit(RobotAction.ROTATE_LEFT, 0.9f, 0L)
        val locked = stabilizer.submit(RobotAction.ROTATE_LEFT, 0.9f, 310L)

        assertEquals(RobotAction.ROTATE_LEFT, locked.action)
        assertEquals(ConfidenceTier.LOCKED, locked.tier)
    }

    @Test
    fun gestur_yang_berkedip_tidak_pernah_mengunci() {
        val stabilizer = stabilizer(holdMs = 300L)
        var now = 0L

        // Bolak-balik tiap 80 ms: tidak ada kandidat yang pernah bertahan 300 ms.
        repeat(10) { index ->
            val raw = if (index % 2 == 0) RobotAction.MOVE_FORWARD else RobotAction.ROTATE_RIGHT
            assertEquals(RobotAction.IDLE, stabilizer.submit(raw, 0.9f, now).action)
            now += 80L
        }
    }

    @Test
    fun keyakinan_di_bawah_ambang_kandidat_tidak_memajukan_timer() {
        val stabilizer = stabilizer(holdMs = 300L)

        // 0.6 masuk DETECTING: ditampilkan, tapi timer kandidat tidak jalan.
        stabilizer.submit(RobotAction.MOVE_FORWARD, 0.6f, 0L)
        val afterHold = stabilizer.submit(RobotAction.MOVE_FORWARD, 0.6f, 400L)

        assertEquals(ConfidenceTier.DETECTING, afterHold.tier)
        assertEquals(RobotAction.IDLE, afterHold.action)
    }

    @Test
    fun keyakinan_kolaps_melepas_lock_yang_sudah_ada() {
        val stabilizer = stabilizer(holdMs = 300L)
        stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 0L)
        assertEquals(RobotAction.MOVE_FORWARD, stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 320L).action)

        val collapsed = stabilizer.submit(RobotAction.MOVE_FORWARD, 0.2f, 340L)

        assertEquals(ConfidenceTier.UNKNOWN, collapsed.tier)
        assertEquals(RobotAction.IDLE, collapsed.action, "keyakinan di bawah 50% harus melepas lock")
    }

    /** Histeresis: lock bertahan di zona DETECTING supaya tidak lepas-ambil berulang. */
    @Test
    fun lock_bertahan_saat_keyakinan_turun_tapi_belum_kolaps() {
        val stabilizer = stabilizer(holdMs = 300L)
        stabilizer.submit(RobotAction.CROUCH, 0.9f, 0L)
        stabilizer.submit(RobotAction.CROUCH, 0.9f, 320L)

        val sagging = stabilizer.submit(RobotAction.CROUCH, 0.66f, 360L)

        assertEquals(RobotAction.CROUCH, sagging.action)
        assertEquals(ConfidenceTier.DETECTING, sagging.tier)
    }

    @Test
    fun kandidat_antara_75_dan_85_persen_tidak_boleh_mengunci() {
        val stabilizer = stabilizer(holdMs = 300L)

        stabilizer.submit(RobotAction.MOVE_BACKWARD, 0.80f, 0L)
        val afterHold = stabilizer.submit(RobotAction.MOVE_BACKWARD, 0.80f, 500L)

        assertEquals(ConfidenceTier.CANDIDATE, afterHold.tier)
        assertEquals(RobotAction.IDLE, afterHold.action)
        assertTrue(afterHold.progress >= 1f, "timer stabilitas tetap berjalan")
    }

    @Test
    fun progres_naik_secara_proporsional_terhadap_waktu() {
        val stabilizer = stabilizer(holdMs = 400L)

        stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 0L)
        val half = stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 200L)

        assertTrue(half.progress in 0.45f..0.55f, "progres di tengah jendela: ${half.progress}")
    }

    @Test
    fun reset_mengembalikan_ke_idle() {
        val stabilizer = stabilizer(holdMs = 300L)
        stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 0L)
        stabilizer.submit(RobotAction.MOVE_FORWARD, 0.9f, 320L)

        stabilizer.reset()

        assertEquals(RobotAction.IDLE, stabilizer.current)
    }
}
