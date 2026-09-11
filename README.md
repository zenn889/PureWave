# PureWave — Music Player Offline (Android)

> **Panduan install manual (sideload) ada di [INSTALL.md](INSTALL.md)** —
> termasuk kenapa muncul peringatan & cara tetap install dengan aman.
>
> **Dokumentasi teknis & buku resep: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**
> — peta kode, cara ubah tema, cara menambah fitur, dan langkah rilis APK.

Music player Android **native** (Kotlin + Jetpack Compose + Media3/ExoPlayer).
Memutar semua musik dari penyimpanan internal HP. **Tanpa internet, tanpa
akun, tanpa iklan.**

> **v2.18.0**: normalisasi volume antar lagu (ReplayGain, opsional di Setelan),
> plus 12 unit test pertama dan CI GitHub Actions yang memeriksa tiap push.
>
> **v2.19.1**: perbaikan kontras teks — judul lagu di daftar, mini player,
> layar pemutar, kartu album, dan bar judul sekarang memakai warna tinta
> eksplisit (bukan warna bawaan komponen), jadi tidak lagi menyatu dengan
> latar di tema gelap. Sorotan lagu yang sedang diputar ditebalkan dan pendar
> coral di latar atas dilembutkan.
>
> **v2.19.0**: tampilan diperbarui dengan gaya **gelap premium** — sekarang
> semua sudut, jarak, bayangan, dan durasi gerak diambil dari satu sistem token
> (`ui/theme/Tokens.kt`), jadi tidak ada lagi sudut 2/9/14/15/18/20 dp yang
> campur aduk. Daftar lagu tidak lagi berbentuk kotak per baris (hanya lagu
> yang sedang diputar yang disorot), kartu album/artis/folder seragam dengan
> bayangan lembut + efek mengecil saat ditekan, kartu sambutan bersudut lebih
> besar, kolom pencarian jadi pil tanpa garis tepi, layar pemutar memakai
> bayangan lebih dalam, dan bar navigasi bawah beranimasi (warna pil + label
> muncul halus).
>
> **v2.17.0**: urutkan lagu di dalam playlist (seret gagang atau tombol
> naik/turun), sortir jadi 10 pilihan (termasuk tersimpan & berlaku juga di
> daftar album/artis/folder), dan pencarian lebih pintar (kata tidak perlu
> berurutan, mencakup judul/artis/album/folder, diakritik diabaikan).
>
> **v2.16.0**: Picture-in-Picture bersih (overlay kontrol otomatis hilang
> saat jendela mengecil) + widget layar utama kini menampilkan bar progres
> dan waktu lagu.
>
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
- **Sortir lengkap** (pilihannya diingat): Judul A-Z, Judul Z-A, Artis,
  Album, Terbaru ditambahkan, Terlama ditambahkan, Durasi terpendek,
  Durasi terpanjang, Paling sering diputar, Terakhir diputar — berlaku di
  tab Lagu, Favorit, dan daftar album/artis/folder.
- **Menu konteks** (tekan lama pada lagu): putar sekarang / berikutnya,
  tambah ke antrian, tandai favorit, tambah ke playlist.
- **Favorit**: tab Favorit + tombol hati di layar pemutar penuh.
- **Playlist**: buat & kelola (dari Setelan), tambah lewat menu konteks,
  urutkan lagunya sesuka hati (seret gagang atau tombol naik/turun), hapus
  lagu dari playlist, putar langsung — tersimpan permanen.
- **Pencarian** di setiap tab: kata boleh tidak berurutan ("sheila adu" ketemu
  "Adu · Sheila On 7"), mencari di judul, artis, album, dan folder sekaligus,
  dan mengabaikan tanda diakritik.
- **Album art asli** dari metadata file (Coil).
- **Pemutar sesungguhnya (Media3/ExoPlayer)**: play/pause, next/prev, seek,
  shuffle, repeat (off/semua/satu), "Acak semua" satu ketukan.
- **Equalizer** native (android.media.audiofx): 5 pita (60 Hz–14 kHz),
  preset Pop/Rock/Jazz/Klasik/Dance/Bass, Bass Boost, reset — setelan
  tersimpan & menempel otomatis ke sesi audio.
- **Normalisasi volume** (opsional, Setelan): mengikuti tag ReplayGain di file
  — `REPLAYGAIN_TRACK_GAIN` + `_PEAK` — supaya kenyaringan antar lagu rata dan
  tetap aman dari clipping. Lagu tanpa tag dibiarkan apa adanya. Didukung:
  MP3 (ID3v2) & FLAC; OGG/Opus dan M4A belum.
