package com.zenn889.putar.ui

import androidx.media3.ui.AspectRatioFrameLayout
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mode tampilan video (V5). Yang dijaga di sini: urutan putarannya benar dan
 * tiap label menunjuk ke mode ExoPlayer yang tepat — mudah tertukar saat
 * menyunting, dan salah pasang baru terasa sebagai "gambarnya kok melar".
 */
class FitModeTest {

    @Test
    fun `urutan mode adalah Fit, Isi, Zoom`() {
        assertEquals(listOf("Fit", "Isi", "Zoom"), FIT_MODES.map { it.label })
    }

    @Test
    fun `tiap label menunjuk mode ExoPlayer yang benar`() {
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT, FIT_MODES[0].resize)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, FIT_MODES[1].resize)
        assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, FIT_MODES[2].resize)
    }

    @Test
    fun `putaran tombol kembali ke Fit setelah Zoom`() {
        var mode = 0
        val urut = mutableListOf(FIT_MODES[mode].label)
        repeat(3) {
            mode = (mode + 1) % FIT_MODES.size
            urut.add(FIT_MODES[mode].label)
        }
        assertEquals(listOf("Fit", "Isi", "Zoom", "Fit"), urut)
    }
}
