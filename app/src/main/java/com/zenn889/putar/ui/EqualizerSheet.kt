package com.zenn889.putar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenn889.putar.AudioFx
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.SurfaceHigh

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var enabled by remember { mutableStateOf(AudioFx.isEnabled(context)) }
    var preset by remember { mutableStateOf(AudioFx.preset(context)) }
    var bands by remember { mutableStateOf(IntArray(AudioFx.BAND_COUNT) { AudioFx.bandLevel(context, it) }) }
    var bassLevel by remember { mutableIntStateOf(AudioFx.bassLevel(context)) }

    val attached = AudioFx.currentSession() > 0
    val failed = attached && !AudioFx.available

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Equalizer, contentDescription = null, tint = Coral)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Equalizer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Tutup", tint = MutedInk)
                }
            }

            if (failed) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Equalizer tidak didukung perangkat/keluaran audio ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Aktifkan",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        AudioFx.setEnabled(context, it)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Coral)
                )
            }

            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AudioFx.PRESET_NAMES) { name ->
                    FilterChip(
                        selected = preset == name,
                        onClick = {
                            preset = name
                            AudioFx.setPreset(context, name)
                            bands = IntArray(AudioFx.BAND_COUNT) { AudioFx.bandLevel(context, it) }
                        },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Coral,
                            selectedLabelColor = Color(0xFF190902)
                        )
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            AudioFx.BAND_LABELS.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedInk,
                        modifier = Modifier.width(64.dp)
                    )
                    val v = bands.getOrElse(i) { 0 }
                    Slider(
                        value = (v / 100f).coerceIn(-15f, 15f),
                        onValueChange = { newDb ->
                            val nb = bands.copyOf()
                            nb[i] = (newDb * 100f).toInt()
                            bands = nb
                            preset = "Kustom"
                            AudioFx.setBand(context, i, nb[i])
                        },
                        valueRange = -15f..15f,
                        colors = SliderDefaults.colors(thumbColor = Coral, activeTrackColor = Coral),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (v == 0) "0" else "%+d dB".format(v / 100),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (v == 0) FaintInk else Coral,
                        modifier = Modifier.width(52.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Bass Boost",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Slider(
                    value = (bassLevel / 100f).coerceIn(0f, 10f),
                    onValueChange = {
                        bassLevel = (it * 100f).toInt()
                        AudioFx.setBass(context, bassLevel)
                    },
                    valueRange = 0f..10f,
                    colors = SliderDefaults.colors(thumbColor = Coral, activeTrackColor = Coral),
                    modifier = Modifier.width(180.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = {
                    AudioFx.reset(context)
                    enabled = AudioFx.isEnabled(context)
                    preset = AudioFx.preset(context)
                    bands = IntArray(AudioFx.BAND_COUNT) { AudioFx.bandLevel(context, it) }
                    bassLevel = AudioFx.bassLevel(context)
                }) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = "Reset equalizer",
                        tint = MutedInk
                    )
                }
            }

            if (!attached) {
                Text(
                    "Setelan tersimpan — efek menempel otomatis saat lagu diputar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = FaintInk,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
