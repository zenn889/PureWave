# PureWave — Music Player Offline (Android)

> **Panduan install manual (sideload) ada di [INSTALL.md](INSTALL.md)** —
> termasuk kenapa muncul peringatan & cara tetap install dengan aman.

Music player Android **native** (Kotlin + Jetpack Compose + Media3/ExoPlayer).
Memutar semua musik dari penyimpanan internal HP. **Tanpa internet, tanpa
akun, tanpa iklan.**

> **v2.7.2**: kartu pustaka gradien (HeroCard) + penanda lagu aktif
> (mini-EQ animasi) + info aplikasi lengkap. Splash, onboarding, dan APK
> release signed (R8) hadir sejak v2.7.0.

## Fitur

- **Pustaka terstruktur**: tab Lagu / Album / Artis / Folder — browse per
  album (artwork asli), artis, atau folder penyimpanan; ketuk untuk
  memutar seluruh isinya.
- **Video**: tab Video memindai video di HP — ketuk untuk memutar
  fullscreen di dalam app (play/pause/seek), tetap offline.
- **Auto-resume**: lagu terakhir + posisinya + antrian diingat; saat app
  dibuka lagi tinggal tekan play untuk melanjutkan.
- **Antrian terlihat**: ikon antrian di layar pemutar penuh — lihat daftar
  putar berikutnya, lompat ke lagu, urut-ulang (atas/bawah), hapus item.
- **Kecepatan putar** 0,5×–2× (pil di layar pemutar penuh, ketuk untuk
  siklus 0.5/0.75/1/1.25/1.5/2×).
- **Sleep timer** kini juga punya mode "Setelah lagu ini selesai".
- **Mini player** menampilkan progress tipis di bawah bar.
- **Sortir lengkap** di tab Lagu & Favorit: Judul A-Z, Artis, Album,
  Terbaru, Durasi.
- **Menu konteks** (tekan lama pada lagu): putar sekarang / berikutnya,
  tambah ke antrian, tandai favorit, tambah ke playlist.
- **Favorit**: tab Favorit + tombol hati di layar pemutar penuh.
- **Playlist**: buat & kelola (dari Setelan), tambah lewat menu konteks,
  hapus lagu dari playlist, putar langsung — tersimpan permanen.
- **Pencarian** di setiap tab (lagu/album/artis/folder).
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
- Identitas: logo not musik gradient coral (adaptive icon), nama "PureWave"
  (label aplikasi; package teknis tetap `com.zenn889.putar` agar update
  langsung tanpa kehilangan data).

## Unduh

https://github.com/zenn889/music-player/releases — ambil
`purewave-v2.9.7.apk` terbaru. Karena tanda tangan sama dengan v2.7.0,
install langsung menggantikan (update) versi tersebut.

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
      ui/theme/Theme.kt        tema gelap khas PureWave

## Catatan

- Versi 2.x = tulis ulang penuh native (framework resmi Android).
  Versi 1.x sebelumnya berbasis WebView sudah tidak dipakai.
- Min Android 7.0 (API 24). Sejak v2.7.0 APK dirilis adalah **release**
  (R8+shrink, signing keystore `putar`); debug-signed hanya untuk
  pengembangan.
- Hi-res: FLAC/WAV hi-res ikut terpindai & diputar oleh ExoPlayer.
  Output mengikuti kemampuan DAC HP (bukan bit-perfect; DSD tidak
  didukung oleh ExoPlayer standar).
