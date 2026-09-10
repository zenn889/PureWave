#!/usr/bin/env python3
"""Cek keseimbangan {} dan () di setiap file Kotlin.

Dipakai sebelum build, karena patch teks besar pada Compose mudah menggeser
kurung kurawal dan errornya baru muncul sebagai pesan Gradle yang membingungkan
("Expecting '}'", "Modifier 'private' is not applicable to 'local function'").

Pemakaian:
    python3 scripts/check_braces.py            # cek semua file .kt
    python3 scripts/check_braces.py path.kt    # cek file tertentu

Exit code 1 kalau ada file yang tidak seimbang.
"""
from __future__ import annotations

import sys
from pathlib import Path

SRC = Path(__file__).resolve().parent.parent / "app/src/main/java"


def balance(text: str) -> tuple[int, int]:
    """Kembalikan (selisih_kurawal, selisih_kurung) dengan mengabaikan
    isi string, komentar //, dan komentar /* */."""
    curly = paren = 0
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if ch == "/" and i + 1 < n and text[i + 1] == "/":
            i = text.find("\n", i)
            if i == -1:
                break
            continue
        if ch == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            i = n if j == -1 else j + 2
            continue
        if ch == '"':
            if text.startswith('"""', i):
                j = text.find('"""', i + 3)
                i = n if j == -1 else j + 3
                continue
            i += 1
            while i < n and text[i] != '"':
                if text[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        if ch == "'":
            i += 1
            while i < n and text[i] != "'":
                if text[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        if ch == "{":
            curly += 1
        elif ch == "}":
            curly -= 1
        elif ch == "(":
            paren += 1
        elif ch == ")":
            paren -= 1
        i += 1
    return curly, paren


def main(argv: list[str]) -> int:
    targets = [Path(a) for a in argv[1:]] or sorted(SRC.rglob("*.kt"))
    bad = 0
    for path in targets:
        curly, paren = balance(path.read_text(encoding="utf-8"))
        if curly or paren:
            bad += 1
            print(f"TIDAK SEIMBANG  {path}: kurung kurawal {curly:+d}, kurung biasa {paren:+d}")
    print(f"diperiksa {len(targets)} file — {bad} bermasalah")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
