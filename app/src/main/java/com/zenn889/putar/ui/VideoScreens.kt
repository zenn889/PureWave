package com.zenn889.putar.ui

import android.view.ViewGroup
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
import com.zenn889.putar.ui.theme.MutedInk

/** Satu baris video di tab Video. */
@Composable
fun VideoRow(item: VideoItem, onClick: () -> Unit) {
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
                .size(54.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF2A2E38), Color(0xFF191C22))),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Movie,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(24.dp)
            )
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
                "Video · ${fmtMs(item.durationMs)}",
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

/** Layar video fullscreen di dalam app (offline, ExoPlayer lokal). */
@Composable
fun VideoPlayerScreen(item: VideoItem, onBack: () -> Unit) {
    val context = LocalContext.current
    var exo by remember { mutableStateOf<ExoPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var dragMs by remember { mutableLongStateOf(-1L) }

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

    // detak progres
    LaunchedEffect(exo) {
        while (true) {
            val p = exo ?: break
            playing = p.isPlaying
            positionMs = p.currentPosition.coerceAtLeast(0L)
            durationMs = p.duration.coerceAtLeast(0L)
            kotlinx.coroutines.delay(400)
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
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
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
                                // pasang player begitu view siap
                                view.post { view.player = exo }
                            }
                        },
                        update = { view ->
                            if (view.player !== exo) view.player = exo
                        }
                    )
                    if (!playing) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Coral, CircleShape)
                                .clickable { exo?.play() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0x66000000), CircleShape)
                                .clickable { exo?.pause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Pause,
                                contentDescription = "Jeda",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    val dur = durationMs.coerceAtLeast(1L)
                    val shown = if (dragMs >= 0L) dragMs else positionMs
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fmtMs(shown),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(44.dp)
                        )
                        Slider(
                            value = (shown.toFloat() / 1000f).coerceIn(0f, dur / 1000f),
                            onValueChange = { dragMs = (it * 1000f).toLong() },
                            onValueChangeFinished = {
                                exo?.seekTo(dragMs.coerceAtLeast(0L))
                                dragMs = -1L
                            },
                            valueRange = 0f..(dur / 1000f).coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Coral, activeTrackColor = Coral
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            fmtMs(dur),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(44.dp)
                        )
                    }
                }
            }
        }
    }
}
