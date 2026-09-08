# putar — Android app (branch `android`)

Wrapper Android (Capacitor 8) untuk music player web di repo ini
(branch `main` berisi web app untuk deploy Vercel; branch `android`
berisi proyek Android + salinan aset web di `www/`).

Hasil build dirilis sebagai APK di GitHub Releases:
https://github.com/zenn889/music-player/releases

## Bangun sendiri APK-nya

Prasyarat: Node 20+, JDK 21, Android SDK (platform 36, build-tools 36).

    npm install
    npx cap sync android        # salin www/ -> android/app/src/main/assets/public
    cd android
    ./gradlew assembleDebug     # hasil: app/build/outputs/apk/debug/app-debug.apk

APK debug ditandatangani debug keystore lokal — cukup untuk sideload.
Untuk Play Store nanti: konfigurasi signing release di
`android/app/build.gradle` dengan keystore sendiri.

## Catatan teknis

- `MainActivity.java` menambahkan:
  - Bridge file picker asli Android utk tombol "Tambah file".
  - Bridge `PutarNative.scan()/port()/hasPerm()` — pindai MediaStore
    (judul/artis/durasi/mime + sample rate utk penanda hi-res).
  - `MusicServer.java`: HTTP server lokal (dukung Range + CORS) utk
    streaming lagu MediaStore ke WebView — seek mulus & visualizer hidup.
- Pustaka musik HP tersimpan di localStorage app: daftar otomatis dimuat
  tiap app dibuka (tanpa internet), lalu disinkron diam-diam bila izin
  sudah diberikan — lagu baru ikut tanpa perlu scan manual.
- **Hi-res**: file FLAC/WAV hi-res (mis. 24-bit/96-192kHz) ikut terpindai
  dan diputar; lagu >48kHz ditandai "Hi-Res xxxkHz" di barisnya. Catatan
  jujur: output melalui WebView di-resample ke kemampuan DAC HP — bukan
  bit-perfect, dan DSD tidak didukung. Bit-perfect/Direct-DAC/USB butuh
  mesin audio native (proyek terpisah).
- Aset web dimuat lokal dari dalam APK (offline). Service worker sengaja
  nonaktif di dalam app (guard localhost).
- WebView dikonfigurasi: mixed content diizinkan (lagu MediaStore diputar via
  http://127.0.0.1 dari halaman https://localhost) + zoom (pinch/double-tap)
  dimatikan via settings native, viewport user-scalable=no, dan
  `touch-action: manipulation` di CSS.
- Aplikasi memakai id `com.zenn889.putar`, ikon mengikuti artwork "putar".
