package com.zenn889.putar

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.zenn889.putar.data.Album
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.SessionStore
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.AlbumRow
import com.zenn889.putar.ui.ArtistRow
import com.zenn889.putar.ui.BackBar
import com.zenn889.putar.ui.EqualizerSheet
import com.zenn889.putar.ui.FolderRow
import com.zenn889.putar.ui.LibraryList
import com.zenn889.putar.ui.LibraryTab
import com.zenn889.putar.ui.LibraryTabBar
import com.zenn889.putar.ui.MiniPlayer
import com.zenn889.putar.ui.NowPlayingSheet
import com.zenn889.putar.ui.PlayerMirror
import com.zenn889.putar.ui.SettingsSheet
import com.zenn889.putar.ui.SimpleEmpty
import com.zenn889.putar.ui.buildArtistItems
import com.zenn889.putar.ui.buildFolderItems
import com.zenn889.putar.ui.fmtMs
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.PutarTheme
import com.zenn889.putar.ui.theme.SurfaceHigh
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashLogger(this)
        enableEdgeToEdge()
        setContent {
            PutarTheme {
                PlayerApp()
            }
        }
    }
}

/** Catat crash ke <app>/files/crash.txt supaya gampang dilaporkan. */
private fun installCrashLogger(context: Context) {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = java.io.File(dir, "crash.txt")
            val stamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                .format(java.util.Date())
            file.appendText(
                "\n=== $stamp ===\n" +
                    (thread?.name ?: "?") + "\n" +
                    throwable.stackTraceToString() + "\n"
            )
        }
        previous?.uncaughtException(thread, throwable)
    }
}

private fun readPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

private fun Context.hasReadPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, readPermission()) == PackageManager.PERMISSION_GRANTED

fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull() ?: ""

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
                    // simpan posisi saat berhenti / lagu berakhir
                    if (!player.isPlaying && player.playbackState != Player.STATE_IDLE &&
                        player.mediaItemCount > 0
                    ) {
                        SessionStore.savePosition(
                            context, player.currentMediaItemIndex,
                            player.currentPosition.coerceAtLeast(0L)
                        )
                    }
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

    // --- pustaka, izin ---
    var granted by remember { mutableStateOf(context.hasReadPermission()) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(LibraryTab.LAGU) }

    // detail yang sedang dibuka (album/artis/folder)
    var selAlbum by remember { mutableStateOf<Album?>(null) }
    var selArtist by remember { mutableStateOf<String?>(null) }
    var selFolder by remember { mutableStateOf<String?>(null) }
    val inDetail = selAlbum != null || selArtist != null || selFolder != null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = result[readPermission()] == true
    }

    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            val lib = repo.loadLibrary()
            val alb = repo.loadAlbums()
            tracks = lib
            albums = alb
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

    // --- daftar per tab (root), dihitung sekali per perubahan ---
    val songsByUri = remember(tracks) { tracks.associateBy { it.contentUri.toString() } }
    val artistItems = remember(tracks) { buildArtistItems(tracks) }
    val folderItems = remember(tracks) { buildFolderItems(tracks) }

    val q = query.trim()
    val rootSongs = remember(tracks, q) {
        if (q.isEmpty()) tracks
        else tracks.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.displayArtist.contains(q, ignoreCase = true)
        }
    }
    val rootAlbums = remember(albums, q) {
        if (q.isEmpty()) albums
        else albums.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.displayArtist.contains(q, ignoreCase = true)
        }
    }
    val rootArtists = remember(artistItems, q) {
        if (q.isEmpty()) artistItems
        else artistItems.filter { it.first.contains(q, ignoreCase = true) }
    }
    val rootFolders = remember(folderItems, q) {
        if (q.isEmpty()) folderItems
        else folderItems.filter {
            it.name.contains(q, ignoreCase = true) || it.path.contains(q, ignoreCase = true)
        }
    }

    // daftar detail
    val detailSongs = when {
        selAlbum != null -> tracks.filter { it.albumId != null && it.albumId == selAlbum!!.albumId }
        selArtist != null -> tracks.filter { it.displayArtist == selArtist }
        selFolder != null -> tracks.filter { it.folder.orEmpty() == selFolder }
        else -> emptyList()
    }

    // --- pemutaran ---
    var pendingResumeMs by remember { mutableLongStateOf(-1L) }
    var restoredApplied by remember { mutableStateOf(false) }
    var lastSavedPos by remember { mutableLongStateOf(-1L) }

    fun playList(list: List<Track>, index: Int, shuffled: Boolean) {
        if (list.isEmpty()) return
        val c = controller ?: return
        val items = list.map { it.toMediaItem() }
        val start = if (shuffled) Random.nextInt(items.size) else index.coerceIn(0, items.size - 1)
        pendingResumeMs = -1L
        c.shuffleModeEnabled = false
        c.setMediaItems(items, start, 0L)
        if (shuffled) c.shuffleModeEnabled = true
        c.prepare()
        c.play()
        SessionStore.saveQueue(
            context, items.map { it.mediaId }, start, 0L
        )
    }

    fun resumePlay() {
        val c = controller ?: return
        if (c.mediaItemCount == 0) return
        if (c.playbackState == Player.STATE_IDLE) c.prepare()
        if (pendingResumeMs >= 0L) {
            runCatching { c.seekTo(c.currentMediaItemIndex, pendingResumeMs) }
            pendingResumeMs = -1L
        }
        c.play()
    }

    // auto-resume saat pustaka & controller siap
    LaunchedEffect(granted, controller != null, tracks.isEmpty().not(), restoredApplied) {
        if (granted && controller != null && tracks.isNotEmpty() && !restoredApplied) {
            restoredApplied = true
            val sess = SessionStore.load(context) ?: return@LaunchedEffect
            val matched = sess.first.mapNotNull { songsByUri[it] }
            if (matched.isNotEmpty()) {
                val idx = sess.second.coerceIn(0, matched.size - 1)
                controller?.setMediaItems(matched.map { it.toMediaItem() }, idx, 0L)
                pendingResumeMs = sess.third
                controller?.let { mirror = readMirror(it) }
            }
        }
    }

    // posisi slider + simpan progres berkala
    LaunchedEffect(mirror.playing) {
        while (mirror.playing) {
            delay(400)
            val c = controller
            if (c != null) {
                mirror = mirror.copy(positionMs = c.currentPosition.coerceAtLeast(0L))
                val pos = c.currentPosition.coerceAtLeast(0L)
                if (abs(pos - lastSavedPos) > 4000L) {
                    lastSavedPos = pos
                    SessionStore.savePosition(context, c.currentMediaItemIndex, pos)
                }
            }
        }
    }

    // --- sleep timer ---
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var sleepLeftMs by remember { mutableLongStateOf(0L) }
    var showSleepDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sleepUntil) {
        while (sleepUntil > 0L) {
            val left = sleepUntil - SystemClock.elapsedRealtime()
            if (left <= 0L) {
                sleepUntil = 0L
                sleepLeftMs = 0L
                controller?.pause()
                break
            }
            sleepLeftMs = left
            delay(1000)
        }
    }

    // --- layar tambahan ---
    var showFullPlayer by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            MiniPlayer(
                mirror = mirror,
                onClick = { if (mirror.hasMedia) showFullPlayer = true },
                onPlayPause = {
                    val c = controller ?: return@MiniPlayer
                    if (mirror.playing) c.pause()
                    else if (c.mediaItemCount > 0 &&
                        (c.playbackState == Player.STATE_IDLE || c.currentMediaItem != null)
                    ) resumePlay()
                },
                onNext = { controller?.seekToNextMediaItem() }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !granted -> PermissionScreen(onRequest = ::requestPermissions)
                loading -> CircularProgressIndicator(
                    color = Coral,
                    modifier = Modifier.align(Alignment.Center)
                )
                tracks.isEmpty() -> EmptyLibraryScreen()
                else -> Column(Modifier.fillMaxSize()) {
                    if (inDetail) {
                        val back = {
                            selAlbum = null
                            selArtist = null
                            selFolder = null
                        }
                        when {
                            selAlbum != null -> {
                                BackBar(
                                    selAlbum!!.title,
                                    "${selAlbum!!.displayArtist} · ${selAlbum!!.songCount} lagu",
                                    onBack = back
                                )
                                LibraryList(
                                    tracks = detailSongs,
                                    currentMediaId = currentMediaItemUri(controller),
                                    onPlay = { playList(detailSongs, it, false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            selArtist != null -> {
                                BackBar(
                                    selArtist!!,
                                    "${detailSongs.size} lagu",
                                    onBack = back
                                )
                                LibraryList(
                                    tracks = detailSongs,
                                    currentMediaId = currentMediaItemUri(controller),
                                    onPlay = { playList(detailSongs, it, false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            else -> {
                                val f = folderItems.firstOrNull { it.key == selFolder }
                                BackBar(
                                    f?.name ?: "Folder",
                                    if (f != null)
                                        (f.path.ifBlank { "Penyimpanan utama" } + " · ${f.songCount} lagu")
                                    else "${detailSongs.size} lagu",
                                    onBack = back
                                )
                                LibraryList(
                                    tracks = detailSongs,
                                    currentMediaId = currentMediaItemUri(controller),
                                    onPlay = { playList(detailSongs, it, false) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        LibraryHeader(
                            context = context,
                            tab = tab,
                            totalSongs = tracks.size,
                            rootSongs = rootSongs.size,
                            rootAlbums = rootAlbums.size,
                            rootArtists = rootArtists.size,
                            rootFolders = rootFolders.size,
                            query = query,
                            onQueryChange = {
                                query = it
                                if (inDetail) {
                                    selAlbum = null; selArtist = null; selFolder = null
                                }
                            },
                            onTabSelect = {
                                if (it != tab) query = ""
                                tab = it
                            },
                            onOpenSettings = { showSettings = true },
                            onPlayAllShuffled = { playList(rootSongs, 0, shuffled = true) }
                        )
                        when (tab) {
                            LibraryTab.LAGU -> if (rootSongs.isEmpty()) {
                                SimpleEmpty("Tidak ada lagu cocok")
                            } else LibraryList(
                                tracks = rootSongs,
                                currentMediaId = currentMediaItemUri(controller),
                                onPlay = { playList(rootSongs, it, false) },
                                modifier = Modifier.weight(1f)
                            )
                            LibraryTab.ALBUM -> if (rootAlbums.isEmpty()) {
                                SimpleEmpty("Tidak ada album cocok")
                            } else LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp, vertical = 6.dp
                                )
                            ) {
                                items(rootAlbums, key = { it.albumId }) { album ->
                                    AlbumRow(album) { selAlbum = album }
                                }
                            }
                            LibraryTab.ARTIS -> if (rootArtists.isEmpty()) {
                                SimpleEmpty("Tidak ada artis cocok")
                            } else LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp, vertical = 6.dp
                                )
                            ) {
                                items(rootArtists, key = { it.first }) { (name, count) ->
                                    ArtistRow(name, count) { selArtist = name }
                                }
                            }
                            LibraryTab.FOLDER -> if (rootFolders.isEmpty()) {
                                SimpleEmpty("Tidak ada folder cocok")
                            } else LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp, vertical = 6.dp
                                )
                            ) {
                                items(rootFolders, key = { it.path }) { item ->
                                    FolderRow(item) { selFolder = item.key }
                                }
                            }
                        }
                    }
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
                controller?.let { c ->
                    if (mirror.playing) c.pause()
                    else if (c.mediaItemCount > 0) resumePlay()
                }
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
            },
            onOpenEqualizer = { showEq = true },
            sleepActive = sleepUntil > 0L,
            sleepLabel = if (sleepUntil > 0L) "Sleep ${fmtMs(sleepLeftMs)}" else null,
            onSleep = { showSleepDialog = true }
        )
    }

    if (showEq) {
        EqualizerSheet(onDismiss = { showEq = false })
    }

    if (showSettings) {
        SettingsSheet(
            onEqualizer = {
                showSettings = false
                showEq = true
            },
            onSleep = {
                showSettings = false
                showSleepDialog = true
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            active = sleepUntil > 0L,
            onCancel = {
                sleepUntil = 0L
                sleepLeftMs = 0L
                showSleepDialog = false
            },
            onPickMinutes = { minutes ->
                sleepUntil = SystemClock.elapsedRealtime() + minutes * 60_000L
                sleepLeftMs = minutes * 60_000L
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false }
        )
    }
}

@Composable
private fun LibraryHeader(
    context: Context,
    tab: LibraryTab,
    totalSongs: Int,
    rootSongs: Int,
    rootAlbums: Int,
    rootArtists: Int,
    rootFolders: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onTabSelect: (LibraryTab) -> Unit,
    onOpenSettings: () -> Unit,
    onPlayAllShuffled: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFFFF744A), Color(0xFFB22C12)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = "putar",
                    tint = Color(0xFFFFF8F2),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "putar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Pemutar offline · v${context.versionName()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Setelan", tint = MutedInk)
            }
            Spacer(Modifier.width(2.dp))
            if (tab == LibraryTab.LAGU) {
                FilledTonalButton(onClick = onPlayAllShuffled) {
                    Icon(
                        Icons.Filled.Shuffle, contentDescription = null,
                        tint = Coral, modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Acak semua", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Cari…", color = FaintInk) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedInk) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Bersihkan", tint = FaintInk)
                    }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Coral,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                cursorColor = Coral,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        LibraryTabBar(current = tab, onSelect = onTabSelect)

        Spacer(Modifier.height(6.dp))
        val info = when (tab) {
            LibraryTab.LAGU ->
                if (query.isBlank()) "$totalSongs lagu di perangkat" else "$rootSongs dari $totalSongs lagu"
            LibraryTab.ALBUM ->
                if (query.isBlank()) "${rootAlbums} album" else "$rootAlbums album cocok"
            LibraryTab.ARTIS ->
                if (query.isBlank()) "$rootArtists artis" else "$rootArtists artis cocok"
            LibraryTab.FOLDER ->
                if (query.isBlank()) "$rootFolders folder" else "$rootFolders folder cocok"
        }
        Text(
            info,
            style = MaterialTheme.typography.bodySmall,
            color = MutedInk,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@Composable
private fun SleepTimerDialog(
    active: Boolean,
    onCancel: () -> Unit,
    onPickMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Sleep timer", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (active) {
                    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Matikan sleep timer", color = Coral, fontWeight = FontWeight.SemiBold)
                    }
                }
                listOf(10, 15, 30, 45, 60, 90).forEach { minutes ->
                    TextButton(
                        onClick = { onPickMinutes(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (minutes < 60) "$minutes menit" else "1 jam ${minutes - 60} menit".trimEnd(),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = MutedInk) }
        }
    )
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
            textAlign = TextAlign.Center
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
            textAlign = TextAlign.Center
        )
    }
}
