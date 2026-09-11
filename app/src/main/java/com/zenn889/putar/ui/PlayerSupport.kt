package com.zenn889.putar.ui

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Track
import kotlin.math.abs

internal fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(contentUri)
        .setUri(Uri.parse(contentUri))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(displayArtist)
                .setArtworkUri(MusicRepository.albumArtUri(albumId))
                .build()
        )
        .build()

internal fun currentMediaItemUri(c: MediaController?): String? =
    c?.currentMediaItem?.mediaId

internal fun readMirror(player: Player): PlayerMirror {
    val meta = player.mediaMetadata
    val has = player.mediaItemCount > 0
    return PlayerMirror(
        title = meta.title?.toString() ?: "",
        artist = meta.artist?.toString() ?: "",
        artwork = meta.artworkUri,
        durationMs = player.duration.coerceAtLeast(0L),
        positionMs = player.currentPosition.coerceAtLeast(0L),
        playing = player.isPlaying,
        shuffle = player.shuffleModeEnabled,
        repeat = player.repeatMode,
        hasMedia = has,
        index = player.currentMediaItemIndex,
        speed = player.playbackParameters.speed
    )
}

internal val SPEED_STEPS = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

internal fun fmtSpeed(speed: Float): String {
    val s = SPEED_STEPS.minByOrNull { abs(it - speed) } ?: speed
    val txt = if (s == s.toInt().toFloat()) s.toInt().toString()
    else s.toString().trimEnd('0').trimEnd('.')
    return "${txt}x"
}

internal fun nextSpeed(current: Float): Float {
    val idx = SPEED_STEPS.indexOfFirst { abs(it - current) < 0.01f }
    val base = if (idx >= 0) idx else 2 // default 1x
    return SPEED_STEPS[(base + 1) % SPEED_STEPS.size]
}
