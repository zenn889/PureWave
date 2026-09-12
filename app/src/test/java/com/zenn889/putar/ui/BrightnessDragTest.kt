package com.zenn889.putar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Geser atas-bawah untuk kecerahan (V6). Yang paling gampang salah di sini
 * adalah arahnya: geser ke atas harus menambah terang, bukan mengurangi.
 * Karena itu arah dan penjepitannya diuji, walaupun gesturnya sendiri hanya
 * bisa dinilai di HP.
 */
class BrightnessDragTest {

    @Test
    fun `geser ke atas menambah terang`() {
        assertTrue(brightnessAfterDrag(0.5f, -100f) > 0.5f)
    }

    @Test
    fun `geser ke bawah mengurangi terang`() {
        assertTrue(brightnessAfterDrag(0.5f, 100f) < 0.5f)
    }

    @Test
    fun `hasil selalu dijepit antara 2 persen dan 100 persen`() {
        assertEquals(1f, brightnessAfterDrag(0.9f, -500f), 0.0001f)
        assertEquals(0.02f, brightnessAfterDrag(0.1f, 500f), 0.0001f)
    }

    @Test
    fun `geser 700 piksel setara seluruh rentang`() {
        // dari 0.5 dengan satu rentang penuh ke atas: mentok di 100%
        assertEquals(1f, brightnessAfterDrag(0.5f, -350f), 0.0001f)
        // dan separuh rentang ke bawah memotong nilai sekitar 0.36
        assertEquals(0.14f, brightnessAfterDrag(0.5f, 250f), 0.01f)
    }
}
