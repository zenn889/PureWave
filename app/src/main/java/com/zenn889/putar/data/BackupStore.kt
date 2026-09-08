package com.zenn889.putar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Cadangkan/pulihkan data pribadi (favorit & playlist) ke satu JSON. */
object BackupStore {

    fun buildJson(c: Context): String {
        val root = JSONObject()
        root.put("app", "PureWave")
        root.put("version", 1)

        val favs = JSONArray()
        FavStore.load(c).sorted().forEach { favs.put(it) }
        root.put("favorites", favs)

        val pl = JSONArray()
        PlaylistStore.list(c).forEach { p ->
            val o = JSONObject()
            o.put("name", p.name)
            val ua = JSONArray()
            p.uris.forEach { ua.put(it) }
            o.put("uris", ua)
            pl.put(o)
        }
        root.put("playlists", pl)
        return root.toString(2)
    }

    /** Terapkan isi cadangan; true bila berhasil. */
    fun applyJson(c: Context, text: String): Boolean = runCatching {
        val root = JSONObject(text)
        val favs = buildList {
            val arr = root.optJSONArray("favorites") ?: JSONArray()
            for (i in 0 until arr.length()) {
                arr.optString(i).takeIf { it.isNotEmpty() }?.let { add(it) }
            }
        }
        val lists = buildList {
            val arr = root.optJSONArray("playlists") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name").trim()
                if (name.isEmpty()) continue
                val uris = buildList {
                    val ua = o.optJSONArray("uris") ?: JSONArray()
                    for (j in 0 until ua.length()) {
                        ua.optString(j).takeIf { it.isNotEmpty() }?.let { add(it) }
                    }
                }
                add(Playlist(name, uris))
            }
        }
        FavStore.replaceAll(c, favs)
        PlaylistStore.replaceAll(c, lists)
        true
    }.getOrDefault(false)
}
