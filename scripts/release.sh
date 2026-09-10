#!/usr/bin/env bash
# Rilis PureWave satu perintah: bump versi → build → verifikasi → dist → push → gh release.
#
# Pemakaian:
#   bash scripts/release.sh 2.11.1 "Catatan rilis baris pertama..."
#   DRY=1 bash scripts/release.sh 2.11.1 "uji coba"     # hanya simulasi, tidak mengubah apa pun
#
# Prasyarat: JAVA_HOME, ANDROID_HOME (lihat docs/ARCHITECTURE.md), gh sudah login.
set -euo pipefail

VERSION="${1:?pakai: bash scripts/release.sh <versionName> \"<catatan rilis>\"}"
NOTES="${2:-Perbaikan dan polesan.}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/home/hermes/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-/home/hermes/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
GRADLE_FILE="app/build.gradle.kts"
APK_OUT="app/build/outputs/apk/release/app-release.apk"
DIST_APK="dist/purewave-v$VERSION.apk"

if [[ -n "${DRY:-}" ]]; then
  echo "[DRY] bump versi -> $VERSION"
  echo "[DRY] ./gradlew :app:assembleRelease --no-daemon"
  echo "[DRY] $BT/apksigner verify $APK_OUT"
  echo "[DRY] $BT/aapt2 dump badging $APK_OUT"
  echo "[DRY] cp $APK_OUT $DIST_APK && sha256sum -> $DIST_APK.sha256"
  echo "[DRY] git commit + push, gh release create v$VERSION"
  exit 0
fi

echo "== 0. cek kurung kurawal =="
python3 scripts/check_braces.py

echo "== 1. bump versi ke $VERSION =="
python3 - "$VERSION" <<'PY'
import re, sys, pathlib
version = sys.argv[1]
p = pathlib.Path("app/build.gradle.kts")
src = p.read_text(encoding="utf-8")
m = re.search(r"versionCode\s*=\s*(\d+)", src)
if not m:
    raise SystemExit("versionCode tidak ditemukan di app/build.gradle.kts")
new_code = int(m.group(1)) + 1
src = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {new_code}", src, count=1)
src = re.sub(r'versionName\s*=\s*"[\d.]+"', f'versionName = "{version}"', src, count=1)
p.write_text(src, encoding="utf-8")
print(f"   versionCode={new_code} versionName={version}")
r = pathlib.Path("README.md")
if r.exists():
    t = r.read_text(encoding="utf-8")
    t = re.sub(r"purewave-v[\d.]+\.apk", f"purewave-v{version}.apk", t)
    r.write_text(t, encoding="utf-8")
PY

echo "== 2. build release (R8) =="
./gradlew :app:assembleRelease --no-daemon

echo "== 3. verifikasi tanda tangan & badging =="
"$BT/apksigner" verify "$APK_OUT"
"$BT/aapt2" dump badging "$APK_OUT" | grep -E "^package"

echo "== 4. artefak dist =="
mkdir -p dist
cp "$APK_OUT" "$DIST_APK"
sha256sum "$DIST_APK" | tee "$DIST_APK.sha256"
ls -lh "$DIST_APK"

echo "== 5. commit & push =="
git add -A
git commit -m "v$VERSION: $NOTES"
git push origin main

echo "== 6. GitHub Release =="
gh release create "v$VERSION" --repo zenn889/PureWave \
  --title "PureWave v$VERSION" --notes "$NOTES" \
  "$DIST_APK" "$DIST_APK.sha256"

echo "== 7. verifikasi rilis =="
gh release view "v$VERSION" --repo zenn889/PureWave --json tagName,assets \
  -q '.tagName, (.assets[].name)'
echo "SELESAI: https://github.com/zenn889/PureWave/releases/tag/v$VERSION"
