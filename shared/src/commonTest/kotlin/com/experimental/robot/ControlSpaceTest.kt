package com.experimental.robot

import com.experimental.robot.domain.command.AxisZone
import com.experimental.robot.domain.command.ControlSpace
import com.experimental.robot.domain.command.ControlSpaceConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ControlSpaceTest {

    private val config = ControlSpaceConfig()
    private fun space() = ControlSpace(config)

    @Test
    fun pusat_frame_berada_di_dead_zone() {
        val vector = space().resolve(x = 0.5f, y = 0.5f)

        assertEquals(AxisZone.CENTER, vector.horizontal)
        assertEquals(AxisZone.CENTER, vector.vertical)
        assertEquals(0f, vector.magnitude)
        assertTrue(vector.inDeadZone)
    }

    @Test
    fun ambang_batas_memetakan_zona_pada_kedua_sumbu() {
        val vector = space().resolve(x = 0.20f, y = 0.80f)

        assertEquals(AxisZone.LOW, vector.horizontal)
        assertEquals(AxisZone.HIGH, vector.vertical)
    }

    /**
     * Tanpa histeresis, tangan yang berhenti tepat di ambang batas akan membuat zona
     * berkedip-kedip - dan arah robot ikut berkedip bersamanya.
     */
    @Test
    fun histeresis_menahan_zona_saat_tangan_berhenti_di_ambang() {
        val space = space()

        assertEquals(AxisZone.LOW, space.resolve(0.30f, 0.5f).horizontal)
        // 0.36 sudah melewati ambang masuk (0.35) tapi belum melewati ambang keluar (0.38).
        assertEquals(AxisZone.LOW, space.resolve(0.36f, 0.5f).horizontal)
        assertEquals(AxisZone.CENTER, space.resolve(0.40f, 0.5f).horizontal)
    }

    @Test
    fun zona_tinggi_juga_punya_histeresis() {
        val space = space()

        assertEquals(AxisZone.HIGH, space.resolve(0.70f, 0.5f).horizontal)
        assertEquals(AxisZone.HIGH, space.resolve(0.64f, 0.5f).horizontal)
        assertEquals(AxisZone.CENTER, space.resolve(0.60f, 0.5f).horizontal)
    }

    @Test
    fun magnitude_naik_bersama_jarak_radial_dari_pusat() {
        val space = space()

        val near = space.resolve(0.5f, 0.30f).magnitude
        val far = space.resolve(0.5f, 0.12f).magnitude

        assertTrue(near > 0f, "sudah keluar dead zone")
        assertTrue(far > near, "makin jauh makin cepat: $near -> $far")
    }

    @Test
    fun magnitude_penuh_dicapai_pada_radius_kecepatan_penuh() {
        val vector = space().resolve(0.5f, 0.05f)

        assertEquals(1f, vector.magnitude)
    }

    /** Gerakan pertama begitu keluar dead zone harus terasa, bukan merambat dari nol. */
    @Test
    fun kecepatan_minimum_berlaku_tepat_di_luar_dead_zone() {
        val vector = space().resolve(0.5f, 0.5f - config.deadRadius - 0.005f)

        assertTrue(
            vector.magnitude >= config.minSpeed,
            "magnitude ${vector.magnitude} harus mulai dari minSpeed ${config.minSpeed}",
        )
    }

    @Test
    fun pusat_hasil_kalibrasi_menggeser_dead_zone() {
        val shifted = ControlSpace(config.copy(neutralX = 0.4f, neutralY = 0.6f))

        assertEquals(0f, shifted.resolve(0.4f, 0.6f).magnitude, "pusat baru = diam")
        assertTrue(shifted.resolve(0.5f, 0.5f).magnitude >= 0f)
    }

    @Test
    fun reset_mengembalikan_zona_ke_center() {
        val space = space()
        space.resolve(0.20f, 0.5f)

        space.reset()

        assertEquals(AxisZone.CENTER, space.resolve(0.36f, 0.5f).horizontal)
    }
}
