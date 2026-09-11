package com.zenn889.putar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.MutedInk

@Composable
internal fun SleepTimerDialog(
    active: Boolean,
    endOfTrackActive: Boolean,
    songsActive: Boolean,
    onCancel: () -> Unit,
    onEndOfTrack: () -> Unit,
    onPickSongs: (Int) -> Unit,
    onPickMinutes: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Sleep timer", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (active) {
                    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                        Text("Matikan sleep timer", color = Coral, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onEndOfTrack, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Setelah lagu ini selesai",
                        modifier = Modifier.fillMaxWidth(),
                        color = if (endOfTrackActive) Coral else MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (endOfTrackActive) FontWeight.Bold else FontWeight.Normal
                    )
                }
                listOf(2, 3, 5, 10).forEach { n ->
                    TextButton(
                        onClick = { onPickSongs(n) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (n == 2) "2 lagu berikutnya" else "$n lagu berikutnya",
                            modifier = Modifier.fillMaxWidth(),
                            color = if (songsActive) Coral else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (songsActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
                listOf(10, 15, 30, 45, 60, 90).forEach { minutes ->
                    TextButton(
                        onClick = { onPickMinutes(minutes) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (minutes < 60) "$minutes menit" else "1 jam ${minutes - 60} menit".trimEnd(),
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = MutedInk) }
        }
    )
}
