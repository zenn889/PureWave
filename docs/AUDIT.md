# Audit Teknis PureWave — peta state, temuan, dan guardrail

Hasil pembacaan kode menyeluruh (v2.11.0 → v2.11.1), diperbarui v2.16.0
(PiP bersih + progres widget). Semua temuan di bawah diverifikasi lewat
pembacaan file, `grep`, dan pemeriksaan APK hasil build — bukan dugaan.
Tidak ada emulator di mesin ini (`/dev/kvm` tidak bisa diakses), jadi bagian
runtime tetap ditandai sebagai "perlu tes di HP".

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
| 9 | Mode Picture-in-Picture belum menyembunyikan overlay kontrol | ringan | **Ditutup (v2.16.0)** — `VideoPlayback.inPip` + `onPictureInPictureModeChanged`; overlay & penangkap ketukan tidak dikomposisi selama PiP |
| 10 | Lirik hanya dibaca dari file `.lrc` senama (lirik tertanam di tag ID3 belum dibaca) | fitur | TODO |
| 11 | Backup JSON belum menyertakan statistik, tema, dan posisi video | fitur | TODO |
| 12 | Widget belum menampilkan progres lagu | fitur | **Ditutup (v2.16.0)** — bar progres + waktu; didorong `pushWidget()` dari event player & loop 1 detik di `PlaybackService` |
| 13 | Dua warning deprecasi tersisa dari rework tampilan: `Icons.Filled.PlaylistPlay` (SettingsSheet.kt:135) & field `PackageInfo.versionCode` (:177) | ringan | **Diganti (v2.16.0)** — `Icons.AutoMirrored.Filled.PlaylistPlay` + `PackageInfoCompat.getLongVersionCode` |
| 14 | Properti `putarStoreFile` dipanggil `error()` saat konfigurasi Gradle → build **debug** pun gagal di mesin tanpa keystore (clone baru / mesin lain) | sedang (alur kerja) | **Diperbaiki (v2.16.0)** — signing config dibuat kondisional; rilis dijaga guardrail eksplisit di `app/build.gradle.kts` (tidak boleh dibangun tanpa keystore asli) |
| 15 | `lintDebug` menyimpan 10 error warisan (7× `UnstableApi` media3, `startForegroundService` NewApi, 2× `AppLinkUrlError`) + 22 warning; `scripts/release.sh` tidak menjalankan lint, jadi tidak pernah terlihat | ringan (utang teknis) | Dibiarkan — pekerjaan v2.16.0 tidak menambah satu pun (13 error saat `pipParams()` ditulis, turun jadi 10 setelah diberi `@RequiresApi(O)`; 10 sisanya terbukti ada di baris v2.15.0 lewat `git show HEAD:<file>`). Kandidat: `@OptIn(UnstableApi::class)` + tambah lint ke alur rilis |
| 16 | Password `putar-release.jks` (kunci rilis v2.7.0–v2.15.0) hilang dan tidak ada cadangannya — keystore PKCS12 tidak bisa dibuka tanpa password | tinggi (kesinambungan rilis) | **Kunci baru dibuat (v2.16.0)** di `~/keystores/putar-release.jks` (PKCS12, alias `putar`, RSA 2048, 10000 hari, DN sama). Konsekuensi: pengguna lama wajib uninstall dulu (ekspor `Cadangkan data` → uninstall → install → `Pulihkan data`), didokumentasikan di INSTALL.md + README. Password baru dicadangkan di password manager |
| 17 | `Track.contentUri` bertipe `android.net.Uri` → model tidak bisa dipakai di unit test JVM (harus emulator/Robolectric), dan memaksa `.contentUri.toString()` di 27 tempat | sedang (testabilitas + kebersihan) | **Diperbaiki (refactor setelah v2.18.0)** — jadi `String`; 27 pemanggilan `.toString()` hilang, titik yang benar-benar butuh Uri memakai `Uri.parse`. Sekaligus logika sortir & pencarian diangkat ke `ui/SearchSort.kt` yang murni + 13 tes baru (total 25) |
| 18 | Keputusan stack (tetap Kotlin vs pindah bahasa) tidak terdokumentasi sehingga bisa diperdebatkan ulang tiap sesi | ringan (dokumentasi) | **Ditulis (refactor setelah v2.18.0)** di `docs/STACK.md`: angka nyata, perbandingan lintas platform, syarat wajib kalau suatu hari pindah, dan pemicu peninjauan ulang |
| 19 | `MainActivity.kt` 1.843 baris menampung UI layar (header, dialog, layar izin/kosong) + helper pemutar, sehingga perubahan kecil menyentuh file raksasa | sedang (perawatan) | **Potongan kedua (refactor setelah v2.18.0)**: 13 deklarasi dipindah ke `ui/LibraryHeader.kt`, `ui/PlayerSupport.kt`, `ui/SleepTimerDialog.kt`, `ui/ScreenStates.kt`, `buildAlbumsFrom` ke `ui/LibraryScreens.kt`; 38 import yatim dibuang. MainActivity 1.843 → 1.429 baris. Sisa: `PlayerApp` sendiri (~1.000 baris) belum dipecah |

| 20 | Ukuran visual (sudut, bayangan, durasi animasi) ditulis langsung di tempat pemakaian sehingga permukaan yang mirip terlihat berbeda-beda — sudut 2–30 dp & bayangan 6–30 dp bercampur, dan baris daftar dipaksa jadi kartu di setiap baris (bising) | sedang (rasa visual) | **Ditutup v2.19.0**: `ui/theme/Tokens.kt` (Radius/Space/Elev/Motion + `pressScale`), daftar lagu dibuat tanpa kotak per baris, kartu album/artis/folder + kartu sambutan + kolom cari + bar navigasi bawah diseragamkan dan diberi animasi. Prinsip & pedoman pemakaian ditulis di docs/ARCHITECTURE.md bagian 4h |

