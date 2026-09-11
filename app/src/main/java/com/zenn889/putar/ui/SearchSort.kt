package com.zenn889.putar.ui

import com.zenn889.putar.data.Track

/**
 * Logika sortir & pencarian pustaka — sengaja dipisah dari MainActivity dan
 * dibuat murni (tanpa Android) supaya bisa diuji unit di JVM.
 * Tesnya: app/src/test/java/com/zenn889/putar/ui/SearchSortTest.kt
 */

/**
 * Urutkan daftar lagu. `plays` (jumlah putar) dan `recency` (posisi di daftar
 * terakhir diputar, 0 = paling baru) datang dari StatsStore dan hanya dipakai
 * dua opsi terakhir.
 */
internal fun sortedTracks(
    list: List<Track>,
    sort: SortOption,
    plays: Map<String, Int> = emptyMap(),
    recency: Map<String, Int> = emptyMap()
): List<Track> = when (sort) {
    SortOption.JUDUL -> list.sortedBy { it.title.lowercase() }
    SortOption.JUDUL_ZA -> list.sortedByDescending { it.title.lowercase() }
    SortOption.ARTIS -> list.sortedWith(
        compareBy({ it.displayArtist.lowercase() }, { it.title.lowercase() })
    )
    SortOption.ALBUM -> list.sortedWith(
        compareBy({ (it.albumTitle ?: "").lowercase() }, { it.title.lowercase() })
    )
    SortOption.TERBARU -> list.sortedByDescending { it.dateAddedMs }
    SortOption.TERLAMA -> list.sortedBy { it.dateAddedMs }
    SortOption.DURASI -> list.sortedBy { it.durationMs }
    SortOption.DURASI_PANJANG -> list.sortedByDescending { it.durationMs }
    SortOption.SERING -> list.sortedWith(
        compareByDescending<Track> { plays[it.contentUri] ?: 0 }
            .thenBy { it.title.lowercase() }
    )
    SortOption.TERAKHIR -> list.sortedWith(
        compareBy<Track> { recency[it.contentUri] ?: Int.MAX_VALUE }
            .thenBy { it.title.lowercase() }
    )
}

/* ---------- pencarian: abaikan huruf besar/kecil & tanda diakritik ---------- */

/** Normalisasi teks untuk pencarian: huruf kecil + tanda diakritik dibuang. */
internal fun normText(s: String): String =
    java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase()

/** Kata kunci dipecah jadi token: "sheila adu" menemukan "Adu - Sheila On 7". */
internal fun searchTokens(q: String): List<String> =
    normText(q).split(' ', '\t', '\n').filter { it.isNotBlank() }

/** True bila semua token muncul di teks yang dicari (urutan tidak penting). */
internal fun matchesTokens(text: String, tokens: List<String>): Boolean {
    if (tokens.isEmpty()) return true
    val hay = normText(text)
    return tokens.all { hay.contains(it) }
}

/** Cari di judul, artis, album, dan folder sekaligus. */
internal fun Track.matchesQuery(tokens: List<String>): Boolean =
    matchesTokens(
        listOfNotNull(title, displayArtist, albumTitle, folder).joinToString(" "),
        tokens
    )
