package com.zenn889.putar.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Langkah kecepatan dipakai bersama oleh pemutar musik dan pemutar video
 * (sejak V4: pemutar video memakai `fmtSpeed`/`nextSpeed` yang sama, bukan
 * salinannya sendiri) — jadi perilakunya diuji di sini satu kali.
 */
class SpeedStepsTest {

    @Test
    fun `label kecepatan memakai langkah terdekat`() {
        assertEquals("0.5x", fmtSpeed(0.5f))
        assertEquals("0.75x", fmtSpeed(0.75f))
        assertEquals("1x", fmtSpeed(1f))
        assertEquals("1.25x", fmtSpeed(1.24f))
        assertEquals("1.5x", fmtSpeed(1.5f))
        assertEquals("2x", fmtSpeed(2f))
    }

    @Test
    fun `tekanan berikutnya melewati semua langkah lalu kembali ke awal`() {
        var s = SPEED_STEPS.first()
        val urut = mutableListOf(s)
        repeat(SPEED_STEPS.size - 1) {
            s = nextSpeed(s)
            urut.add(s)
        }
        assertEquals(SPEED_STEPS.toList(), urut)
        assertEquals(0.5f, nextSpeed(s), 0.001f)
    }

    @Test
    fun `nilai yang tidak ada di daftar dimulai dari 1x`() {
        assertEquals(1.25f, nextSpeed(1.1f), 0.001f)
        assertEquals(1.25f, nextSpeed(0f), 0.001f)
    }
}
