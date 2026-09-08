package com.zenn889.putar.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.CoralBright
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk

/** Layar sambutan sekali jalan untuk pemakai baru. */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val features = listOf(
        Triple(Icons.Filled.LibraryMusic, "Pustaka HP-mu", "Lagu, Album, Artis & Folder"),
        Triple(Icons.Filled.Favorite, "Favorit & Playlist", "Koleksi pribadi yang tersimpan"),
        Triple(Icons.Filled.QueueMusic, "Antrian cerdas", "Atur urutan, sortir, kecepatan putar"),
        Triple(Icons.Filled.Equalizer, "Suara sesuai selera", "EQ 5 pita + Bass Boost bawaan"),
        Triple(Icons.Filled.OfflinePin, "100% offline", "Tanpa internet, tanpa iklan")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0B0C0E), Color(0xFF191009)))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // lingkaran logo bercahaya
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Coral.copy(alpha = 0.28f), Color.Transparent)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Coral, CoralBright))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "putar",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )
            Text(
                "Pemutar musik offline untuk HP kamu",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk
            )

            Spacer(Modifier.height(30.dp))
            features.forEach { (icon, title, sub) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Coral,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(
                            title,
                            color = Ink,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            sub,
                            color = FaintInk,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Coral)
            ) {
                Text(
                    "Mulai mendengarkan",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "putar v2.7.0 — tanpa internet, musik tetap jalan",
                color = FaintInk,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
