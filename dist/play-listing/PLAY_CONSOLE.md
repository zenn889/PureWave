# Materi listing Google Play — PureWave

## Isian dasar Play Console
- Nama aplikasi: **PureWave**
- Nama pendek: **PureWave**
- Bahasa: Indonesia
- Kategori: **Musik & Audio**
- Aplikasi: gratis, tanpa iklan
- Situs: (repo) https://github.com/zenn889/PureWave

## Deskripsi singkat (80 karakter)
Pemutar musik offline dari penyimpanan HP — tanpa internet, tanpa iklan.

## Deskripsi lengkap
PureWave adalah pemutar musik offline untuk Android. Semua lagu
dimainkan langsung dari penyimpanan HP kamu — tanpa internet, tanpa
akun, tanpa iklan, tanpa pelacakan.

Fitur utama:
- Pustaka terstruktur: Lagu, Album, Artis, Folder, Favorit
- Pencarian + sortir (judul/artis/album/terbaru/durasi)
- Equalizer 5 pita & Bass Boost bawaan
- Playlist & antrian: buat, atur urutan (seret), putar berikutnya menumpuk
- Sejarah "Baru diputar" & statistik jumlah putar/menit didengar
- Kecepatan putar 0,5×–2× (podcast, audio book)
- Sleep timer: menit, akhir lagu ini, atau N lagu berikutnya + fade-out
- Tema: gelap, gelap OLED, terang, ikut sistem, Material You, 5 warna aksen
- Widget pemutar di layar utama
- Cadangkan & pulihkan favorit/playlist ke file
- Kontrol di notifikasi & lock screen
- Dukungan hi-res FLAC/WAV (mengikuti kemampuan DAC perangkat)
- 100% offline: tanpa izin internet sama sekali

## Kebijakan privasi (ringkas, taruh di halaman web/GitHub)
PureWave TIDAK mengumpulkan data apa pun. Aplikasi tidak memiliki izin
internet, tidak memuat iklan, tidak memakai analitik/pelacakan, dan
tidak mengunggah apa pun. Semua data pribadi (favorit, playlist,
statistik, setelan) hanya disimpan di dalam perangkat kamu.
Izin yang dipakai:
- READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE — membaca file musik kamu
  (audio saja, tidak mengubah/menghapus)
- MODIFY_AUDIO_SETTINGS — equalizer & efek audio
- Notifikasi — kontrol pemutar saat berjalan di latar belakang

## Kelas data (Data safety, wajib diisi Play Console)
Tidak ada data yang dikumpulkan/dibagikan. Isi formulir: "Tidak ada
data yang dikumpulkan".

## Aset (sudah disiapkan)
- dist/play-listing/icon-512.png (ikon aplikasi 512×512)
- dist/play-listing/icon-192.png
- dist/play-listing/feature-graphic-1024x500.png (banner toko)
- Screenshot HP: ambil dari aplikasi (minimal 2, saran 4–6: beranda,
  tab Album, pemutar penuh, EQ) — harus dari perangkat sungguhan.

## AAB untuk upload
- dist/purewave-v2.8.0.aab (signed dengan keystore lokal `putar`,
  versionCode 19 / versi 2.8.0)
- Keystore: ~/keystores/putar-release.jks (kredensial di
  ~/.gradle/gradle.properties) — SIMPAN, jangan hilang; ini kunci update
  app selamanya.
