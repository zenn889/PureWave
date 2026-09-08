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

- `MainActivity.java` menambahkan bridge `onShowFileChooser` agar tombol
  "Tambah file" membuka picker audio asli Android (WebView Capacitor tidak
  menyediakannya secara bawaan).
- Aset web dimuat lokal dari dalam APK (offline). Stream via URL tetap
  butuh internet. Service worker sengaja nonaktif di dalam app (guard
  localhost), tidak diperlukan karena aset sudah bundel lokal.
- Aplikasi memakai id `com.zenn889.putar`, ikon mengikuti artwork "putar".
