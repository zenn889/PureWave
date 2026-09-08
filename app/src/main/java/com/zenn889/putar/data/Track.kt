package com.zenn889.putar.data

import android.net.Uri

/** Satu lagu dari pustaka MediaStore. */
data class Track(
    val mediaId: Long,
    val contentUri: Uri,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumId: Long?
) {
    val displayArtist: String
        get() = artist.ifBlank { "Artis tak dikenal" }
}
