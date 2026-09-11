package com.zenn889.putar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.zenn889.putar.data.Album
import com.zenn889.putar.data.FolderItem
import com.zenn889.putar.data.Track
import com.zenn889.putar.ui.theme.PutarTheme
import org.junit.Rule
import org.junit.Test

/**
 * Screenshot test tampilan (Paparazzi, JVM tanpa emulator).
 *
 * Guna utamanya: menangkap "teks tak terlihat" — masalah di v2.19.0–v2.19.2
 * yang berawal dari `MaterialTheme` tidak menetapkan warna teks bawaan
 * (lihat docs/ARCHITECTURE.md bagian 4h). Setiap permukaan dirender dengan
 * tema gelap default dan disimpan sebagai gambar acuan; kalau suatu perubahan
 * membuat teks menyatu dengan latar, `verifyPaparazziDebug` akan menggagalkan
 * build. Baseline dibuat dengan `./gradlew :app:recordPaparazziDebug`.
 */
class VisualScreenshotsTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        showSystemUi = false
    )

    private val track = Track(
        mediaId = 1L,
        contentUri = "content://media/external/audio/media/1",
        title = "Sempurna",
        artist = "Andra and The Backbone",
        durationMs = 243_000L,
        albumId = null
    )

    private fun snap(content: @Composable () -> Unit) {
        paparazzi.snapshot {
            PutarTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }

    @Test fun daftarLagu_biasa() = snap { TrackRow(track, isCurrent = false, onClick = {}) }

    @Test fun daftarLagu_diputar() = snap { TrackRow(track, isCurrent = true, onClick = {}) }

    @Test fun daftarArtis() = snap { ArtistRow("Andra and The Backbone", 12, {}) }

    @Test fun daftarFolder() = snap { FolderRow(FolderItem("Download", "Download", "Download", 30), {}) }

    @Test fun kartuAlbum() = snap { AlbumCard(Album(0L, "Album Contoh", "Artis", 8), {}) }

    @Test fun miniPlayer() = snap {
        MiniPlayer(
            mirror = PlayerMirror(
                title = "Sempurna",
                artist = "Andra and The Backbone",
                artwork = null,
                durationMs = 243_000L,
                positionMs = 61_000L,
                playing = true,
                hasMedia = true
            ),
            progress = ProgressState(),
            onClick = {},
            onPlayPause = {},
            onNext = {}
        )
    }

    @Test fun barNavigasi() = snap { PureWaveBottomBar(LibraryTab.LAGU, {}) }

    @Test fun kartuSambutan() = snap { HeroCard(listOf(track, track, track), {}) }

    @Test fun headerPustaka() = snap {
        LibraryHeader(
            tab = LibraryTab.LAGU,
            totalSongs = 128,
            rootSongs = 128,
            rootAlbums = 12,
            rootArtists = 40,
            rootFolders = 5,
            rootVideos = 3,
            rootFavs = 7,
            query = "",
            onQueryChange = {},
            onOpenSettings = {},
            onPlayAllShuffled = {},
            onTabSelect = {},
            showSort = true,
            sortChoice = SortOption.entries.first(),
            onSortChange = {}
        )
    }

    @Test fun layarPustakaKosong() = snap { EmptyLibraryScreen() }
}
