# putar — Music Player Offline (Android)

Aplikasi Android pemutar musik offline dari penyimpanan internal HP.
Tanpa iklan, tanpa akun, tanpa internet — semua audio di perangkatmu
dipindai dan dimainkan lokal.

## Fitur

- **Pustaka otomatis**: sekali izinkan akses audio, semua lagu di HP
  (MediaStore) dimuat ke pustaka — judul, artis, durasi, urut abjad.
  Pustaka tersimpan; tiap app dibuka langsung muncul + sinkron diam-diam
  (lagu baru otomatis masuk).
- **Pemutar lengkap**: play/pause, next/prev, seek, volume, mute, shuffle,
  repeat (semua/satu lagu), visualizer frekuensi real-time, piringan
  animasi berwarna mengikuti lagu.
- **Hi-res**: file FLAC/WAV >48kHz terdeteksi & ditandai "Hi-Res xxxkHz"
  (output tetap mengikuti kemampuan DAC HP — bukan bit-perfect; DSD tidak
  didukung).
- **Tambah file manual**: tombol "Tambah audio" membuka picker audio asli
  Android (file yang dipilih tidak diunggah ke mana pun).
- 100% offline: aset aplikasi & font dibundel lokal.

## Unduh

APK di GitHub Releases: https://github.com/zenn889/music-player/releases
(pilih versi terbaru, download `putar-vX.Y.Z.apk`).

## Bangun sendiri

Prasyarat: Node 20+, JDK 21, Android SDK (platform 36).

    npm install
    npx cap sync android        # www/ -> android/app/src/main/assets/public
    cd android
    ./gradlew assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk

## Struktur

    www/               UI aplikasi (HTML/CSS/JS + font lokal) — sumber UI
    android/           proyek native Android (Capacitor)
    MainActivity.java  picker audio + bridge scan MediaStore (PutarNative)
    MusicServer.java   HTTP server lokal utk streaming lagu (Range + CORS)

## Catatan teknis

- WebView dikonfigurasi: mixed content diizinkan (audio dari
  http://127.0.0.1 di halaman https://localhost), zoom mati
  (native + viewport user-scalable=no + touch-action).
- Pustaka perangkat disimpan di localStorage app; URL lagu dibangun ulang
  dari port server saat boot.
- Izin: READ_MEDIA_AUDIO (Android 13+) / READ_EXTERNAL_STORAGE (7–12).
- Aplikasi id `com.zenn889.putar`; update via APK langsung (debug-signed).
