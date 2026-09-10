package com.zenn889.putar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * Bar navigasi bawah bergaya "kartu mengambang": pil membulat dengan bayangan,
 * tab terpilih melebar dan memunculkan namanya (ikon + label), tab lain ikon
 * saja supaya ruangnya lega.
 */
@Composable
fun PureWaveBottomBar(current: LibraryTab, onSelect: (LibraryTab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(top = 4.dp, bottom = 10.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.97f),
            shape = RoundedCornerShape(26.dp),
            shadowElevation = 20.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LibraryTab.entries.forEach { tab ->
                    val selected = tab == current
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Coral.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onSelect(tab) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            tab.icon(),
                            contentDescription = tab.label,
                            tint = if (selected) Coral else MutedInk,
                            modifier = Modifier.size(22.dp)
                        )
                        if (selected) {
                            Spacer(Modifier.width(5.dp))
                            Text(
                                tab.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Coral,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
