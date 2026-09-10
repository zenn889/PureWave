# PureWave — Dasar Teknis & Buku Resep

Dokumen ini untuk siapa pun (termasuk AI agent di sesi berikutnya) yang mau
mengubah tema, menambah fitur, atau merilis APK. Ditulis dalam Bahasa
Indonesia, singkat tapi lengkap.

---

## 1. Fundamental (kenapa strukturnya begini)

- **APK** = paket instal Android. Isinya: kode (DEX), resource (res/), dan
  manifest. Ditandatangani (signing) supaya bisa di-install & bisa update.
  Tanda tangan berbeda = app dianggap berbeda (harus uninstall dulu).
- **Activity** = satu layar masuk. Kita hanya punya satu:
  `MainActivity` (single-activity). Semua layar lain = Compose (bukan
  Activity baru).
- **Jetpack Compose** = UI ditulis sebagai fungsi Kotlin (`@Composable`).
  "Recomposition" = fungsi dijalankan ulang saat state berubah. Ini alasan
  penting: state yang berubah terlalu sering di root akan membuat seluruh
  layar ikut dihitung ulang → lag. Karena itu posisi lagu ditaruh di
  `ProgressState` (hanya dibaca mini player & layar pemutar).
- **Gradle** = build system. `./gradlew :app:assembleDebug|assembleRelease`.
  Versi app ada di `app/build.gradle.kts` (`versionCode`, `versionName`).
- **AndroidManifest.xml** = daftar izin, activity, service, receiver widget,
  intent-filter ("buka dengan"), dan tema awal (splash).
- **Media3/ExoPlayer** = mesin pemutar + `MediaSessionService`
  (`PlaybackService`) yang memberi kontrol notifikasi/lock screen dan tetap
  jalan saat app di latar belakang. UI mengendalikan lewat `MediaController`.
- **Penyimpanan data**: semua data pribadi (favorit, playlist, statistik,
  sesi terakhir, posisi video, setelan tema) disimpan di
  `SharedPreferences` dengan isi JSON. Tidak ada database & tidak ada
  internet sama sekali (tanpa izin INTERNET — bukti offline murni).

---

## 2. Peta kode

Root: `app/src/main/java/com/zenn889/putar/`

Inti aplikasi:
- `MainActivity.kt` (~1.500 baris) — jantung app: izin, pemindaian pustaka,
  state (tab, pencarian, pemutar, sheet), dan seluruh perakitan layar.
  Function utamanya `PlayerApp()`.
- `PlaybackService.kt` — service Media3: player ExoPlayer, audio focus,
  pause saat headset dicabut, tempel EQ ke sesi audio, dorong info ke widget.
- `AudioFx.kt` — Equalizer & BassBoost native (`android.media.audiofx`).
- `PlayerWidget.kt` — widget home screen (RemoteViews) + snapshot lagu.

Data (`data/`):
- `Track.kt` / `VideoItem.kt` — model lagu & video (uri, judul, artist,
  durasi, album, folder, `filePath`, tanggal tambah).
- `MusicRepository.kt` — query MediaStore: `loadLibrary()`, `loadAlbums()`,
  `loadVideos()`, `albumArtUri()`.
- `FavStore.kt`, `PlaylistStore.kt`, `SessionStore.kt` (auto-resume),
  `StatsStore.kt` (jumlah putar & menit), `VideoPosStore.kt` (resume video),
  `Lyrics.kt` (baca .lrc & cari file subtitle), `BackupStore.kt` (ekspor/
  impor JSON favorit+playlist).

UI (`ui/`):
- `theme/Theme.kt` — **semua warna & tipografi** (lihat bagian 3).
- `PlayerUi.kt` — baris lagu, `MiniPlayer`, `NowPlayingSheet` (layar pemutar
  penuh + ambience), `LibraryList`, `AlbumArt`, `ProgressState`, `fmtMs`.
- `LibraryScreens.kt` — enum `LibraryTab`, `LibraryTabBar` (versi lama),
  `AlbumRow/AlbumCard/ArtistRow/FolderRow`, util pengelompokan.
