package com.zenn889.putar.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.CoralBright
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink

private fun fmtDurLabel(msTotal: Long): String {
    val totalMin = msTotal / 60_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0L && m > 0L -> "$h jam $m mnt"
        h > 0L -> "$h jam"
        m > 0L -> "$m mnt"
        else -> "<1 mnt"
    }
}

private fun fmtCount(n: Int): String =
    n.toString().reversed().chunked(3).joinToString(".").reversed()

/** Kartu gradien penyambut di atas daftar Lagu & Favorit. */
@Composable
fun HeroCard(songs: List<Track>, onShuffleAll: () -> Unit) {
    val totalMs = remember(songs) { songs.sumOf { it.durationMs } }
    val albumCount = remember(songs) { songs.mapNotNull { it.albumId }.distinct().size }
    val artistCount = remember(songs) { songs.map { it.displayArtist }.distinct().size }
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(
                Brush.linearGradient(
                    if (light) listOf(Color(0xFFFDF1EA), Color(0xFFF8E1D4), Color(0xFFF3D3C3))
                    else listOf(Color(0xFF2B2126), Color(0xFF231B1F), Color(0xFF191316))
                ),
                RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onShuffleAll)
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .align(Alignment.TopEnd)
                .background(
                    Brush.radialGradient(
                        listOf(Coral.copy(alpha = 0.18f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "PUSTAKA KAMU",
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = CoralBright
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        fmtCount(songs.size),
                        fontSize = 38.sp,
                        lineHeight = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Text(
                        "lagu • ${fmtDurLabel(totalMs)}",
                        fontSize = 13.sp,
                        color = if (light) Color(0xFF8A4A2E) else Color(0xFFFFD9CB)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "$albumCount album • $artistCount artis",
                        fontSize = 11.sp,
                        color = if (light) Color(0xFFA2664A) else Color(0xFFC9A392)
                    )
                }
                Spacer(Modifier.size(10.dp))
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Coral, CircleShape)
                        .clickable(onClick = onShuffleAll),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Shuffle,
                        contentDescription = "Acak semua",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Ketuk untuk memutar acak seluruh pustaka",
                fontSize = 11.sp,
                color = FaintInk
            )
        }
    }
}
