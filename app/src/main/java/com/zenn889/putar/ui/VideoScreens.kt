package com.zenn889.putar.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenn889.putar.data.LyricsLoader
import com.zenn889.putar.data.VideoItem
import com.zenn889.putar.data.VideoPosStore
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Status pemutaran video (dipakai untuk PiP saat user tekan Home). */
object VideoPlayback {
    @Volatile var active: Boolean = false
    @Volatile var playing: Boolean = false
    @Volatile var aspect: Float = 16f / 9f

    /** true selama jendela Activity berada di mode Picture-in-Picture. */
    val inPip = mutableStateOf(false)
}

/**
 * Parameter Picture-in-Picture standar: rasio mengikuti video, dan di
 * Android 12+ transisi dimatikan supaya gambar tidak melar saat mengecil.
 * Pemanggil wajib sudah menjaga versi (PiP baru ada sejak Android 8/O).
 */
@RequiresApi(Build.VERSION_CODES.O)
fun pipParams(aspect: Float): PictureInPictureParams {
    val safe = if (aspect in 0.2f..5f) aspect else 16f / 9f
    val ratio = if (safe >= 1f) Rational((safe * 100).toInt(), 100)
    else Rational(100, (100 / safe).toInt())
    val builder = PictureInPictureParams.Builder().setAspectRatio(ratio)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        builder.setAutoEnterEnabled(false)
        builder.setSeamlessResizeEnabled(false)
    }
    return builder.build()
}

/* ---------- cache thumbnail video ---------- */

private val thumbCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
        size > 160
}

private suspend fun loadVideoThumb(context: Context, item: VideoItem): Bitmap? =
    withContext(Dispatchers.IO) {
        val key = item.contentUri.toString()
        thumbCache[key]?.let { return@withContext it }
        val bmp = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                context.contentResolver.loadThumbnail(item.contentUri, Size(480, 270), null)
            } else {
                @Suppress("DEPRECATION")
                android.provider.MediaStore.Video.Thumbnails.getThumbnail(
                    context.contentResolver, item.mediaId,
                    android.provider.MediaStore.Video.Thumbnails.MINI_KIND, null
                )
            }
        }.getOrNull()
        if (bmp != null) thumbCache[key] = bmp
        bmp
    }

/** Satu baris video dengan thumbnail (preview) asli. */
@Composable
fun VideoRow(item: VideoItem, onClick: () -> Unit) {
    val context = LocalContext.current
    var thumb by remember(item.contentUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(item.contentUri) {
        thumb = loadVideoThumb(context, item)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF1C1E24))
        ) {
            val bmp = thumb
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Movie,
                        contentDescription = null,
                        tint = FaintInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(3.dp)
                    .background(Color(0x99000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    fmtMs(item.durationMs),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = Ink
            )
            Text(
                "Video · ketuk untuk memutar",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        }
        Icon(
            Icons.Filled.PlayArrow,
            contentDescription = "Putar video",
            tint = MutedInk,
            modifier = Modifier.size(26.dp)
        )
    }
}

private fun subtitleMime(file: File): String = when (file.extension.lowercase()) {
    "vtt" -> MimeTypes.TEXT_VTT
    "ttml" -> MimeTypes.APPLICATION_TTML
    else -> MimeTypes.APPLICATION_SUBRIP
}

/**
 * Pemutar video fullscreen (di window utama — siap Picture-in-Picture):
 * antrian prev/next, mundur/maju 10 dtk, subtitle .srt/.vtt otomatis,
 * resume posisi tonton, kontrol auto-hide.
 */
