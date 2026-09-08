package com.zenn889.putar.data

import android.content.Context
import org.json.JSONArray

/** Favorit: kumpulan content-URI lagu yang ditandai bintang. */
object FavStore {

    private const val PREFS = "putar_favs"
    private const val KEY = "uris"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(c: Context): Set<String> = runCatching {
        val arr = JSONArray(prefs(c).getString(KEY, "[]") ?: "[]")
        buildSet { for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotEmpty() }?.let { add(it) } }
    }.getOrDefault(emptySet())

    fun isFavorite(c: Context, uri: String): Boolean = uri in load(c)

    /** Tambah/hapus; mengembalikan status baru. */
    fun toggle(c: Context, uri: String): Boolean {
        val current = load(c).toMutableSet()
        val nowFav = uri !in current
        if (nowFav) current.add(uri) else current.remove(uri)
        save(c, current)
        return nowFav
    }

    private fun save(c: Context, set: Set<String>) {
        val arr = JSONArray()
        set.sorted().forEach { arr.put(it) }
        prefs(c).edit().putString(KEY, arr.toString()).apply()
    }
}
