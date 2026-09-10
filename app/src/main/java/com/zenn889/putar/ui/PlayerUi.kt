package com.zenn889.putar.ui

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
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
import com.zenn889.putar.ui.theme.CoralBright
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.SurfaceHigh

/** Wadah progres yang berdetak — hanya konsumennya (mini & layar penuh)
 *  yang ikut recompose, bukan seluruh pohon UI. */
class ProgressState {
    val positionMs = mutableLongStateOf(0L)
}

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

/** Tiga batang EQ kecil yang beranimasi — penanda lagu sedang diputar. */
@Composable
private fun MiniEq() {
    val t = rememberInfiniteTransition(label = "eq-mini")
    val h1 = t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "a")
    val h2 = t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(560), RepeatMode.Reverse), label = "b")
    val h3 = t.animateFloat(0.45f, 1f, infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "c")
    Row(
        modifier = Modifier.padding(end = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(h1.value, h2.value, h3.value).forEach { v ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(6.dp + 8.dp * v)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Coral)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isCurrent) Coral.copy(alpha = 0.08f) else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .pointerInput(onSwipeLeft, onSwipeRight) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, d -> acc += d },
                    onDragEnd = {
                        if (acc <= -110f) onSwipeLeft?.invoke()
                        else if (acc >= 110f) onSwipeRight?.invoke()
                        acc = 0f
                    },
                    onDragCancel = { acc = 0f }
                )
            }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AlbumArt(MusicRepository.albumArtUri(track.albumId), size = 52.dp, shape = RoundedCornerShape(12.dp))
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
        if (isCurrent) {
            MiniEq()
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
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
    progress: ProgressState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    if (!mirror.hasMedia) return
    val posMs = progress.positionMs.longValue
    Column {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(onClick) {
                    var acc = 0f
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, d -> acc += d },
                        onDragEnd = { if (acc <= -90f) onClick() }
                    )
                }
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumArt(mirror.artwork, size = 50.dp, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = mirror.title.ifBlank { "PureWave" },
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
            Surface(
                shape = CircleShape,
                color = if (mirror.playing) Coral else Coral.copy(alpha = 0.85f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(42.dp)) {
                        Icon(
                            imageVector = if (mirror.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (mirror.playing) "Jeda" else "Putar",
                            tint = Color(0xFF190902),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
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
                    (posMs.toFloat() / mirror.durationMs).coerceIn(0f, 1f)
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
    progress: ProgressState,
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
    onOpenLyrics: () -> Unit,
    speedLabel: String,
    onCycleSpeed: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dark = isSystemInDarkTheme()
    val surf = MaterialTheme.colorScheme.surface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Box(Modifier.fillMaxWidth()) {
            // --- latar khas Spotify: artwork diburamkan besar + gradasi turun ---
            Box(Modifier.matchParentSize().background(surf))
            val art = mirror.artwork
            if (art != null) {
                SubcomposeAsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(2.2f)
                        .alpha(if (dark) 0.55f else 0.42f)
                        .blur(90.dp)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to surf.copy(alpha = 0.12f),
                                0.45f to surf.copy(alpha = if (dark) 0.58f else 0.48f),
                                1f to surf
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                )

                // --- bar atas: tutup • label • sleep ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Tutup",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "SEDANG DIPUTAR",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.6.sp,
                            color = MutedInk
                        )
                        if (sleepLabel != null) {
                            Text(
                                sleepLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = Coral
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSleep) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = "Sleep timer",
                            tint = if (sleepActive) Coral else FaintInk
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // --- artwork besar ala Spotify ---
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val artSize = if (maxWidth > 340.dp) 340.dp else maxWidth
                    Box(
                        modifier = Modifier
                            .size(artSize + 60.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Coral.copy(alpha = 0.16f), Color.Transparent)
                                ),
                                CircleShape
                            )
                    )
                    AlbumArt(
                        uri = mirror.artwork,
                        size = artSize,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .shadow(26.dp, RoundedCornerShape(12.dp), clip = false)
                    )
                }

                Spacer(Modifier.height(22.dp))

                // --- judul + artis (kiri) dan tombol hati (kanan) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = mirror.title.ifBlank { "Belum ada lagu" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = mirror.artist.ifBlank { "Artis tidak diketahui" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Hapus favorit" else "Favorit",
                            tint = if (isFavorite) Coral else FaintInk,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // --- bar progres tipis khas Spotify (bisa digeser & diklik) ---
                var dragMs by remember { mutableLongStateOf(-1L) }
                val durMs = mirror.durationMs.coerceAtLeast(1L)
                val livePos = progress.positionMs.longValue
                val shownMs = if (dragMs >= 0L) dragMs else livePos
                val frac = (shownMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .pointerInput(durMs) {
                            detectTapGestures { off ->
                                val f = (off.x / size.width.toFloat()).coerceIn(0f, 1f)
                                controller?.seekTo((f * durMs).toLong())
                            }
                        }
                        .pointerInput(durMs) {
                            detectHorizontalDragGestures(
                                onDragStart = { dragMs = progress.positionMs.longValue },
                                onDragEnd = {
                                    controller?.seekTo(dragMs.coerceAtLeast(0L))
                                    dragMs = -1L
                                },
                                onDragCancel = { dragMs = -1L },
                                onHorizontalDrag = { change, delta ->
                                    change.consume()
                                    val base = if (dragMs >= 0L) dragMs else progress.positionMs.longValue
                                    val add = (delta / size.width.toFloat() * durMs.toFloat()).toLong()
                                    dragMs = (base + add).coerceIn(0L, durMs)
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(trackColor)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(frac)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Coral)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (maxWidth - 12.dp) * frac)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Coral)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtMs(shownMs), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                    Text(fmtMs(durMs), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                }

                Spacer(Modifier.height(6.dp))

                // --- transport ala Spotify: acak • sebelum • play besar • sesudah • ulangi ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Acak",
                            tint = if (mirror.shuffle) Coral else FaintInk,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    IconButton(onClick = onPrev) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "Sebelumnya",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .shadow(20.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Coral, CoralBright)))
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (mirror.playing) Icons.Filled.Pause
                            else Icons.Filled.PlayArrow,
                            contentDescription = if (mirror.playing) "Jeda" else "Putar",
                            tint = Color(0xFF190902),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "Berikutnya",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                    IconButton(onClick = onToggleRepeat) {
                        Icon(
                            imageVector = if (mirror.repeat == androidx.media3.common.Player.REPEAT_MODE_ONE)
                                Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Ulangi",
                            tint = if (mirror.repeat != androidx.media3.common.Player.REPEAT_MODE_OFF) Coral else FaintInk,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // --- bar bawah: kecepatan • lirik • equalizer • antrian ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    IconButton(onClick = onOpenLyrics) {
                        Icon(Icons.Filled.Lyrics, contentDescription = "Lirik", tint = FaintInk)
                    }
                    IconButton(onClick = onOpenEqualizer) {
                        Icon(Icons.Filled.Equalizer, contentDescription = "Equalizer", tint = FaintInk)
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Antrian",
                            tint = FaintInk
                        )
                    }
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
        itemsIndexed(tracks, key = { _, t -> t.contentUri.toString() }) { index, track ->
            TrackRow(
                track = track,
                isCurrent = track.contentUri.toString() == currentMediaId,
                onClick = { onPlay(index) },
                onLongClick = onLongClickTrack?.let { { it(track) } }
            )
        }
    }
}

