package com.zenn889.putar.data

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile

/** Nilai ReplayGain yang ditemukan di tag sebuah file. */
data class ReplayGain(
    val trackGainDb: Float,
    val trackPeak: Float? = null
)

/**
 * Pembaca tag ReplayGain tanpa library luar.
 *
 * Didukung:
 * - ID3v2 (mp3): frame `TXXX` (`REPLAYGAIN_TRACK_GAIN` / `_PEAK`) dan `RVA2`.
 * - FLAC: blok `VORBIS_COMMENT` (`REPLAYGAIN_TRACK_GAIN` / `_PEAK`).
 * - OGG (Vorbis) & Opus: blok Vorbis comment di paket kepala
 *   (`REPLAYGAIN_TRACK_GAIN` / `_PEAK`).
 * - MP4/M4A: atom bebas iTunes
 *   (`----:com.apple.iTunes:replaygain_track_gain` / `_peak`), termasuk file
 *   yang menaruh atom `moov` di akhir file (tanpa faststart).
 *
 * Sengaja TIDAK dipakai: `R128_TRACK_GAIN` milik Opus. Acuannya berbeda
 * (-23 LUFS, sedangkan ReplayGain -18 LUFS), jadi menerapkannya langsung akan
 * membuat lagu Opus terdengar sekitar 5 dB lebih pelan daripada lagu lain.
 *
 * File tanpa tag ini tidak terpengaruh sama sekali — gain tetap 1.0 — jadi
 * pustaka campur tetap aman.
 *
 * Fungsi [parse] menerima ByteArray (bukan File) supaya bisa diuji unit tanpa
 * file nyata.
 */
object ReplayGainReader {

    /** Tag selalu ada di awal file; 512 KB lebih dari cukup. */
    private const val HEAD_BYTES = 512 * 1024

    private val NUMBER = Regex("([-+]?\\d+(?:[.,]\\d+)?)")
    private val KEY_GAIN = "replaygain_track_gain"
    private val KEY_PEAK = "replaygain_track_peak"

    fun read(file: File): ReplayGain? = runCatching {
        if (!file.isFile) return null
        val head = readFrom(file, 0L)
        parse(head)?.let { return it }
        // MP4/M4A hasil encoder yang tidak "faststart" menyimpan atom `moov`
        // (tempat tag berada) di AKHIR file, jadi tidak ikut terbaca di kepala.
        if (isMp4(head)) parseMp4Tail(readFrom(file, -HEAD_BYTES.toLong())) else null
    }.getOrNull()

