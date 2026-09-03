package com.experimental.robot

import com.experimental.robot.domain.gesture.GestureDebouncer
import com.experimental.robot.domain.model.RobotAction
import kotlin.test.Test
import kotlin.test.assertEquals

class GestureDebouncerTest {

    @Test
    fun aksi_baru_butuh_beberapa_frame_sebelum_diterima() {
        val debouncer = GestureDebouncer(framesToConfirm = 3)

        assertEquals(RobotAction.IDLE, debouncer.submit(RobotAction.MOVE_FORWARD))
        assertEquals(RobotAction.IDLE, debouncer.submit(RobotAction.MOVE_FORWARD))
        assertEquals(RobotAction.MOVE_FORWARD, debouncer.submit(RobotAction.MOVE_FORWARD))
    }

    @Test
    fun gestur_berkedip_tidak_mengubah_aksi() {
        val debouncer = GestureDebouncer(framesToConfirm = 4)

        // Dua frame noise berbeda tidak pernah mencapai ambang konfirmasi.
        repeat(3) {
            debouncer.submit(RobotAction.CROUCH)
            debouncer.submit(RobotAction.ROTATE_LEFT)
        }
        assertEquals(RobotAction.IDLE, debouncer.current)
    }

    @Test
    fun progress_naik_mengikuti_streak_dan_penuh_saat_stabil() {
        val debouncer = GestureDebouncer(framesToConfirm = 4)

        debouncer.submit(RobotAction.ROTATE_RIGHT)
        assertEquals(0.25f, debouncer.progress)
        debouncer.submit(RobotAction.ROTATE_RIGHT)
        assertEquals(0.5f, debouncer.progress)

        repeat(2) { debouncer.submit(RobotAction.ROTATE_RIGHT) }
        assertEquals(RobotAction.ROTATE_RIGHT, debouncer.current)
        assertEquals(1f, debouncer.progress)
    }

    @Test
    fun reset_kembali_ke_idle() {
        val debouncer = GestureDebouncer(framesToConfirm = 2)
        repeat(2) { debouncer.submit(RobotAction.CROUCH) }
        assertEquals(RobotAction.CROUCH, debouncer.current)

        debouncer.reset()
        assertEquals(RobotAction.IDLE, debouncer.current)
    }
}
