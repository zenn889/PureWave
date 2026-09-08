package com.zenn889.putar.data

import android.net.Uri

/** Satu lagu dari pustaka MediaStore. */
data class Track(
    val mediaId: Long,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumId: Long?,
    /** lokasi folder (RELATIVE_PATH API 29+, path induk utk versi lama); null = tak diketahui */
    val folder: String? = null
) {
    val displayArtist: String
        get() = artist.ifBlank { "Artis tak dikenal" }
}

/** Satu album dari MediaStore. */
data class Album(
    val albumId: Long,
    val title: String,
    val artist: String,
    val songCount: Int
) {
    val displayArtist: String
        get() = artist.ifBlank { "Artis tak dikenal" }
}

/** Grup folder di pustaka. key kosong = lagu tanpa folder (akar). */
data class FolderItem(
    val key: String,
    val name: String,
    val path: String,
    val songCount: Int
)
