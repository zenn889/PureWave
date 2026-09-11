package com.zenn889.putar.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Token desain PureWave — "premium dark".
 *
 * Sebelum ini ukuran sudut, bayangan, dan durasi animasi ditulis langsung di
 * tiap composable (radius 2, 9, 10, 13, 14, 15, 16, 18, 20 dp semuanya ada),
 * sehingga permukaan yang mirip terlihat berbeda-beda dan sulit disetel
 * seragam. Semua nilai visual sekarang diambil dari sini supaya satu
 * perubahan berlaku di seluruh aplikasi.
 *
 * Aturan pakai:
 * - [Radius.sm] 54 dp ke bawah (artwork baris daftar, chip)
 * - [Radius.md] permukaan sedang (kartu, kolom isian, baris yang disorot)
 * - [Radius.lg] kartu besar, mini player, dan artwork layar pemutar
 * - [Radius.pill] segalanya yang berbentuk pil (tombol putar, filter)
 */

object Radius {
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 22.dp
    val xl: Dp = 30.dp
    val pill: Dp = 999.dp
}

/** Jarak antar elemen. Skala 4 dp supaya rapi tanpa pikir. */
object Space {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
}

/** Tingkat bayangan. Tiga tingkat saja supaya kedalaman terasa berlapis. */
object Elev {
    /** kartu di atas latar (album, chip besar) */
    val card: Dp = 6.dp

    /** mengapung jelas (mini player, tombol putar) */
    val raised: Dp = 14.dp

    /** titik fokus (artwork layar pemutar) */
    val hero: Dp = 30.dp
}

/** Durasi gerak (ms). Selalu pakai ini, jangan angka telanjang. */
object Motion {
    /** tekan/lepas, ganti ikon */
    const val quick = 160

    /** buka-tutup, ganti tab */
    const val base = 280

    /** pendar/putaran lambat di belakang artwork */
    const val slow = 900
}

/**
 * Balasan sentuh ala aplikasi modern: permukaan mengecil sedikit saat ditekan
 * lalu memantul balik dengan pegas. Dipakai pada kartu dan tombol besar.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressed: Float = 0.97f
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 900f),
        label = "pressScale"
    )
    this.scale(scale)
}
