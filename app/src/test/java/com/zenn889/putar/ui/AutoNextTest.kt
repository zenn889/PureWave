package com.zenn889.putar.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan lanjut-otomatis (V7). Yang dijaga: aplikasi tidak boleh melompat ke
 * video berikutnya saat video belum habis, saat ini video terakhir, atau saat
 * user sudah menekan "Batal".
 */
class AutoNextTest {

    @Test
    fun `jalan saat video habis dan masih ada berikutnya`() {
        assertTrue(shouldAutoNext(ended = true, hasNext = true, cancelled = false))
    }

    @Test
    fun `tidak jalan kalau video belum habis`() {
        assertFalse(shouldAutoNext(ended = false, hasNext = true, cancelled = false))
    }

    @Test
    fun `tidak jalan di video terakhir`() {
        assertFalse(shouldAutoNext(ended = true, hasNext = false, cancelled = false))
    }

    @Test
    fun `tidak jalan setelah user menekan Batal`() {
        assertFalse(shouldAutoNext(ended = true, hasNext = true, cancelled = true))
    }
}
