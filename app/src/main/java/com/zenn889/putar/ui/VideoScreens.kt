package com.zenn889.putar.ui

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.zenn889.putar.data.VideoItem
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/* ---------- cache thumbnail video (kecil, mencegah decode ulang) ---------- */

private val thumbCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>): Boolean =
        size > 160
}

private suspend fun loadVideoThumb(context: android.content.Context, item: VideoItem): Bitmap? =
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

/** Satu baris video dengan thumbnail (preview) asli dari MediaStore. */
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
            // lencana durasi
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
                fontWeight = FontWeight.Medium
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

/**
 * Layar video fullscreen ala pemutar bawaan HP.
 * Urutan lapisan dibuat eksplisit agar tombol kontrol pasti bisa ditekan:
 * video → penangkap ketukan (tampil/sembunyi kontrol) → bar atas → tengah → bawah.
 */
@Composable
fun VideoPlayerScreen(item: VideoItem, onBack: () -> Unit) {
    val context = LocalContext.current
    var exo by remember { mutableStateOf<ExoPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var ended by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var dragMs by remember { mutableLongStateOf(-1L) }
    var controls by remember { mutableStateOf(true) }
    var seeking by remember { mutableStateOf(false) }

    DisposableEffect(item.contentUri) {
        val player = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(item.contentUri))
            prepare()
            playWhenReady = true
        }
        exo = player
        onDispose {
            player.release()
            exo = null
        }
    }

    LaunchedEffect(exo) {
        while (true) {
            val p = exo ?: break
            playing = p.isPlaying
            ended = p.playbackState == androidx.media3.common.Player.STATE_ENDED
            positionMs = p.currentPosition.coerceAtLeast(0L)
            durationMs = p.duration.coerceAtLeast(0L)
            delay(400)
        }
    }

    // kontrol menutup sendiri saat video berjalan
    LaunchedEffect(playing, controls, ended, seeking) {
        if (playing && controls && !ended && !seeking) {
            delay(3200)
            if (playing && controls && !seeking) controls = false
        }
    }

    Dialog(
        onDismissRequest = onBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
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
                },
                modifier = Modifier.fillMaxSize()
            )

            // 2) penangkap ketukan: ketuk video → tampil/sembunyi kontrol
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { controls = !controls }
            )

            if (controls) {
                // 3) gradien & bar atas: kembali + judul
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    }
                }

                // 4) tombol play besar di tengah saat berhenti / selesai
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

                // 5) kontrol bawah: play/jeda + waktu + slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
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
                            .padding(start = 8.dp, end = 14.dp, top = 2.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (playing && !ended) exo?.pause()
                            else {
                                if (ended) exo?.seekTo(0L)
                                exo?.play()
                            }
                        }) {
                            Icon(
                                if (playing && !ended) Icons.Filled.Pause
                                else Icons.Filled.PlayArrow,
                                contentDescription = "Putar/Jeda",
                                tint = Color.White
                            )
                        }
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
                }
            }
        }
    }
}