- `BottomTabs.kt` — navigasi bawah berikon (6 tab).
- `HomeSections.kt` — strip horizontal (Baru ditambahkan, Baru diputar, Paling sering).
- `HeroCard.kt` — kartu ringkasan pustaka.
- `QueueSort.kt` — menu sortir + `QueueSheet` (antrian: seret untuk urutkan).
- `TrackSheets.kt` — sheet aksi tekan-lama (putar berikutnya, playlist, favorit).
- `SettingsSheet.kt` + `ThemeSheet.kt` + `DataDialogs.kt` — Setelan, Tema,
  Filter pustaka & Statistik.
- `EqualizerSheet.kt` — UI EQ.
- `VideoScreens.kt` — `VideoRow` (thumbnail) & `VideoPlayerScreen`
  (fullscreen + subtitle + PiP + resume; objek `VideoPlayback` untuk PiP).
- `LyricsSheet.kt` — lirik bergulir.
- `WelcomeScreen.kt` — onboarding sekali jalan.

Resource penting (`app/src/main/res/`):
- `values/themes.xml` — tema splash + warna window/status bar (harus
  disamakan dengan `Bg` di Theme.kt).
- `drawable/ic_launcher_foreground.xml` — mark logo (batang EQ + gelombang);
  `drawable/ic_launcher_background.xml` — gradien coral.
- `mipmap-*/ic_launcher*.png` — ikon untuk Android 7 (dibuat via Pillow).
- `layout/widget_player.xml`, `xml/player_widget_info.xml` — widget.

---

## 3. Sistem tema (cara paling sering diubah)

Kunci: `ui/theme/Theme.kt`.

- Token warna yang dipakai seluruh app: `Coral`, `CoralBright`, `Ink`,
  `MutedInk`, `FaintInk`, `Bg`, `Surface`, `SurfaceHigh`. Semuanya
  `val ... get() = Palette.xxx` — artinya **nilainya dibaca ulang tiap
  komposisi** dari objek `Palette` (var). Itulah kenapa mengganti tema bisa
  berlaku ke seluruh app tanpa mengubah file lain.
- Pilihan tema: `ThemeMode` (SYSTEM / LIGHT / DARK / OLED) dan
  `AccentChoice` (CORAL / MINT / SKY / VIOLET / GOLD), plus opsi Material You
  (warna wallpaper, Android 12+) lewat `theme_dynamic = true`.
- Disimpan di `SharedPreferences("putar_prefs")`; UI pemilihnya
  `ui/ThemeSheet.kt`; setelah memilih, activity **di-recreate**
  (`Activity.recreate()`) supaya semua komposisi membaca palet baru.
- `PutarTheme()` juga menyetel ikon status/nav bar terang/gelap mengikuti
  tema app. Tipografi kustom ada di `PureWaveTypography`.

Cara mengubah:
1. **Warna dasar (mis. coral lebih tua):** ubah nilai heksa di
   `accentSet()` / `applyPalette()` di `Theme.kt`. Karena token dibaca
   dinamis, tidak perlu menyentuh layar.
2. **Tambah aksen baru:** tambah entri di `enum AccentChoice`, tambah warna
   di `accentSet()`, dan tambah swatch di `ThemeSheet.kt`.
3. **Ubah warna splash (layar pembuka):** `values/themes.xml`
   (`windowSplashScreenBackground`).
4. **Ubah latar di HP lama:** `Bg` di `Theme.kt` DAN tiga warna di
   `values/themes.xml` harus sama, biar tidak "kedip" beda warna.

---

## 4. Resep menambah fitur

### 4a. Fitur yang menyimpan data (pola standar)
Buat `data/XxxStore.kt` meniru `FavStore.kt`:
- `prefs(c) = c.getSharedPreferences("putar_xxx", MODE_PRIVATE)`
- simpan sebagai JSON string, API: `load()`, `save()`, `toggle()`/`add()`.
- Panggil dari `MainActivity` di dalam `LaunchedEffect` atau handler UI.
Ingat: baca/tulis harus kecil (JSON sekian ratus entri aman). Untuk data
besar (statistik) tulis ditahan ~20 detik sekali.