Status kompilasi (terakhir diperiksa setelah refactor di atas): **BUILD
SUCCESSFUL** — `check_braces.py` bersih (38 file di `app/src/main`), `assembleDebug` tanpa
warning deprecasi, 25 unit test lolos, dan isi APK debug diperiksa lewat
`aapt2 dump badging` + `xmltree` + `strings` pada dex.

---

## 3. Verifikasi yang bisa & tidak bisa dilakukan di mesin ini

Bisa: kompilasi, `apksigner verify`, `aapt2 dump badging` (package +
versionCode), daftar isi APK (`unzip -l`), cek kurung kurawal, sha256,
pemeriksaan isi APK (`aapt2 dump xmltree` untuk layout widget, `strings` pada
classes.dex untuk memastikan kode baru benar-benar ikut terpaket), serta
**unit test JVM** (`./gradlew :app:testDebugUnitTest`) untuk logika murni —
25 tes (parser tag ReplayGain & perhitungan gain, sortir 10 pilihan, pencarian
token/diakritik).

Tidak bisa (harus di HP user atau emulator dengan KVM): perilaku tema
terang/gelap, gesture, widget di home screen, EQ, PiP nyata, lirik, resume
video, izin MediaStore nyata, performa di RAM kecil. Catatan v2.16.0:
`/dev/kvm` di mesin ini dimiliki `root:kvm` dan user tidak punya akses, jadi
emulator Android tidak bisa dipakai — PiP & widget tetap **perlu tes di HP**.

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
- Guardrail tanda tangan: `app/build.gradle.kts` menolak `assembleRelease` /
  `bundleRelease` kalau `putarStoreFile` belum di-set — jadi tidak mungkin
  ada APK rilis bertanda tangan lain (v2.16.0).
- Audit ini: `docs/AUDIT.md`.

## 5. Langkah berikutnya yang disarankan

1. M3U (impor/ekspor playlist) — nilai tinggi untuk komunitas kecil.
2. Multi-pilih & aksi massal (favorit, playlist, hapus dari daftar).
3. Pemulihan sesi yang tahan filter (#8) dan lirik dari tag (#10).
4. Backup menyertakan statistik, tema, dan posisi video (#11).
5. Bersihkan utang lint (#15): `@OptIn(UnstableApi::class)` untuk pemakaian
   media3, tangani `startForegroundService` untuk API 24–25, dan jalankan
   lint di alur rilis supaya tidak menumpuk lagi.
6. Amankan kunci rilis: simpan password `putar-release.jks` (alias `putar`) di
   password manager dan taruh salinan `.jks` di dua tempat. Tanpa itu, satu
   mesin hilang = rilis berikutnya tidak bisa menimpa versi terpasang.
7. ~~Tambah dukungan tag ReplayGain untuk OGG/Opus dan M4A/MP4~~ — **selesai
   v2.22.0** (Ogg/Vorbis, Opus, dan atom iTunes MP4/M4A termasuk `moov` di
   akhir file; 7 tes baru). Yang tersisa dari butir ini: `R128_TRACK_GAIN`
   Opus sengaja tidak dipakai karena acuan kenaringannya berbeda.

Sudah selesai: PiP bersih (#9) dan progres di widget (#12) — v2.16.0.
Urut-ulang lagu di playlist, sortir 10 pilihan (tersimpan), dan pencarian
token/diakritik — v2.17.0.
Normalisasi volume ReplayGain + 12 unit test + CI GitHub Actions — v2.18.0.
Refactor `Track.contentUri` (String) + `ui/SearchSort.kt` + 13 tes (total 25) +
`docs/STACK.md` — menumpang v2.19.0.
`ui/theme/Tokens.kt` (sistem token tampilan) — v2.19.0; perbaikan warna teks
bawaan tema (`LocalContentColor`) — v2.19.2; notis lisensi MIT di dalam app +
perbaikan URL repo — v2.21.0.
Dukungan ReplayGain OGG/Opus & M4A + `moov` di akhir file (total 32 tes) —
v2.22.0.
Album kompilasi lewat tag `ALBUM_ARTIST` + label "Berbagai artis" + sampul dari
folder lagu (`data/ArtResolver.kt`; total 47 tes) — v2.23.0.
Cache sampul per album (memperbaiki loading awal yang lambat) — v2.23.1.
Simpanan pustaka di disk (`data/LibraryCache.kt`) sehingga aplikasi terbuka
tanpa lingkaran loading; total 59 tes — v2.24.0.
Pemutar video: kecepatan putar 0,5×–2× (memakai `fmtSpeed`/`nextSpeed` yang
sama dengan pemutar musik) + mode tampilan Fit/Isi/Zoom; penghitung antrian
pindah ke baris judul; total 65 tes — v2.24.1.
Sentuhan pemutar video: ketuk dua kali di sisi kiri/kanan = ±10 detik, geser
atas-bawah = kecerahan jendela (dikembalikan saat keluar); total 69 tes —
v2.24.2.

Catatan: hasil normalisasi volume tetap perlu dinilai telinga di HP, karena
mesin build ini tidak punya perangkat audio. Yang bisa dibuktikan di sini
adalah pembacaan tagnya (unit test) — bukan seberapa enak hasilnya terdengar.
