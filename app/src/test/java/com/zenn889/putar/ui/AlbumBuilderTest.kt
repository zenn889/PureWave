package com.zenn889.putar.ui

import com.zenn889.putar.data.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Uji penamaan album (pemilik album vs artis lagu) dan pemilihan sampul/folder
 * album. Keduanya logika murni, jadi bisa diuji tanpa perangkat.
 */
class AlbumBuilderTest {

    private var nextId = 1L

    private fun track(
        title: String,
        artist: String,
        albumId: Long? = 10L,
        albumTitle: String? = "Album Uji",
        albumArtist: String? = null,
        folder: String? = null,
        artUri: String? = null
    ) = Track(
        mediaId = nextId++,
        contentUri = "content://media/external/audio/media/$nextId",
        title = title,
        artist = artist,
        durationMs = 200_000L,
        albumId = albumId,
        folder = folder,
        albumTitle = albumTitle,
        albumArtist = albumArtist,
        artUri = artUri
    )

    @Test
    fun `tag ALBUM_ARTIST yang seragam dipakai sebagai pemilik album`() {
        val albums = buildAlbumsFrom(
            listOf(
                track("Lagu Satu", "Artis A", albumArtist = "Berbagai Penyanyi"),
                track("Lagu Dua", "Artis B", albumArtist = "Berbagai Penyanyi")
            )
        )
        assertEquals("Berbagai Penyanyi", albums.single().artist)
        assertEquals(2, albums.single().songCount)
    }

    @Test
    fun `ALBUM_ARTIST yang berbeda-beda jadi Berbagai artis`() {
        val albums = buildAlbumsFrom(
            listOf(
                track("Lagu Satu", "Artis A", albumArtist = "Artis A"),
                track("Lagu Dua", "Artis B", albumArtist = "Artis B")
            )
        )
        assertEquals(VARIOUS_ARTISTS, albums.single().artist)
    }

    @Test
    fun `tanpa tag album artist memakai artis lagu kalau seragam`() {
        val albums = buildAlbumsFrom(
            listOf(
                track("Lagu Satu", "Sheila On 7"),
                track("Lagu Dua", "Sheila On 7")
            )
        )
        assertEquals("Sheila On 7", albums.single().artist)
    }

    @Test
    fun `kompilasi tanpa tag tidak lagi dinamai dari lagu pertama`() {
        // Inilah perbaikan utamanya: dulu album ini dinamai "Penyanyi Satu"
        // hanya karena lagu pertamanya milik dia.
        val albums = buildAlbumsFrom(
            listOf(
                track("Lagu Satu", "Penyanyi Satu"),
                track("Lagu Dua", "Penyanyi Dua"),
                track("Lagu Tiga", "Penyanyi Tiga")
            )
        )
        assertEquals(VARIOUS_ARTISTS, albums.single().artist)
    }

    @Test
    fun `lagu tanpa album id tidak membentuk album`() {
        assertTrue(buildAlbumsFrom(listOf(track("Tanpa Album", "Artis", albumId = null))).isEmpty())
    }

    @Test
    fun `album memakai folder dan sampul dari lagu yang punya`() {
        val albums = buildAlbumsFrom(
            listOf(
                track("Lagu Satu", "Artis A"),
                track("Lagu Dua", "Artis A", folder = "Music/Album Uji", artUri = "file:///x/folder.jpg")
            )
        )
        assertEquals("Music/Album Uji", albums.single().folder)
        assertEquals("file:///x/folder.jpg", albums.single().artUri)
    }

    @Test
    fun `album tanpa sampul sama sekali menghasilkan artUri null`() {
        val album = buildAlbumsFrom(listOf(track("Lagu Satu", "Artis A"))).single()
        assertNull(album.artUri)
        assertNull(album.folder)
    }

    @Test
    fun `album diurutkan berdasarkan judul tanpa peduli huruf besar kecil`() {
        val albums = buildAlbumsFrom(
            listOf(
                track("Lagu", "Artis", albumId = 1L, albumTitle = "zebra"),
                track("Lagu", "Artis", albumId = 2L, albumTitle = "Angsa"),
                track("Lagu", "Artis", albumId = 3L, albumTitle = "Mawar")
            )
        )
        assertEquals(listOf("Angsa", "Mawar", "zebra"), albums.map { it.title })
    }

    @Test
    fun `album tanpa judul memakai label Tanpa album`() {
        val album = buildAlbumsFrom(listOf(track("Lagu", "Artis", albumTitle = null))).single()
        assertEquals("Tanpa album", album.title)
    }
}