- **Sleep timer**: 10–90 menit, musik berhenti sendiri; status & sisa
  waktu tampil di layar pemutar.
- **Kontrol di notifikasi & lock screen** + lanjut main di latar belakang
  (MediaSessionService; audio focus; pause otomatis saat headset dicabut).
- **Widget layar utama**: lagu aktif + bar progres & waktu berjalan +
  tombol sebelumnya/putar-berikutnya (progres disegarkan tiap detik).
- **Picture-in-Picture**: video tetap tampil sebagai jendela kecil saat
  ditekan Home; seluruh kontrol otomatis disembunyikan di mode itu.
- **Tampilan**: seluruh ukuran visual (sudut, jarak, bayangan, durasi animasi)
  berasal dari satu berkas token — `ui/theme/Tokens.kt` — sehingga permukaan
  yang mirip selalu terlihat konsisten. Daftar lagu bersih tanpa kotak per
  baris; lagu yang sedang diputar disorot halus, kartu album mengecil sedikit
  saat ditekan, dan bar navigasi bawah berpindah dengan animasi warna.
- 100% offline — APK bahkan tidak meminta izin INTERNET.
- Identitas: logo not musik gradient coral (adaptive icon), nama "PureWave"
  (label aplikasi; package teknis tetap `com.zenn889.putar` agar update
  langsung tanpa kehilangan data).

## Unduh

https://github.com/zenn889/PureWave/releases — ambil `purewave-v2.19.1.apk`.

> **Pindah dari v2.7.0–v2.15.0 (wajib baca):** rilis v2.16.0 memakai kunci
> penandatangan **baru**. Kunci lama (v2.7.0–v2.15.0) tidak bisa dipakai lagi
> karena password-nya hilang, dan Android menolak update yang tandatangannya
> berbeda — jadi versi lama harus di-uninstall dulu. Langkah lengkapnya di
> [INSTALL.md](INSTALL.md#pindah-dari-versi-lama). Setelah pindah ke v2.16.0,
> tanda tangan baru ini yang dipakai untuk rilis-rilis berikutnya.

## Bangun sendiri

Prasyarat: JDK 17+, Android SDK (compileSdk 36).

    ./gradlew :app:assembleDebug
    # hasil: app/build/outputs/apk/debug/app-debug.apk

    ./gradlew :app:testDebugUnitTest
    # unit test JVM (tanpa perangkat/buatan): pembaca tag ReplayGain & gain

Setiap push ke `main` dan setiap pull request diperiksa otomatis oleh GitHub
Actions (`.github/workflows/ci.yml`): cek kurung kurawal, unit test, dan build
APK debug.

## Struktur

    app/src/main/java/com/zenn889/putar/
      MainActivity.kt          layar & alur: izin → pustaka → pemutar + sleep timer
      PlaybackService.kt       service Media3 (notifikasi/lock screen + tempel AudioFx)
      AudioFx.kt               equalizer 5 pita & bass boost (persist, menempel ke sesi)
      data/MusicRepository.kt  query MediaStore (audio perangkat)
      data/Track.kt            model lagu
      ui/PlayerUi.kt           daftar lagu, mini player, layar penuh
      ui/LibraryHeader.kt      kepala pustaka: sapaan, pencarian, chip tab
      ui/ScreenStates.kt       layar izin & pustaka kosong
      ui/SleepTimerDialog.kt   dialog sleep timer
      ui/BottomTabs.kt         bar navigasi bawah mengambang
      ui/EqualizerSheet.kt     panel equalizer & bass
      ui/theme/Theme.kt        tema gelap khas PureWave
      ui/theme/Tokens.kt       token desain: sudut, jarak, bayangan, durasi gerak

## Catatan

- Versi 2.x = tulis ulang penuh native (framework resmi Android).
  Versi 1.x sebelumnya berbasis WebView sudah tidak dipakai.
- Min Android 7.0 (API 24). Sejak v2.7.0 APK dirilis adalah **release**
  (R8+shrink, signing keystore `putar`); debug-signed hanya untuk
  pengembangan.
- Hi-res: FLAC/WAV hi-res ikut terpindai & diputar oleh ExoPlayer.
  Output mengikuti kemampuan DAC HP (bukan bit-perfect; DSD tidak
  didukung oleh ExoPlayer standar).
