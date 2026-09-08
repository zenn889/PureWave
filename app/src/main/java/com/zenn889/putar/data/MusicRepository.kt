package com.zenn889.putar.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Akses pustaka audio perangkat (MediaStore) — murni offline. */
class MusicRepository(private val context: Context) {

    suspend fun loadLibrary(): List<Track> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )
        val selection = "${MediaStore.Audio.Media.DURATION} > 3000" // buang bunyi < 3 detik
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        try {
            context.contentResolver.query(collection, projection, selection, null, order)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val albumId = if (c.isNull(iAlbum)) null else c.getLong(iAlbum)
                    result.add(
                        Track(
                            mediaId = id,
                            contentUri = ContentUris.withAppendedId(collection, id),
                            title = c.getString(iTitle)?.ifBlank { "Tanpa judul" } ?: "Tanpa judul",
                            artist = c.getString(iArtist)?.trim().orEmpty().let {
                                if (it.equals("<unknown>", true)) "" else it
                            },
                            durationMs = c.getLong(iDur),
                            albumId = albumId
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // pustaka kosong / provider bermasalah
        }
        result
    }

    companion object {
        /** URI art album klasik (content://media/external/audio/albumart/<id>). */
        fun albumArtUri(albumId: Long?): Uri? {
            if (albumId == null) return null
            return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"), albumId
            )
        }
    }
}