### 4b. Tab baru di navigasi bawah
1. `ui/LibraryScreens.kt`: tambah entri `enum LibraryTab` (+ label).
2. `ui/BottomTabs.kt`: tambah ikon di `LibraryTab.icon()`.
3. `MainActivity.kt`: di `when (tab)` (bagian konten) tambah cabang baru,
   mis. `LibraryTab.X -> LazyColumn(...) { items(...) { ... } }`.
4. Kalau perlu hitungan di header: tambahkan parameter di `LibraryHeader`
   dan cabang di `val info = when (tab)`.
5. Kalau perlu data baru: tambah query di `MusicRepository`.

### 4c. Fitur setelan / dialog baru
1. Buat composable baru di `ui/` (contoh paling gampang: `DataDialogs.kt`).
2. Tambah baris di `SettingsSheet.kt` (`SettingsRow(ikon, judul, subjudul, onClick)`).
3. Di `MainActivity`: state `var showX by remember { mutableStateOf(false) }`,
   passing `onX = { showSettings = false; showX = true }`, lalu render
   `if (showX) XDialog(...)`.
4. Kalau setelannya perlu persist: simpan ke `putar_prefs` dan baca lagi
   saat state diinisialisasi.

### 4d. Fitur yang berkaitan dengan pemutaran
- Semua kontrol lewat `controller: MediaController?` di `MainActivity`
  (`controller?.seekTo()`, `.setPlaybackSpeed()`, `.addMediaItem()`, dst).
- Info yang ditampilkan UI berasal dari `mirror: PlayerMirror` (dibaca dari
  event player) dan posisi dari `ProgressState` (detak 400 ms).
- Menambah kontrol di layar pemutar: tambah parameter callback di
  `NowPlayingSheet` (PlayerUi.kt) lalu tombol `IconButton` di baris aksi;
  sambungkan di pemanggilan `NowPlayingSheet(...)` di `MainActivity`.

### 4e. Fitur video
`VideoScreens.kt` — `VideoPlayerScreen(queue, startIndex, onBack)` memakai
ExoPlayer lokal + `PlayerView` (useController=false) dan overlay kontrol
buatan sendiri. Aturan lapisan (penting): video → penangkap ketukan →
bar atas → tengah → kontrol bawah, supaya tombol selalu bisa ditekan.
Subtitle: `LyricsLoader.subtitleFile()` lalu `MediaItem.SubtitleConfiguration`.
Resume: `VideoPosStore`. PiP: tombol + `onUserLeaveHint()` di MainActivity,
dan pemutar harus berada di window Activity (bukan Dialog) — sekarang sudah.

### 4f. Ikon / logo
- Adaptive icon (Android 8+): `res/drawable/ic_launcher_foreground.xml`
  (vector 108x108, mark saja) + `ic_launcher_background.xml` (gradien).
- Android 7: PNG `mipmap-{mdpi..xxxhdpi}/ic_launcher*.png` — dibuat ulang
  dengan script Pillow (lihat riwayat: uv run --with pillow python3 ...).
- Splash: `res/drawable/ic_stat_music.xml`.

---

## 5. Rilis APK (langkah lengkap)

```bash
cd ~/putar-native
# 1) naikkan versi di app/build.gradle.kts (versionCode = versionCode+1)
#    versionCode berdiri sendiri; versionName untuk manusia (mis. 2.11.1)

export JAVA_HOME=/home/hermes/jdk
export ANDROID_HOME=/home/hermes/android-sdk
export PATH=$JAVA_HOME/bin:$PATH

# 2) build release (R8 + shrink aktif, ±3-4 menit)
./gradlew :app:assembleRelease --no-daemon

# 3) verifikasi
APK=app/build/outputs/apk/release/app-release.apk
/home/hermes/android-sdk/build-tools/36.0.0/apksigner verify $APK
/home/hermes/android-sdk/build-tools/36.0.0/aapt2 dump badging $APK | grep -E "^package"

# 4) artefak
cp $APK dist/purewave-vX.Y.Z.apk
sha256sum dist/purewave-vX.Y.Z.apk | tee dist/purewave-vX.Y.Z.apk.sha256

# 5) kirim
git add -A && git commit -m "vX.Y.Z: ..." && git push origin main
~/.local/bin/gh release create vX.Y.Z --repo zenn889/PureWave \
  --title "PureWave vX.Y.Z — ..." --notes "..." \
  dist/purewave-vX.Y.Z.apk dist/purewave-vX.Y.Z.apk.sha256
```

