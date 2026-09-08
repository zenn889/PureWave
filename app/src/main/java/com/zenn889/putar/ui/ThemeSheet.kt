package com.zenn889.putar.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zenn889.putar.ui.theme.AccentChoice
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk
import com.zenn889.putar.ui.theme.ThemeMode
import com.zenn889.putar.ui.theme.accentFromPrefs
import com.zenn889.putar.ui.theme.themeModeFromPrefs
import com.zenn889.putar.ui.theme.themePrefs

fun themeSummary(context: android.content.Context): String {
    val mode = themeModeFromPrefs(context)
    val accent = accentFromPrefs(context)
    val dyn = context.getSharedPreferences("putar_prefs", android.content.Context.MODE_PRIVATE)
        .getBoolean("theme_dynamic", false)
    return if (dyn) "${mode.label} · Warna wallpaper"
    else "${mode.label} · Aksen ${accent.label}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = themePrefs(context)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(themeModeFromPrefs(context)) }
    var accent by remember { mutableStateOf(accentFromPrefs(context)) }
    var dynamic by remember {
        mutableStateOf(prefs.getBoolean("theme_dynamic", false))
    }

    fun persist() {
        prefs.edit()
            .putString("theme_mode", mode.key)
            .putString("theme_accent", accent.key)
            .putBoolean("theme_dynamic", dynamic)
            .apply()
        (context as? Activity)?.recreate()
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(
                "Tampilan & tema",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Perubahan langsung diterapkan",
                style = MaterialTheme.typography.bodySmall,
                color = MutedInk
            )

            Spacer(Modifier.height(14.dp))
            Text("Mode", fontWeight = FontWeight.SemiBold, color = MutedInk)
            ThemeMode.entries.forEach { m ->
                CheckRow(label = m.label, checked = mode == m && !(dynamic && m != ThemeMode.SYSTEM)) {
                    if (!dynamic || m == ThemeMode.SYSTEM) {
                        mode = m; persist()
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Warna wallpaper (Material You)", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Android 12+ · ikut warna wallpaper HP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedInk
                    )
                }
                Switch(
                    checked = dynamic,
                    onCheckedChange = {
                        dynamic = it
                        if (!it) mode = ThemeMode.DARK
                        persist()
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Coral)
                )
            }

            if (!dynamic) {
                Spacer(Modifier.height(10.dp))
                Text("Aksen", fontWeight = FontWeight.SemiBold, color = MutedInk)
                Row(Modifier.padding(vertical = 6.dp)) {
                    val swatches = mapOf(
                        AccentChoice.CORAL to Color(0xFFFF5A36),
                        AccentChoice.MINT to Color(0xFF1FBF8F),
                        AccentChoice.SKY to Color(0xFF3D8BFF),
                        AccentChoice.VIOLET to Color(0xFF8B5CF6),
                        AccentChoice.GOLD to Color(0xFFE8A33D)
                    )
                    AccentChoice.entries.forEach { a ->
                        val c = swatches[a] ?: Coral
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(end = 14.dp)
                                .clickable { accent = a; persist() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { accent = a; persist() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = if (accent == a && !dynamic) Color.White
                                    else Color.Transparent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                a.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (accent == a) Coral else FaintInk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onSelect)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = if (checked) Coral else Color.Transparent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (checked) FontWeight.Bold else FontWeight.Normal
        )
    }
}
