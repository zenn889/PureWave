package com.zenn889.putar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

/**
 * Uji pencarian gambar sampul di folder lagu (bagian murni dari ArtResolver —
 * tanpa Context, tanpa MediaStore).
 */
class ArtResolverTest {

    private fun dir(vararg names: String): File {
        val d = File.createTempFile("purewave-art", "").let { f ->
            f.delete(); f.mkdirs(); f
        }
        names.forEach { File(d, it).writeBytes(ByteArray(4)) }
        return d
    }

    @Test
    fun `cover jpg di folder ditemukan`() {
        val d = dir("lagu.mp3", "cover.jpg")
        assertEquals("cover.jpg", ArtResolver.findImage(d)?.name)
    }

    @Test
    fun `folder jpg diutamakan daripada cover`() {
        val d = dir("cover.jpg", "folder.jpg", "albumart.png")
        assertEquals("folder.jpg", ArtResolver.findImage(d)?.name)
    }

    @Test
    fun `huruf besar kecil nama berkas diabaikan`() {
        val d = dir("FOLDER.JPG")
        assertEquals("FOLDER.JPG", ArtResolver.findImage(d)?.name)
        val d2 = dir("Cover.PnG")
        assertEquals("Cover.PnG", ArtResolver.findImage(d2)?.name)
    }

    @Test
    fun `berkas non-gambar diabaikan`() {
        val d = dir("cover.txt", "folder.lrc", "album.jpg")
        assertEquals("album.jpg", ArtResolver.findImage(d)?.name)
    }

    @Test
    fun `gambar tanpa nama yang dikenal tidak dipakai`() {
        // Sengaja: folder musik sering berisi foto yang tidak ada hubungannya
        // dengan album, jadi hanya nama yang dikenal yang diterima.
        val d = dir("IMG_20260101_123456.jpg", "screenshot.png")
        assertNull(ArtResolver.findImage(d))
    }

    @Test
    fun `folder kosong atau tidak ada menghasilkan null`() {
        assertNull(ArtResolver.findImage(dir("lagu.mp3")))
        assertNull(ArtResolver.findImage(File("/tidak/ada/folder-ajaib")))
        assertNull(ArtResolver.findImage(null))
    }

    /* ---------- kunci cache (sumber kelambatan membuka aplikasi) ---------- */

    @Test
    fun `lagu dalam satu album memakai kunci cache yang sama`() {
        // Regresi v2.23.0: kunci cache ikut memuat path file, sehingga setiap
        // lagu memicu pemeriksaan MediaStore sendiri-sendiri dan aplikasi jadi
        // lambat dibuka. Kunci harus sama untuk semua lagu dalam satu album.
        assertEquals(
            artCacheKey(10L, "/storage/emulated/0/Music/Album"),
            artCacheKey(10L, "/storage/emulated/0/Music/Album")
        )
    }

    @Test
    fun `album berbeda memakai kunci berbeda`() {
        assertNotEquals(artCacheKey(10L, "/x"), artCacheKey(11L, "/x"))
    }

    @Test
    fun `album id tetap jadi kunci walau lagunya beda folder`() {
        assertEquals(artCacheKey(7L, "/a"), artCacheKey(7L, "/b"))
    }

    @Test
    fun `tanpa album id kunci mengikuti folder`() {
        assertEquals(artCacheKey(null, "/x/a"), artCacheKey(null, "/x/a"))
        assertNotEquals(artCacheKey(null, "/x/a"), artCacheKey(null, "/x/b"))
    }
}
