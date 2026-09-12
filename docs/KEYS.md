# Kunci rilis & cadangannya — apa saja yang wajib disimpan

Dokumen ini untuk siapa pun yang akan merilis PureWave (termasuk AI agent di
sesi berikutnya) . Isinya **tidak ada rahasia**: hanya daftar hal yang harus
dicadangkan dan cara memeriksanya. File `.jks` dan password-nya disimpan di
luar repo.

## Kenapa ini penting

Android hanya menerima update kalau sertifikat penandatangannya sama dengan
versi yang terpasang. Kehilangan kunci = semua pengguna wajib uninstall dulu
(favorit & playlist hilang kecuali sudah diekspor lewat Setelan → Cadangkan
data). Keystore PKCS12 **tidak bisa dibuka tanpa password** — tidak ada
pemulihan, jadi password termasuk bahan cadangan.

## Peta kunci

| Rentang rilis | Keystore | Status |
|---|---|---|
| v2.7.0 – v2.15.0 | `putar-release.jks` lama | password hilang → **tidak bisa dipakai lagi** |
| v2.16.0 – sekarang | `~/keystores/putar-release.jks` (alias `putar`) | aktif |

Identitas kunci aktif:

- format: PKCS12 · alias: `putar` · kunci: RSA 2048 / SHA256withRSA
- DN: `CN=putar, OU=zenn889, O=zenn889, C=ID`
- masa berlaku: 2026-09-10 → 2054-01-26
- sertifikat SHA-256: `2c40c65b6cc436334a34cd37fe51d75337ca3b937611c6bca3fc0b68bf25cc42`
- sertifikat SHA-1: `9e46349819e85695feb37d23297407b465213dc8`
- sha256 file keystore: `ccc5b95fdbcbb9b4a94b79638d03d3d7622f410626281ea808a6a60bfc87d323`

Sertifikat SHA-256 ini bisa dicocokkan dengan APK yang beredar:

    apksigner verify --print-certs purewave-vX.Y.Z.apk

`scripts/release-api.sh` memeriksa ulang sidik jari ini setiap kali merilis dan
**berhenti** kalau sertifikatnya berbeda — jadi rilis dengan kunci yang salah
tidak mungkin lolos tanpa disadari.

## Yang wajib dicadangkan (2 tempat terpisah, jangan satu)

1. **File keystore** `putar-release.jks` — salin ke minimal dua tempat di luar
   mesin rilis: cloud pribadi dan satu media offline (flashdisk/HDD eksternal).
2. **Password keystore** — simpan di password manager (entri terpisah dari
   file), DAN satu salinan offline (kertas di tempat aman, atau kotak
   terenkripsi). PKCS12: store password = key password.
3. **Kartu keterangan** berisi alias, DN, masa berlaku, sha256 file, dan
   sidik jari sertifikat (ada di paket `BACKUP-kunci-rilis-purewave.zip`).
   Tanpa itu, file `.jks` beberapa tahun lagi sulit dikenali.
4. **Akses password manager itu sendiri** — kode pemulihan/2FA-nya. Kalau
   password manager terkunci permanen, isinya ikut hilang. Karena itu poin 2
   butuh salinan fisik.
5. **Setelan signing** untuk mengembalikan mesin baru dengan cepat:
   `putarStoreFile`, `putarStorePass`, `putarKeyAlias=putar`, `putarKeyPass`
   di `~/.gradle/gradle.properties` (file ini tidak pernah di-commit).

Opsional tapi berguna: arsip APK + `.sha256` tiap rilis, dan catatan rilis,
supaya bisa ditelusuri kunci mana yang menandatangani versi mana.

## Latihan memulihkan (lakukan sekali, jangan tunggu panik)

Dari salinan cadangan, bukan dari mesin rilis:

    cp BACKUP/putar-release.jks /tmp/uji.jks
    keytool -list -v -keystore /tmp/uji.jks -alias putar

Masukkan password dari password manager, lalu cocokkan `SHA256`-nya dengan yang
di atas. Kalau cocok: cadangan benar-benar bisa dipakai. Cadangan yang belum
pernah diuji bukan cadangan.

## Yang JANGAN dilakukan

- Jangan menaruh password di repo, di nama file, atau hanya di
  `~/.gradle/gradle.properties` satu mesin (itu penyebab kejadian v2.16.0).
- Jangan membuat kunci tiruan untuk "melewati" error signing.
- Jangan mengganti kunci hanya karena malas mencari password — biayanya
  ditanggung pengguna lama.
