package com.zenn889.putar

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.GraphicEq
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.zenn889.putar.data.BackupStore
import com.zenn889.putar.data.FavStore
import com.zenn889.putar.data.LyricsLoader
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Playlist
import com.zenn889.putar.data.PlaylistStore
import com.zenn889.putar.data.SessionStore
import com.zenn889.putar.data.StatsStore
import com.zenn889.putar.data.Track
import com.zenn889.putar.data.VideoItem
import com.zenn889.putar.data.VolumeNorm
import com.zenn889.putar.ui.AddToPlaylistSheet
import com.zenn889.putar.ui.AlbumCard
import com.zenn889.putar.ui.ArtistRow
import com.zenn889.putar.ui.BackBar
import com.zenn889.putar.ui.EqualizerSheet
import com.zenn889.putar.ui.FolderRow
import com.zenn889.putar.ui.HeroCard
import com.zenn889.putar.ui.LibraryFilterDialog
import com.zenn889.putar.ui.LibraryList
import com.zenn889.putar.ui.LibraryTab
import com.zenn889.putar.ui.QuickAccessGrid
import com.zenn889.putar.ui.QuickEntry
import com.zenn889.putar.ui.SectionHeader
import com.zenn889.putar.ui.LyricsSheet
import com.zenn889.putar.ui.MiniPlayer
import com.zenn889.putar.ui.NowPlayingSheet
import com.zenn889.putar.ui.PlaylistBrowserSheet
import com.zenn889.putar.ui.PlayerMirror
import com.zenn889.putar.ui.ProgressState
import com.zenn889.putar.ui.PureWaveBottomBar
import com.zenn889.putar.ui.QueueEntry
import com.zenn889.putar.ui.QueueSheet
import com.zenn889.putar.ui.RecentlyAddedRow
import com.zenn889.putar.ui.SettingsSheet
import com.zenn889.putar.ui.SimpleEmpty
import com.zenn889.putar.ui.SortMenuButton
import com.zenn889.putar.ui.SortOption
import com.zenn889.putar.ui.StatsDialog
import com.zenn889.putar.ui.TrackContextSheet
import com.zenn889.putar.ui.TrackRow
import com.zenn889.putar.ui.TrackStrip
import com.zenn889.putar.ui.VideoPlayback
import com.zenn889.putar.ui.VideoPlayerScreen
import com.zenn889.putar.ui.VideoRow
import com.zenn889.putar.ui.WelcomeScreen
import com.zenn889.putar.ui.buildArtistItems
import com.zenn889.putar.ui.buildFolderItems
import com.zenn889.putar.ui.fmtMs
import com.zenn889.putar.ui.matchesQuery
import com.zenn889.putar.ui.matchesTokens
import com.zenn889.putar.ui.pipParams
import com.zenn889.putar.ui.searchTokens
import com.zenn889.putar.ui.sortedTracks
import com.zenn889.putar.ui.toast
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.CoralBright
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.PutarTheme
import com.zenn889.putar.ui.theme.SurfaceHigh
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        installCrashLogger(this)
        enableEdgeToEdge()
        handleOpenIntent(intent)
        setContent {
            PutarTheme {
                PlayerApp()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenIntent(intent)
    }

    /** Terima "buka dengan PureWave" dari File Manager / aplikasi lain. */
    private fun handleOpenIntent(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (intent.action == android.content.Intent.ACTION_VIEW) {
            OpenRequest.uri = data
            OpenRequest.token.value += 1
        }
    }

    /** Video jalan + user tekan Home → masuk Picture-in-Picture. */
    override fun onUserLeaveHint() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            VideoPlayback.active && VideoPlayback.playing
        ) {
            runCatching { enterPictureInPictureMode(pipParams(VideoPlayback.aspect)) }
        }
        super.onUserLeaveHint()
    }

    /**
     * Masuk/keluar PiP menandai UI supaya seluruh overlay kontrol
     * disembunyikan — hanya gambarnya yang tampil (temuan audit #9).
     */
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        VideoPlayback.inPip.value = isInPictureInPictureMode
    }
}

/** Permintaan buka file dari luar app. */
object OpenRequest {
    val token = androidx.compose.runtime.mutableStateOf(0)
    var uri: android.net.Uri? = null
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

/** Izin video terpisah sejak Android 13; di bawah itu sudah tercakup. */
private fun videoPermission(): String? =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else null

private fun Context.hasReadPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, readPermission()) == PackageManager.PERMISSION_GRANTED

private fun Context.hasVideoGranted(): Boolean {
    val perm = videoPermission() ?: return true
    return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
}

private fun Context.hasAllMediaPermission(): Boolean =
    hasReadPermission() && hasVideoGranted()

fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull() ?: ""

/** Bangun Track minimal dari URI eksternal (buka-dengan dari app lain). */
private fun buildTrackFromUri(context: Context, uri: Uri): Track? = runCatching {
    val name = context.contentResolver.query(
        uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
    )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    val title = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Audio"
    Track(
        mediaId = 0L,
        contentUri = uri.toString(),
        title = title,
        artist = "",
        durationMs = 0L,
        albumId = null,
        folder = null
    )
}.getOrNull()

private fun greetingLine(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 4..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..17 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

private fun Track.toMediaItem(): MediaItem =
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

private fun currentMediaItemUri(c: MediaController?): String? =
    c?.currentMediaItem?.mediaId

private fun buildAlbumsFrom(tracks: List<Track>): List<Album> =
    tracks.filter { it.albumId != null }
        .groupBy { it.albumId!! }
        .map { (_, list) ->
            Album(
                albumId = list.first().albumId!!,
                title = list.first().albumTitle ?: "Tanpa album",
                artist = list.first().artist,
                songCount = list.size
            )
        }
        .sortedBy { it.title.lowercase() }

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
        index = player.currentMediaItemIndex,
        speed = player.playbackParameters.speed
    )
}

