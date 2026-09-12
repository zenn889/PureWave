package com.zenn889.putar.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * Kunci cache sampul: **satu album diperiksa sekali**, bukan sekali per lagu.
 *
 * Ini penting untuk kecepatan membuka aplikasi: pemindaian pustaka memanggil
 * [resolve] satu kali untuk setiap lagu, sedangkan pemeriksaan MediaStore
 * (membuka gambar lewat ContentResolver) relatif mahal. Versi pertama fungsi
 * ini ikut memasukkan `filePath` ke dalam kunci, sehingga kuncinya selalu baru
 * untuk tiap lagu dan cache tidak pernah kena — akibatnya satu pustaka bisa
 * memicu ribuan pemeriksaan saat aplikasi dibuka.
 */
internal fun artCacheKey(albumId: Long?, dirPath: String?): String =
    if (albumId != null) "a$albumId" else "f:${dirPath.orEmpty()}"

/**
 * Menentukan sampul untuk sebuah album/lagu, sekali saat pemindaian pustaka.
 *
 * Urutannya: sampul dari MediaStore (`content://media/external/audio/albumart/<id>`)
 * kalau gambarnya benar-benar ada, kalau tidak gambar di folder lagu
 * (`folder.jpg`, `cover.jpg`, `albumart.jpg`, `album.jpg`, `front.jpg`,
 * `artwork.jpg` — juga .jpeg/.png/.webp, huruf besar-kecil diabaikan).
 *
 * Alasannya: banyak file hasil salin dari komputer punya gambar di foldernya
 * tapi tidak terindeks MediaStore (atau malah terindeks tanpa sampul). File
 * seperti itu sebelumnya selalu tampil ikon catatan musik kosong. Aplikasi ini
 * sudah membaca berkas di folder lagu untuk mencari lirik `.lrc` dan subtitle,
 * jadi mencari gambar di folder yang sama sejalan dengan itu.
 *
 * Hasil dicache per album dan per folder, supaya pemindaian pustaka tetap
 * seringkas mungkin (lihat [artCacheKey]).
 */
object ArtResolver {

    /** Nama berkas yang diakui, urut prioritas. */
    private val NAMES = listOf("folder", "cover", "albumart", "album", "front", "artwork")

    private val IMAGE_EXT = listOf(".jpg", ".jpeg", ".png", ".webp")

    /** albumId (atau folder, kalau albumnya tak diketahui) → sampul. */
    private val cache = HashMap<String, String?>()

    /** folder → gambar di dalamnya (murni nama berkas, tanpa MediaStore). */
    private val folderCache = HashMap<String, String?>()

    /** Buang cache — dipanggil saat pustaka dipindai ulang. */
    fun clear() {
        cache.clear()
        folderCache.clear()
    }

    /**
     * @return String URI sampul, atau null kalau tidak ada di mana pun.
     */
    fun resolve(context: Context, albumId: Long?, folder: String?, filePath: String?): String? {
        val dir = dirOf(folder, filePath)
        val key = artCacheKey(albumId, dir?.absolutePath)
        if (cache.containsKey(key)) return cache[key]
        val value = find(context, albumId, dir)
        cache[key] = value
        return value
    }

    private fun find(context: Context, albumId: Long?, dir: File?): String? {
        val fromStore = MusicRepository.albumArtUri(albumId)
        if (fromStore != null && hasImage(context, fromStore)) return fromStore.toString()
        return folderArt(dir)?.let { Uri.fromFile(it).toString() }
    }

    /** Apakah MediaStore sungguh menyediakan gambar untuk URI itu? */
    private fun hasImage(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openInputStream(uri)?.use { true } ?: false
    }.getOrDefault(false)

    /** Folder absolut: dari path file kalau ada, kalau tidak dari RELATIVE_PATH. */
    private fun dirOf(folder: String?, filePath: String?): File? {
        filePath?.takeIf { it.isNotEmpty() }?.let { path ->
            File(path).parent?.let { return File(it) }
        }
        val rel = folder?.trim('/')?.takeIf { it.isNotEmpty() } ?: return null
        return File(Environment.getExternalStorageDirectory(), rel)
    }

    /** Gambar di folder, dicache per folder (banyak album bisa satu folder). */
    private fun folderArt(dir: File?): File? {
        if (dir == null) return null
        val path = dir.absolutePath
        if (folderCache.containsKey(path)) return folderCache[path]?.let { File(it) }
        val found = findImage(dir)
        folderCache[path] = found?.absolutePath
        return found
    }

    /**
     * Cari gambar sampul di dalam [dir] — murni berdasarkan nama berkas, jadi
     * bisa diuji unit tanpa perangkat. Null kalau tidak ada nama yang dikenal
     * (sengaja tidak mengambil sembarang gambar: folder musik bisa berisi foto
     * yang tidak ada hubungannya dengan album).
     */
    fun findImage(dir: File?): File? {
        if (dir == null || !dir.isDirectory) return null
        val images = dir.listFiles()?.filter { f ->
            f.isFile && IMAGE_EXT.any { f.name.endsWith(it, ignoreCase = true) }
        }.orEmpty()
        if (images.isEmpty()) return null
        for (name in NAMES) {
            images.firstOrNull { it.nameWithoutExtension.equals(name, ignoreCase = true) }
                ?.let { return it }
        }
        return null
    }
}

/** Ubah sampul hasil pemindaian (String) jadi Uri untuk Coil. */
internal fun String?.toArtUri(): Uri? = this?.takeIf { it.isNotEmpty() }?.let(Uri::parse)
