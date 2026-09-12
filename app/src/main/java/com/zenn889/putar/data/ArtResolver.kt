package com.zenn889.putar.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

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
 * Hasil dicache per album/folder karena pemindaian memanggil ini sekali per
 * lagu — pemeriksaannya sendiri (buka gambar lewat MediaStore) relatif mahal.
 */
object ArtResolver {

    /** Nama berkas yang diakui, urut prioritas. */
    private val NAMES = listOf("folder", "cover", "albumart", "album", "front", "artwork")

    private val IMAGE_EXT = listOf(".jpg", ".jpeg", ".png", ".webp")

    private val cache = HashMap<String, String?>()

    /** Buang cache — dipanggil saat pustaka dipindai ulang. */
    fun clear() = cache.clear()

    /**
     * @return String URI sampul, atau null kalau tidak ada di mana pun.
     */
    fun resolve(context: Context, albumId: Long?, folder: String?, filePath: String?): String? {
        val key = "a${albumId ?: -1}|${folder.orEmpty()}|${filePath.orEmpty()}"
        if (cache.containsKey(key)) return cache[key]
        val value = find(context, albumId, folder, filePath)
        cache[key] = value
        return value
    }

    private fun find(context: Context, albumId: Long?, folder: String?, filePath: String?): String? {
        val fromStore = MusicRepository.albumArtUri(albumId)
        if (fromStore != null && hasImage(context, fromStore)) return fromStore.toString()
        return findImage(dirOf(folder, filePath))?.let { Uri.fromFile(it).toString() }
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
