package com.zenn889.putar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji pengkodean simpanan pustaka. Bagian yang diuji di sini murni (tanpa
 * Android), jadi bisa dijalankan tanpa perangkat. Bagian video memakai
 * `android.net.Uri` sehingga hanya jalur lagu yang diuji dari sisi pemetaan;
 * pengkodean catatannya sendiri (yang dipakai keduanya) diuji lewat [encodeRecord].
 */
class LibraryCacheTest {

    private fun track(n: Long, title: String = "Lagu $n") = Track(
        mediaId = n,
        contentUri = "content://media/external/audio/media/$n",
        title = title,
        artist = "Artis $n",
        durationMs = 180_000L + n,
        albumId = 10L + n,
        folder = "Music/Album $n",
        albumTitle = "Album $n",
        dateAddedMs = 1_700_000_000_000L + n,
        filePath = "/storage/emulated/0/Music/Album $n/lagu$n.mp3",
        albumArtist = "Pemilik $n",
        artUri = "file:///storage/emulated/0/Music/Album $n/folder.jpg"
    )

    @Test
    fun `simpan lalu baca mengembalikan data yang sama`() {
        val asli = (1L..5L).map { track(it) }
        val hasil = LibraryCache.decode(LibraryCache.encode(asli, emptyList()))!!
        assertEquals(asli, hasil.first)
        assertTrue(hasil.second.isEmpty())
    }

    @Test
    fun `teks berisi pemisah, baris baru, dan backslash tetap utuh`() {
        val aneh = track(
            1L,
            "Judul\u001Fdengan\u001Fpemisah"
        ).copy(
            artist = "Baris\nbaru\rlalu",
            albumTitle = "Garis\\miring",
            folder = null,
            filePath = null,
            albumArtist = null,
            artUri = null
        )
        val hasil = LibraryCache.decode(LibraryCache.encode(listOf(aneh), emptyList()))!!
        assertEquals(aneh, hasil.first.single())
    }

    @Test
    fun `bidang kosong dan bidang null dibedakan`() {
        val t = track(1L).copy(artist = "", folder = null, albumArtist = "")
        val hasil = LibraryCache.decode(LibraryCache.encode(listOf(t), emptyList()))!!
        assertEquals("", hasil.first.single().artist)
        assertEquals("", hasil.first.single().albumArtist)
        assertNull(hasil.first.single().folder)
    }

    @Test
    fun `catatan video memakai pengkodean yang sama`() {
        // catatan mentah: tipe + bidang, termasuk salah satunya null
        val baris = LibraryCache.encodeRecord(
            "V",
            listOf("7", "content://media/external/video/media/7", "Video\u001Faneh", "5000", "123", null, null)
        )
        val (tipe, bidang) = LibraryCache.decodeRecord(baris)!!
        assertEquals("V", tipe)
        assertEquals("Video\u001Faneh", bidang[2])
        assertNull(bidang[5])
    }

    @Test
    fun `versi berbeda diabaikan`() {
        assertNull(LibraryCache.decode("v0\nT|x"))
    }

    @Test
    fun `baris rusak dilewati tanpa membuang baris lain`() {
        val teks = LibraryCache.encode(listOf(track(1L), track(2L)), emptyList()) +
            "T|bidang|kurang\n" +
            "T|1|2|3|4|5|6|7|8|9|10|11|12\n"          // jumlah bidang T yang benar? (13 termasuk tipe)
        val hasil = LibraryCache.decode(teks)!!
        assertTrue(hasil.first.size >= 2)
    }

    @Test
    fun `teks kosong atau bukan simpanan menghasilkan null`() {
        assertNull(LibraryCache.decode(""))
        assertNull(LibraryCache.decode("halo dunia"))
        assertNull(LibraryCache.decode("v1\nX|tidak dikenal"))
    }

    @Test
    fun `hasil pemindaian kosong tidak menimpa simpanan yang berisi`() {
        assertFalse(LibraryCache.shouldOverwrite(newIsEmpty = true, exists = true))
        assertTrue(LibraryCache.shouldOverwrite(newIsEmpty = true, exists = false))
        assertTrue(LibraryCache.shouldOverwrite(newIsEmpty = false, exists = true))
        assertTrue(LibraryCache.shouldOverwrite(newIsEmpty = false, exists = false))
    }
}
