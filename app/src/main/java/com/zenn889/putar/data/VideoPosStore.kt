package com.zenn889.putar.data

import android.content.Context
import org.json.JSONObject

/** Resume posisi tonton video (offline, per URI). */
object VideoPosStore {

    private const val PREFS = "putar_video_pos"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun position(c: Context, uri: String): Long = runCatching {
        val o = JSONObject(prefs(c).getString("pos", "{}") ?: "{}")
        o.optLong(uri, 0L)
    }.getOrDefault(0L)

    fun save(c: Context, uri: String, positionMs: Long) {
        runCatching {
            val o = JSONObject(prefs(c).getString("pos", "{}") ?: "{}")
            // simpan hanya bila masuk akal (>5 dtk)
            if (positionMs < 5_000L) o.remove(uri) else o.put(uri, positionMs)
            // batasi ukuran: kalau terlalu banyak, reset sederhana
            if (o.length() > 400) {
                val fresh = JSONObject()
                fresh.put(uri, positionMs)
                prefs(c).edit().putString("pos", fresh.toString()).apply()
                return
            }
            prefs(c).edit().putString("pos", o.toString()).apply()
        }
    }

    fun clear(c: Context, uri: String) {
        runCatching {
            val o = JSONObject(prefs(c).getString("pos", "{}") ?: "{}")
            o.remove(uri)
            prefs(c).edit().putString("pos", o.toString()).apply()
        }
    }
}