    /** Baca sampai [HEAD_BYTES] mulai dari [offset]; offset negatif = dari akhir. */
    private fun readFrom(file: File, offset: Long): ByteArray {
        val total = file.length()
        val from = if (offset < 0L) (total + offset).coerceAtLeast(0L) else offset.coerceAtMost(total)
        val size = minOf(total - from, HEAD_BYTES.toLong())
        if (size < 16L) return ByteArray(0)
        val buf = ByteArray(size.toInt())
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(from)
            var read = 0
            while (read < buf.size) {
                val n = raf.read(buf, read, buf.size - read)
                if (n <= 0) break
                read += n
            }
            return if (read == buf.size) buf else buf.copyOf(read)
        }
    }

    fun parse(bytes: ByteArray): ReplayGain? = when {
        isId3(bytes) -> parseId3(bytes)
        isFlac(bytes) -> parseFlac(bytes)
        isOgg(bytes) -> parseOgg(bytes)
        isMp4(bytes) -> parseMp4(bytes, 0, bytes.size)
        else -> null
    }

    /* ---------------- ID3v2 ---------------- */

    private fun isId3(b: ByteArray): Boolean =
        b.size >= 10 && b[0] == 'I'.code.toByte() && b[1] == 'D'.code.toByte() &&
            b[2] == '3'.code.toByte()

    private fun parseId3(b: ByteArray): ReplayGain? {
        val major = b[3].toInt() and 0xFF          // 2 = v2.2, 3 = v2.3, 4 = v2.4
        val flags = b[5].toInt() and 0xFF
        val declared = syncSafe(b, 6)
        val end = minOf(b.size, 10 + declared)
        var pos = 10
        if (flags and 0x40 != 0) pos += if (major >= 4) 6 else 10   // extended header

        val idLen = if (major == 2) 3 else 4
        val headLen = if (major == 2) 6 else 10
        var gain: Float? = null
        var peak: Float? = null

        while (pos + headLen <= end) {
            val id = String(b, pos, idLen, Charsets.ISO_8859_1)
            if (id[0] == '\u0000') break
            val size = when {
                major == 2 -> ((b[pos + 3].toInt() and 0xFF) shl 16) or
                    ((b[pos + 4].toInt() and 0xFF) shl 8) or (b[pos + 5].toInt() and 0xFF)
                major == 4 -> syncSafe(b, pos + 4)
                else -> ((b[pos + 4].toInt() and 0xFF) shl 24) or
                    ((b[pos + 5].toInt() and 0xFF) shl 16) or
                    ((b[pos + 6].toInt() and 0xFF) shl 8) or (b[pos + 7].toInt() and 0xFF)
            }
            val body = pos + headLen
            if (size <= 0 || body + size > b.size) break
            when (id) {
                "TXXX" -> parseTxxx(b, body, size)?.let { (key, value) ->
                    when (key) {
                        KEY_GAIN -> if (gain == null) gain = parseDb(value)
                        KEY_PEAK -> if (peak == null) peak = value.toFloatOrNull()
                    }
                }
                "RVA2" -> if (gain == null) {
                    parseRva2(b, body, size)?.let { gain = it.trackGainDb; peak = it.trackPeak }
                }
            }
            pos = body + size
        }
        return gain?.let { ReplayGain(it, peak) }
    }

    /** TXXX: byte encoding, deskripsi, 0x00, nilai. */
    private fun parseTxxx(b: ByteArray, start: Int, size: Int): Pair<String, String>? {
        if (size < 3) return null
        val enc = b[start].toInt() and 0xFF
        val dataStart = start + 1
        val dataEnd = start + size
        val wide = enc == 1 || enc == 2
        var i = dataStart
        var descEnd = -1
        while (i < dataEnd) {
            if (wide) {
                if (i + 1 < dataEnd && b[i] == 0.toByte() && b[i + 1] == 0.toByte()) {
                    descEnd = i; break
                }
                i += 2
            } else {
                if (b[i] == 0.toByte()) { descEnd = i; break }
                i += 1
            }
        }
        if (descEnd < 0) return null
        val valueStart = minOf(descEnd + if (wide) 2 else 1, dataEnd)
        val desc = decodeText(b, dataStart, descEnd - dataStart, enc)
        val value = decodeText(b, valueStart, dataEnd - valueStart, enc)
        return desc.trim().lowercase() to value.trim()
    }

    /** RVA2: id (null-terminated), channel, gain int16 (dB x512), bit peak, peak. */
    private fun parseRva2(b: ByteArray, start: Int, size: Int): ReplayGain? {
        val end = start + size
        var i = start
        while (i < end && b[i] != 0.toByte()) i++
        i++
        if (i + 4 > end) return null
        val channel = b[i].toInt() and 0xFF
        if (channel != 1) return null                              // hanya master gain
        val raw = (((b[i + 1].toInt() and 0xFF) shl 8) or (b[i + 2].toInt() and 0xFF))
        val gainDb = raw.toShort().toInt() / 512f
        val peakBits = b[i + 3].toInt() and 0xFF
        var peak: Float? = null
        if (peakBits in 1..32) {
            val bytes = (peakBits + 7) / 8
            if (i + 4 + bytes <= end) {
                var v = 0L
                for (k in 0 until bytes) v = (v shl 8) or (b[i + 4 + k].toLong() and 0xFF)
                peak = (v.toDouble() / (1L shl peakBits)).toFloat()
            }
        }
        return ReplayGain(gainDb, peak)
    }

    /* ---------------- FLAC ---------------- */

    private fun isFlac(b: ByteArray): Boolean =
        b.size >= 8 && b[0] == 'f'.code.toByte() && b[1] == 'L'.code.toByte() &&
            b[2] == 'a'.code.toByte() && b[3] == 'C'.code.toByte()

    private fun parseFlac(b: ByteArray): ReplayGain? {
        var pos = 4
        while (pos + 4 <= b.size) {
            val header = b[pos].toInt() and 0xFF
            val last = header and 0x80 != 0
            val type = header and 0x7F
            val size = ((b[pos + 1].toInt() and 0xFF) shl 16) or
                ((b[pos + 2].toInt() and 0xFF) shl 8) or (b[pos + 3].toInt() and 0xFF)
            val start = pos + 4
            if (start + size > b.size) return null
            if (type == 4) return parseVorbisComment(b, start, size)
            if (last) return null
            pos = start + size
        }
        return null
    }

    /** VORBIS_COMMENT: vendor, jumlah entri, lalu "KUNCI=nilai" UTF-8. */
    private fun parseVorbisComment(b: ByteArray, start: Int, size: Int): ReplayGain? {
        val end = start + size
        var i = start
        if (i + 4 > end) return null
        i += 4 + leInt(b, i)                     // lewati vendor
        if (i + 4 > end) return null
        val count = leInt(b, i)
        i += 4
        var gain: Float? = null
        var peak: Float? = null
        for (n in 0 until count) {
            if (i + 4 > end) break
            val len = leInt(b, i)
            i += 4
            if (len <= 0 || i + len > end) break
            val entry = String(b, i, len, Charsets.UTF_8)
            i += len
            val eq = entry.indexOf('=')
            if (eq <= 0) continue
            val key = entry.substring(0, eq).trim().lowercase()
            val value = entry.substring(eq + 1).trim()
            if (key == KEY_GAIN && gain == null) gain = parseDb(value)
            else if (key == KEY_PEAK && peak == null) peak = value.toFloatOrNull()
        }
        return gain?.let { ReplayGain(it, peak) }
    }

    /* ---------------- OGG (Vorbis & Opus) ---------------- */

    private fun isOgg(b: ByteArray): Boolean =
        b.size >= 4 && b[0] == 'O'.code.toByte() && b[1] == 'g'.code.toByte() &&
            b[2] == 'g'.code.toByte() && b[3] == 'S'.code.toByte()

    /**
     * Header komentar Ogg ada di paket kedua: `0x03 "vorbis"` untuk Vorbis dan
     * `"OpusTags"` untuk Opus. Isi keduanya blok Vorbis comment, jadi pembaca
     * FLAC di atas dipakai ulang apa adanya.
     */
    private fun parseOgg(b: ByteArray): ReplayGain? {
        for (packet in oggPackets(b, limit = 3)) {
            val sig = when {
                packet.size >= 7 && packet[0] == 0x03.toByte() &&
                    String(packet, 1, 6, Charsets.ISO_8859_1) == "vorbis" -> 7
                packet.size >= 8 &&
                    String(packet, 0, 8, Charsets.ISO_8859_1) == "OpusTags" -> 8
                else -> 0
            }
            if (sig > 0) return parseVorbisComment(packet, sig, packet.size - sig)
        }
        return null
    }

    /**
     * Kumpulkan [limit] paket pertama dengan menelusuri halaman Ogg.
     * Tabel segmen menentukan batas paket: panjang < 255 menandakan paket tamat
     * (paket boleh bersambung ke halaman berikutnya).
     */
    private fun oggPackets(b: ByteArray, limit: Int): List<ByteArray> {
        val packets = ArrayList<ByteArray>()
        var page = 0
        var pending = ByteArrayOutputStream()
        while (packets.size < limit && page + 27 <= b.size) {
            if (b[page] != 'O'.code.toByte() || b[page + 1] != 'g'.code.toByte() ||
                b[page + 2] != 'g'.code.toByte() || b[page + 3] != 'S'.code.toByte()
            ) break
            val segments = b[page + 26].toInt() and 0xFF
            val table = page + 27
            if (table + segments > b.size) break
            var data = table + segments
            var next = page + 28 + segments
            var stop = false
            for (s in 0 until segments) {
                val segLen = b[table + s].toInt() and 0xFF
                val end = minOf(data + segLen, b.size)
                if (end > data) pending.write(b, data, end - data)
                data = end
                next = data
                if (segLen < 255) {
                    packets.add(pending.toByteArray())
                    pending = ByteArrayOutputStream()
                    if (packets.size >= limit) { stop = true; break }
                }
                if (data >= b.size) { stop = true; break }
            }
            if (stop || next <= page) break
            page = next
        }
        return packets
    }

    /* ---------------- MP4 / M4A ---------------- */

    private fun isMp4(b: ByteArray): Boolean =
        b.size >= 12 && String(b, 4, 4, Charsets.ISO_8859_1) == "ftyp"

    private fun parseMp4(b: ByteArray, start: Int, end: Int): ReplayGain? {
        if (end <= start) return null
        val tags = HashMap<String, String>()
        walkMp4(b, start, end, 0, tags)
        val gain = tags[KEY_GAIN]?.let { parseDb(it) } ?: return null
        return ReplayGain(gain, tags[KEY_PEAK]?.toFloatOrNull())
    }

    /** Telusuri atom MP4 sampai daftar tag iTunes (`ilst`) di dalam `moov`. */
    private fun walkMp4(b: ByteArray, start: Int, end: Int, depth: Int, tags: MutableMap<String, String>) {
        if (depth > 6) return
        var pos = start
        while (pos + 8 <= end) {
            var size = beInt(b, pos).toLong() and 0xFFFFFFFFL
            val type = String(b, pos + 4, 4, Charsets.ISO_8859_1)
            var body = pos + 8
            if (size == 1L) {                                  // ukuran 64-bit
                if (body + 8 > end) return
                size = beLong(b, body)
                body += 8
            }
            if (size == 0L) size = (end - pos).toLong()        // sampai akhir
            val stop = minOf(pos + size, end.toLong()).toInt()
            if (stop <= body) return
            when (type) {
                "moov", "udta", "ilst" -> walkMp4(b, body, stop, depth + 1, tags)
                // `meta` adalah full box: ada 4 byte versi+flag sebelum isinya.
                "meta" -> walkMp4(b, minOf(body + 4, stop), stop, depth + 1, tags)
                "----" -> readFreeform(b, body, stop, tags)
            }
            pos = stop
        }
    }

    /** Atom bebas `----`: `mean` (ruang nama), `name` (kunci), `data` (nilai). */
    private fun readFreeform(b: ByteArray, start: Int, end: Int, tags: MutableMap<String, String>) {
        var key: String? = null
        var value: String? = null
        var pos = start
        while (pos + 8 <= end) {
            var size = beInt(b, pos).toLong() and 0xFFFFFFFFL
            if (size == 0L) size = (end - pos).toLong()
            val type = String(b, pos + 4, 4, Charsets.ISO_8859_1)
            val stop = minOf(pos + size, end.toLong()).toInt()
            when (type) {
                // name: 4 byte versi+flag lalu teks (kunci, mis. replaygain_track_gain)
                "name" -> key = text(b, minOf(pos + 12, stop), stop)
                // data: 4 byte penanda tipe + 4 byte lokal, baru teks
                "data" -> value = text(b, minOf(pos + 16, stop), stop)
            }
            if (stop <= pos + 8) return
            pos = stop
        }
        if (!key.isNullOrEmpty() && !value.isNullOrEmpty()) tags[key.lowercase()] = value
    }

    /** Cari atom `moov` di dalam potongan ekor file, lalu baca dari situ. */
    private fun parseMp4Tail(b: ByteArray): ReplayGain? {
        if (b.size < 8) return null
        for (i in 4 until b.size - 4) {
            if (b[i] != 'm'.code.toByte() || b[i + 1] != 'o'.code.toByte() ||
                b[i + 2] != 'o'.code.toByte() || b[i + 3] != 'v'.code.toByte()
            ) continue
            val declared = beInt(b, i - 4)
            if (declared < 8 && declared != 1) continue       // bukan kepala atom
            parseMp4(b, i + 4, b.size)?.let { return it }
        }
        return null
    }

    /* ---------------- util ---------------- */

    /** Ambil angka pertama, mis. "-6.54 dB" -> -6.54 (koma juga diterima). */
    fun parseDb(text: String): Float? =
        NUMBER.find(text)?.groupValues?.get(1)?.replace(',', '.')?.toFloatOrNull()

    private fun decodeText(b: ByteArray, start: Int, len: Int, enc: Int): String {
        if (len <= 0 || start < 0 || start + len > b.size) return ""
        val raw = b.copyOfRange(start, start + len)
        val cs = when (enc) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16                  // BOM menentukan urutan byte
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return String(raw, cs).trim('\u0000', ' ', '\uFEFF')
    }

    private fun syncSafe(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0x7F) shl 21) or ((b[at + 1].toInt() and 0x7F) shl 14) or
            ((b[at + 2].toInt() and 0x7F) shl 7) or (b[at + 3].toInt() and 0x7F)

    private fun leInt(b: ByteArray, at: Int): Int =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or
            ((b[at + 2].toInt() and 0xFF) shl 16) or ((b[at + 3].toInt() and 0xFF) shl 24)

    private fun beInt(b: ByteArray, at: Int): Int =
        ((b[at].toInt() and 0xFF) shl 24) or ((b[at + 1].toInt() and 0xFF) shl 16) or
            ((b[at + 2].toInt() and 0xFF) shl 8) or (b[at + 3].toInt() and 0xFF)

    private fun beLong(b: ByteArray, at: Int): Long {
        var v = 0L
        for (k in 0 until 8) v = (v shl 8) or (b[at + k].toLong() and 0xFF)
        return v
    }

    /** Teks atom MP4 (UTF-8), dipangkas dari byte nol. */
    private fun text(b: ByteArray, from: Int, to: Int): String {
        if (to <= from || from < 0 || to > b.size) return ""
        return String(b, from, to - from, Charsets.UTF_8).trim('\u0000', ' ')
    }
}
