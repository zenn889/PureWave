package com.zenn889.putar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenn889.putar.data.Track
import com.zenn889.putar.data.toArtUri
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk

/**
 * Judul seksi beranda: batang aksen kecil + judul tebal + jumlah di kanan.
 * Dipakai semua bagian beranda supaya ritmenya konsisten.
 */
@Composable
fun SectionHeader(
    title: String,
    count: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Coral)
        )
        Spacer(Modifier.width(9.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            ) {
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedInk,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/** Strip horizontal kartu lagu ber-artwork album — dipakai beberapa bagian beranda. */
@Composable
fun TrackStrip(
    title: String,
    tracks: List<Track>,
    onPlay: (List<Track>, Int) -> Unit
) {
    if (tracks.isEmpty()) return
    Column {
        SectionHeader(title, tracks.size)
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp)) {
            items(tracks, key = { it.contentUri }) { track ->
                val idx = tracks.indexOf(track)
                Column(
                    modifier = Modifier
                        .width(148.dp)
                        .padding(end = 12.dp)
                        .clickable { onPlay(tracks, idx) }
                ) {
                    Box {
                        AlbumArt(
                            uri = track.artUri.toArtUri(),
                            size = 148.dp,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.shadow(14.dp, RoundedCornerShape(18.dp), clip = false)
                        )
                        // tombol putar kecil menempel di sudut sampul
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(7.dp)
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Putar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    )
                    Text(
                        track.displayArtist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedInk
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

/**
 * Deret "Baru ditambahkan" ala beranda — satu kartu per album terbaru,
 * unik per album, maksimal 12.
 */
@Composable
fun RecentlyAddedRow(songs: List<Track>, onPlay: (List<Track>, Int) -> Unit) {
    val picks = remember(songs) {
        val seen = mutableSetOf<Long>()
        val out = ArrayList<Track>()
        for (t in songs.sortedByDescending { it.dateAddedMs }) {
            val a = t.albumId
            val key = if (a != null && a != 0L) a else (t.mediaId ?: -1L)
            if (seen.add(key)) out.add(t)
            if (out.size >= 12) break
        }
        out
    }
    if (picks.isEmpty()) return
    TrackStrip("Baru ditambahkan", picks, onPlay)
}

/** Satu petak pintasan di beranda. */
data class QuickEntry(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val count: Int? = null,
    val accent: Boolean = false
)

/**
 * Grid pintasan dua kolom ala beranda Spotify: tiap petak bisa ditekan dan
 * membawa ke bagian/aksi terkait. Dibuat dari Column+Row (bukan grid malas)
 * supaya aman dipakai di dalam LazyColumn beranda.
 */
@Composable
fun QuickAccessGrid(
    entries: List<QuickEntry>,
    onSelect: (QuickEntry) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
        entries.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                pair.forEach { entry ->
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = if (entry.accent) Coral.copy(alpha = 0.17f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(entry) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (entry.accent) Coral.copy(alpha = 0.26f)
                                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    entry.icon,
                                    contentDescription = entry.label,
                                    tint = if (entry.accent) Coral else MutedInk,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(Modifier.width(9.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (entry.count != null) {
                                    Text(
                                        "${entry.count} item",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = FaintInk
                                    )
                                }
                            }
                        }
                    }
                }
                // jaga lebar bila jumlah entri ganjil
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