@Composable
fun VideoPlayerScreen(
    queue: List<VideoItem>,
    startIndex: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val safeStart = startIndex.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
    var idx by remember(safeStart) { mutableIntStateOf(safeStart) }
    val item = queue.getOrNull(idx) ?: return

    var exo by remember { mutableStateOf<ExoPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var dragMs by remember { mutableLongStateOf(-1L) }
    var controls by remember { mutableStateOf(true) }
    var seeking by remember { mutableStateOf(false) }
    var subsOn by remember { mutableStateOf(true) }
    var aspect by remember { mutableFloatStateOf(16f / 9f) }
    val inPip by VideoPlayback.inPip

    // subtitle di sebelah video (.srt / .vtt / .ttml)
    val subtitleFile = remember(item.contentUri) { LyricsLoader.subtitleFile(item.filePath) }
    val resumeMs = remember(item.contentUri) {
        VideoPosStore.position(context, item.contentUri.toString())
    }

    DisposableEffect(item.contentUri) {
        VideoPlayback.active = true
        val builder = MediaItem.Builder().setUri(item.contentUri)
        if (subtitleFile != null) {
            builder.setSubtitleConfigurations(
                listOf(
                    MediaItem.SubtitleConfiguration.Builder(android.net.Uri.fromFile(subtitleFile))
                        .setMimeType(subtitleMime(subtitleFile))
                        .setLanguage("id")
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build()
                )
            )
        }
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(builder.build(), resumeMs)
            prepare()
            playWhenReady = true
        }
        exo = player
        onDispose {
            val pos = runCatching { player.currentPosition }.getOrDefault(0L)
            val dur = runCatching { player.duration }.getOrDefault(0L)
            if (dur > 0L && pos > dur - 5_000L) {
                VideoPosStore.clear(context, item.contentUri.toString())
            } else if (pos > 5_000L) {
                VideoPosStore.save(context, item.contentUri.toString(), pos)
            }
            player.release()
            exo = null
            VideoPlayback.active = false
            VideoPlayback.playing = false
        }
    }

    LaunchedEffect(exo) {
        var sinceSave = 0L
        while (true) {
            val p = exo ?: break
            playing = p.isPlaying
            VideoPlayback.playing = playing
            ended = p.playbackState == Player.STATE_ENDED
            positionMs = p.currentPosition.coerceAtLeast(0L)
            durationMs = p.duration.coerceAtLeast(0L)
            val vs = p.videoSize
            if (vs.height > 0) {
                aspect = vs.width.toFloat() / vs.height.toFloat()
                VideoPlayback.aspect = aspect
            }
            // jaga status PiP tetap akurat walau callback activity terlewat
            val pipNow = activity?.isInPictureInPictureMode == true
            if (VideoPlayback.inPip.value != pipNow) VideoPlayback.inPip.value = pipNow
            sinceSave += 400
            if (sinceSave >= 4_000L) {
                sinceSave = 0L
                if (!ended && positionMs > 5_000L) {
                    VideoPosStore.save(context, item.contentUri.toString(), positionMs)
                }
            }
            delay(400)
        }
    }

    LaunchedEffect(idx) {
        ended = false
        positionMs = 0L
        dragMs = -1L
        controls = true
        seeking = false
    }

    // masuk PiP → semua kontrol disembunyikan; keluar PiP → muncul lagi
    LaunchedEffect(inPip) {
        controls = !inPip
    }

    LaunchedEffect(playing, controls, ended, seeking) {
        if (playing && controls && !ended && !seeking) {
            delay(3200)
            if (playing && controls && !seeking) controls = false
        }
    }

    fun togglePlay() {
        val p = exo ?: return
        if (p.isPlaying) p.pause()
        else {
            if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
            p.play()
        }
    }

    fun skipBy(secs: Int) {
        val p = exo ?: return
        val target = (p.currentPosition + secs * 1000L).coerceIn(0L, p.duration.coerceAtLeast(0L))
        p.seekTo(target)
    }

    fun enterPip() {
        val act = activity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { act.enterPictureInPictureMode(pipParams(aspect)) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1) video
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }.also { view ->
                    view.post { view.player = exo }
                }
            },
            update = { view ->
                if (view.player !== exo) view.player = exo
                view.subtitleView?.visibility = if (subsOn) android.view.View.VISIBLE
                else android.view.View.GONE
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2) penangkap ketukan (tidak aktif selama PiP)
        if (!inPip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { controls = !controls }
            )
        }

        if (controls && !inPip) {
            // 3) bar atas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xE6000000), Color(0x00000000))
                        )
                    )
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                    Text(
                        item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        subsOn = !subsOn
                    }) {
                        Icon(
                            if (subsOn) Icons.Filled.Subtitles else Icons.Filled.SubtitlesOff,
                            contentDescription = "Subtitle",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { enterPip() }) {
                        Icon(
                            Icons.Filled.PictureInPictureAlt,
                            contentDescription = "Picture-in-Picture",
                            tint = Color.White
                        )
                    }
                    Text(
                        "${idx + 1}/${queue.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            }

            // 4) play besar di tengah
            if (!playing || ended) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color(0x99000000), CircleShape)
                        .align(Alignment.Center)
                        .clickable {
                            if (ended) exo?.seekTo(0L)
                            exo?.play()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Putar",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            // 5) kontrol bawah
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0x00000000), Color(0xE6000000))
                            )
                        )
                )
                val dur = durationMs.coerceAtLeast(1L)
                val shown = if (dragMs >= 0L) dragMs else positionMs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE6000000))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        fmtMs(shown),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(42.dp)
                    )
                    Slider(
                        value = (shown.toFloat() / 1000f).coerceIn(0f, dur / 1000f),
                        onValueChange = {
                            seeking = true
                            dragMs = (it * 1000f).toLong()
                        },
                        onValueChangeFinished = {
                            exo?.seekTo(dragMs.coerceAtLeast(0L))
                            dragMs = -1L
                            seeking = false
                        },
                        valueRange = 0f..(dur / 1000f).coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Coral,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x66FFFFFF)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        fmtMs(dur),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(42.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xE6000000))
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (idx > 0) idx--
                            else exo?.seekTo(0L)
                        },
                        enabled = queue.size > 1
                    ) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "Video sebelumnya",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { skipBy(-10) }) {
                        Icon(
                            Icons.Filled.FastRewind,
                            contentDescription = "Mundur 10 detik",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { togglePlay() }) {
                        Icon(
                            if (playing && !ended) Icons.Filled.Pause
                            else Icons.Filled.PlayArrow,
                            contentDescription = "Putar/Jeda",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = { skipBy(10) }) {
                        Icon(
                            Icons.Filled.FastForward,
                            contentDescription = "Maju 10 detik",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { if (idx < queue.size - 1) idx++ },
                        enabled = queue.size > 1
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Video berikutnya",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
