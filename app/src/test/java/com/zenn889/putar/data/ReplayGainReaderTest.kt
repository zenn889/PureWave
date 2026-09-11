package com.zenn889.putar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Uji pembacaan tag ReplayGain (ID3v2 & FLAC) dan perhitungan gain-nya.
 * Semua data dibuat sintetis di memori — tidak butuh file audio nyata.
 */
class ReplayGainReaderTest {

    /* ---------- pembantu pembuat tag sintetis ---------- */

    private fun syncSafe(n: Int) = byteArrayOf(
        ((n shr 21) and 0x7F).toByte(), ((n shr 14) and 0x7F).toByte(),
        ((n shr 7) and 0x7F).toByte(), (n and 0x7F).toByte()
    )

    private fun beInt(n: Int) = byteArrayOf(
        ((n shr 24) and 0xFF).toByte(), ((n shr 16) and 0xFF).toByte(),
        ((n shr 8) and 0xFF).toByte(), (n and 0xFF).toByte()
    )

    private fun leInt(n: Int) = byteArrayOf(
        (n and 0xFF).toByte(), ((n shr 8) and 0xFF).toByte(),
        ((n shr 16) and 0xFF).toByte(), ((n shr 24) and 0xFF).toByte()
    )

    /** Frame TXXX (deskripsi + nilai), encoding UTF-8. */
    private fun txxx(description: String, value: String): ByteArray {
        val payload = byteArrayOf(3) +
            description.toByteArray(Charsets.UTF_8) + byteArrayOf(0) +
            value.toByteArray(Charsets.UTF_8)
        return "TXXX".toByteArray(Charsets.ISO_8859_1) + beInt(payload.size) +
            byteArrayOf(0, 0) + payload
    }

