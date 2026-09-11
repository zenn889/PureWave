package com.zenn889.putar.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/**
 * Normalisasi volume antar lagu (ReplayGain).
 *
 * Alurnya: tag ReplayGain dibaca dari file lagu → diubah jadi gain linear
 * (dengan pencegahan clipping memakai nilai peak) → diterapkan sebagai volume
 * pemutar. Lagu tanpa tag dibiarkan apa adanya (gain 1.0), jadi pustaka campur
 * tidak rusak.
 *
 * Satu sumber kebenaran untuk gain aktif: [baseGain]. Fade sleep timer di UI
 * mengalikan nilai ini, bukan menimpanya.
 */
object VolumeNorm {

    private const val PREFS = "putar_prefs"
    private const val KEY = "volume_norm"

    /** Batas aman supaya gain ekstrem tidak membuat suara pecah atau hilang. */
    private const val MIN_GAIN = 0.25f
    private const val MAX_GAIN = 2.0f

    /** Cache gain per content-URI (baca file tidak diulang tiap ganti lagu). */
    private val cache = object : LinkedHashMap<String, Float>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Float>): Boolean =
            size > 500
    }

    /** Gain yang sedang berlaku di pemutar (1.0 = tanpa perubahan). */
    @Volatile
    var baseGain: Float = 1f
        private set

    /** Dipakai pemutar saat ganti lagu / setelan diubah. */
    fun setBaseGain(gain: Float) {
        baseGain = gain.coerceIn(MIN_GAIN, MAX_GAIN)
    }

    fun enabled(c: Context): Boolean =
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(c: Context, on: Boolean) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY, on)
            .apply()
        if (!on) baseGain = 1f
    }

    /** Gain untuk satu lagu. 1.0 kalau fitur mati atau lagunya tanpa tag. */
    fun gainFor(c: Context, uri: String, filePath: String?): Float {
        if (!enabled(c)) return 1f
        cache[uri]?.let { return it }
        val path = filePath ?: return 1f
        val gain = linearFor(ReplayGainReader.read(File(path)))
        cache[uri] = gain
        return gain
    }

    /**
     * ReplayGain → gain linear. Kalau `peak` diketahui dan gain akan melewati
     * 1.0, gain ditahan di 1/peak supaya tidak clipping (perilaku ReplayGain).
     */
    fun linearFor(rg: ReplayGain?): Float {
        if (rg == null) return 1f
        var gain = Math.pow(10.0, rg.trackGainDb / 20.0).toFloat()
        val peak = rg.trackPeak
        if (peak != null && peak > 0f && gain * peak > 1f) gain = 1f / peak
        return gain.coerceIn(MIN_GAIN, MAX_GAIN)
    }

    /**
     * Path file untuk sebuah item. URI `file://` langsung dipakai; URI
     * MediaStore ditanyakan ke MediaStore (kolom DATA masih satu-satunya cara
     * menemukan file di disk, sama seperti pencarian lirik .lrc).
     */
    @Suppress("DEPRECATION")
    fun filePathFor(c: Context, uriString: String): String? = runCatching {
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") return@runCatching uri.path
        c.contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.DATA),
            null, null, null
        )?.use { cur ->
            if (cur.moveToFirst()) cur.getString(0) else null
        }
    }.getOrNull()

    fun clearCache() = cache.clear()
}
