package com.zenn889.putar.ui

import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
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
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isCurrent) Coral.copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            )
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
        AlbumArt(
            MusicRepository.albumArtUri(track.albumId),
            size = 54.dp,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.shadow(9.dp, RoundedCornerShape(15.dp), clip = false)
        )
        Spacer(Modifier.width(13.dp))
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
    val dark = isSystemInDarkTheme()
    val surf = MaterialTheme.colorScheme.surface
    val tint = rememberArtColor(mirror.artwork) ?: Coral
    val (deep, bright) = remember(tint) { tonalPair(tint) }
    val accent = if (dark) bright else deep

    Surface(
        color = surf,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        Box {
            // semburat warna sampul yang sedang diputar
            if (mirror.artwork != null) {
                SubcomposeAsyncImage(
                    model = mirror.artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(2f)
                        .alpha(if (dark) 0.30f else 0.22f)
                        .blur(48.dp)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                accent.copy(alpha = if (dark) 0.26f else 0.16f),
                                surf.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
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
                    .padding(start = 12.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArt(
                    mirror.artwork,
                    size = 50.dp,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.shadow(10.dp, RoundedCornerShape(13.dp), clip = false)
                )
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
                        text = mirror.artist.ifBlank { "PureWave" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(12.dp, CircleShape, clip = false)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Coral, CoralBright)))
                        .clickable(onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (mirror.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (mirror.playing) "Jeda" else "Putar",
                        tint = Color(0xFF190902),
                        modifier = Modifier.size(26.dp)
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
            // garis progres tipis menempel di dasar kartu
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(tint.copy(alpha = 0.22f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(
                        if (mirror.durationMs > 0L)
                            (posMs.toFloat() / mirror.durationMs).coerceIn(0f, 1f)
                        else 0f
                    )
                    .height(2.5.dp)
                    .background(accent)
            )
        }
    }
}

/** Tombol bar bawah layar pemutar: ikon dengan label kecil di bawahnya. */
@Composable
private fun SheetAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = label, tint = FaintInk, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedInk)
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
    val art = mirror.artwork

    // warna dinamis dari sampul (mundur ke coral kalau tak terbaca)
    val tint = rememberArtColor(art) ?: Coral
    val (deep, bright) = remember(tint) { tonalPair(tint) }
    val accent = if (dark) bright else deep

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Box(Modifier.fillMaxWidth()) {
            // lapis 1: dasar
            Box(Modifier.matchParentSize().background(surf))

            // lapis 2: sampul diburamkan
            if (art != null) {
                SubcomposeAsyncImage(
                    model = art,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .scale(2.4f)
                        .alpha(if (dark) 0.50f else 0.38f)
                        .blur(96.dp)
                )
            }

            // lapis 3: semburat warna sampul melebur ke warna permukaan
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            if (dark) listOf(
                                deep.copy(alpha = 0.94f),
                                surf.copy(alpha = 0.88f),
                                surf
                            ) else listOf(
                                bright.copy(alpha = 0.34f),
                                surf.copy(alpha = 0.92f),
                                surf
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 42.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                )

                // --- bar atas ---
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
                            letterSpacing = 1.8.sp,
                            color = MutedInk
                        )
                        if (sleepLabel != null) {
                            Text(
                                sleepLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onSleep) {
                        Icon(
                            Icons.Filled.Timer,
                            contentDescription = "Sleep timer",
                            tint = if (sleepActive) accent else FaintInk
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // --- artwork besar + pendar berputar mengikuti irama warna sampul ---
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val artSize = if (maxWidth > 360.dp) 360.dp else maxWidth
                    val glow = rememberInfiniteTransition(label = "glow")
                    val rot by glow.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(26000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rot"
                    )
                    val breath by glow.animateFloat(
                        initialValue = 0.86f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "breath"
                    )
                    Box(
                        modifier = Modifier
                            .size(artSize + 96.dp)
                            .rotate(rot)
                            .scale(breath)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        bright.copy(alpha = 0.32f),
                                        bright.copy(alpha = 0f),
                                        bright.copy(alpha = 0.22f),
                                        bright.copy(alpha = 0f)
                                    )
                                ),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(artSize + 44.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(bright.copy(alpha = 0.18f), bright.copy(alpha = 0f))
                                ),
                                CircleShape
                            )
                    )
                    AlbumArt(
                        uri = art,
                        size = artSize,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .aspectRatio(1f)
                            .shadow(30.dp, RoundedCornerShape(18.dp), clip = false)
                    )
                }

                Spacer(Modifier.height(24.dp))

                // --- judul, artis, hati ---
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
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(46.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Hapus favorit" else "Favorit",
                            tint = if (isFavorite) accent else FaintInk,
                            modifier = Modifier.size(29.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // --- bar progres tipis gaya Spotify ---
                var dragMs by remember { mutableLongStateOf(-1L) }
                val durMs = mirror.durationMs.coerceAtLeast(1L)
                val livePos = progress.positionMs.longValue
                val shownMs = if (dragMs >= 0L) dragMs else livePos
                val frac = (shownMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f)
                val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
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
                            .background(accent)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (maxWidth - 13.dp) * frac)
                            .size(13.dp)
                            .shadow(6.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtMs(shownMs), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                    Text(fmtMs(durMs), style = MaterialTheme.typography.labelSmall, color = MutedInk)
                }

                Spacer(Modifier.height(8.dp))

                // --- transport ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Acak",
                            tint = if (mirror.shuffle) accent else FaintInk,
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
                            .size(72.dp)
                            .shadow(22.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(bright, deep))
                            )
                            .clickable(onClick = onPlayPause),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (mirror.playing) Icons.Filled.Pause
                            else Icons.Filled.PlayArrow,
                            contentDescription = if (mirror.playing) "Jeda" else "Putar",
                            tint = Color.White,
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
                            tint = if (mirror.repeat != androidx.media3.common.Player.REPEAT_MODE_OFF) accent else FaintInk,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // --- bar bawah: kecepatan • lirik • equalizer • antrian ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onCycleSpeed)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            speedLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(Modifier.height(3.dp))
                        Text("Kecepatan", style = MaterialTheme.typography.labelSmall, color = MutedInk)
                    }
                    SheetAction(Icons.Filled.Lyrics, "Lirik", onOpenLyrics)
                    SheetAction(Icons.Filled.Equalizer, "Equalizer", onOpenEqualizer)
                    SheetAction(Icons.AutoMirrored.Filled.QueueMusic, "Antrian", onOpenQueue)
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

