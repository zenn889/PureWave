package com.zenn889.putar.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Aturan-aturan UI baru yang bisa diuji tanpa perangkat:
 * - kartu "Lanjutkan mendengarkan" (U4)
 * - pembersihan simpanan miniatur video (U6)
 * Kalau salah, akibatnya terasa jauh di HP: kartu muncul untuk lagu yang baru
 * 1 detik diputar, atau folder cache tumbuh tanpa batas / terhapus berlebihan.
 */
class HomeUiRulesTest {

    @Test
    fun `kartu Lanjutkan hanya untuk posisi di atas 5 detik`() {
        assertFalse(shouldOfferContinue(0L))
        assertFalse(shouldOfferContinue(5_000L))
        assertTrue(shouldOfferContinue(5_001L))
        assertTrue(shouldOfferContinue(600_000L))
    }

    @Test
    fun `miniatur tidak dihapus selama masih di bawah batas`() {
        val semua = (1..50).map { "/tmp/v$it.jpg" to it.toLong() }
        assertTrue(thumbFilesToDelete(semua).isEmpty())
    }

    @Test
    fun `miniatur paling lama yang dihapus lebih dulu`() {
        // 205 berkas: 5 yang paling lama (timestamp terkecil) harus dihapus.
        // Urutan penghapusan tidak penting, jadi dibandingkan sebagai himpunan.
        val semua = (1..205).map { "/tmp/v$it.jpg" to it.toLong() }
        val hapus = thumbFilesToDelete(semua)
        assertEquals(5, hapus.size)
        assertEquals(
            setOf("/tmp/v1.jpg", "/tmp/v2.jpg", "/tmp/v3.jpg", "/tmp/v4.jpg", "/tmp/v5.jpg"),
            hapus.toSet()
        )
    }

    @Test
    fun `batas jumlah miniatur bisa disetel`() {
        val semua = (1..10).map { "/tmp/v$it.jpg" to it.toLong() }
        assertEquals(4, thumbFilesToDelete(semua, keep = 6).size)
        assertTrue(thumbFilesToDelete(semua, keep = 10).isEmpty())
    }
}
