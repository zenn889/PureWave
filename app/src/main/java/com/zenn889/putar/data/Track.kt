package com.zenn889.putar.data

/** Satu lagu dari pustaka MediaStore. */
data class Track(
    val mediaId: Long,
    /**
     * Content-URI lagu, disimpan sebagai String (bukan android.net.Uri) supaya
     * model ini murni: bisa dipakai di unit test JVM tanpa emulator, dan tidak
     * perlu `.toString()` di puluhan tempat pemakaian.
     */
    val contentUri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val albumId: Long?,
    /** lokasi folder (RELATIVE_PATH API 29+, path induk utk versi lama); null = tak diketahui */
    val folder: String? = null,
    /** nama album (utk sortir) */
    val albumTitle: String? = null,
    /** epoch millis saat file ditambahkan (utk sortir terbaru) */
    val dateAddedMs: Long = 0L,
    /** path file fisik (utk mencari .lrc di sebelah lagu); null bila tak tersedia */
    val filePath: String? = null
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
