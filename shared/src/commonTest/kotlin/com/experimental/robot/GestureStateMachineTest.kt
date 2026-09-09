package com.experimental.robot

import com.experimental.robot.domain.gesture.ConfidenceTier
import com.experimental.robot.domain.gesture.GestureMachineConfig
import com.experimental.robot.domain.gesture.GesturePhase
import com.experimental.robot.domain.gesture.GestureStateMachine
import com.experimental.robot.domain.gesture.PerceptionSnapshot
import com.experimental.robot.domain.model.HaltMode
import com.experimental.robot.domain.model.RobotAction
import com.experimental.robot.domain.model.StopReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GestureStateMachineTest {

    private val config = GestureMachineConfig(handLostStopMs = 300L, handLostLockMs = 2_000L)

    private fun machine() = GestureStateMachine(config)

    private fun locked(action: RobotAction, atMs: Long) = PerceptionSnapshot(
        handDetected = true,
        action = action,
        tier = ConfidenceTier.LOCKED,
        confidence = 0.95f,
        frameAtMs = atMs,
    )

    @Test
    fun kondisi_awal_adalah_stop_karena_belum_ada_tangan() {
        val state = machine().update(PerceptionSnapshot(), nowMs = 0L)

        assertEquals(GesturePhase.NO_HAND, state.phase)
        assertEquals(HaltMode.STOP, state.halt)
        assertEquals(StopReason.NO_HAND, state.reason)
    }

    @Test
    fun gestur_terkunci_melewati_stable_lalu_active() {
        val machine = machine()

        val first = machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)
        val second = machine.update(locked(RobotAction.MOVE_FORWARD, 16L), nowMs = 16L)

        assertEquals(GesturePhase.GESTURE_STABLE, first.phase)
        assertEquals(GesturePhase.COMMAND_ACTIVE, second.phase)
        assertEquals(HaltMode.RUNNING, second.halt)
        assertEquals(RobotAction.MOVE_FORWARD, second.action)
    }

    @Test
    fun keyakinan_nol_dengan_tangan_terlihat_tetap_menghentikan_robot() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)

        val state = machine.update(
            PerceptionSnapshot(handDetected = true, tier = ConfidenceTier.UNKNOWN, frameAtMs = 20L),
            nowMs = 20L,
        )

        assertEquals(HaltMode.STOP, state.halt)
        assertEquals(StopReason.LOW_CONFIDENCE, state.reason)
        assertEquals(RobotAction.IDLE, state.action)
    }

    @Test
    fun tangan_hilang_sebentar_masuk_release_bukan_stop() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)

        val state = machine.update(PerceptionSnapshot(handDetected = false), nowMs = 200L)

        assertEquals(GesturePhase.COMMAND_RELEASE, state.phase)
        assertEquals(HaltMode.RUNNING, state.halt, "jendela pendek: melambat halus, belum stop keras")
    }

    @Test
    fun tangan_hilang_lebih_dari_300ms_menghentikan_robot() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)

        val state = machine.update(PerceptionSnapshot(handDetected = false), nowMs = 350L)

        assertEquals(GesturePhase.NO_HAND, state.phase)
        assertEquals(HaltMode.STOP, state.halt)
        assertEquals(StopReason.HAND_LOST, state.reason)
        assertEquals(RobotAction.IDLE, state.action)
    }

    @Test
    fun tangan_hilang_lebih_dari_2_detik_mengunci_safety() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)

        val state = machine.update(PerceptionSnapshot(handDetected = false), nowMs = 2_100L)

        assertEquals(HaltMode.SAFETY_LOCK, state.halt)
        assertTrue(state.halt.isLatched)
    }

    @Test
    fun safety_lock_tidak_lepas_walau_tangan_kembali() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)
        machine.update(PerceptionSnapshot(handDetected = false), nowMs = 2_100L)

        val returned = machine.update(locked(RobotAction.MOVE_FORWARD, 2_200L), nowMs = 2_200L)

        assertEquals(HaltMode.SAFETY_LOCK, returned.halt, "kunci hanya lepas lewat reset manual")
    }

    /**
     * Kasus yang paling mudah terlewat: kamera macet, jadi `handDetected` tetap true
     * padahal frame-nya sudah basi. Karena umur tangan dihitung dari [PerceptionSnapshot.frameAtMs]
     * dan bukan dari saat update dipanggil, kasus ini tertangani oleh timer yang sama.
     */
    @Test
    fun kamera_macet_diperlakukan_sama_dengan_tangan_hilang() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)

        // Snapshot tidak pernah diperbarui: frameAtMs tetap 0 sementara waktu berjalan.
        val state = machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 400L)

        assertEquals(HaltMode.STOP, state.halt)
        assertEquals(StopReason.HAND_LOST, state.reason)
    }

    @Test
    fun gestur_darurat_mengunci_emergency_stop() {
        val machine = machine()
        machine.update(locked(RobotAction.MOVE_FORWARD, 0L), nowMs = 0L)

        val state = machine.update(
            locked(RobotAction.MOVE_FORWARD, 100L).copy(emergencyTriggered = true),
            nowMs = 100L,
        )

        assertEquals(HaltMode.EMERGENCY_STOP, state.halt)
        assertEquals(StopReason.EMERGENCY, state.reason)
        assertTrue(state.halt.isLatched)
    }

    @Test
    fun reset_melepas_kunci_darurat() {
        val machine = machine()
        machine.update(
            locked(RobotAction.MOVE_FORWARD, 0L).copy(emergencyTriggered = true),
            nowMs = 0L,
        )

        machine.reset()
        val state = machine.update(locked(RobotAction.MOVE_FORWARD, 10L), nowMs = 10L)

        assertEquals(HaltMode.RUNNING, state.halt)
        assertEquals(GesturePhase.GESTURE_STABLE, state.phase)
    }

    @Test
    fun kandidat_belum_boleh_menerbitkan_perintah_penuh_tapi_belum_stop() {
        val state = machine().update(
            PerceptionSnapshot(
                handDetected = true,
                action = RobotAction.MOVE_FORWARD,
                tier = ConfidenceTier.CANDIDATE,
                confidence = 0.8f,
                frameAtMs = 0L,
            ),
            nowMs = 0L,
        )

        assertEquals(GesturePhase.GESTURE_CANDIDATE, state.phase)
        assertEquals(HaltMode.RUNNING, state.halt)
    }
}
