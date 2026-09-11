#!/usr/bin/env bash
# Rilis PureWave satu perintah TANPA gh CLI (cukup curl + token).
#
# Pemakaian:
#   GH_TOKEN=<token> bash scripts/release-api.sh 2.17.1 "Catatan rilis..."
#   DRY=1 GH_TOKEN=x bash scripts/release-api.sh 2.17.1 "uji"   # simulasi saja
#
# Alur: cek kurung → naikkan versi → assembleRelease (R8) → verifikasi tanda
# tangan & badging → dist + sha256 → commit & push → GitHub Release (langsung
# ditandai Latest) → unduh balik asetnya untuk memastikan byte-nya utuh.
#
# Prasyarat: JAVA_HOME, ANDROID_HOME (lihat docs/ARCHITECTURE.md), properti
# signing di ~/.gradle/gradle.properties (lihat docs/KEYS.md), dan GH_TOKEN
# dengan akses Contents: read/write ke repo ini.
set -euo pipefail

VERSION="${1:?pakai: bash scripts/release-api.sh <versionName> \"<catatan rilis>\"}"
NOTES="${2:-Perbaikan dan polesan.}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

export JAVA_HOME="${JAVA_HOME:-/home/hermes/jdk}"
export ANDROID_HOME="${ANDROID_HOME:-/home/hermes/android-sdk}"
export PATH="$JAVA_HOME/bin:$PATH"

REPO="zenn889/PureWave"
TAG="v$VERSION"
API=https://api.github.com
# Sidik jari sertifikat kunci rilis aktif. Kalau keystore yang dipakai berbeda,
# rilis digagalkan: tanda tangan berubah = semua pengguna harus uninstall.
KEY_FPR="2c40c65b6cc436334a34cd37fe51d75337ca3b937611c6bca3fc0b68bf25cc42"

BT="$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)"
APK_OUT="app/build/outputs/apk/release/app-release.apk"
DIST_APK="dist/purewave-$TAG.apk"

if [[ -n "${DRY:-}" ]]; then
  echo "[DRY] cek kurung → bump versi → assembleRelease → verifikasi tanda tangan"
  echo "[DRY] dist/purewave-$TAG.apk + .sha256 → commit & push → Release $TAG (Latest)"
  exit 0
fi

: "${GH_TOKEN:?set GH_TOKEN dulu (fine-grained PAT, Contents: read/write)}"

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
src = re.sub(r"versionCode\s*=\s*\d+", f"versionCode = {int(m.group(1)) + 1}", src, count=1)
src = re.sub(r'versionName\s*=\s*"[\d.]+"', f'versionName = "{version}"', src, count=1)
p.write_text(src, encoding="utf-8")
print("   " + [l for l in src.splitlines() if "versionCode" in l][0].strip())
r = pathlib.Path("README.md")
if r.exists():
    t = r.read_text(encoding="utf-8")
    r.write_text(re.sub(r"purewave-v[\d.]+\.apk", f"purewave-{version}.apk", t), encoding="utf-8")
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
  echo "GAGAL: sertifikat ($NEW_FPR) tidak sama dengan kunci rilis aktif ($KEY_FPR)."
  echo "Jangan dirilis — pengguna lama harus uninstall kalau tanda tangannya berubah."
  exit 6
fi
echo "   tanda tangan cocok dengan kunci rilis aktif"

echo "== 4. artefak dist =="
mkdir -p dist
cp "$APK_OUT" "$DIST_APK"
sha256sum "$DIST_APK" | tee "$DIST_APK.sha256"

echo "== 5. commit & push =="
git add -A
git commit -m "$TAG: $NOTES"
git push origin main

echo "== 6. GitHub Release (langsung Latest) =="
AUTH=(-H "Authorization: token $GH_TOKEN" -H "Accept: application/vnd.github+json")
python3 - "$TAG" "$NOTES" > /tmp/rel-body.json <<'PY'
import json, sys
tag, notes = sys.argv[1], sys.argv[2]
print(json.dumps({
    "tag_name": tag, "target_commitish": "main",
    "name": f"PureWave {tag}", "body": notes,
    "draft": False, "prerelease": False, "make_latest": "true",
}, ensure_ascii=False))
PY
curl -s -X POST "${AUTH[@]}" "$API/repos/$REPO/releases" -d @/tmp/rel-body.json \
  -o /tmp/rel-created.json -w "   create release -> HTTP %{http_code}\n"
UP="$(python3 -c "import json;print(json.load(open('/tmp/rel-created.json')).get('upload_url','').split('{')[0])")"
[ -n "$UP" ] || { echo "gagal membuat rilis"; exit 7; }
for f in "$DIST_APK" "$DIST_APK.sha256"; do
  curl -s -X POST "${AUTH[@]}" -H "Content-Type: application/octet-stream" \
    --data-binary "@$f" "$UP?name=$(basename "$f")" -o /dev/null \
    -w "   upload $(basename "$f") -> HTTP %{http_code}\n"
done

echo "== 7. verifikasi rilis & aset =="
curl -s "${AUTH[@]}" "$API/repos/$REPO/releases/tags/$TAG" -o /tmp/rel-check.json
python3 -c "
import json; d = json.load(open('/tmp/rel-check.json'))
print('   tag:', d['tag_name'], '| prerelease:', d['prerelease'], '| draft:', d['draft'])
for a in d['assets']: print('   aset:', a['name'], a['size'], 'byte', a['state'])
open('/tmp/dl-url.txt','w').write([a['browser_download_url'] for a in d['assets'] if a['name'].endswith('.apk')][0])
"
curl -sL -o /tmp/dl-check.apk "$(cat /tmp/dl-url.txt)"
L="$(awk '{print $1}' "$DIST_APK.sha256")"
R="$(sha256sum /tmp/dl-check.apk | awk '{print $1}')"
if [ "$L" = "$R" ]; then echo "   sha256 lokal == aset GitHub ($L)"; else echo "   TIDAK COCOK!"; exit 8; fi
echo "SELESAI: https://github.com/$REPO/releases/tag/$TAG"