    /** Frame RVA2: id, channel 1, gain int16 (dB x512), bit peak, peak. */
    private fun rva2(gainDb: Float, peak: Float?): ByteArray {
        val peakBits = if (peak == null) 0 else 16
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0))                                  // id kosong
        out.write(1)                                               // master channel
        val raw = Math.round(gainDb * 512f)
        out.write(byteArrayOf(((raw shr 8) and 0xFF).toByte(), (raw and 0xFF).toByte()))
        out.write(peakBits)
        if (peak != null) {
            val v = Math.round(peak * (1 shl 16))
            out.write(byteArrayOf(((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte()))
        }
        val payload = out.toByteArray()
        return "RVA2".toByteArray(Charsets.ISO_8859_1) + beInt(payload.size) +
            byteArrayOf(0, 0) + payload
    }

    private fun id3(major: Int, frames: List<ByteArray>): ByteArray {
        var body = ByteArray(0)
        frames.forEach { body += it }
        return "ID3".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(major.toByte(), 0, 0) + syncSafe(body.size) + body
    }

    private fun flac(vararg entries: Pair<String, String>): ByteArray {
        val vendor = "purewave-test"
        val vc = ByteArrayOutputStream()
        vc.write(leInt(vendor.length)); vc.write(vendor.toByteArray(Charsets.UTF_8))
        vc.write(leInt(entries.size))
        entries.forEach { (k, v) ->
            val s = "$k=$v".toByteArray(Charsets.UTF_8)
            vc.write(leInt(s.size)); vc.write(s)
        }
        val block = vc.toByteArray()
        val size = byteArrayOf(
            ((block.size shr 16) and 0xFF).toByte(),
            ((block.size shr 8) and 0xFF).toByte(),
            (block.size and 0xFF).toByte()
        )
        return "fLaC".toByteArray(Charsets.ISO_8859_1) +
            byteArrayOf(0x84.toByte()) + size + block      // last block, tipe 4
    }

    /* ---------- tes ---------- */

    @Test
    fun `ID3v2_3 TXXX gain dan peak terbaca`() {
        val bytes = id3(
            3, listOf(
                txxx("REPLAYGAIN_TRACK_GAIN", "-6.54 dB"),
                txxx("REPLAYGAIN_TRACK_PEAK", "0.988")
            )
        )
        val rg = ReplayGainReader.parse(bytes)
        assertEquals(-6.54f, rg!!.trackGainDb, 0.001f)
        assertEquals(0.988f, rg.trackPeak!!, 0.001f)
    }

    @Test
    fun `nama kunci huruf kecil juga dikenali`() {
        val bytes = id3(3, listOf(txxx("replaygain_track_gain", "-2 dB")))
        assertEquals(-2f, ReplayGainReader.parse(bytes)!!.trackGainDb, 0.001f)
    }

    @Test
    fun `ID3v2_4 memakai ukuran syncsafe`() {
        // nilai > 128 byte: hanya benar kalau panjang frame dibaca sebagai syncsafe
        val panjang = "-1.23 dB" + " ".repeat(200)
        val bytes = id3(4, listOf(txxx("REPLAYGAIN_TRACK_GAIN", panjang)))
        assertEquals(-1.23f, ReplayGainReader.parse(bytes)!!.trackGainDb, 0.001f)
    }

    @Test
    fun `RVA2 dipakai kalau TXXX tidak ada`() {
        val bytes = id3(3, listOf(rva2(-6.0f, 0.5f)))
        val rg = ReplayGainReader.parse(bytes)!!
        assertEquals(-6.0f, rg.trackGainDb, 0.01f)
        assertEquals(0.5f, rg.trackPeak!!, 0.01f)
    }

    @Test
    fun `FLAC vorbis comment terbaca`() {
        val bytes = flac(
            "ARTIST" to "Sheila On 7",
            "REPLAYGAIN_TRACK_GAIN" to "+3.20 dB",
            "REPLAYGAIN_TRACK_PEAK" to "0.977"
        )
        val rg = ReplayGainReader.parse(bytes)!!
        assertEquals(3.20f, rg.trackGainDb, 0.001f)
        assertEquals(0.977f, rg.trackPeak!!, 0.001f)
    }

    @Test
    fun `file tanpa tag ReplayGain menghasilkan null`() {
        assertNull(ReplayGainReader.parse(flac("ARTIST" to "Tanpa Gain")))
        assertNull(ReplayGainReader.parse(id3(3, listOf(txxx("TXXX", "bukan gain")))))
        assertNull(ReplayGainReader.parse(ByteArray(64)))
    }

    @Test
    fun `tag rusak atau terpotong tidak melempar exception`() {
        val penuh = id3(3, listOf(txxx("REPLAYGAIN_TRACK_GAIN", "-6.54 dB")))
        assertNull(ReplayGainReader.parse(penuh.copyOf(12)))     // header terpotong
        assertNull(ReplayGainReader.parse(ByteArray(0)))
    }

    @Test
    fun `parseDb menerima dB dan koma`() {
        assertEquals(-6.54f, ReplayGainReader.parseDb("-6.54 dB")!!, 0.001f)
        assertEquals(3f, ReplayGainReader.parseDb("+3 dB")!!, 0.001f)
        assertEquals(0.5f, ReplayGainReader.parseDb("0,5 dB")!!, 0.001f)
        assertNull(ReplayGainReader.parseDb("tidak ada angka"))
    }

    /* ---------- perhitungan gain ---------- */

    @Test
    fun `tanpa tag gain tetap 1`() {
        assertEquals(1f, VolumeNorm.linearFor(null), 0.0001f)
    }

    @Test
    fun `gain negatif mengecilkan sesuai desibel`() {
        // -6.02 dB ~ setengah amplitudo
        assertEquals(0.5f, VolumeNorm.linearFor(ReplayGain(-6.02f)), 0.01f)
    }

    @Test
    fun `peak mencegah clipping`() {
        // +6 dB (x2) tapi peak 0.8 -> ditahan di 1/0.8 = 1.25
        val g = VolumeNorm.linearFor(ReplayGain(6f, 0.8f))
        assertEquals(1.25f, g, 0.01f)
        assertTrue(g * 0.8f <= 1.001f)
    }

    @Test
    fun `gain ekstrem dibatasi rentang aman`() {
        assertEquals(2f, VolumeNorm.linearFor(ReplayGain(30f)), 0.0001f)
        assertEquals(0.25f, VolumeNorm.linearFor(ReplayGain(-40f)), 0.0001f)
    }
}
