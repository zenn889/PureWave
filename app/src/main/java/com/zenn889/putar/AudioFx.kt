package com.zenn889.putar

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer

/**
 * Equalizer + Bass Boost native (android.media.audiofx), ditempelkan ke sesi
 * audio ExoPlayer. Persist di SharedPreferences; semua operasi dari UI/main.
 */
object AudioFx {

    const val BAND_COUNT = 5
    val BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3,6 kHz", "14 kHz")
    private const val PREFS = "putar_audiofx"

    // preset: dB per pita (60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz)
    val PRESET_NAMES = listOf("Normal", "Pop", "Rock", "Jazz", "Klasik", "Dance", "Bass")
    private val PRESET_TABLE = mapOf(
        "Pop" to intArrayOf(-1, 2, 4, 2, -1),
        "Rock" to intArrayOf(4, 2, -2, 1, 3),
        "Jazz" to intArrayOf(2, 1, -1, 1, 3),
        "Klasik" to intArrayOf(3, 1, -1, 1, 3),
        "Dance" to intArrayOf(5, 3, 0, 1, 4),
        "Bass" to intArrayOf(6, 4, 0, -1, -2)
    )

    private var eq: Equalizer? = null
    private var bass: BassBoost? = null
    private var sessionId = 0

    /** false = belum pernah berhasil menempel ke sesi (perangkat tanpa efek / belum ada lagu). */
    @Volatile
    var available = false
        private set

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(c: Context): Boolean = prefs(c).getBoolean("enabled", false)

    fun preset(c: Context): String = prefs(c).getString("preset", "Normal") ?: "Normal"

    fun bassLevel(c: Context): Int = prefs(c).getInt("bass", 0) // 0..1000

    fun bandLevel(c: Context, index: Int): Int = prefs(c).getInt("band$index", 0) // mB

    fun currentSession(): Int = sessionId

    fun setEnabled(c: Context, value: Boolean) {
        prefs(c).edit().putBoolean("enabled", value).apply()
        applyCurrent(c)
    }

    fun setPreset(c: Context, name: String) {
        val p = prefs(c).edit().putString("preset", name)
        val table = PRESET_TABLE[name]
        if (name == "Normal") {
            for (i in 0 until BAND_COUNT) p.putInt("band$i", 0)
        } else if (table != null) {
            for (i in 0 until BAND_COUNT) p.putInt("band$i", table[i] * 100)
        }
        p.apply()
        applyCurrent(c)
    }

    fun setBand(c: Context, index: Int, millibels: Int) {
        // sentuh pita = preset jadi Kustom
        prefs(c).edit()
            .putString("preset", "Kustom")
            .putInt("band$index", millibels)
            .apply()
        applyCurrent(c)
    }

    fun setBass(c: Context, strength: Int) {
        prefs(c).edit().putInt("bass", strength.coerceIn(0, 1000)).apply()
        applyCurrent(c)
    }

    fun reset(c: Context) {
        prefs(c).edit().clear().apply()
        applyCurrent(c)
    }

    /** Tempel efek ke sesi audio aktif (dipanggil service saat audioSessionId berubah). */
    fun attach(context: Context, session: Int) {
        if (session <= 0) return
        release()
        sessionId = session
        available = false
        try {
            val eq = Equalizer(0, session)
            val bb = BassBoost(0, session)
            val c = context.applicationContext
            val enabled = isEnabled(c)

            if (enabled) {
                val range = eq.bandLevelRange // short[]
                val min = range[0].toInt()
                val max = range[1].toInt()
                val bandCount = eq.numberOfBands.toInt().coerceAtMost(BAND_COUNT)
                for (i in 0 until bandCount) {
                    val mB = bandLevel(c, i).coerceIn(min, max)
                    eq.setBandLevel(i.toShort(), mB.toShort())
                }
                val bs = bassLevel(c).coerceIn(0, 1000)
                bb.setStrength(bs.toShort())
                bb.enabled = bs > 0
            }

            eq.enabled = enabled
            this.eq = eq
            this.bass = bb
            available = true
        } catch (_: Exception) {
            release()
            available = false
        }
    }

    /** Terapkan ulang ke sesi yang sedang aktif (dipanggil UI setelah ubah setelan). */
    fun applyCurrent(context: Context) {
        if (sessionId > 0) attach(context, sessionId)
    }

    fun release() {
        try { eq?.release() } catch (_: Exception) { }
        try { bass?.release() } catch (_: Exception) { }
        eq = null
        bass = null
    }
}
