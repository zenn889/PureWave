package com.zenn889.putar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenn889.putar.data.MusicRepository
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.theme.MutedInk

/** Strip horizontal kartu lagu ber-artwork album — dipakai beberapa bagian. */
@Composable
fun TrackStrip(
    title: String,
    tracks: List<Track>,
    onPlay: (List<Track>, Int) -> Unit
) {
    if (tracks.isEmpty()) return
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
        ) {
            items(tracks, key = { it.contentUri.toString() }) { track ->
                val idx = tracks.indexOf(track)
                Column(
                    modifier = Modifier
                        .width(132.dp)
                        .padding(end = 10.dp)
                        .clickable { onPlay(tracks, idx) }
                ) {
                    AlbumArt(
                        uri = MusicRepository.albumArtUri(track.albumId),
                        size = 132.dp,
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        track.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
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
