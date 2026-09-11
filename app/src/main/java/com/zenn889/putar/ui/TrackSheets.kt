package com.zenn889.putar.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Playlist
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk

fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, tint: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

/* ============ menu konteks tekan-lama pada lagu ============ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackContextSheet(
    track: Track,
    isFavorite: Boolean,
    onPlayNow: () -> Unit,
    onPlayNext: () -> Unit,
    onAddQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    track.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    track.displayArtist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
            Spacer(Modifier.height(10.dp))
            ActionRow(Icons.Filled.PlayArrow, "Putar sekarang") { onPlayNow() }
            ActionRow(Icons.Filled.SkipNext, "Putar berikutnya") { onPlayNext() }
            ActionRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Tambah ke antrian") { onAddQueue() }
            ActionRow(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (isFavorite) "Hapus dari favorit" else "Tandai favorit",
                tint = if (isFavorite) Coral else MaterialTheme.colorScheme.onSurface
            ) { onToggleFavorite() }
            ActionRow(Icons.AutoMirrored.Filled.QueueMusic, "Tambah ke playlist") { onAddToPlaylist() }
        }
    }
}

/* ============ pilih playlist (dari menu konteks) ============ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<Playlist>,
    onCreate: (String) -> Unit,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCreate by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(bottom = 22.dp)) {
            Text(
                "Tambah ke playlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            if (playlists.isEmpty()) {
                Text(
                    "Belum ada playlist.",
                    color = MutedInk,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            playlists.forEach { pl ->
                ActionRow(Icons.AutoMirrored.Filled.PlaylistPlay, "${pl.name}  ·  ${pl.uris.size} lagu") {
                    onPick(pl.name)
                }
            }
            ActionRow(Icons.Filled.Add, "Buat playlist baru…", tint = Coral) {
                showCreate = true
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Playlist baru", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Nama playlist", color = FaintInk) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = name.trim()
                    if (clean.isEmpty()) {
                        toast(context, "Nama playlist kosong")
                        return@TextButton
                    }
                    showCreate = false
                    onCreate(clean)
                }) { Text("Buat", color = Coral) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Batal", color = MutedInk) }
            }
        )
    }
}

/* ============ kelola playlist (dari Setelan) ============ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBrowserSheet(
    playlists: List<Playlist>,
    tracks: List<Track>,
    onCreate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onPlay: (String) -> Unit,
    onRemoveTrack: (String, String) -> Unit,
    onMoveTrack: (String, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var openName by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val byUri = remember(tracks) { tracks.associateBy { it.contentUri } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val open = openName
        if (open == null) {
            Column(Modifier.padding(bottom = 22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Playlist",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalButton(onClick = { showCreate = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null,
                            tint = Coral, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Buat")
                    }
                }
                if (playlists.isEmpty()) {
                    Text(
                        "Belum ada playlist — buat satu untuk mulai menyusun lagu.",
                        color = MutedInk,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
                playlists.forEach { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openName = pl.name }
                            .padding(horizontal = 20.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null, tint = Coral)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(pl.name, style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium)
                            Text(
                                if (pl.uris.size == 1) "1 lagu" else "${pl.uris.size} lagu",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                        }
                        IconButton(onClick = {
                            onDelete(pl.name)
                            toast(context, "Playlist \"${pl.name}\" dihapus")
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus playlist",
                                tint = FaintInk)
                        }
                    }
                }
            }
        } else {
            val pl = playlists.firstOrNull { it.name == open }
            if (pl == null) {
                openName = null
            } else {
                // pasangan (indeks urutan di playlist, lagu): pengurutan tetap
                // benar walau ada lagu yang sudah hilang dari perangkat
                val songs = pl.uris.mapIndexedNotNull { i, uri -> byUri[uri]?.let { i to it } }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { openName = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold)
                        Text(
                            if (songs.size == 1) "1 lagu" else "${songs.size} lagu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk
                        )
                    }
                    IconButton(onClick = { onPlay(pl.name) }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Putar playlist",
                            tint = Coral, modifier = Modifier.size(28.dp))
                    }
                }
                if (songs.isEmpty()) {
                    Text(
                        "Lagu tidak ditemukan (mungkin sudah dihapus dari perangkat).",
                        color = MutedInk,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                LazyColumn(Modifier.padding(bottom = 16.dp)) {
                    itemsIndexed(songs, key = { i, e -> "$i|${e.second.contentUri}" }) { index, entry ->
                        val uriIndex = entry.first
                        val track = entry.second
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlay(pl.name) }
                                .padding(start = 8.dp, end = 20.dp, top = 5.dp, bottom = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // gagang seret: geser vertikal untuk mengubah urutan
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .pointerInput(uriIndex, songs.size) {
                                        var acc = 0f
                                        detectVerticalDragGestures(
                                            onDragEnd = { acc = 0f },
                                            onDragCancel = { acc = 0f },
                                            onVerticalDrag = { _, d ->
                                                acc += d
                                                while (acc <= -150f) {
                                                    onMoveTrack(pl.name, uriIndex, uriIndex - 1)
                                                    acc += 150f
                                                }
                                                while (acc >= 150f) {
                                                    onMoveTrack(pl.name, uriIndex, uriIndex + 1)
                                                    acc -= 150f
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = "Ubah urutan lagu",
                                    tint = FaintInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            AlbumArt(MusicRepository.albumArtUri(track.albumId), size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Ink)
                                Text(track.displayArtist, maxLines = 1,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedInk)
                            }
                            IconButton(
                                onClick = { onMoveTrack(pl.name, uriIndex, uriIndex - 1) },
                                enabled = index > 0
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp,
                                    contentDescription = "Naikkan urutan", tint = FaintInk)
                            }
                            IconButton(
                                onClick = { onMoveTrack(pl.name, uriIndex, uriIndex + 1) },
                                enabled = index < songs.size - 1
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Turunkan urutan", tint = FaintInk)
                            }
                            IconButton(onClick = {
                                onRemoveTrack(pl.name, track.contentUri)
                                toast(context, "Dihapus dari playlist")
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Hapus dari playlist",
                                    tint = FaintInk)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Playlist baru", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    placeholder = { Text("Nama playlist", color = FaintInk) }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val clean = name.trim()
                    if (clean.isEmpty()) {
                        toast(context, "Nama playlist kosong")
                        return@TextButton
                    }
                    showCreate = false
                    onCreate(clean)
                }) { Text("Buat", color = Coral) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text("Batal", color = MutedInk) }
            }
        )
    }
}
