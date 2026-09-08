# putar — music player di browser

Music player web statis (HTML + CSS + JS murni, tanpa dependency) yang siap
di-deploy ke Vercel. Semua lagu diputar langsung di browser kamu — tidak ada
server, tidak ada database.

## Fitur

- Playlist: tambah file audio lokal (atau drag & drop), atau tempel URL stream
- Kontrol pemutar: play/pause, prev/next, seek, volume, mute, shuffle, repeat
  (off / semua / satu lagu)
- Visualizer frekuensi real-time (Web Audio API)
- Piringan hitam animasi + warna label unik per lagu
- Shortcut keyboard: Spasi play/jeda · ←/→ lompat 5 dtk · ↑/↓ volume · M bisu ·
  S acak · R ulangi · N lagu berikut · P lagu sebelumnya
- Media Session API (tombol media di keyboard/OS/browser ikut berfungsi)
- Playlist URL tersimpan otomatis di localStorage; lagu lokal berlaku per sesi
- 3 lagu demo (SoundHelix) untuk mencoba langsung — butuh internet

## Struktur

    index.html      halaman utama
    styles.css      semua styling
    app.js          semua logika pemutar
    vercel.json     konfigurasi deploy (opsional)

## Cara deploy ke Vercel

### Cara 1 — Dashboard (paling gampang)
1. Buka https://vercel.com → New Project.
2. Import folder ini (atau push dulu ke GitHub lalu import repo-nya).
3. Vercel otomatis mendeteksi "Other" (static). Klik Deploy. Selesai.

### Cara 2 — CLI
    npm i -g vercel
    cd music-player
    vercel            # preview
    vercel --prod     # production

### Cara 3 — drag & drop
Seret folder ini ke https://vercel.new — Vercel langsung membuat project.

## Catatan

- Demo & URL stream butuh internet dan server lagu yang mengizinkan CORS agar
  visualizer jalan; kalau tidak, audio tetap diputar (visualizer nonaktif).
- Lagu dari file tidak diunggah ke mana pun — semuanya lokal di perangkat.
