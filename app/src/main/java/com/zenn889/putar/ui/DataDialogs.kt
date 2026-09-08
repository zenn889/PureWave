package com.zenn889.putar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.MutedInk

/** Aturan penyembunyian di pustaka. */
@Composable
fun LibraryFilterDialog(
    hideShort: Boolean,
    hideSystemDirs: Boolean,
    hideDups: Boolean,
    onToggle: (key: String, value: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Filter pustaka", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                FilterSwitchRow(
                    title = "Sembunyikan audio pendek",
                    subtitle = "Lagu < 30 detik (sering efek suara)",
                    checked = hideShort,
                    onCheck = { onToggle("short", it) }
                )
                FilterSwitchRow(
                    title = "Sembunyikan nada sistem",
                    subtitle = "Folder Ringtones / Notifications / Alarms",
                    checked = hideSystemDirs,
                    onCheck = { onToggle("sysdir", it) }
                )
                FilterSwitchRow(
                    title = "Sembunyikan duplikat",
                    subtitle = "Judul + artis sama, sisakan satu",
                    checked = hideDups,
                    onCheck = { onToggle("dup", it) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Selesai", color = Coral) }
        }
    )
}

@Composable
private fun FilterSwitchRow(
    title: String, subtitle: String, checked: Boolean, onCheck: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedInk)
        }
        Spacer(Modifier.padding(4.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheck,
            colors = SwitchDefaults.colors(checkedTrackColor = Coral)
        )
    }
}

/** Ringkasan statistik mendengar. */
@Composable
fun StatsDialog(
    totalMinutes: Long,
    rows: List<String>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Statistik mendengar", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (totalMinutes < 60) "Total $totalMinutes menit didengar"
                    else "Total ${totalMinutes / 60} jam ${totalMinutes % 60} mnt didengar",
                    fontWeight = FontWeight.Bold,
                    color = Coral
                )
                Spacer(Modifier.height(10.dp))
                if (rows.isEmpty()) {
                    Text(
                        "Belum ada data — putar beberapa lagu dulu ya.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedInk
                    )
                } else {
                    Text("Paling sering diputar:", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    rows.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedInk,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = Coral) }
        }
    )
}
