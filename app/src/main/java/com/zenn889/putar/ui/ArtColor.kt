package com.zenn889.putar.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Warna dinamis dari sampul album (ala Spotify/Apple Music).
 *
 * Sampul dimuat lewat coil, warna dominannya diambil dengan Palette, lalu
 * disimpan di cache memori (per content-URI) supaya tidak dihitung ulang
 * setiap recompose. Semua di IO dispatcher, jadi tidak mengganggu UI.
 */
private val artColorCache = HashMap<String, Int>()

@Composable
fun rememberArtColor(uri: Uri?): Color? {
    val context = LocalContext.current
    var color by remember(uri) {
        mutableStateOf(uri?.let { artColorCache[it.toString()] }?.let { Color(it) })
    }
    LaunchedEffect(uri) {
        if (uri == null) {
            color = null
            return@LaunchedEffect
        }
        val key = uri.toString()
        artColorCache[key]?.let {
            color = Color(it)
            return@LaunchedEffect
        }
        val rgb = withContext(Dispatchers.IO) { extractColor(context, uri) }
        if (rgb != null) {
            artColorCache[key] = rgb
            color = Color(rgb)
        }
    }
    return color
}

private suspend fun extractColor(context: Context, uri: Uri): Int? = runCatching {
    val request = ImageRequest.Builder(context)
        .data(uri)
        .allowHardware(false)
        .size(140)
        .build()
    val drawable = context.imageLoader.execute(request).drawable
    val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: return@runCatching null
    val palette = Palette.from(bitmap).maximumColorCount(16).generate()
    val swatch = palette.vibrantSwatch
        ?: palette.lightVibrantSwatch
        ?: palette.dominantSwatch
        ?: palette.mutedSwatch
    swatch?.rgb
}.getOrNull()

/**
 * Dua variasi tonal dari warna sampul:
 * - `deep` : versi gelap untuk dasar latar layar pemutar
 * - `bright`: versi terang-jenuh untuk aksen (bar progres, tombol putar, pendar)
 */
fun tonalPair(base: Color): Pair<Color, Color> {
    val r = base.red
    val g = base.green
    val b = base.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val hue = when {
        d == 0f -> 0f
        max == r -> (((g - b) / d).mod(6f)) * 60f
        max == g -> ((b - r) / d + 2f) * 60f
        else -> ((r - g) / d + 4f) * 60f
    }
    val sat = if (max == 0f) 0f else d / max
    val hNorm = ((hue / 360f) % 1f + 1f) % 1f

    fun hsvToColor(h: Float, s: Float, v: Float): Color {
        val i = (h * 6f).toInt()
        val f = h * 6f - i
        val p = v * (1f - s)
        val q = v * (1f - f * s)
        val t = v * (1f - (1f - f) * s)
        val (rr, gg, bb) = when (i % 6) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        return Color(rr, gg, bb)
    }

    val s = sat.coerceIn(0.34f, 0.90f)
    val deep = hsvToColor(hNorm, (s * 0.95f).coerceAtMost(0.92f), 0.22f)
    val bright = hsvToColor(hNorm, (s * 1.15f).coerceAtMost(1f), 0.74f)
    return deep to bright
}
