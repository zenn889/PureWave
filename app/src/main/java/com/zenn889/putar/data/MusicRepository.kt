package com.zenn889.putar.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Akses pustaka audio perangkat (MediaStore) — murni offline. */
class MusicRepository(private val context: Context) {

    suspend fun loadLibrary(): List<Track> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val useRelative = Build.VERSION.SDK_INT >= 29
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DATE_ADDED)
            add(if (useRelative) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA)
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.DURATION} > 3000" // buang bunyi < 3 detik
        val order = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        try {
            context.contentResolver.query(collection, projection, selection, null, order)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iAlbumName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iFolder = c.getColumnIndexOrThrow(
                    if (useRelative) MediaStore.MediaColumns.RELATIVE_PATH
                    else MediaStore.MediaColumns.DATA
                )
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val albumId = if (c.isNull(iAlbum)) null else c.getLong(iAlbum)
                    val rawFolder = c.getString(iFolder)
                    val folder = when {
                        useRelative -> rawFolder?.trim('/')?.takeIf { it.isNotEmpty() }
                        rawFolder.isNullOrEmpty() -> null
                        else -> File(rawFolder).parent?.let { p ->
                            // "/storage/emulated/0/Music/X" -> "Music/X"
                            p.removePrefix("/storage/emulated/0/")
                                .removePrefix("/sdcard/")
                                .trim('/')
                                .takeIf { it.isNotEmpty() }
                        }
                    }
                    result.add(
                        Track(
                            mediaId = id,
                            contentUri = ContentUris.withAppendedId(collection, id),
                            title = c.getString(iTitle)?.ifBlank { "Tanpa judul" } ?: "Tanpa judul",
                            artist = c.getString(iArtist)?.trim().orEmpty().let {
                                if (it.equals("<unknown>", true)) "" else it
                            },
                            durationMs = c.getLong(iDur),
                            albumId = albumId,
                            folder = folder,
                            albumTitle = c.getString(iAlbumName),
                            dateAddedMs = if (c.isNull(iDate)) 0L else c.getLong(iDate) * 1000L
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // pustaka kosong / provider bermasalah
        }
        result
    }

    suspend fun loadAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val result = mutableListOf<Album>()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )
        val order = "${MediaStore.Audio.Albums.ALBUM} COLLATE NOCASE ASC"
        try {
            context.contentResolver.query(collection, projection, null, null, order)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
                val iCount = c.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
                while (c.moveToNext()) {
                    val artist = c.getString(iArtist)?.trim().orEmpty()
                    result.add(
                        Album(
                            albumId = c.getLong(iId),
                            title = c.getString(iTitle)?.ifBlank { "Album Tanpa Judul" }
                                ?: "Album Tanpa Judul",
                            artist = if (artist.equals("<unknown>", true)) "" else artist,
                            songCount = c.getInt(iCount)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // abaikan
        }
        result
    }

    suspend fun loadVideos(): List<VideoItem> = withContext(Dispatchers.IO) {
        val result = mutableListOf<VideoItem>()
        try {
            val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.TITLE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED
            )
            context.contentResolver.query(
                collection, projection, null, null, null
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val title = c.getString(iTitle)?.trim()
                        ?.ifBlank { "Video $id" } ?: "Video $id"
                    val dur = c.getLong(iDur).coerceAtLeast(0L)
                    if (dur < 1_000L) continue // abaikan klip/efek super pendek
                    result.add(
                        VideoItem(
                            mediaId = id,
                            contentUri = uri,
                            title = title,
                            durationMs = dur,
                            dateAddedMs = c.getLong(iDate) * 1000L,
                            folder = null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // abaikan
        }
        result.sortedBy { it.title.lowercase() }
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
