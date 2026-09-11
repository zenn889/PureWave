# Keputusan stack: tetap Kotlin (Android native)

Dicatat 2026-09-11. Dokumen ini menjelaskan **kenapa** PureWave tidak pindah
bahasa, supaya pertanyaan yang sama tidak diulang tiap sesi dan supaya
keputusannya bisa ditinjau ulang dengan sadar kalau tujuan berubah.

## Keputusan

PureWave tetap **Kotlin + Jetpack Compose + Media3**, dan prioritas pengembangan
bergeser ke **refactor + tes otomatis + CI**, bukan ganti bahasa.

## Konteks yang dipakai menimbang (angka nyata saat keputusan diambil)

- 7.878 baris Kotlin di 32 file (+182 baris tes), 26 file resource.
- **31 dari 32 file memanggil API Android langsung.** Jadi pindah bahasa bukan
  menerjemahkan, tapi menulis ulang ~97% kode sambil menyelesaikan ulang
  masalah platform yang sudah beres.
- APK rilis 2,9 MB. Ini dipandang sebagai nilai jual (offline, ringan, tanpa
  runtime tambahan).

## Yang harus dilawan stack lintas platform di proyek ini

| Kebutuhan | Dipakai sekarang | Padanan di Flutter/RN |
|---|---|---|
| Pindai pustaka | MediaStore (`MusicRepository`) | plugin, tetap lewat native |
| Kontrol notifikasi & lock screen, lanjut di latar belakang | `MediaSessionService` + foreground service | dibungkus plugin, kontrol terbatas |
| Equalizer 5 pita & bass boost | `android.media.audiofx` | **tidak ada** — tetap harus Kotlin (platform channel) |
| Widget home screen + progres | `RemoteViews` | plugin terpisah, fitur terbatas |
| Picture-in-Picture video | API PiP Android | platform channel |
| Mesin pemutar + audio processor | Media3/ExoPlayer langsung | dibungkus (just_audio / Track Player), kontrol level rendah hilang |

Kesimpulan tabel ini: lintas platform memindahkan pekerjaan ke jembatan
platform, bukan menghapusnya — tetap menulis Kotlin, tapi dengan dua bahasa dan
satu lapisan jembatan yang bisa rusak.

## Opsi yang dipertimbangkan

- **Java** — tidak ada keuntungan, ekosistem Compose hilang. Ditolak.
- **Flutter (Dart)** — pilihan terbaik kalau targetnya iPhone + desktop juga.
  Biaya: EQ & widget tetap plugin/channel sendiri, APK naik ~8–15 MB.
  Ditunda, bukan ditolak: ini jalur utama kalau target berubah.
- **React Native (TS)** — paling cepat kalau sudah kuat JS/TS; kontrol audio
  level rendah tetap perlu native module. Ditunda.
- **Kotlin Multiplatform** — tetap Kotlin; logika murni dipakai bersama,
  UI tetap native per platform. Paling murah dari sisi Android. Dicatat
  sebagai jalur yang paling masuk akal **kalau** suatu hari perlu iOS tanpa
  membuang investasi Android.
- **Swift (iOS) + Kotlin (Android) dua codebase** — kualitas terbaik per
  platform, biaya rawat paling tinggi. Ditolak untuk sekarang.
- **Rust/C++ untuk mesin audio** — tidak membeli apa pun di atas ExoPlayer
  untuk pemutaran file lokal. Ditolak.
- **PWA / web** — tidak bisa mengandalkan pustaka lokal + pemutaran latar
  belakang di iOS. Ditolak.

## Kapan keputusan ini ditinjau ulang

Tinjau lagi **hanya kalau** salah satu ini benar:

1. Pengguna minta versi iPhone dan itu dinilai penting untuk kelanjutan proyek.
2. Ada kebutuhan desktop (Windows/macOS/Linux) sebagai produk, bukan iseng.
3. Perawatan Android native mulai gagal karena alasan non-teknis (mis. tidak
   ada lagi yang nyaman dengan Kotlin) — ini alasan organisasi, bukan teknis.

Kalau salah satu terjadi: baca bagian di bawah sebelum menulis satu baris pun.

## Syarat wajib kalau suatu hari benar-benar pindah

1. **applicationId tetap `com.zenn889.putar`** dan ditandatangani **keystore
   yang sama** (`~/keystores/putar-release.jks`). Bahasa tidak menentukan
   update bisa menimpa atau tidak — package dan sertifikat yang menentukan.
   Kalau ini dilanggar, semua pengguna harus uninstall dan kehilangan data.
2. **Data pengguna** harus selamat: pertahankan nama SharedPreferences
   (`putar_prefs`, `putar_fav`, `putar_playlists`, `putar_stats`,
   `putar_session`, `putar_videopos`, `putar_widget`) atau pastikan pengguna
   memakai Setelan → Cadangkan data lalu Pulihkan data setelah pindah.
3. **Daftar fitur di README = spesifikasi paritas.** Jangan rilis ulang sebelum
   paritas tercapai, karena fitur yang hilang akan terasa seperti kemunduran
   (equalizer, widget, PiP, auto-resume, lirik dari .lrc, sortir tersimpan).
4. **Spike 1–2 hari dulu** di kandidat stack, khusus menguji tiga hal tersulit:
   pindai MediaStore, kontrol dari notifikasi, dan equalizer native. Kalau salah
   satu tidak bisa dibersihkan, hentikan di situ.
5. **Jangan berhenti merilis di Android** selama transisi (tidak ada "hari
   pindah"). Versi tulis ulang dirilis sebagai v3.0.0, dan v2.x tetap hidup
   sebagai pembanding; `versionCode` lanjut dari versi terakhir di main.
6. Perlakukan `MainActivity.kt` sebagai bukti, bukan warisan: kalau tulis ulang
   dimulai, susun dulu struktur yang benar (satu file per layar/panel), jangan
   menyalin file 1.900 baris ke bahasa baru.