Catatan penting:
- **Keystore**: `~/keystores/putar-release.jks` (alias `putar`, PKCS12).
  Password di `~/.gradle/gradle.properties` (`putarStoreFile/Pass/KeyAlias/
  KeyPass`). Jangan di-commit. Password store & key HARUS sama untuk PKCS12,
  kalau beda → error "Given final block not properly padded".
- Tanda tangan tetap sama sejak v2.7.0 → user bisa update tanpa hapus data.
- `versionCode` harus selalu naik, kalau tidak akan ditolak saat update.

---

## 6. Jebakan yang sudah pernah kena (hindari!)

1. **Forward reference Kotlin**: variabel lokal (`var x by remember`) harus
   dideklarasikan SEBELUM dipakai di fungsi lokal/lambda lain. Ini beberapa
   kali membuat gagal kompilasi (contoh: `nextStack`, `progressState`,
   `videoQueue`).
2. **Kurung kurawal Compose**: patch besar mudah menggeser `{` `}` → error
   "Expecting '}'" atau "Modifier 'private' is not applicable to 'local
   function'". Cara cek cepat: hitung selisih `{` dan `}` per file dengan
   Python; kalau akhirnya bukan 0, ada yang kurang/lewat.
3. **Inset dialog ≠ inset activity**: ukuran navigation bar tidak dikirim ke
   window `Dialog` (padding jadi 0). Untuk kontrol video kita akhirnya
   memindahkan pemutar ke window Activity dan memakai
   `Modifier.navigationBarsPadding()`.
4. **`MaterialTheme.colorScheme.surfaceHigh` tidak ada** di M3 — pakai
   `surfaceVariant`.
5. **Ikon extended**: sebagian ikon hanya ada di `material-icons-extended`
   (GraphicEq, Lyrics, Subtitles, PiP, DragHandle, dll). Kalau "unresolved",
   cek dependensi itu dulu.
6. **R8 + shrinkResources**: nama resource di APK bisa berubah hash
   (`res/BW.xml`) — normal. Cari resource dengan `unzip -l`, bukan nama asli.
7. **DATA (path file) deprecated** tapi masih diperlukan untuk mencari .lrc
   dan subtitle → dipakai dengan `@Suppress("DEPRECATION")`.
8. **MediaStore**: video butuh izin `READ_MEDIA_VIDEO` (Android 13+),
   audio `READ_MEDIA_AUDIO`; di bawah 13 cukup `READ_EXTERNAL_STORAGE`.
9. **Performa**: jangan taruh state yang berdetak (posisi lagu) di root
   komposisi — pakai `ProgressState`. Jangan lupa `key` unik pada `items()`
   agar daftar tidak dihitung ulang.
10. **Bug crash user**: app menulis
    `Android/data/com.zenn889.putar/files/crash.txt` dan `eq.log` — minta
    user mengirim isinya.

---

## 7. Membangun ulang dari nol (kalau perlu)

```bash
# toolchain (tanpa sudo)
export JAVA_HOME=/home/hermes/jdk          # Temurin 21
export ANDROID_HOME=/home/hermes/android-sdk
# SDK: cmdline-tools/latest, platforms;android-36, build-tools;36.0.0,
#      platform-tools  → semua diunduh via sdkmanager
./gradlew :app:assembleDebug --no-daemon
```

Tanpa emulator, verifikasi yang bisa dilakukan di mesin: kompilasi, `aapt2
dump badging`, isi APK (`unzip -l`), verifikasi tanda tangan. Tes perilaku
harus di HP user (atau emulator headless bila `/dev/kvm` bisa diakses).
