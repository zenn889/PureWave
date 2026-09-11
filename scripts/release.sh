#!/usr/bin/env bash
# Rilis PureWave satu perintah memakai GitHub CLI (gh).
#
# Pemakaian:
#   bash scripts/release.sh 2.19.0 "Catatan rilis singkat..."
#   NOTES_FILE=/path/catatan.md bash scripts/release.sh 2.19.0 "Judul komit"
#   DRY=1 bash scripts/release.sh 2.19.0 "uji coba"     # simulasi, tidak mengubah apa pun
#
# Prasyarat: JAVA_HOME, ANDROID_HOME (docs/ARCHITECTURE.md), properti signing di
# ~/.gradle/gradle.properties (docs/KEYS.md), dan `gh auth status` yang hijau.
# Jalur tanpa gh ada di scripts/release-api.sh.
#
# Alur: cek kurung → naikkan versi → assembleRelease (R8) → verifikasi tanda
# tangan & badging → dist + sha256 → commit & push → GitHub Release (langsung
# Latest) → unduh balik asetnya untuk memastikan byte-nya utuh.
set -euo pipefail

VERSION="${1:?pakai: bash scripts/release.sh <versionName> \"<catatan rilis>\"}"
NOTES="${2:-Perbaikan dan polesan.}"
if [[ -n "${NOTES_FILE:-}" && -f "${NOTES_FILE}" ]]; then
  NOTES_SUBJECT="$NOTES"
  NOTES_ARGS=(--notes-file "$NOTES_FILE")
else
  NOTES_SUBJECT="$NOTES"
  NOTES_ARGS=(--notes "$NOTES")
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/home/hermes/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-/home/hermes/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

REPO="zenn889/PureWave"
TAG="v$VERSION"
# Sidik jari sertifikat kunci rilis aktif (docs/KEYS.md). Kalau keystore yang
# dipakai berbeda, rilis digagalkan: tanda tangan berubah = pengguna harus
# uninstall dulu.
KEY_FPR="2c40c65b6cc436334a34cd37fe51d75337ca3b937611c6bca3fc0b68bf25cc42"

BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
APK_OUT="app/build/outputs/apk/release/app-release.apk"
DIST_APK="dist/purewave-$TAG.apk"

if [[ -n "${DRY:-}" ]]; then
  echo "[DRY] cek kurung → bump versi ke $VERSION → assembleRelease"
  echo "[DRY] verifikasi badging + sertifikat harus $KEY_FPR"
  echo "[DRY] $DIST_APK + .sha256 → commit & push → gh release create $TAG --latest"
  echo "[DRY] unduh balik aset & cocokkan sha256"
  exit 0
fi

command -v gh >/dev/null || { echo "gh tidak ada di PATH — pakai scripts/release-api.sh"; exit 2; }
gh auth status >/dev/null 2>&1 || { echo "gh belum login (gh auth login --with-token)"; exit 2; }

echo "== 0. cek kurung kurawal =="
python3 scripts/check_braces.py

echo "== 1. bump versi ke $VERSION =="
python3 - "$VERSION" <<'PY'
import re, sys, pathlib
version = sys.argv[1]
p = pathlib.Path("app/build.gradle.kts")
src = p.read_text(encoding="utf-8")
code = int(re.search(r"versionCode\s*=\s*(\d+)", src).group(1))
current = re.search(r'versionName\s*=\s*"([\d.]+)"', src).group(1)
if current == version:
    print(f"   versi sudah {version} — versionCode tetap {code} (tidak menaikkan dua kali)")
else:
    code += 1
    src = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {code}", src, count=1)
    src = re.sub(r'versionName\s*=\s*"[\d.]+"', f'versionName = "{version}"', src, count=1)
    p.write_text(src, encoding="utf-8")
    print(f"   versionCode={code} versionName={version}")
r = pathlib.Path("README.md")
if r.exists():
    t = r.read_text(encoding="utf-8")
    r.write_text(re.sub(r"purewave-v[\d.]+\.apk", f"purewave-v{version}.apk", t), encoding="utf-8")
PY

echo "== 2. build rilis (R8 + shrink) =="
./gradlew :app:assembleRelease --no-daemon | grep -E "^BUILD" || { echo "build gagal"; exit 5; }
[ -f "$APK_OUT" ] || { echo "APK rilis tidak terbentuk"; exit 5; }

echo "== 3. verifikasi identitas & tanda tangan =="
"$BT/aapt2" dump badging "$APK_OUT" | grep -E "^package"
"$BT/apksigner" verify --print-certs "$APK_OUT" > /tmp/release-cert.txt 2>&1
grep -E "Signer #1 certificate SHA-256 digest" /tmp/release-cert.txt
NEW_FPR="$(grep -m1 'Signer #1 certificate SHA-256 digest' /tmp/release-cert.txt | awk '{print $NF}')"
if [ "$NEW_FPR" != "$KEY_FPR" ]; then
  echo "GAGAL: sertifikat ($NEW_FPR) bukan kunci rilis aktif ($KEY_FPR) — jangan dirilis."
  exit 6
fi
echo "   tanda tangan cocok dengan kunci rilis aktif"

echo "== 4. artefak dist =="
mkdir -p dist
cp "$APK_OUT" "$DIST_APK"
sha256sum "$DIST_APK" | tee "$DIST_APK.sha256"
ls -lh "$DIST_APK" | awk '{print "   ukuran:", $5}'

echo "== 5. commit & push =="
git add -A
git commit -m "$TAG: $NOTES_SUBJECT"
git push origin main

echo "== 6. GitHub Release (langsung Latest) =="
gh release create "$TAG" --repo "$REPO" \
  --title "PureWave $TAG" "${NOTES_ARGS[@]}" --latest \
  "$DIST_APK" "$DIST_APK.sha256"

echo "== 7. verifikasi rilis & aset =="
gh release view "$TAG" --repo "$REPO" --json tagName,isPrerelease,assets \
  -q '"tag: \(.tagName)  prerelease: \(.isPrerelease)", (.assets[] | "aset: \(.name)  \(.size) byte  state=\(.state)")'

rm -rf /tmp/verify-release && mkdir -p /tmp/verify-release
gh release download "$TAG" --repo "$REPO" --pattern '*.apk' --dir /tmp/verify-release --clobber
L="$(awk '{print $1}' "$DIST_APK.sha256")"
R="$(sha256sum /tmp/verify-release/*.apk | awk '{print $1}')"
if [ "$L" = "$R" ]; then
  echo "   sha256 lokal == aset GitHub ($L)"
else
  echo "   TIDAK COCOK: lokal=$L github=$R"
  exit 8
fi
echo "SELESAI: https://github.com/$REPO/releases/tag/$TAG"
