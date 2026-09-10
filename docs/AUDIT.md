# Audit Teknis PureWave — peta state, temuan, dan guardrail

Hasil pembacaan kode menyeluruh (v2.11.0 → v2.11.1). Semua temuan di bawah
diverifikasi lewat pembacaan file + `grep`, bukan dugaan. Tidak ada emulator
di mesin ini, jadi bagian runtime tetap ditandai sebagai "perlu tes di HP".

---

## 1. Peta state & aliran data

```
MediaStore (audio/video di HP)
        │  query (MusicRepository, IO dispatcher, izin READ_MEDIA_*)
        ▼
   tracks / videos  (state di MainActivity)
        │  dikelompokkan: album, artis, folder (LibraryScreens util)
        ▼
   UI Compose (LazyColumn/LazyGrid, key unik per content-URI)

MainActivity ──Intent(BUTTON/uri)──► PlaybackService (MediaSessionService)
      ▲                                   │ ExoPlayer (audio + EQ/BassBoost)
      │ MediaController (state mirror)    │
      └───── event: title, isPlaying,    └──► notifikasi, lock screen, widget
             speed, repeat, shuffle

ProgressState (detak 400 ms, terisolasi)
      └── hanya dibaca oleh: slider mini player + layar pemutar + lirik
          (sengaja TIDAK di root komposisi → scroll daftar tetap mulus)

Penyimpanan lokal (SharedPreferences + JSON, tanpa database):
  putar_prefs  : tema, filter, sleep, EQ…      FavStore      : content-URI lagu favorit
  SessionStore : lagu terakhir + posisi        PlaylistStore  : playlist pengguna
  StatsStore   : jumlah putar & menit          VideoPosStore  : posisi tonton video
  Lyrics       : baca .lrc & cari subtitle senama (butuh kolom DATA/path)
  BackupStore  : ekspor/impor JSON favorit+playlist
```

Aturan penting yang sudah dipegang konsisten: satu Activity, banyak
Composable; data pribadi selalu di SharedPreferences; tidak ada izin
INTERNET di manifest.

---

## 2. Temuan audit & tindakan

| # | Temuan (bukti) | Tingkat | Tindakan |
|---|----------------|---------|----------|
| 1 | `albums` state di MainActivity hanya ditulis, tidak pernah dibaca; `repo.loadAlbums()` ikut terpanggil setiap pemindaian | ringan (I/O sia-sia) | **Dihapus** — pemindaian pustaka jadi lebih ringan |
| 2 | `LibraryTabBar` (LibraryScreens.kt:73) tak dipakai lagi sejak tab bawah ber-ikon + import menggantung di MainActivity:106 | ringan (kode mati) | **Dihapus** |
| 3 | `AlbumRow` (LibraryScreens.kt:105) tak dipakai sejak ada `AlbumCard` | ringan (kode mati) | **Dihapus** |
| 4 | `itemsIndexed(entries)` di QueueSort.kt:147 tanpa `key` → Compose menghitung ulang baris antrian tiap perubahan urutan | sedang (performa) | **Ditambah** `key = { i, e -> "$i|${e.uri}" }` |
| 5 | `HeroCard` & `WelcomeScreen` memakai warna literal gelap → di tema Terang kartunya tetap gelap (kontras janggal) | sedang (visual) | **Dibuat sadar-tema** via `MaterialTheme.colorScheme.background.luminance() > 0.5f` |
| 6 | Ikon `Icons.Filled.Sort`/`QueueMusic` deprecated (warning kompilasi) | ringan | **Diganti** `Icons.AutoMirrored.Filled.*` (4 file) |
| 7 | `PlayerMirror.positionMs` sudah tidak dibaca siapa pun sejak posisi pindah ke `ProgressState` | ringan | Dibiarkan (kompatibilitas), kandidat dihapus saat refactor berikutnya |
| 8 | `songsByUri` dibangun dari pustaka yang sudah difilter → pemulihan sesi terakhir bisa gagal bila lagunya tersembunyi filter | sedang (kebiasaan user) | Dibiarkan + didokumentasikan; kandidat: cari di pustaka mentah |
| 9 | Mode Picture-in-Picture belum menyembunyikan overlay kontrol | ringan | TODO: `onPictureInPictureModeChanged` → sembunyikan bar |
| 10 | Lirik hanya dibaca dari file `.lrc` senama (lirik tertanam di tag ID3 belum dibaca) | fitur | TODO |
| 11 | Backup JSON belum menyertakan statistik, tema, dan posisi video | fitur | TODO |
| 12 | Widget belum menampilkan progres lagu | fitur | TODO (poin #6 daftar 19) |

Status kompilasi setelah perubahan: **BUILD SUCCESSFUL**, tanpa warning
deprecasi lagi.

---

## 3. Verifikasi yang bisa & tidak bisa dilakukan di mesin ini

Bisa: kompilasi, `apksigner verify`, `aapt2 dump badging` (package +
versionCode), daftar isi APK (`unzip -l`), cek kurung kurawal, sha256.

Tidak bisa (harus di HP user atau emulator): perilaku tema terang/gelap,
gesture, widget, EQ, PiP, lirik, resume video, izin MediaStore nyata,
performa di RAM kecil.

---

## 4. Guardrail (agar kesalahan lama tidak terulang)

- `scripts/check_braces.py` — menghitung keseimbangan `{}` dan `()` per file
  Kotlin dengan mengabaikan isi string/komentar. Jalankan sebelum build;
  patch Compose besar hampir selalu gagal di sini kalau tidak dicek.
- `scripts/release.sh <versi> "<catatan>"` — satu perintah untuk seluruh
  alur rilis: cek kurung → naikkan versionCode/versionName (+ ganti nama APK
  di README) → `assembleRelease` → verifikasi tanda tangan & badging → salin
  ke `dist/` + sha256 → commit + push → `gh release create` → verifikasi
  rilis. Mode `DRY=1` untuk simulasi tanpa mengubah apa pun.
- Dokumentasi dasar & resep: `docs/ARCHITECTURE.md`.
- Audit ini: `docs/AUDIT.md`.

## 5. Langkah berikutnya yang disarankan

1. PiP rapi (#9) + widget progres (#12) — dua-duanya kecil dan terlihat
   hasilnya sehari-hari.
2. M3U (impor/ekspor playlist) — nilai tinggi untuk komunitas kecil.
3. Multi-pilih & aksi massal (favorit, playlist, hapus dari daftar).
4. Pemulihan sesi yang tahan filter (#8) dan lirik dari tag (#10).
