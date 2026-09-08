package com.zenn889.putar.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zenn889.putar.ui.theme.Coral
import com.zenn889.putar.ui.theme.FaintInk
import com.zenn889.putar.ui.theme.MutedInk

/* ============ sortir ============ */

enum class SortOption(val label: String) {
    JUDUL("Judul A-Z"),
    ARTIS("Artis"),
    ALBUM("Album"),
    TERBARU("Terbaru"),
    DURASI("Durasi")
}

@Composable
fun SortMenuButton(
    current: SortOption,
    onSelect: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Sort, contentDescription = "Urutkan", tint = MutedInk)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            SortOption.entries.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt.label, color = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                    trailingIcon = {
                        if (opt == current) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Coral)
                        }
                    }
                )
            }
        }
    }
}

/* ============ antrian ============ */

data class QueueEntry(
    val uri: String,
    val title: String,
    val artist: String,
    val artwork: Uri?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    entries: List<QueueEntry>,
    currentUri: String?,
    onPlay: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.QueueMusic, contentDescription = null, tint = Coral)
            Spacer(Modifier.width(10.dp))
            Text(
                "Antrian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${entries.size} lagu",
                style = MaterialTheme.typography.labelMedium,
                color = MutedInk
            )
        }
        if (entries.isEmpty()) {
            Text(
                "Antrian kosong.",
                color = MutedInk,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(Modifier.padding(top = 6.dp, bottom = 20.dp)) {
                itemsIndexed(entries) { index, entry ->
                    val isCurrent = entry.uri == currentUri
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPlay(index) }
                            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // gagang seret: geser vertikal untuk mengubah urutan
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .pointerInput(index, onMoveUp, onMoveDown) {
                                    var acc = 0f
                                    detectVerticalDragGestures(
                                        onDragEnd = { acc = 0f },
                                        onDragCancel = { acc = 0f },
                                        onVerticalDrag = { _, d ->
                                            acc += d
                                            while (acc <= -150f) {
                                                onMoveUp(index)
                                                acc += 150f
                                            }
                                            while (acc >= 150f) {
                                                onMoveDown(index)
                                                acc -= 150f
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.DragHandle,
                                contentDescription = "Ubah urutan",
                                tint = FaintInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        AlbumArt(entry.artwork, size = 42.dp, shape = RoundedCornerShape(10.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) Coral else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                entry.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedInk
                            )
                        }
                        IconButton(
                            onClick = { onMoveUp(index) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Naikkan", tint = FaintInk)
                        }
                        IconButton(
                            onClick = { onMoveDown(index) },
                            enabled = index < entries.size - 1
                        ) {
                            Icon(Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Turunkan", tint = FaintInk)
                        }
                        IconButton(onClick = { onRemove(index) }) {
                            Icon(Icons.Filled.Delete,
                                contentDescription = "Hapus dari antrian", tint = FaintInk)
                        }
                    }
                }
            }
        }
    }
}
