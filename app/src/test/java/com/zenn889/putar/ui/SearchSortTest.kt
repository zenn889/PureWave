package com.zenn889.putar.ui

import com.zenn889.putar.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji logika sortir & pencarian pustaka (diangkat keluar dari MainActivity di
 * v2.19.0 supaya bisa diuji tanpa perangkat).
 */
class SearchSortTest {

    private fun track(
        id: Long,
        title: String,
        artist: String = "",
        album: String? = null,
        durationMs: Long = 0L,
        dateAddedMs: Long = 0L,
        folder: String? = null
    ) = Track(
        mediaId = id,
        contentUri = "content://media/external/audio/media/$id",
        title = title,
        artist = artist,
        durationMs = durationMs,
        albumId = null,
        folder = folder,
        albumTitle = album,
        dateAddedMs = dateAddedMs
    )

    private val adu = track(1, "Adu", "Sheila On 7", album = "07 Des", durationMs = 200_000, dateAddedMs = 300)
    private val dan = track(2, "Dan", "Sheila On 7", album = "07 Des", durationMs = 300_000, dateAddedMs = 100)
    private val biru = track(3, "Biru", "Efek Rumah Kaca", album = "Kamar Gelap", durationMs = 100_000, dateAddedMs = 200)
    private val tanpaArtis = track(4, "Instrumental", "", album = null, durationMs = 400_000, dateAddedMs = 400)

    private val semua = listOf(adu, dan, biru, tanpaArtis)

    /* ---------- pencarian ---------- */

    @Test
    fun `token dipecah dan urutan kata tidak penting`() {
        assertEquals(listOf("sheila", "adu"), searchTokens("Sheila Adu"))
        val tokens = searchTokens("adu sheila")
        assertTrue(adu.matchesQuery(tokens))
    }

    @Test
    fun `huruf besar kecil dan diakritik diabaikan`() {
        val lagu = track(99, "Café Malam", "CHAIRIL")
        assertTrue(lagu.matchesQuery(searchTokens("cafe")))
        assertTrue(lagu.matchesQuery(searchTokens("chairil")))
        assertTrue(lagu.matchesQuery(searchTokens("café")))
    }

    @Test
    fun `pencarian mencakup judul artis album dan folder`() {
        assertTrue(adu.matchesQuery(searchTokens("adu")))          // judul
        assertTrue(adu.matchesQuery(searchTokens("sheila")))       // artis
        assertTrue(adu.matchesQuery(searchTokens("07 des")))       // album
        val berfolder = track(98, "Lagu", "X", folder = "Download/Baru")
        assertTrue(berfolder.matchesQuery(searchTokens("download")))
    }

    @Test
    fun `semua token harus cocok`() {
        assertFalse(adu.matchesQuery(searchTokens("adu biru")))
        assertTrue(adu.matchesQuery(searchTokens("adu sheila")))
        assertTrue(adu.matchesQuery(emptyList()))                  // tanpa kata kunci = semua lolos
    }

    @Test
    fun `spasi berlebih dan tab tidak menghasilkan token kosong`() {
        assertEquals(listOf("adu"), searchTokens("   adu \t "))
        assertEquals(emptyList<String>(), searchTokens("   "))
    }

    /* ---------- sortir ---------- */

    @Test
    fun `judul A-Z dan Z-A`() {
        assertEquals(
            listOf("Adu", "Biru", "Dan", "Instrumental"),
            sortedTracks(semua, SortOption.JUDUL).map { it.title }
        )
        assertEquals(
            listOf("Instrumental", "Dan", "Biru", "Adu"),
            sortedTracks(semua, SortOption.JUDUL_ZA).map { it.title }
        )
    }

    @Test
    fun `artis lalu judul`() {
        // Artis tanpa nama memakai "Artis tak dikenal" (huruf 'A') sehingga
        // muncul lebih dulu; sisanya urut nama artis, lalu judul.
        assertEquals(
            listOf("Instrumental", "Biru", "Adu", "Dan"),
            sortedTracks(semua, SortOption.ARTIS).map { it.title }
        )
        // bukti bahwa nama artis yang menentukan, bukan nama lagu:
        // "efek rumah kaca" < "sheila on 7"
        val duaArtis = listOf(
            track(10, "Zebra", "AAA Band"),
            track(11, "Angsa", "BBB Band")
        )
        assertEquals(
            listOf("Zebra", "Angsa"),
            sortedTracks(duaArtis, SortOption.ARTIS).map { it.title }
        )
    }

    @Test
    fun `album tanpa nama ditaruh paling atas`() {
        val hasil = sortedTracks(semua, SortOption.ALBUM).map { it.title }
        assertEquals("Instrumental", hasil.first())
    }

    @Test
    fun `terbaru dan terlama memakai tanggal ditambahkan`() {
        assertEquals("Instrumental", sortedTracks(semua, SortOption.TERBARU).first().title)
        assertEquals("Dan", sortedTracks(semua, SortOption.TERLAMA).first().title)
    }

    @Test
    fun `durasi terpendek dan terpanjang`() {
        assertEquals("Biru", sortedTracks(semua, SortOption.DURASI).first().title)
        assertEquals("Instrumental", sortedTracks(semua, SortOption.DURASI_PANJANG).first().title)
    }

    @Test
    fun `paling sering diputar memakai peta plays dan seri diurutkan judul`() {
        val plays = mapOf(adu.contentUri to 3, biru.contentUri to 7)
        val hasil = sortedTracks(semua, SortOption.SERING, plays = plays).map { it.title }
        assertEquals("Biru", hasil.first())
        assertEquals("Adu", hasil[1])
        // yang belum pernah diputar (0 kali) tetap ikut, diurutkan menurut judul
        assertEquals(listOf("Dan", "Instrumental"), hasil.drop(2))
    }

    @Test
    fun `terakhir diputar memakai peta recency dan yang belum pernah ditaruh paling akhir`() {
        val recency = mapOf(dan.contentUri to 0, adu.contentUri to 1, biru.contentUri to 2)
        val hasil = sortedTracks(semua, SortOption.TERAKHIR, recency = recency).map { it.title }
        assertEquals(listOf("Dan", "Adu", "Biru", "Instrumental"), hasil)
    }

    @Test
    fun `sortir tidak mengubah daftar asal`() {
        val salinan = semua.toList()
        sortedTracks(semua, SortOption.JUDUL_ZA)
        assertEquals(salinan, semua)
    }
}
