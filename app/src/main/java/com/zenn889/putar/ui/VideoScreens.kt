package com.zenn889.putar.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.Rational
import android.util.Size
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.BrightnessHigh
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.zenn889.putar.data.LyricsLoader
import com.zenn889.putar.data.VideoItem
import com.zenn889.putar.data.VideoPosStore
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.Radius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

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

/* ---------- mode tampilan video ---------- */

/**
 * Tiga mode tampilan. [label] yang tampil di tombol, [resize] mode ExoPlayer,
 * [desc] sebutan untuk pembaca layar sekaligus penjelasan singkat:
 * "Fit" = seluruh gambar terlihat (sisa layar jadi bar hitam),
 * "Isi" = memenuhi layar, tapi gambar bisa melar,
 * "Zoom" = memenuhi layar dengan memotong tepi (tidak melar).
 */
internal data class FitMode(val label: String, val resize: Int, val desc: String)

/** Urutan putaran tombol tampilan: Fit → Isi → Zoom → Fit. */
internal val FIT_MODES = listOf(
    FitMode("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT, "utuh, sisa layar hitam"),
    FitMode("Isi", AspectRatioFrameLayout.RESIZE_MODE_FILL, "isi, gambar bisa melar"),
    FitMode("Zoom", AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "zoom, tepi terpotong")
)

/* ---------- kecerahan layar ---------- */

/** Kecerahan layar yang sedang berlaku (0..1) — titik awal saat mulai menggeser. */
private fun systemBrightness(context: Context): Float = runCatching {
    android.provider.Settings.System.getInt(
        context.contentResolver,
        android.provider.Settings.System.SCREEN_BRIGHTNESS
    ) / 255f
}.getOrDefault(0.5f).coerceIn(0.05f, 1f)

/**
 * Kecerahan setelah menggeser sejauh [dyPx] piksel. Geser ke atas (dy negatif)
 * = lebih terang, sesuai kebiasaan pemutar video. 700 px kira-kira mewakili
 * seluruh rentang layar, dan hasilnya selalu dijepit ke 2%–100%.
 */
internal fun brightnessAfterDrag(base: Float, dyPx: Float): Float =
    (base - dyPx / 700f).coerceIn(0.02f, 1f)

/**
 * Ubah kecerahan jendela layar pemutar. Hanya berlaku untuk jendela Activity
 * ini — tidak menyentuh setelan sistem, dan tidak butuh izin apa pun. Nilai
 * [WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE] mengembalikannya ke
 * kecerahan sistem (dipakai saat layar pemutar ditutup, supaya sisa aplikasi
 * tidak ikut meredup).
 */
private fun applyWindowBrightness(activity: Activity?, value: Float) {
    val act = activity ?: return
    runCatching {
        val attrs = act.window.attributes
        attrs.screenBrightness = value
        act.window.attributes = attrs
    }
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
    // Kecepatan putar: bertahan selama layar pemutar ini terbuka (termasuk saat
    // pindah ke video berikutnya), dan kembali ke 1x tiap layar dibuka ulang —
    // sama seperti pemutar musik yang menyimpan kecepatan selama sesi berjalan.
    var speed by remember { mutableFloatStateOf(1f) }
    /**
     * Mode tampilan video (0 Fit, 1 Isi, 2 Zoom). Ikut terbawa saat pindah
     * video, dan kembali Fit saat layar pemutar dibuka ulang.
     */
    var mode by remember { mutableIntStateOf(0) }
    /**
     * Kecerahan jendela saat menggeser atas-bawah. -1 = belum disentuh (ikut
     * kecerahan sistem). Hanya berlaku untuk jendela ini dan dikembalikan ke
     * bawaan sistem saat layar pemutar ditutup.
     */
    var brightness by remember { mutableFloatStateOf(-1f) }
    var brightHint by remember { mutableStateOf(false) }
    /** -1 = kilatan "mundur 10 detik", +1 = "maju 10 detik", 0 = tidak tampil. */
    var seekFlash by remember { mutableIntStateOf(0) }
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
            setPlaybackSpeed(speed)   // kecepatan ikut terbawa saat pindah video
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

    // kecerahan layar dikembalikan ke bawaan sistem begitu layar ini ditutup,
    // supaya tidak ikut meredupkan bagian aplikasi yang lain
    DisposableEffect(Unit) {
        onDispose {
            applyWindowBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        }
    }

    // penanda geser kecerahan & ketuk-dua-kali hilang sendiri
    LaunchedEffect(brightHint) {
        if (brightHint) {
            delay(900)
            brightHint = false
        }
    }

    LaunchedEffect(seekFlash) {
        if (seekFlash != 0) {
            delay(700)
            seekFlash = 0
        }
    }

    // perubahan kecepatan langsung berlaku pada pemutar yang sedang berjalan
    LaunchedEffect(speed) {
        exo?.setPlaybackSpeed(speed)
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
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                // mode tampilan: Fit / Isi / Zoom (dipakai juga oleh jendela PiP)
                view.resizeMode = FIT_MODES[mode.coerceIn(0, FIT_MODES.size - 1)].resize
                view.subtitleView?.visibility = if (subsOn) android.view.View.VISIBLE
                else android.view.View.GONE
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2) penangkap ketukan & geser (tidak aktif selama PiP)
        if (!inPip) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // geser atas-bawah = kecerahan layar
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                if (brightness < 0f) brightness = systemBrightness(context)
                                brightHint = true
                            },
                            onDragEnd = {},
                            onDragCancel = {}
                        ) { change, dy ->
                            change.consume()
                            val base = if (brightness < 0f) systemBrightness(context) else brightness
                            brightness = brightnessAfterDrag(base, dy)
                            applyWindowBrightness(activity, brightness)
                            brightHint = true
                        }
                    }
            ) {
                Row(Modifier.fillMaxSize()) {
                    // Sisi kiri & kanan: ketuk dua kali = mundur/maju 10 detik.
                    // Ketukan tunggal di sisi juga menampilkan/menyembunyikan
                    // kontrol, tapi tertunda sebentar karena aplikasi menunggu
                    // kemungkinan ketukan kedua — itulah harga ketuk-dua-kali.
                    // (Bagian tengah tidak menunggu, jadi terasa seketika.)
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { controls = !controls },
                                    onDoubleTap = {
                                        skipBy(-10)
                                        seekFlash = -1
                                    }
                                )
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.6f)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { controls = !controls })
                            }
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.7f)
                            .fillMaxHeight()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { controls = !controls },
                                    onDoubleTap = {
                                        skipBy(10)
                                        seekFlash = 1
                                    }
                                )
                            }
                    )
                }
            }
        }

        // penanda geser kecerahan — di bawah bar atas supaya tidak menutupinya
        if (brightHint && !inPip) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(Radius.sm))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.BrightnessHigh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(brightness.coerceAtLeast(0f) * 100).roundToInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // penanda ketuk-dua-kali: ikon + "10 detik" di sisi yang diketuk
        if (seekFlash != 0 && !inPip) {
            val back = seekFlash < 0
            Box(
                modifier = Modifier
                    .align(if (back) Alignment.CenterStart else Alignment.CenterEnd)
                    .padding(horizontal = 28.dp)
                    .background(Color(0x99000000), CircleShape)
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (back) Icons.Filled.FastRewind else Icons.Filled.FastForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "10 detik",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
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
                        // penghitung antrian ikut di baris judul: dua tombol baru
                        // (kecepatan & tampilan) butuh ruang, dan ini menjaga judul
                        // tetap lega
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xB3FFFFFF))) {
                                append("${idx + 1}/${queue.size}  ")
                            }
                            append(item.title)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    // kecepatan putar — ditaruh sebelum tombol yang sudah ada supaya
                    // posisi subtitle/PiP/penghitung tidak bergeser
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.xs))
                            .background(Color(0x66000000))
                            .clickable { speed = nextSpeed(speed) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .semantics {
                                contentDescription = "Kecepatan putar ${fmtSpeed(speed)}"
                            }
                    ) {
                        Text(
                            fmtSpeed(speed),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // mode tampilan — sebelah tombol subtitle supaya kontrol yang
                    // berhubungan dengan gambar berkumpul
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.xs))
                            .background(Color(0x66000000))
                            .clickable { mode = (mode + 1) % FIT_MODES.size }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .semantics {
                                contentDescription = "Tampilan video: ${FIT_MODES[mode].desc}"
                            }
                    ) {
                        Text(
                            FIT_MODES[mode].label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
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
