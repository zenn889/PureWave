package com.zenn889.putar.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenn889.putar.AudioFx
import com.zenn889.putar.versionName
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onEqualizer: () -> Unit,
    onSleep: () -> Unit,
    onPlaylists: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = 26.dp)
        ) {
            Text(
                "Setelan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(6.dp))

            val fxOk = AudioFx.available && AudioFx.currentSession() > 0
            SettingsRow(
                icon = Icons.Filled.Equalizer,
                title = "Equalizer & Bass",
                subtitle = if (fxOk) "Aktif — terpasang di sesi audio"
                else "5 pita, preset, bass boost",
                onClick = onEqualizer
            )
            SettingsRow(
                icon = Icons.Filled.PlaylistPlay,
                title = "Playlist",
                subtitle = "Buat & kelola daftar putar",
                onClick = onPlaylists
            )
            SettingsRow(
                icon = Icons.Filled.Timer,
                title = "Sleep timer",
                subtitle = "Musik berhenti otomatis (10–90 menit)",
                onClick = onSleep
            )
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Tentang putar",
                subtitle = "Versi, info offline & hi-res",
                onClick = { showAbout = true }
            )

            Spacer(Modifier.height(14.dp))
            Text(
                "Ada masalah? Lampirkan isi file ini saat melapor:",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
            Text(
                "Android/data/com.zenn889.putar/files/eq.log" +
                    " dan crash.txt",
                style = MaterialTheme.typography.bodySmall,
                color = FaintInk,
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
    }

    if (showAbout) {
        val version = context.versionName()
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("putar $version", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Pemutar musik offline dari penyimpanan HP — tanpa internet, " +
                            "tanpa akun, tanpa iklan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Dibuat dengan Kotlin, Jetpack Compose, dan Media3 (ExoPlayer).\n\n" +
                            "Hi-res: FLAC/WAV ikut diputar sesuai kemampuan DAC HP " +
                            "(bukan bit-perfect; DSD tidak didukung).\n\n" +
                            "Rilis & sumber: github.com/zenn889/music-player",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) { Text("Tutup", color = Coral) }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = Coral.copy(alpha = 0.12f)) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Coral, modifier = Modifier.size(21.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )
        }
    }
}
