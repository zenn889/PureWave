package com.zenn889.putar.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Simpanan pustaka terakhir di disk.
 *
 * Kenapa ada: pemindaian MediaStore berjalan setiap aplikasi dibuka dan itulah
 * yang tampil sebagai lingkaran loading. Dengan menyimpan hasil pemindaian,
 * pembukaan berikutnya bisa menampilkan daftar lagu seketika, lalu memindai
 * ulang di belakang layar untuk menyegarkan.
 *
 * Format sengaja berupa teks baris-per-baris (bukan JSON) supaya pengkodeannya
 * murni Kotlin dan bisa diuji unit tanpa perangkat. Satu berkas, satu baris
 * per lagu/video, bidang dipisah karakter Unit Separator dan di-escape.
 */
object LibraryCache {

    private const val FILE_NAME = "pustaka-terakhir.txt"
    private const val VERSION = "v1"
    private const val SEP = '\u001F'
    private const val NULL = "\\e"
    private const val TYPE_TRACK = "T"
    private const val TYPE_VIDEO = "V"

    /**
     * Baca simpanan. null kalau belum ada, versinya beda, atau berkasnya rusak
     * — semuanya ditangani sebagai "belum ada" supaya pemindaian yang jalan.
     */
    suspend fun load(context: Context): Pair<List<Track>, List<VideoItem>>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val f = File(context.filesDir, FILE_NAME)
                if (!f.isFile) return@runCatching null
                decode(f.readText(Charsets.UTF_8))
            }.getOrNull()
        }

    /**
     * Tulis simpanan. Ditulis ke berkas sementara lalu diganti, supaya
     * pemutusan di tengah penulisan tidak meninggalkan berkas setengah jadi.
     */
    suspend fun save(context: Context, tracks: List<Track>, videos: List<VideoItem>) {
        withContext(Dispatchers.IO) {
            val target = File(context.filesDir, FILE_NAME)
            if (!shouldOverwrite(tracks.isEmpty() && videos.isEmpty(), target.isFile)) {
                return@withContext
            }
            runCatching {
                val tmp = File(context.filesDir, "$FILE_NAME.tmp")
                tmp.writeText(encode(tracks, videos), Charsets.UTF_8)
                if (!tmp.renameTo(target)) {
                    target.delete()
                    tmp.renameTo(target)
                }
            }
            Unit
        }
    }

    /**
     * Boleh menimpa simpanan lama dengan hasil pemindaian baru?
     *
     * Hasil kosong **tidak** menimpa simpanan yang berisi: pemindaian yang
     * gagal sesaat (izin dicabut, provider bermasalah) juga mengembalikan daftar
     * kosong, dan kalau itu ditulis, pembukaan berikutnya kehilangan pustaka.
     */
    internal fun shouldOverwrite(newIsEmpty: Boolean, exists: Boolean): Boolean =
        !(newIsEmpty && exists)

    /* ---------------- pengkodean (murni, bisa diuji) ---------------- */

    internal fun encode(tracks: List<Track>, videos: List<VideoItem>): String = buildString {
        append(VERSION).append('\n')
        tracks.forEach { append(encodeRecord(TYPE_TRACK, trackFields(it))).append('\n') }
        videos.forEach { append(encodeRecord(TYPE_VIDEO, videoFields(it))).append('\n') }
    }

    /** Satu catatan: "TIPE<SEP>bidang<SEP>bidang..." */
    internal fun encodeRecord(type: String, values: List<String?>): String =
        (listOf(type) + values).joinToString(SEP.toString()) { enc(it) }

    /** @return pasangan (tipe, bidang) atau null kalau barisnya tidak sah. */
    internal fun decodeRecord(line: String): Pair<String, List<String?>>? {
        val parts = line.split(SEP)
        val type = parts.firstOrNull() ?: return null
        if (type != TYPE_TRACK && type != TYPE_VIDEO) return null
        val values = parts.drop(1).map { dec(it) }
        if (values.size != fieldCount(type)) return null
        return type to values
    }

    internal fun decode(text: String): Pair<List<Track>, List<VideoItem>>? {
        val lines = text.lineSequence().filter { it.isNotEmpty() }.toList()
        if (lines.firstOrNull() != VERSION) return null
        val tracks = ArrayList<Track>()
        val videos = ArrayList<VideoItem>()
        for (line in lines.drop(1)) {
            val (type, values) = decodeRecord(line) ?: continue
            when (type) {
                TYPE_TRACK -> trackFrom(values)?.let { tracks.add(it) }
                TYPE_VIDEO -> videoFrom(values)?.let { videos.add(it) }
            }
        }
        return if (tracks.isEmpty() && videos.isEmpty()) {
            // Versi cocok tapi tidak ada satu pun catatan yang sah: lebih
            // mungkin berkasnya rusak daripada pustaka yang benar-benar kosong.
            // Dianggap "belum ada" supaya jalur pemindaian yang dipakai.
            null
        } else {
            tracks to videos
        }
    }

    /* ---------------- pemetaan model ---------------- */

    private fun fieldCount(type: String) = if (type == TYPE_TRACK) 12 else 7

    private fun trackFields(t: Track) = listOf(
        t.mediaId.toString(),
        t.contentUri,
        t.title,
        t.artist,
        t.durationMs.toString(),
        t.albumId?.toString(),
        t.folder,
        t.albumTitle,
        t.dateAddedMs.toString(),
        t.filePath,
        t.albumArtist,
        t.artUri
    )

    private fun trackFrom(v: List<String?>): Track? {
        val mediaId = v[0]?.toLongOrNull() ?: return null
        val duration = v[4]?.toLongOrNull() ?: 0L
        val dateAdded = v[8]?.toLongOrNull() ?: 0L
        return Track(
            mediaId = mediaId,
            contentUri = v[1] ?: return null,
            title = v[2] ?: return null,
            artist = v[3].orEmpty(),
            durationMs = duration,
            albumId = v[5]?.toLongOrNull(),
            folder = v[6],
            albumTitle = v[7],
            dateAddedMs = dateAdded,
            filePath = v[9],
            albumArtist = v[10],
            artUri = v[11]
        )
    }

    private fun videoFields(v: VideoItem) = listOf(
        v.mediaId.toString(),
        v.contentUri.toString(),
        v.title,
        v.durationMs.toString(),
        v.dateAddedMs.toString(),
        v.folder,
        v.filePath
    )

    private fun videoFrom(v: List<String?>): VideoItem? {
        val mediaId = v[0]?.toLongOrNull() ?: return null
        val uri = v[1] ?: return null
        return VideoItem(
            mediaId = mediaId,
            contentUri = Uri.parse(uri),
            title = v[2] ?: return null,
            durationMs = v[3]?.toLongOrNull() ?: 0L,
            dateAddedMs = v[4]?.toLongOrNull() ?: 0L,
            folder = v[5],
            filePath = v[6]
        )
    }

    /* ---------------- escape ---------------- */

    private fun enc(s: String?): String {
        if (s == null) return NULL
        val out = StringBuilder(s.length + 2)
        s.forEach { c ->
            when (c) {
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                SEP -> out.append("\\s")
                else -> out.append(c)
            }
        }
        return out.toString()
    }

    private fun dec(s: String): String? {
        if (s == NULL) return null
        if (s.indexOf('\\') < 0) return s
        val out = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> { out.append('\\'); i += 2; continue }
                    'n' -> { out.append('\n'); i += 2; continue }
                    'r' -> { out.append('\r'); i += 2; continue }
                    's' -> { out.append(SEP); i += 2; continue }
                }
            }
            out.append(c)
            i++
        }
        return out.toString()
    }
}