private val SPEED_STEPS = floatArrayOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

private fun fmtSpeed(speed: Float): String {
    val s = SPEED_STEPS.minByOrNull { abs(it - speed) } ?: speed
    val txt = if (s == s.toInt().toFloat()) s.toInt().toString()
    else s.toString().trimEnd('0').trimEnd('.')
    return "${txt}x"
}

private fun nextSpeed(current: Float): Float {
    val idx = SPEED_STEPS.indexOfFirst { abs(it - current) < 0.01f }
    val base = if (idx >= 0) idx else 2 // default 1x
    return SPEED_STEPS[(base + 1) % SPEED_STEPS.size]
}

@Composable
fun PlayerApp() {
    val context = LocalContext.current.applicationContext
    val repo = remember { MusicRepository(context) }

    // --- onboarding sekali jalan (tampil sebelum apa pun) ---
    var onboarded by remember {
        mutableStateOf(
            context.getSharedPreferences("putar_prefs", Context.MODE_PRIVATE)
                .getBoolean("onboarded", false)
        )
    }
    if (!onboarded) {
        WelcomeScreen(onDone = {
            context.getSharedPreferences("putar_prefs", Context.MODE_PRIVATE)
                .edit().putBoolean("onboarded", true).apply()
            onboarded = true
        })
        return
    }

    Box(Modifier.fillMaxSize()) {

    // latar: dasar + pendar coral tipis dari atas (biar tidak terasa datar)
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(MaterialTheme.colorScheme.background)
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    listOf(Coral.copy(alpha = 0.13f), Color.Transparent, Color.Transparent),
                    endY = 950f
                )
            )
    )

    // --- kontrol pemutar (Media3) ---
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var mirror by remember { mutableStateOf(PlayerMirror()) }
    val progressState = remember { ProgressState() }
    val uiScope = rememberCoroutineScope()
    // pelacak pemutaran untuk statistik & sejarah
    var lastPlayUri by remember { mutableStateOf<String?>(null) }
    var statsVersion by remember { mutableLongStateOf(0L) }
    // pemutar video
    var videoQueue by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var videoIndex by remember { mutableStateOf(0) }
    var showVideoPlayer by remember { mutableStateOf(false) }

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
                        Player.EVENT_POSITION_DISCONTINUITY,
                        Player.EVENT_PLAYBACK_PARAMETERS_CHANGED
                    )
                ) {
                    mirror = readMirror(player)
                    // catat mulai lagu untuk statistik & sejarah
                    if (player.isPlaying) {
                        val id = player.currentMediaItem?.mediaId
                        if (id != null && id != lastPlayUri) {
                            lastPlayUri = id
                            StatsStore.recordPlay(context, id)
                            statsVersion += 1L
                        }
                    }
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
    var granted by remember { mutableStateOf(context.hasAllMediaPermission()) }
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(LibraryTab.LAGU) }

    // filter pustaka (aturannya di Setelan → Filter pustaka)
    val libPrefs = context.getSharedPreferences("putar_prefs", Context.MODE_PRIVATE)
    var hideShort by remember { mutableStateOf(libPrefs.getBoolean("hide_short", false)) }
    var hideSystemDirs by remember { mutableStateOf(libPrefs.getBoolean("hide_system_dirs", false)) }
    var hideDups by remember { mutableStateOf(libPrefs.getBoolean("hide_dups", false)) }
    fun setHide(key: String, set: (Boolean) -> Unit, value: Boolean) {
        set(value)
        libPrefs.edit().putBoolean(key, value).apply()
    }

    // pustaka yang benar-benar ditampilkan setelah filter
    val library = remember(tracks, hideShort, hideSystemDirs, hideDups) {
        var list = tracks
        if (hideShort) list = list.filter { it.durationMs >= 30_000L }
        if (hideSystemDirs) {
            val bad = listOf("ringtones", "notifications", "alarms", "alarm", "ringtone", "notification")
            list = list.filter { t ->
                val p = t.folder.orEmpty().lowercase()
                bad.none { p.contains(it) }
            }
        }
        if (hideDups) {
            val seen = HashSet<String>()
            list = list.filter { t ->
                seen.add("${t.title.lowercase()}|${t.displayArtist.lowercase()}")
            }
        }
        list
    }
    val libraryAlbums = remember(library) { buildAlbumsFrom(library) }
    var favUris by remember { mutableStateOf(FavStore.load(context)) }
    var playlists by remember { mutableStateOf(PlaylistStore.list(context)) }
    var sortChoice by remember {
        // ingat pilihan sortir terakhir (tersimpan di putar_prefs)
        val saved = context.getSharedPreferences("putar_prefs", Context.MODE_PRIVATE)
            .getString("sort_choice", null)
        mutableStateOf(
            SortOption.entries.firstOrNull { it.name == saved } ?: SortOption.JUDUL
        )
    }
    // normalisasi volume antar lagu (ReplayGain)
    var volumeNormOn by remember { mutableStateOf(VolumeNorm.enabled(context)) }

    /** Ganti sortir + simpan pilihannya supaya menempel saat app dibuka lagi. */
    fun setSort(opt: SortOption) {
        sortChoice = opt
        context.getSharedPreferences("putar_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("sort_choice", opt.name)
            .apply()
    }

    // antrian (lihat/urut/hapus)
    var showQueue by remember { mutableStateOf(false) }
    var queueEntries by remember { mutableStateOf<List<QueueEntry>>(emptyList()) }

    fun refreshQueueEntries() {
        val c = controller
        if (c == null) { queueEntries = emptyList(); return }
        val n = c.mediaItemCount
        if (n == 0) { queueEntries = emptyList(); return }
        queueEntries = List(n) { i ->
            val mi = c.getMediaItemAt(i)
            val m = mi.mediaMetadata
            QueueEntry(
                uri = mi.mediaId,
                title = m.title?.toString() ?: "Tanpa judul",
                artist = m.artist?.toString() ?: "",
                artwork = m.artworkUri
            )
        }
    }

    // menu konteks (tekan lama), tambah-ke-playlist, kelola playlist
    var contextTrack by remember { mutableStateOf<Track?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showPlaylistBrowser by remember { mutableStateOf(false) }

    fun reloadPlaylists() { playlists = PlaylistStore.list(context) }

    // detail yang sedang dibuka (album/artis/folder)
    var selAlbum by remember { mutableStateOf<Album?>(null) }
    var selArtist by remember { mutableStateOf<String?>(null) }
    var selFolder by remember { mutableStateOf<String?>(null) }
    val inDetail = selAlbum != null || selArtist != null || selFolder != null

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        granted = (result[readPermission()] == true) &&
            (videoPermission()?.let { result[it] == true } ?: true)
    }

    LaunchedEffect(granted) {
        if (granted) {
            loading = true
            val lib = repo.loadLibrary()
            val vids = repo.loadVideos()
            tracks = lib
            videos = vids
            loading = false
        }
    }

    fun requestPermissions() {
        val perms = buildList {
            add(readPermission())
            videoPermission()?.let { add(it) }
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    // --- daftar per tab (root), dihitung sekali per perubahan ---
    val songsByUri = remember(library) { library.associateBy { it.contentUri } }
    val artistItems = remember(library) { buildArtistItems(library) }
    val folderItems = remember(library) { buildFolderItems(library) }

    val q = query.trim()
    // pencarian: dipecah jadi token, urutan kata bebas, diakritik diabaikan
    val tokens = remember(q) { searchTokens(q) }
    // statistik untuk opsi sortir "paling sering diputar" & "terakhir diputar"
    val playsMap = remember(library, statsVersion) { StatsStore.playsMap(context) }
    val recencyMap = remember(library, statsVersion) {
        StatsStore.recent(context).withIndex().associate { (i, uri) -> uri to i }
    }
    val rootSongs = remember(library, tokens, sortChoice, playsMap, recencyMap) {
        val base = if (tokens.isEmpty()) library else library.filter { it.matchesQuery(tokens) }
        sortedTracks(base, sortChoice, playsMap, recencyMap)
    }
    val rootAlbums = remember(libraryAlbums, tokens) {
        if (tokens.isEmpty()) libraryAlbums
        else libraryAlbums.filter { matchesTokens("${it.title} ${it.displayArtist}", tokens) }
    }
    val rootArtists = remember(artistItems, tokens) {
        if (tokens.isEmpty()) artistItems
        else artistItems.filter { matchesTokens(it.first, tokens) }
    }
    val rootFolders = remember(folderItems, tokens) {
        if (tokens.isEmpty()) folderItems
        else folderItems.filter { matchesTokens("${it.name} ${it.path}", tokens) }
    }
    val rootVideos = remember(videos, tokens) {
        if (tokens.isEmpty()) videos
        else videos.filter { matchesTokens(it.title, tokens) }
    }
    val favTracks = remember(library, favUris) {
        library.filter { favUris.contains(it.contentUri) }
    }
    val favQueryTracks = remember(favTracks, tokens, sortChoice, playsMap, recencyMap) {
        val base = if (tokens.isEmpty()) favTracks else favTracks.filter { it.matchesQuery(tokens) }
        sortedTracks(base, sortChoice, playsMap, recencyMap)
    }

    // daftar detail (album / artis / folder) — ikut aturan sortir yang aktif
    val detailSongs = remember(
        library, selAlbum, selArtist, selFolder, sortChoice, playsMap, recencyMap
    ) {
        val base = when {
            selAlbum != null -> library.filter { it.albumId != null && it.albumId == selAlbum!!.albumId }
            selArtist != null -> library.filter { it.displayArtist == selArtist }
            selFolder != null -> library.filter { it.folder.orEmpty() == selFolder }
            else -> emptyList()
        }
        sortedTracks(base, sortChoice, playsMap, recencyMap)
    }

    // sejarah & paling sering diputar (untuk beranda)
    val statsRecent = remember(library, statsVersion) {
        StatsStore.recent(context).mapNotNull { songsByUri[it] }.take(12)
    }
    val statsTop = remember(library, statsVersion) {
        val plays = StatsStore.playsMap(context)
        library.filter { (plays[it.contentUri] ?: 0) > 0 }
            .sortedByDescending { plays[it.contentUri] ?: 0 }
            .take(10)
    }

    // --- pemutaran ---
    var pendingResumeMs by remember { mutableLongStateOf(-1L) }
    var restoredApplied by remember { mutableStateOf(false) }
    var lastSavedPos by remember { mutableLongStateOf(-1L) }

    // sleep timer: "setelah lagu ini selesai" & "setelah N lagu"
    var sleepEndOfTrack by remember { mutableStateOf(false) }
    var sleepPrevIndex by remember { mutableStateOf(-1) }
    var sleepSongsLeft by remember { mutableStateOf(0) }
    var sleepSongsPrevIndex by remember { mutableStateOf(-1) }
    var nextStack by remember { mutableStateOf(0) }

    // fade-out lembut ~3 dtk lalu pause (gain normalisasi tetap dihormati)
    fun fadePause() {
        val c = controller ?: return
        val base = VolumeNorm.baseGain
        uiScope.launch {
            val steps = 14
            for (i in steps downTo 1) {
                c.volume = base * i.toFloat() / steps
                delay(220)
            }
            c.pause()
            c.volume = base
        }
    }

    fun playList(list: List<Track>, index: Int, shuffled: Boolean) {
        if (list.isEmpty()) return
        val c = controller ?: return
        val items = list.map { it.toMediaItem() }
        val start = if (shuffled) Random.nextInt(items.size) else index.coerceIn(0, items.size - 1)
        pendingResumeMs = -1L
        sleepEndOfTrack = false
        sleepPrevIndex = -1
        nextStack = 0
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

    // --- aksi lagu (favorit / antrian / playlist) ---
    fun playNextOf(track: Track) {
        val c = controller
        if (c == null || c.playbackState == Player.STATE_IDLE || c.mediaItemCount == 0) {
            playList(listOf(track), 0, false)
            return
        }
        // menumpuk: tiap "berikutnya" disisipkan tepat setelah tumpukan sebelumnya
        val idx = c.currentMediaItemIndex + 1 + nextStack
        runCatching { c.addMediaItem(idx, track.toMediaItem()) }
        nextStack++
        toast(context, "Berikutnya: ${track.title}")
    }

    fun addToQueue(track: Track) {
        val c = controller
        if (c == null || c.playbackState == Player.STATE_IDLE || c.mediaItemCount == 0) {
            playList(listOf(track), 0, false)
            return
        }
        runCatching { c.addMediaItem(track.toMediaItem()) }
        toast(context, "Ditambahkan ke antrian")
    }

    fun toggleFav(uri: String) {
        val nowFav = FavStore.toggle(context, uri)
        favUris = FavStore.load(context)
        toast(context, if (nowFav) "Ditandai favorit" else "Dihapus dari favorit")
    }

    fun pickPlaylist(name: String) {
        contextTrack?.let { PlaylistStore.addTrack(context, name, it.contentUri) }
        reloadPlaylists()
        contextTrack = null
        showAddToPlaylist = false
        toast(context, "Ditambahkan ke \"$name\"")
    }

    fun createPlaylistWithContextTrack(name: String) {
        val tr = contextTrack
        val ok = PlaylistStore.create(context, name, tr?.contentUri)
        reloadPlaylists()
        contextTrack = null
        showAddToPlaylist = false
        toast(context, if (ok) "Playlist \"$name\" dibuat" else "Nama playlist sudah dipakai")
    }

    fun playlistPlay(name: String) {
        val pl = playlists.firstOrNull { it.name == name } ?: return
        val songs = pl.uris.mapNotNull { songsByUri[it] }
        if (songs.isEmpty()) {
            toast(context, "Playlist kosong atau lagunya sudah tidak ada")
            return
        }
        playList(songs, 0, false)
        showPlaylistBrowser = false
    }

    fun playlistDelete(name: String) {
        PlaylistStore.delete(context, name)
        reloadPlaylists()
        toast(context, "Playlist \"$name\" dihapus")
    }

    fun playlistRemoveTrack(name: String, uri: String) {
        PlaylistStore.removeTrack(context, name, uri)
        reloadPlaylists()
    }

    /** Geser lagu di dalam playlist (indeks mengikuti urutan di playlist itu). */
    fun playlistMoveTrack(name: String, from: Int, to: Int) {
        if (PlaylistStore.moveTrack(context, name, from, to)) reloadPlaylists()
    }

    // auto-resume saat pustaka & controller siap
    // buka file dari luar app ("Buka dengan PureWave")
    LaunchedEffect(granted, controller, OpenRequest.token.value) {
        val uri = OpenRequest.uri ?: return@LaunchedEffect
        if (!granted || controller == null) return@LaunchedEffect
        OpenRequest.uri = null
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull().orEmpty()
        if (mime.startsWith("video/")) {
            val name = runCatching {
                context.contentResolver.query(
                    uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull()
            val v = VideoItem(
                mediaId = 0L,
                contentUri = uri,
                title = name?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Video",
                durationMs = 0L,
                dateAddedMs = 0L,
                folder = null,
                filePath = null
            )
            controller?.pause()
            videoQueue = listOf(v)
            videoIndex = 0
            showVideoPlayer = true
        } else {
            val track = buildTrackFromUri(context, uri)
            if (track != null) playList(listOf(track), 0, false)
        }
    }

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
        var lastSample = 0L
        var accMs = 0L
        while (mirror.playing) {
            delay(400)
            val c = controller
            if (c != null) {
                val pos = c.currentPosition.coerceAtLeast(0L)
                progressState.positionMs.longValue = pos
                val dt = pos - lastSample
                if (dt in 1..8_000L) accMs += dt
                lastSample = pos
                if (accMs >= 20_000L) {
                    c.currentMediaItem?.mediaId?.let { id ->
                        StatsStore.addMinutes(context, id, accMs)
                        statsVersion += 1L
                    }
                    accMs = 0L
                }
                if (abs(pos - lastSavedPos) > 4000L) {
                    lastSavedPos = pos
                    SessionStore.savePosition(context, c.currentMediaItemIndex, pos)
                }
            }
        }
    }

    // sleep timer
    var sleepUntil by remember { mutableLongStateOf(0L) }
    var sleepLeftMs by remember { mutableLongStateOf(0L) }
    var showSleepDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sleepUntil) {
        while (sleepUntil > 0L) {
            val left = sleepUntil - SystemClock.elapsedRealtime()
            if (left <= 0L) {
                sleepUntil = 0L
                sleepLeftMs = 0L
                fadePause()
                break
            }
            sleepLeftMs = left
            delay(1000)
        }
    }

    // berhenti di akhir lagu saat ini (terpicu saat lagu berganti)
    LaunchedEffect(mirror.index, sleepEndOfTrack) {
        if (sleepEndOfTrack && sleepPrevIndex >= 0 &&
            mirror.index != sleepPrevIndex && controller?.isPlaying == true
        ) {
            fadePause()
            sleepEndOfTrack = false
            sleepPrevIndex = -1
            toast(context, "Sleep timer selesai")
        }
    }

    // berhenti setelah N lagu berikutnya
    LaunchedEffect(mirror.index, sleepSongsLeft) {
        if (sleepSongsLeft > 0 && sleepSongsPrevIndex >= 0 &&
            mirror.index != sleepSongsPrevIndex && controller?.isPlaying == true
        ) {
            sleepSongsPrevIndex = mirror.index
            val left = sleepSongsLeft - 1
            if (left <= 0) {
                sleepSongsLeft = 0
                sleepSongsPrevIndex = -1
                fadePause()
                toast(context, "Sleep timer selesai")
            } else {
                sleepSongsLeft = left
            }
        }
    }

    val sleepAnyActive = sleepUntil > 0L || sleepEndOfTrack || sleepSongsLeft > 0

    // --- layar tambahan ---
    var showFullPlayer by remember { mutableStateOf(false) }
    var showEq by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsLines by remember { mutableStateOf<List<LyricsLoader.Line>>(emptyList()) }

    // muat lirik .lrc untuk lagu aktif
    LaunchedEffect(showLyrics, mirror.index) {
        if (showLyrics) {
            val uri = currentMediaItemUri(controller)
            val t = uri?.let { songsByUri[it] }
            lyricsLines = if (t != null) {
                LyricsLoader.load(context, t.filePath, t.title, t.folder)
            } else emptyList()
        }
    }

    // cadangkan / pulihkan data (SAF)
    val backupExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(BackupStore.buildJson(context).toByteArray())
            } != null
        }.getOrDefault(false)
        toast(context, if (ok) "Cadangan tersimpan" else "Gagal menyimpan cadangan")
    }
    val backupImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }.orEmpty()
            BackupStore.applyJson(context, text)
        }.getOrDefault(false)
        favUris = FavStore.load(context)
        reloadPlaylists()
        toast(context, if (ok) "Data dipulihkan" else "File cadangan tidak valid")
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column {
                if (mirror.hasMedia) {
                    MiniPlayer(
                        mirror = mirror,
                        progress = progressState,
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
                PureWaveBottomBar(
                    current = tab,
                    onSelect = { sel ->
                        if (sel != tab) {
                            if (inDetail) {
                                selAlbum = null
                                selArtist = null
                                selFolder = null
                            }
                            query = ""
                            tab = sel
                        }
                    }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                !granted -> PermissionScreen(onRequest = ::requestPermissions)
                loading -> CircularProgressIndicator(
                    color = Coral,
                    modifier = Modifier.align(Alignment.Center)
                )
                tracks.isEmpty() && videos.isEmpty() -> EmptyLibraryScreen()
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
                                    onBack = back,
                                    trailing = { SortMenuButton(current = sortChoice, onSelect = { setSort(it) }) }
                                )
                                LibraryList(
                                    tracks = detailSongs,
                                    currentMediaId = currentMediaItemUri(controller),
                                    onPlay = { playList(detailSongs, it, false) },
                                    onLongClickTrack = { contextTrack = it; showContextMenu = true },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            selArtist != null -> {
                                BackBar(
                                    selArtist!!,
                                    "${detailSongs.size} lagu",
                                    onBack = back,
                                    trailing = { SortMenuButton(current = sortChoice, onSelect = { setSort(it) }) }
                                )
                                LibraryList(
                                    tracks = detailSongs,
                                    currentMediaId = currentMediaItemUri(controller),
                                    onPlay = { playList(detailSongs, it, false) },
                                    onLongClickTrack = { contextTrack = it; showContextMenu = true },
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
                                    onBack = back,
                                    trailing = { SortMenuButton(current = sortChoice, onSelect = { setSort(it) }) }
                                )
                                LibraryList(
                                    tracks = detailSongs,
                                    currentMediaId = currentMediaItemUri(controller),
                                    onPlay = { playList(detailSongs, it, false) },
                                    onLongClickTrack = { contextTrack = it; showContextMenu = true },
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
                            rootVideos = rootVideos.size,
                            rootFavs = favQueryTracks.size,
                            query = query,
                            onQueryChange = {
                                query = it
                                if (inDetail) {
                                    selAlbum = null; selArtist = null; selFolder = null
                                }
                            },
                            onOpenSettings = { showSettings = true },
                            onPlayAllShuffled = { playList(rootSongs, 0, shuffled = true) },
                            onTabSelect = { tab = it },
                            showSort = tab == LibraryTab.LAGU || tab == LibraryTab.FAVORIT,
                            sortChoice = sortChoice,
                            onSortChange = { setSort(it) }
                        )
                        when (tab) {
                            LibraryTab.LAGU -> if (rootSongs.isEmpty()) {
                                SimpleEmpty("Tidak ada lagu cocok")
                            } else LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    top = 4.dp, bottom = 18.dp
                                )
                            ) {
                                item { HeroCard(rootSongs) { playList(rootSongs, 0, true) } }
                                if (q.isEmpty()) {
                                    item {
                                        QuickAccessGrid(
                                            entries = listOf(
                                                QuickEntry("album", "Album", Icons.Filled.Album, rootAlbums.size, true),
                                                QuickEntry("artis", "Artis", Icons.Filled.People, rootArtists.size),
                                                QuickEntry("folder", "Folder", Icons.Filled.Folder, rootFolders.size),
                                                QuickEntry("video", "Video", Icons.Filled.Movie, rootVideos.size),
                                                QuickEntry("favorit", "Favorit", Icons.Filled.Favorite, favQueryTracks.size, true),
                                                QuickEntry("playlist", "Playlist", Icons.AutoMirrored.Filled.PlaylistPlay),
                                                QuickEntry("statistik", "Statistik", Icons.Filled.Insights),
                                                QuickEntry("eq", "Equalizer", Icons.Filled.Equalizer)
                                            )
                                        ) { entry ->
                                            when (entry.key) {
                                                "album" -> tab = LibraryTab.ALBUM
                                                "artis" -> tab = LibraryTab.ARTIS
                                                "folder" -> tab = LibraryTab.FOLDER
                                                "video" -> tab = LibraryTab.VIDEO
                                                "favorit" -> tab = LibraryTab.FAVORIT
                                                "playlist" -> showPlaylistBrowser = true
                                                "statistik" -> showStats = true
                                                "eq" -> showEq = true
                                            }
                                        }
                                    }
                                    item {
                                        RecentlyAddedRow(rootSongs) { list, idx ->
                                            playList(list, idx, false)
                                        }
                                    }
                                    if (statsRecent.isNotEmpty()) {
                                        item {
                                            TrackStrip("Baru diputar", statsRecent) { list, idx ->
                                                playList(list, idx, false)
                                            }
                                        }
                                    }
                                    if (statsTop.isNotEmpty()) {
                                        item {
                                            TrackStrip("Paling sering diputar", statsTop) { list, idx ->
                                                playList(list, idx, false)
                                            }
                                        }
                                    }
                                }
                                item {
                                    SectionHeader(
                                        title = if (q.isEmpty()) "Semua lagu" else "Hasil pencarian",
                                        count = rootSongs.size
                                    )
                                }
                                itemsIndexed(rootSongs, key = { _, t -> t.contentUri }) { index, track ->
                                    TrackRow(
                                        track = track,
                                        isCurrent = track.contentUri == currentMediaItemUri(controller),
                                        onClick = { playList(rootSongs, index, false) },
                                        onLongClick = { contextTrack = track; showContextMenu = true },
                                        onSwipeLeft = { addToQueue(track) },
                                        onSwipeRight = { toggleFav(track.contentUri) }
                                    )
                                }
                            }
                            LibraryTab.ALBUM -> if (rootAlbums.isEmpty()) {
                                SimpleEmpty("Tidak ada album cocok")
                            } else LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 150.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 10.dp, vertical = 6.dp
                                )
                            ) {
                                gridItems(rootAlbums, key = { it.albumId }) { album ->
                                    AlbumCard(album) { selAlbum = album }
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
                            LibraryTab.VIDEO -> if (rootVideos.isEmpty()) {
                                SimpleEmpty("Tidak ada video cocok")
                            } else LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 8.dp, vertical = 6.dp
                                )
                            ) {
                                itemsIndexed(rootVideos, key = { _, v -> v.mediaId }) { vi, video ->
                                    VideoRow(video) {
                                        controller?.pause()
                                        videoQueue = rootVideos
                                        videoIndex = vi
                                        showVideoPlayer = true
                                    }
                                }
                            }
                            LibraryTab.FAVORIT -> Column(Modifier.weight(1f)) {
                                if (favQueryTracks.isEmpty()) {
                                    SimpleEmpty(
                                        if (favTracks.isEmpty())
                                            "Belum ada favorit — tekan lama sebuah lagu lalu pilih Favorit."
                                        else "Tidak ada favorit cocok"
                                    )
                                } else {
                                    HeroCard(favQueryTracks) { playList(favQueryTracks, 0, true) }
                                    LibraryList(
                                        tracks = favQueryTracks,
                                        currentMediaId = currentMediaItemUri(controller),
                                        onPlay = { playList(favQueryTracks, it, false) },
                                        onLongClickTrack = { contextTrack = it; showContextMenu = true },
                                        modifier = Modifier.weight(1f)
                                    )
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
            progress = progressState,
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
            sleepActive = sleepAnyActive,
            sleepLabel = when {
                sleepSongsLeft > 0 -> "Sleep $sleepSongsLeft lagu lagi"
                sleepUntil > 0L -> "Sleep ${fmtMs(sleepLeftMs)}"
                sleepEndOfTrack -> "Akhir lagu ini"
                else -> null
            },
            onSleep = { showSleepDialog = true },
            isFavorite = (currentMediaItemUri(controller) ?: "") in favUris,
            onToggleFavorite = {
                currentMediaItemUri(controller)?.let { toggleFav(it) }
            },
            onOpenQueue = {
                refreshQueueEntries()
                showQueue = true
            },
            onOpenLyrics = { showLyrics = true },
            speedLabel = fmtSpeed(mirror.speed),
            onCycleSpeed = {
                controller?.setPlaybackSpeed(nextSpeed(mirror.speed))
            }
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
            onPlaylists = {
                showSettings = false
                showPlaylistBrowser = true
            },
            onOpenFilters = {
                showSettings = false
                showFilters = true
            },
            onBackup = {
                showSettings = false
                backupExport.launch("purewave-backup.json")
            },
            onRestore = {
                showSettings = false
                backupImport.launch(arrayOf("application/json"))
            },
            onStats = {
                showSettings = false
                showStats = true
            },
            volumeNorm = volumeNormOn,
            onToggleVolumeNorm = { on ->
                volumeNormOn = on
                VolumeNorm.setEnabled(context, on)
                VolumeNorm.clearCache()
                PlaybackService.current()?.refreshVolumeNorm()
            },
            onDismiss = { showSettings = false }
        )
    }

    if (showFilters) {
        LibraryFilterDialog(
            hideShort = hideShort,
            hideSystemDirs = hideSystemDirs,
            hideDups = hideDups,
            onToggle = { key, value ->
                when (key) {
                    "short" -> setHide("hide_short", { hideShort = it }, value)
                    "sysdir" -> setHide("hide_system_dirs", { hideSystemDirs = it }, value)
                    "dup" -> setHide("hide_dups", { hideDups = it }, value)
                }
            },
            onDismiss = { showFilters = false }
        )
    }

    if (showStats) {
        val pMap = StatsStore.playsMap(context)
        val mMap = StatsStore.minutesMap(context)
        val topTracks = remember(library, statsVersion) {
            library.filter { (pMap[it.contentUri] ?: 0) > 0 }
                .sortedByDescending { pMap[it.contentUri] ?: 0 }
                .take(10)
        }
        val rows = remember(statsVersion) {
            topTracks.mapIndexed { i, t ->
                val p = pMap[t.contentUri] ?: 0
                val m = (mMap[t.contentUri] ?: 0L) / 60_000L
                "${i + 1}. ${t.title} — ${t.displayArtist}  ·  ${p}× · ${m} mnt"
            }
        }
        val totalMin = mMap.values.sum() / 60_000L
        StatsDialog(
            totalMinutes = totalMin,
            rows = rows,
            onDismiss = { showStats = false }
        )
    }

    if (showLyrics) {
        LyricsSheet(
            title = mirror.title,
            lines = lyricsLines,
            progress = progressState,
            onSeek = { ms -> controller?.seekTo(ms) },
            onDismiss = { showLyrics = false }
        )
    }

    if (showVideoPlayer && videoQueue.isNotEmpty()) {
        VideoPlayerScreen(
            queue = videoQueue,
            startIndex = videoIndex
        ) {
            showVideoPlayer = false
            videoQueue = emptyList()
        }
    }

    if (showContextMenu && contextTrack != null) {
        val tr = contextTrack!!
        TrackContextSheet(
            track = tr,
            isFavorite = tr.contentUri in favUris,
            onPlayNow = {
                playList(listOf(tr), 0, false)
                contextTrack = null
            },
            onPlayNext = {
                playNextOf(tr)
                contextTrack = null
            },
            onAddQueue = {
                addToQueue(tr)
                contextTrack = null
            },
            onToggleFavorite = {
                toggleFav(tr.contentUri)
                contextTrack = null
            },
            onAddToPlaylist = {
                showContextMenu = false
                showAddToPlaylist = true
            },
            onDismiss = { contextTrack = null }
        )
    }

    if (showAddToPlaylist) {
        AddToPlaylistSheet(
            playlists = playlists,
            onCreate = ::createPlaylistWithContextTrack,
            onPick = ::pickPlaylist,
            onDismiss = {
                showAddToPlaylist = false
                contextTrack = null
            }
        )
    }

    if (showPlaylistBrowser) {
        PlaylistBrowserSheet(
            playlists = playlists,
            tracks = tracks,
            onCreate = { name ->
                if (PlaylistStore.create(context, name)) {
                    reloadPlaylists()
                    toast(context, "Playlist \"$name\" dibuat")
                } else {
                    toast(context, "Nama playlist sudah dipakai")
                }
            },
            onDelete = ::playlistDelete,
            onPlay = ::playlistPlay,
            onRemoveTrack = ::playlistRemoveTrack,
            onMoveTrack = ::playlistMoveTrack,
            onDismiss = { showPlaylistBrowser = false }
        )
    }

    if (showQueue) {
        QueueSheet(
            entries = queueEntries,
            currentUri = currentMediaItemUri(controller),
            onPlay = { i ->
                controller?.let { c ->
                    runCatching { c.seekTo(i, 0L) }
                    c.play()
                }
                refreshQueueEntries()
            },
            onMoveUp = { i ->
                controller?.let { c -> runCatching { c.moveMediaItem(i, i - 1) } }
                refreshQueueEntries()
            },
            onMoveDown = { i ->
                controller?.let { c -> runCatching { c.moveMediaItem(i, i + 1) } }
                refreshQueueEntries()
            },
            onRemove = { i ->
                controller?.let { c -> runCatching { c.removeMediaItem(i) } }
                refreshQueueEntries()
            },
            onDismiss = { showQueue = false }
        )
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            active = sleepAnyActive,
            endOfTrackActive = sleepEndOfTrack,
            songsActive = sleepSongsLeft > 0,
            onCancel = {
                sleepUntil = 0L
                sleepLeftMs = 0L
                sleepEndOfTrack = false
                sleepPrevIndex = -1
                sleepSongsLeft = 0
                sleepSongsPrevIndex = -1
                showSleepDialog = false
            },
            onEndOfTrack = {
                if (mirror.hasMedia) {
                    sleepUntil = 0L
                    sleepLeftMs = 0L
                    sleepSongsLeft = 0
                    sleepSongsPrevIndex = -1
                    sleepPrevIndex = mirror.index
                    sleepEndOfTrack = true
                    showSleepDialog = false
                } else {
                    toast(context, "Belum ada lagu yang dimainkan")
                }
            },
            onPickSongs = { n ->
                if (mirror.hasMedia) {
                    sleepUntil = 0L
                    sleepLeftMs = 0L
                    sleepEndOfTrack = false
                    sleepPrevIndex = -1
                    sleepSongsPrevIndex = mirror.index
                    sleepSongsLeft = n
                    showSleepDialog = false
                } else {
                    toast(context, "Belum ada lagu yang dimainkan")
                }
            },
            onPickMinutes = { minutes ->
                sleepEndOfTrack = false
                sleepPrevIndex = -1
                sleepSongsLeft = 0
                sleepSongsPrevIndex = -1
                sleepUntil = SystemClock.elapsedRealtime() + minutes * 60_000L
                sleepLeftMs = minutes * 60_000L
                showSleepDialog = false
            },
            onDismiss = { showSleepDialog = false }
        )
    }
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
    rootVideos: Int,
    rootFavs: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onPlayAllShuffled: () -> Unit,
    onTabSelect: (LibraryTab) -> Unit,
    showSort: Boolean,
    sortChoice: SortOption,
    onSortChange: (SortOption) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 4.dp)) {
        // --- identitas aplikasi + sapaan ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(14.dp, RoundedCornerShape(16.dp), clip = false)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFFFF8A5B), Color(0xFFC2330F)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.GraphicEq,
                    contentDescription = "PureWave",
                    tint = Color(0xFFFFF8F2),
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    greetingLine(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "PureWave · pemutar offline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
            if (showSort) {
                SortMenuButton(current = sortChoice, onSelect = onSortChange)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Setelan", tint = MutedInk)
            }
        }

        Spacer(Modifier.height(12.dp))

        // --- kolom pencarian berbentuk pil ---
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Cari lagu, album, artis…", color = FaintInk) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MutedInk) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Bersihkan", tint = FaintInk)
                    }
                }
            },
            shape = RoundedCornerShape(999.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Coral.copy(alpha = 0.75f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                cursorColor = Coral,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        // --- chip statistik (sekaligus pintasan pindah tab) ---
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { StatChip("Lagu", rootSongs, tab == LibraryTab.LAGU) { onTabSelect(LibraryTab.LAGU) } }
            item { StatChip("Album", rootAlbums, tab == LibraryTab.ALBUM) { onTabSelect(LibraryTab.ALBUM) } }
            item { StatChip("Artis", rootArtists, tab == LibraryTab.ARTIS) { onTabSelect(LibraryTab.ARTIS) } }
            item { StatChip("Folder", rootFolders, tab == LibraryTab.FOLDER) { onTabSelect(LibraryTab.FOLDER) } }
            item { StatChip("Video", rootVideos, tab == LibraryTab.VIDEO) { onTabSelect(LibraryTab.VIDEO) } }
            item { StatChip("Favorit", rootFavs, tab == LibraryTab.FAVORIT) { onTabSelect(LibraryTab.FAVORIT) } }
            if (rootSongs > 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Coral,
                        modifier = Modifier.clickable(onClick = onPlayAllShuffled)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = Color(0xFF190902),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Acak semua",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF190902)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        val info = when (tab) {
            LibraryTab.LAGU ->
                if (query.isBlank()) "$totalSongs lagu di perangkat" else "$rootSongs dari $totalSongs lagu"
            LibraryTab.ALBUM ->
                if (query.isBlank()) "$rootAlbums album" else "$rootAlbums album cocok"
            LibraryTab.ARTIS ->
                if (query.isBlank()) "$rootArtists artis" else "$rootArtists artis cocok"
            LibraryTab.FOLDER ->
                if (query.isBlank()) "$rootFolders folder" else "$rootFolders folder cocok"
            LibraryTab.VIDEO ->
                if (query.isBlank()) "$rootVideos video" else "$rootVideos video cocok"
            LibraryTab.FAVORIT ->
                if (query.isBlank()) "$rootFavs favorit" else "$rootFavs favorit cocok"
        }
        Text(
            info,
            style = MaterialTheme.typography.bodySmall,
            color = MutedInk,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/** Chip statistik di header — sekaligus pintasan pindah tab. */
@Composable
private fun StatChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Coral.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Coral else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Coral else MutedInk
            )
        }
    }
}

@Composable
private fun SleepTimerDialog(
    active: Boolean,
    endOfTrackActive: Boolean,
    songsActive: Boolean,
    onCancel: () -> Unit,
    onEndOfTrack: () -> Unit,
    onPickSongs: (Int) -> Unit,
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
                TextButton(onClick = onEndOfTrack, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Setelah lagu ini selesai",
                        modifier = Modifier.fillMaxWidth(),
                        color = if (endOfTrackActive) Coral else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (endOfTrackActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
                listOf(2, 3, 5, 10).forEach { n ->
                    TextButton(
                        onClick = { onPickSongs(n) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (n == 2) "2 lagu berikutnya" else "$n lagu berikutnya",
                            modifier = Modifier.fillMaxWidth(),
                            color = if (songsActive) Coral else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (songsActive) FontWeight.Bold else FontWeight.Normal
                        )
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
