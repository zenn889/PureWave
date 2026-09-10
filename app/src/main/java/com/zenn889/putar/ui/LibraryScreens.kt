package com.zenn889.putar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenn889.putar.data.Album
import com.zenn889.putar.data.FolderItem
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.SurfaceHigh

enum class LibraryTab(val label: String) {
    LAGU("Lagu"),
    ALBUM("Album"),
    ARTIS("Artis"),
    FOLDER("Folder"),
    VIDEO("Video"),
    FAVORIT("Favorit")
}

/* ---------- util pengelompokan ---------- */

fun buildArtistItems(tracks: List<Track>): List<Pair<String, Int>> =
    tracks.groupingBy { it.displayArtist }
        .eachCount()
        .toList()
        .sortedBy { it.first.lowercase() }

fun buildFolderItems(tracks: List<Track>): List<FolderItem> =
    tracks.groupingBy { it.folder.orEmpty() }
        .eachCount()
        .map { (key, count) ->
            val name = if (key.isEmpty()) "Lainnya"
            else key.trim('/').substringAfterLast('/')
            FolderItem(key = key, name = name, path = key, songCount = count)
        }
        .sortedBy { it.name.lowercase() }

/* ---------- baris Album / Artis / Folder ---------- */

/** Kartu album persegi untuk tab Album (kisi ala Spotify). */
@Composable
fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AlbumArt(
            uri = MusicRepository.albumArtUri(album.albumId),
            size = 148.dp,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(
                album.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${album.displayArtist} · ${album.songCount} lagu",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                color = MutedInk
            )
        }
    }
}

@Composable
fun ArtistRow(name: String, songCount: Int, onClick: () -> Unit) {
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
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFFF744A), Color(0xFFB22C12)))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials(name),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFF8F2)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                if (songCount == 1) "1 lagu" else "$songCount lagu",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        }
    }
}

@Composable
fun FolderRow(item: FolderItem, onClick: () -> Unit) {
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
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Folder,
                contentDescription = null,
                tint = Coral,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                item.path.ifBlank { "Penyimpanan utama" } + " · ${item.songCount} lagu",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        }
    }
}

@Composable
fun BackBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedInk
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SimpleEmpty(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = FaintInk,
            textAlign = TextAlign.Center)
    }
}

private fun initials(name: String): String {
    val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> (words[0].first().toString() + words[1].first()).uppercase()
    }
}
