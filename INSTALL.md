# Cara install PureWave (APK manual)

PureWave belum tersedia di Play Store, jadi file-nya di-install manual
(sideload). **Peringatan "sumber tidak dikenal" / pemindaian Play Protect
saat install adalah hal normal untuk semua APK di luar Play Store** —
bukan berarti aplikasinya berbahaya.

## Langkah

1. **Unduh APK** dari halaman rilis:
   https://github.com/zenn889/PureWave/releases
   Ambil file `purewave-vX.Y.Z.apk` (bukan `.sha256`).
2. Ketuk file tersebut dari aplikasi Berkas/File Manager (atau dari
   notifikasi unduhan browser).
3. Android akan meminta izin: **"Izinkan instal dari sumber ini?"**
   untuk browser/File Manager yang dipakai — ketuk **Izinkan / Allow**.
4. Play Protect mungkin menampilkan "aplikasi belum dipindai" — pilih
   **Tetap instal / Install anyway**.
5. Selesai. Tidak ada izin internet: PureWave 100% offline.

> Catatan: karena di-install manual, HP tidak otomatis memperbarui app.
> Cek halaman rilis dari waktu ke waktu untuk versi baru, atau minta
> pengingat ke orang yang memasangkannya 😄

## Opsional: cek keaslian file (hash)

Setiap rilis menyertakan `purewave-vX.Y.Z.apk.sha256`. Di HP:
`Berkas → pilih APK → info` biasanya menampilkan hash MD5/SHA — cocokkan
dengan file `.sha256` bila ingin benar-benar yakin file tidak rusak.

## Kenapa ada peringatan?

Android menandai aplikasi yang tidak berasal dari toko resmi (Play
Store). Ini aturan keamanan sistem, berlaku untuk semua developer.
Selama rilis-rilis setelah v2.16.0 memakai kunci yang sama, update
berikutnya bisa langsung menimpa versi lama tanpa hapus data.

## Pindah dari versi lama

Ini hanya perlu kalau kamu masih memakai PureWave **v2.7.0 sampai v2.15.0**.
Rilis v2.16.0 ditandatangani dengan kunci baru (kunci lama, dipakai
v2.7.0–v2.15.0, password-nya hilang sehingga tidak bisa dipakai lagi).
Android hanya mengizinkan update kalau tanda tangannya sama, jadi langkahnya:

1. Buka PureWave lama → **Setelan → Cadangkan data** → simpan file JSON-nya
   (berisi favorit + playlist).
2. **Uninstall** PureWave lama. Statistik dan posisi tonton video tidak ikut
   tercadang, jadi keduanya akan mulai dari nol.
3. Install `purewave-v2.16.0.apk` seperti langkah di atas.
4. **Setelan → Pulihkan data** → pilih file JSON dari langkah 1.

Sudah pakai v2.16.0 atau lebih baru? Lewati bagian ini — cukup install
versi terbaru seperti biasa.

## Mau hilangkan peringatan total?

Hanya dengan terbit di Google Play (biaya developer US$25 sekali).
Kalau suatu saat kamu mau, materi listing-nya sudah disiapkan di folder
`dist/play-listing/`.
