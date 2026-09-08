# putar — Music Player Offline (Android)

Music player Android **native** (Kotlin + Jetpack Compose + Media3/ExoPlayer).
Memutar semua musik dari penyimpanan internal HP. **Tanpa internet, tanpa
akun, tanpa iklan.**

## Fitur

- **Pustaka otomatis**: semua audio di perangkat (MediaStore) dimuat —
  judul, artis, durasi, urut abjad. Sekali izin, langsung muncul.
- **Pencarian** judul/artis di pustaka.
- **Album art asli** dari metadata file (Coil).
- **Pemutar sesungguhnya (Media3/ExoPlayer)**: play/pause, next/prev, seek,
  shuffle, repeat (off/semua/satu), "Acak semua" satu ketukan.
- **Equalizer** native (android.media.audiofx): 5 pita (60 Hz–14 kHz),
  preset Pop/Rock/Jazz/Klasik/Dance/Bass, Bass Boost, reset — setelan
  tersimpan & menempel otomatis ke sesi audio.
- **Sleep timer**: 10–90 menit, musik berhenti sendiri; status & sisa
  waktu tampil di layar pemutar.
- **Kontrol di notifikasi & lock screen** + lanjut main di latar belakang
  (MediaSessionService; audio focus; pause otomatis saat headset dicabut).
- 100% offline — APK bahkan tidak meminta izin INTERNET.
- Identitas: logo not musik gradient coral (adaptive icon), nama "putar".

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
      MainActivity.kt          layar & alur: izin → pustaka → pemutar + sleep timer
      PlaybackService.kt       service Media3 (notifikasi/lock screen + tempel AudioFx)
      AudioFx.kt               equalizer 5 pita & bass boost (persist, menempel ke sesi)
      data/MusicRepository.kt  query MediaStore (audio perangkat)
      data/Track.kt            model lagu
      ui/PlayerUi.kt           daftar lagu, mini player, layar penuh
      ui/EqualizerSheet.kt     panel equalizer & bass
      ui/theme/Theme.kt        tema gelap khas putar

## Catatan

- Versi 2.x = tulis ulang penuh native (framework resmi Android).
  Versi 1.x sebelumnya berbasis WebView sudah tidak dipakai.
- Min Android 7.0 (API 24). APK debug-signed (cukup utk sideload).
- Hi-res: FLAC/WAV hi-res ikut terpindai & diputar oleh ExoPlayer.
  Output mengikuti kemampuan DAC HP (bukan bit-perfect; DSD tidak
  didukung oleh ExoPlayer standar).
