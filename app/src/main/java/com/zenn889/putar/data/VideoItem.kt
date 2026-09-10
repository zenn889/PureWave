package com.zenn889.putar.data

import android.net.Uri

/** Satu video dari pustaka MediaStore (offline, dari storage HP). */
data class VideoItem(
    val mediaId: Long,
    val contentUri: Uri,
    val title: String,
    val durationMs: Long,
    val dateAddedMs: Long,
    val folder: String?,
    /** path file fisik (utk subtitle & resume); null bila tak tersedia */
    val filePath: String? = null
)
