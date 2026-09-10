#!/usr/bin/env bash
# Verifikasi APK yang sudah dirilis: tanda tangan, identitas paket, sha256,
# dan bukti bahwa kode baru benar-benar masuk ke dalam APK (cek teks di dex).
#
# Pemakaian:  bash scripts/verify_apk.sh 2.14.0
set -uo pipefail

VERSION="${1:?pakai: bash scripts/verify_apk.sh <versionName>}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

BT="$(ls -d "${ANDROID_HOME:-/home/hermes/android-sdk}"/build-tools/* | sort -V | tail -1)"
APK="dist/purewave-v$VERSION.apk"
ok=0

[[ -f "$APK" ]] || { echo "GAGAL: $APK tidak ada"; exit 1; }

echo "== tanda tangan =="
"$BT/apksigner" verify --print-certs "$APK" 2>/dev/null | grep -E "^(Signer #1 (certificate DN|digest))" || true
"$BT/apksigner" verify "$APK" && echo "   tanda tangan: OK" || ok=1

echo "== identitas paket =="
"$BT/aapt2" dump badging "$APK" | grep -E "^package|^application-label:'"

echo "== sha256 =="
sha256sum -c "$APK.sha256" || ok=1

echo "== bukti kode ada di dalam APK =="
DEXSTRINGS="$(unzip -p "$APK" classes.dex | strings)"
for s in "SEDANG DIPUTAR" "Cari lagu, album, artis" "pemutar offline" "Acak semua" "Kecepatan"; do
  n=$(printf '%s\n' "$DEXSTRINGS" | grep -cF "$s")
  printf '   %-32s %s\n' "\"$s\"" "$([ "$n" -gt 0 ] && echo "ada ($n)" || { echo TIDAK-ADA; ok=1; })"
done

echo "== isi paket =="
unzip -l "$APK" | tail -1

exit $ok
