package com.zenn889.putar.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.SurfaceHigh

/** Cermin kondisi pemutar untuk UI. */
data class PlayerMirror(
    val title: String = "",
    val artist: String = "",
    val artwork: Uri? = null,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = androidx.media3.common.Player.REPEAT_MODE_OFF,
    val hasMedia: Boolean = false,
    val index: Int = -1,
    val speed: Float = 1f
)

fun fmtMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%d:%02d".format(m, s)
}

@Composable
fun ArtPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(SurfaceHigh),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = FaintInk,
            modifier = Modifier.size(34.dp)
        )
    }
}

@Composable
fun AlbumArt(uri: Uri?, size: Dp, shape: Shape = RoundedCornerShape(14.dp), modifier: Modifier = Modifier) {
    val artMod = modifier.size(size).clip(shape)
    if (uri == null) {
        ArtPlaceholder(artMod)
        return
    }
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = artMod
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> SubcomposeAsyncImageContent()
            else -> ArtPlaceholder(Modifier.fillMaxSize())
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(MusicRepository.albumArtUri(track.albumId), size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) Coral else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = track.displayArtist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = fmtMs(track.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = FaintInk
        )
    }
}

@Composable
fun MiniPlayer(
    mirror: PlayerMirror,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    if (!mirror.hasMedia) return
    Column {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(mirror.artwork, size = 46.dp, shape = RoundedCornerShape(10.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = mirror.title.ifBlank { "putar" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = mirror.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (mirror.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (mirror.playing) "Jeda" else "Putar",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        }
        LinearProgressIndicator(
            progress = {
                if (mirror.durationMs > 0L) {
                    (mirror.positionMs.toFloat() / mirror.durationMs).coerceIn(0f, 1f)
                } else 0f
            },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = Coral,
            trackColor = Color.Transparent
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    mirror: PlayerMirror,
    controller: androidx.media3.session.MediaController?,
    onDismiss: () -> Unit,
    onToggleShuffle: () -> Unit,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenEqualizer: () -> Unit,
    sleepActive: Boolean,
    sleepLabel: String?,
    onSleep: () -> Unit,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    speedLabel: String,
    onCycleSpeed: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // kecepatan putar
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Coral.copy(alpha = 0.14f),
                    modifier = Modifier.clickable(onClick = onCycleSpeed)
                ) {
                    Text(
                        speedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Coral,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Hapus favorit" else "Favorit",
                        tint = if (isFavorite) Coral else FaintInk
                    )
                }
                if (sleepLabel != null) {
                    Text(
                        text = sleepLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Coral,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = onSleep) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Sleep timer",
                        tint = if (sleepActive) Coral else FaintInk
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        imageVector = Icons.Filled.QueueMusic,
                        contentDescription = "Antrian",
                        tint = FaintInk
                    )
                }
                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        imageVector = Icons.Filled.Equalizer,
                        contentDescription = "Equalizer",
                        tint = FaintInk
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            AlbumArt(
                uri = mirror.artwork,
                size = 224.dp,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.aspectRatio(1f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = mirror.title.ifBlank { "Belum ada lagu" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = mirror.artist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk
            )
            Spacer(Modifier.height(10.dp))

            var dragMs by remember { mutableLongStateOf(-1L) }
            val durMs = mirror.durationMs.coerceAtLeast(1L)
            val shownMs = if (dragMs >= 0L) dragMs else mirror.positionMs

            Slider(
                value = (shownMs / 1000f).coerceIn(0f, durMs / 1000f),
                onValueChange = { dragMs = (it * 1000f).toLong() },
                onValueChangeFinished = {
                    controller?.seekTo(dragMs.coerceAtLeast(0L))
                    dragMs = -1L
                },
                valueRange = 0f..(durMs / 1000f).coerceAtLeast(1f),
                colors = SliderDefaults.colors(thumbColor = Coral, activeTrackColor = Coral),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtMs(shownMs), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                Text(fmtMs(durMs), style = MaterialTheme.typography.labelSmall, color = MutedInk)
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Acak",
                        tint = if (mirror.shuffle) Coral else FaintInk
                    )
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Sebelumnya",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = Coral,
                    modifier = Modifier.size(66.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                imageVector = if (mirror.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (mirror.playing) "Jeda" else "Putar",
                                tint = Color(0xFF190902),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Berikutnya",
                        tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onToggleRepeat) {
                    Icon(
                        imageVector = if (mirror.repeat == androidx.media3.common.Player.REPEAT_MODE_ONE)
                            Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Ulangi",
                        tint = if (mirror.repeat != androidx.media3.common.Player.REPEAT_MODE_OFF) Coral else FaintInk
                    )
                }
            }
        }
    }
}

@Composable
fun CenteredLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Coral)
    }
}

@Composable
fun LibraryList(
    tracks: List<Track>,
    currentMediaId: String?,
    onPlay: (index: Int) -> Unit,
    onLongClickTrack: ((Track) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 8.dp, end = 8.dp, top = 4.dp, bottom = 16.dp
        )
    ) {
        itemsIndexed(tracks) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.contentUri.toString() == currentMediaId,
                onClick = { onPlay(index) },
                onLongClick = onLongClickTrack?.let { { it(track) } }
            )
        }
    }
}

