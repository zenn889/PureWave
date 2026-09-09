package com.zenn889.putar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.MutedInk

private fun LibraryTab.icon(): ImageVector = when (this) {
    LibraryTab.LAGU -> Icons.Filled.MusicNote
    LibraryTab.ALBUM -> Icons.Filled.Album
    LibraryTab.ARTIS -> Icons.Filled.People
    LibraryTab.FOLDER -> Icons.Filled.Folder
    LibraryTab.VIDEO -> Icons.Filled.Movie
    LibraryTab.FAVORIT -> Icons.Filled.Favorite
}

/** Bar navigasi bawah ala aplikasi musik — ikon sesuai nama tab. */
@Composable
fun PureWaveBottomBar(current: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LibraryTab.entries.forEach { tab ->
                val selected = tab == current
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // indikator kecil
                    Box(
                        modifier = Modifier
                            .size(width = 16.dp, height = 3.dp)
                            .background(
                                if (selected) Coral else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(Modifier.height(3.dp))
                    Icon(
                        tab.icon(),
                        contentDescription = tab.label,
                        tint = if (selected) Coral else MutedInk,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Coral else MutedInk,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
