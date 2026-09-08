package com.zenn889.putar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Statistik & sejarah pemutaran ringan: 25 lagu terakhir, jumlah putar,
 * dan menit didengar per lagu. Disimpan sebagai satu JSON di prefs.
 */
object StatsStore {

    private const val PREFS = "putar_stats"
    private const val KEY = "data"
    private const val RECENT_CAP = 25

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun recent(c: Context): List<String> = runCatching {
        val o = JSONObject(prefs(c).getString(KEY, "{}") ?: "{}")
        val arr = o.optJSONArray("recent") ?: return emptyList()
        buildList {
            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotEmpty() }?.let { add(it) }
        }
    }.getOrDefault(emptyList())

    fun playsMap(c: Context): Map<String, Int> = runCatching {
        val o = JSONObject(prefs(c).getString(KEY, "{}") ?: "{}")
        val obj = o.optJSONObject("plays") ?: return emptyMap()
        buildMap { obj.keys().forEach { k -> put(k, obj.optInt(k, 0)) } }
    }.getOrDefault(emptyMap())

    fun minutesMap(c: Context): Map<String, Long> = runCatching {
        val o = JSONObject(prefs(c).getString(KEY, "{}") ?: "{}")
        val obj = o.optJSONObject("mins") ?: return emptyMap()
        buildMap { obj.keys().forEach { k -> put(k, obj.optLong(k, 0L)) } }
    }.getOrDefault(emptyMap())

    fun totalMinutes(c: Context): Long = minutesMap(c).values.sum()

    /** Catat satu lagu mulai diputar (sejarah + jumlah putar). */
    fun recordPlay(c: Context, uri: String) {
        val o = runCatching { JSONObject(prefs(c).getString(KEY, "{}") ?: "{}") }
            .getOrElse { JSONObject() }
        val recent = o.optJSONArray("recent")?.let { arr ->
            buildList {
                for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotEmpty() }?.let { add(it) }
            }
        } ?: emptyList()
        val nextRecent = (listOf(uri) + recent.filterNot { it == uri }).take(RECENT_CAP)
        val plays = o.optJSONObject("plays") ?: JSONObject()
        plays.put(uri, plays.optInt(uri, 0) + 1)
        o.put("recent", JSONArray().also { ja -> nextRecent.forEach { ja.put(it) } })
        o.put("plays", plays)
        prefs(c).edit().putString(KEY, o.toString()).apply()
    }

    /** Tambah menit didengar (dipanggil terbatas ~tiap 20 dtk). */
    fun addMinutes(c: Context, uri: String, ms: Long) {
        val o = runCatching { JSONObject(prefs(c).getString(KEY, "{}") ?: "{}") }
            .getOrElse { JSONObject() }
        val mins = o.optJSONObject("mins") ?: JSONObject()
        mins.put(uri, mins.optLong(uri, 0L) + ms)
        o.put("mins", mins)
        prefs(c).edit().putString(KEY, o.toString()).apply()
    }
}
