package com.zenn889.putar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ingatan sesi: antrian terakhir (content-URI lagu), indeks, dan posisi —
 * dipakai untuk auto-resume saat app dibuka lagi.
 */
object SessionStore {

    private const val PREFS = "putar_session"
    private const val KEY_QUEUE = "queue"
    private const val KEY_INDEX = "index"
    private const val KEY_POS = "pos"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveQueue(c: Context, uris: List<String>, index: Int, posMs: Long) {
        runCatching {
            val arr = JSONArray()
            uris.forEach { arr.put(it) }
            prefs(c).edit()
                .putString(KEY_QUEUE, arr.toString())
                .putInt(KEY_INDEX, index)
                .putLong(KEY_POS, posMs)
                .apply()
        }
    }

    fun savePosition(c: Context, index: Int, posMs: Long) {
        runCatching {
            prefs(c).edit()
                .putInt(KEY_INDEX, index)
                .putLong(KEY_POS, posMs)
                .apply()
        }
    }

    fun load(c: Context): Triple<List<String>, Int, Long>? = runCatching {
        val p = prefs(c)
        val raw = p.getString(KEY_QUEUE, null) ?: return@runCatching null
        val arr = JSONArray(raw)
        val uris = buildList {
            for (i in 0 until arr.length()) {
                val u = arr.optString(i)
                if (u.isNotEmpty()) add(u)
            }
        }
        if (uris.isEmpty()) return@runCatching null
        Triple(uris, p.getInt(KEY_INDEX, 0).coerceIn(0, uris.size - 1), p.getLong(KEY_POS, 0L))
    }.getOrNull()

    fun clear(c: Context) {
        prefs(c).edit().remove(KEY_QUEUE).remove(KEY_INDEX).remove(KEY_POS).apply()
    }
}
