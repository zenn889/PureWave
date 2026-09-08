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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zenn889.putar.AudioFx
import com.zenn889.putar.versionName
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.CoralBright
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.Ink
import com.zenn889.putar.ui.theme.MutedInk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onEqualizer: () -> Unit,
    onSleep: () -> Unit,
    onPlaylists: () -> Unit,
    onOpenFilters: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onStats: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAbout by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }

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
                icon = Icons.Filled.Palette,
                title = "Tampilan & tema",
                subtitle = themeSummary(context),
                onClick = { showTheme = true }
            )
            SettingsRow(
                icon = Icons.Filled.FilterAlt,
                title = "Filter pustaka",
                subtitle = "Pendek, nada dering, duplikat",
                onClick = onOpenFilters
            )
            SettingsRow(
                icon = Icons.Filled.Assessment,
                title = "Statistik mendengar",
                subtitle = "Jumlah putar & menit didengar",
                onClick = onStats
            )
            SettingsRow(
                icon = Icons.Filled.Backup,
                title = "Cadangkan data",
                subtitle = "Favorit & playlist → file JSON",
                onClick = onBackup
            )
            SettingsRow(
                icon = Icons.Filled.Restore,
                title = "Pulihkan data",
                subtitle = "Ambil dari file cadangan",
                onClick = onRestore
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
                title = "Info aplikasi",
                subtitle = "PureWave — versi, detail & lisensi",
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

    if (showTheme) {
        ThemeSheet(onDismiss = { showTheme = false })
    }

    if (showAbout) {
        val version = context.versionName()
        val buildCode = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrNull() ?: 0
        val details = listOf(
            "Pemutar musik offline dari penyimpanan HP-mu sendiri.",
            "Tanpa izin internet · tanpa akun · tanpa iklan.",
            "Favorit & playlist tersimpan 100% di perangkat.",
            "Mesin Media3 (ExoPlayer) + kontrol di notifikasi & lock screen.",
            "Format: MP3 · FLAC · WAV · AAC · OGG · M4A, dll.",
            "Hi-res FLAC/WAV diputar sesuai DAC HP (resample, bukan bit-perfect)."
        )
        AlertDialog(
            onDismissRequest = { showAbout = false },
            containerColor = MaterialTheme.colorScheme.surface,
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                Brush.linearGradient(listOf(Coral, CoralBright)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "PureWave",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )
                    Text(
                        "Pemutar musik offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "v$version · build $buildCode",
                        style = MaterialTheme.typography.labelMedium,
                        color = CoralBright,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(14.dp))
                    details.forEach { line ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                "•",
                                color = Coral,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Sumber & rilis: github.com/zenn889/music-player",
                        style = MaterialTheme.typography.bodySmall,
                        color = FaintInk
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
