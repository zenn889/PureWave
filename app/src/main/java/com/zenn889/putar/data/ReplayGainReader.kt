package com.zenn889.putar.data

import java.io.File

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
 *
 * Belum didukung (sengaja, ditulis di README): OGG/Opus (struktur halaman) dan
 * MP4/M4A freeform atom. File tanpa tag ini tidak terpengaruh sama sekali —
 * gain tetap 1.0 — jadi pustaka campur tetap aman.
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
        val len = minOf(file.length(), HEAD_BYTES.toLong()).toInt()
        if (len < 16) return null
        val buf = ByteArray(len)
        file.inputStream().use { stream -> stream.read(buf) }
        parse(buf)
    }.getOrNull()

    fun parse(bytes: ByteArray): ReplayGain? = when {
        isId3(bytes) -> parseId3(bytes)
        isFlac(bytes) -> parseFlac(bytes)
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
}
