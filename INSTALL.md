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
Tanda tangan PureWave konsisten (keystore yang sama tiap rilis), jadi
update berikutnya bisa langsung menimpa versi lama tanpa hapus data.

## Mau hilangkan peringatan total?

Hanya dengan terbit di Google Play (biaya developer US$25 sekali).
Kalau suatu saat kamu mau, materi listing-nya sudah disiapkan di folder
`dist/play-listing/`.
