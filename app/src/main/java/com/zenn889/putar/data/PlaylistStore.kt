package com.zenn889.putar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Playlist buatan pengguna: nama + daftar content-URI lagu. */
data class Playlist(
    val name: String,
    val uris: List<String>
)

object PlaylistStore {

    private const val PREFS = "putar_playlists"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun list(c: Context): List<Playlist> = runCatching {
        val arr = JSONArray(prefs(c).getString("items", "[]") ?: "[]")
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                if (name.isBlank()) continue
                val ua = o.optJSONArray("uris") ?: continue
                val uris = buildList {
                    for (j in 0 until ua.length()) {
                        ua.optString(j).takeIf { it.isNotEmpty() }?.let { add(it) }
                    }
                }
                add(Playlist(name, uris))
            }
        }
    }.getOrDefault(emptyList())

    fun create(c: Context, name: String, initialUri: String? = null): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        val items = list(c)
        if (items.any { it.name.equals(clean, ignoreCase = true) }) return false
        val newList = items + Playlist(clean, listOfNotNull(initialUri))
        save(c, newList)
        return true
    }

    fun delete(c: Context, name: String) {
        save(c, list(c).filterNot { it.name == name })
    }

    fun addTrack(c: Context, name: String, uri: String) {
        val updated = list(c).map { pl ->
            if (pl.name == name && uri !in pl.uris) pl.copy(uris = pl.uris + uri) else pl
        }
        save(c, updated)
    }

    fun removeTrack(c: Context, name: String, uri: String) {
        val updated = list(c).map { pl ->
            if (pl.name == name) pl.copy(uris = pl.uris.filterNot { it == uri }) else pl
        }
        save(c, updated)
    }

    /**
     * Geser satu lagu di dalam playlist (dipakai gagang seret & tombol
     * naik/turun). `from`/`to` adalah indeks pada daftar URI playlist itu.
     * Mengembalikan true kalau urutannya benar-benar berubah.
     */
    fun moveTrack(c: Context, name: String, from: Int, to: Int): Boolean {
        val items = list(c)
        val target = items.firstOrNull { it.name == name } ?: return false
        if (from !in target.uris.indices) return false
        val dest = to.coerceIn(0, target.uris.size - 1)
        if (from == dest) return false
        val moved = target.uris.toMutableList()
        moved.add(dest, moved.removeAt(from))
        save(c, items.map { if (it.name == name) it.copy(uris = moved) else it })
        return true
    }

    /** Ganti seluruh isi (dipakai restore cadangan). */
    fun replaceAll(c: Context, playlists: List<Playlist>) {
        save(c, playlists)
    }

    private fun save(c: Context, playlists: List<Playlist>) {
        val arr = JSONArray()
        playlists.forEach { pl ->
            val o = JSONObject()
            o.put("name", pl.name)
            val ua = JSONArray()
            pl.uris.forEach { ua.put(it) }
            o.put("uris", ua)
            arr.put(o)
        }
        prefs(c).edit().putString("items", arr.toString()).apply()
    }
}
