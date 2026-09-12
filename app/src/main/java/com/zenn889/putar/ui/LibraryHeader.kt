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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.Elev
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.Radius
import com.zenn889.putar.ui.theme.Space
import com.zenn889.putar.ui.theme.SurfaceHigh

@Composable
internal fun LibraryHeader(
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
    onSortChange: (SortOption) -> Unit,
    /**
     * Sapaan di kepala pustaka. Bisa disuntik supaya screenshot test
     * deterministik — nilai aslinya bergantung jam (pagi/siang/sore/malam),
     * sehingga gambar acuan akan selalu berbeda saat CI berjalan di jam lain.
     */
    greeting: String = greetingLine()
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = Space.lg).padding(top = 10.dp, bottom = Space.xs)) {
        // --- identitas aplikasi + sapaan ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(Elev.raised, RoundedCornerShape(Radius.md), clip = false)
                    .clip(RoundedCornerShape(Radius.md))
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
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(
                    greeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Ink
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

        Spacer(Modifier.height(Space.md))

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
            shape = RoundedCornerShape(Radius.pill),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                cursorColor = Coral,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Space.md))

        // --- chip statistik (sekaligus pintasan pindah tab) ---
        LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            item { StatChip("Lagu", rootSongs, tab == LibraryTab.LAGU) { onTabSelect(LibraryTab.LAGU) } }
            item { StatChip("Album", rootAlbums, tab == LibraryTab.ALBUM) { onTabSelect(LibraryTab.ALBUM) } }
            item { StatChip("Artis", rootArtists, tab == LibraryTab.ARTIS) { onTabSelect(LibraryTab.ARTIS) } }
            item { StatChip("Folder", rootFolders, tab == LibraryTab.FOLDER) { onTabSelect(LibraryTab.FOLDER) } }
            item { StatChip("Video", rootVideos, tab == LibraryTab.VIDEO) { onTabSelect(LibraryTab.VIDEO) } }
            item { StatChip("Favorit", rootFavs, tab == LibraryTab.FAVORIT) { onTabSelect(LibraryTab.FAVORIT) } }
            if (rootSongs > 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(Radius.pill),
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

@Composable
private fun StatChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(Radius.pill),
        color = if (selected) Coral.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Space.lg, vertical = 9.dp),
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

private fun greetingLine(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 4..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..17 -> "Selamat sore"
        else -> "Selamat malam"
    }
}
