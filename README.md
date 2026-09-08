# putar — Music Player Offline (Android)

Music player Android **native** (Kotlin + Jetpack Compose + Media3/ExoPlayer).
Memutar semua musik dari penyimpanan internal HP. **Tanpa internet, tanpa
akun, tanpa iklan.**

## Fitur

- **Pustaka otomatis**: semua audio di perangkat (MediaStore) dimuat —
  judul, artis, durasi, urut abjad. Sekali izin, langsung muncul.
- **Album art asli** dari metadata file (Coil).
- **Pemutar sesungguhnya (Media3/ExoPlayer)**: play/pause, next/prev, seek,
  shuffle, repeat (off/semua/satu), "Putar acak" satu ketukan.
- **Kontrol di notifikasi & lock screen** + lanjut main di latar belakang
  (MediaSessionService; audio focus; pause otomatis saat headset dicabut).
- 100% offline — APK bahkan tidak meminta izin INTERNET.

## Unduh

https://github.com/zenn889/music-player/releases — ambil `putar-v2.x.apk`
terbaru. Package id `com.zenn889.putar` sama dengan versi lama, jadi
install v2.0.0 akan menggantikan (update) versi sebelumnya.

## Bangun sendiri

Prasyarat: JDK 17+, Android SDK (compileSdk 36).

    ./gradlew :app:assembleDebug
    # hasil: app/build/outputs/apk/debug/app-debug.apk

## Struktur

    app/src/main/java/com/zenn889/putar/
      MainActivity.kt          layar & alur: izin → pustaka → pemutar
      PlaybackService.kt       service Media3 (notifikasi/lock screen)
      data/MusicRepository.kt  query MediaStore (audio perangkat)
      data/Track.kt            model lagu
      ui/PlayerUi.kt           daftar lagu, mini player, layar penuh
      ui/theme/Theme.kt        tema gelap khas putar

## Catatan

- Versi 2.x = tulis ulang penuh native (framework resmi Android).
  Versi 1.x sebelumnya berbasis WebView sudah tidak dipakai.
- Min Android 7.0 (API 24). APK debug-signed (cukup utk sideload).
- Hi-res: FLAC/WAV hi-res ikut terpindai & diputar oleh ExoPlayer.
  Output mengikuti kemampuan DAC HP (bukan bit-perfect; DSD tidak
  didukung oleh ExoPlayer standar).
