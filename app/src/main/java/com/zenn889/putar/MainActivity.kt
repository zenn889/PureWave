package com.zenn889.putar

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.LibraryList
import com.zenn889.putar.ui.MiniPlayer
import com.zenn889.putar.ui.NowPlayingSheet
import com.zenn889.putar.ui.PlayerMirror
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.PutarTheme
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PutarTheme {
                PlayerApp()
            }
        }
    }
}

private fun readPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun Context.hasReadPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, readPermission()) == PackageManager.PERMISSION_GRANTED

private fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(contentUri.toString())
        .setUri(contentUri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(displayArtist)
                .setArtworkUri(MusicRepository.albumArtUri(albumId))
                .build()
        )
        .build()

@Composable
fun PlayerApp() {
    val context = LocalContext.current.applicationContext
    val repo = remember { MusicRepository(context) }

    // --- kontrol pemutar (Media3) ---
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var mirror by remember { mutableStateOf(PlayerMirror()) }

    DisposableEffect(context) {
        var released = false
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                        Player.EVENT_REPEAT_MODE_CHANGED,
                        Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
                        Player.EVENT_TIMELINE_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY
                    )
                ) {
                    mirror = readMirror(player)
                }
            }
        }
        future.addListener({
            if (!released) {
                val c = runCatching { future.get() }.getOrNull()
                if (c != null) {
                    c.addListener(listener)
                    controller = c
                    mirror = readMirror(c)
                }
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            released = true
            controller?.removeListener(listener)
            controller?.release()
            controller = null
        }
    }

    // posisi pemutar tetap segar utk slider
    LaunchedEffect(mirror.playing) {
        while (mirror.playing) {
            delay(400)
            val c = controller
            if (c != null) mirror = mirror.copy(positionMs = c.currentPosition.coerceAtLeast(0L))
        }
    }

    // --- pustaka & izin ---
    var granted by remember { mutableStateOf(context.hasReadPermission()) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result[readPermission()] == true
    }

    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            tracks = repo.loadLibrary()
            loading = false
        }
    }

    fun requestPermissions() {
        val perms = buildList {
            add(readPermission())
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    fun playFrom(index: Int, shuffled: Boolean) {
        if (tracks.isEmpty()) return
        val c = controller ?: return
        val items = tracks.map { it.toMediaItem() }
        val start = if (shuffled) Random.nextInt(items.size) else index
        c.shuffleModeEnabled = false
        c.setMediaItems(items, start, 0L)
        if (shuffled) c.shuffleModeEnabled = true
        c.prepare()
        c.play()
    }

    var showFullPlayer by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MiniPlayer(
                mirror = mirror,
                onClick = { if (mirror.hasMedia) showFullPlayer = true },
                onPlayPause = {
                    controller?.let { c -> if (mirror.playing) c.pause() else c.play() }
                },
                onNext = { controller?.seekToNextMediaItem() }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !granted -> PermissionScreen(onRequest = ::requestPermissions)
                loading -> androidx.compose.material3.CircularProgressIndicator(
                    color = Coral,
                    modifier = Modifier.align(Alignment.Center)
                )
                tracks.isEmpty() -> EmptyLibraryScreen()
                else -> Column(Modifier.fillMaxSize()) {
                    LibraryHeader(
                        count = tracks.size,
                        onPlayAllShuffled = { playFrom(0, shuffled = true) }
                    )
                    LibraryList(
                        tracks = tracks,
                        currentMediaId = currentMediaItemUri(controller),
                        onPlay = { playFrom(it, shuffled = false) }
                    )
                }
            }
        }
    }

    if (showFullPlayer && mirror.hasMedia) {
        NowPlayingSheet(
            mirror = mirror,
            controller = controller,
            onDismiss = { showFullPlayer = false },
            onToggleShuffle = { controller?.shuffleModeEnabled = !(mirror.shuffle) },
            onPrev = { controller?.seekToPreviousMediaItem() },
            onPlayPause = {
                controller?.let { c -> if (mirror.playing) c.pause() else c.play() }
            },
            onNext = { controller?.seekToNextMediaItem() },
            onToggleRepeat = {
                controller?.let { c ->
                    c.repeatMode = when (c.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                }
            }
        )
    }
}

private fun currentMediaItemUri(c: MediaController?): String? =
    c?.currentMediaItem?.mediaId

private fun readMirror(player: Player): PlayerMirror {
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
        index = player.currentMediaItemIndex
    )
}

@Composable
private fun LibraryHeader(count: Int, onPlayAllShuffled: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Pustaka lagu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (count == 1) "1 lagu di perangkat" else "$count lagu di perangkat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
            FilledTonalButton(onClick = onPlayAllShuffled) {
                Icon(Icons.Filled.Shuffle, contentDescription = null,
                    tint = Coral, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Putar acak", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.padding(20.dp).size(44.dp)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "putar butuh akses musik",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Izinkan akses audio agar putar bisa membaca semua lagu di penyimpanan HP-mu. " +
                "Semua diputar lokal — tanpa internet, tanpa akun.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Coral)
        ) {
            Text("Izinkan akses musik", color = Color(0xFF190902), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyLibraryScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = FaintInk,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(14.dp))
        Text(
            "Tidak ada musik ditemukan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Tidak ada file audio (durasi > 3 detik) di perangkat ini.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedInk,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
