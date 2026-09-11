package com.zenn889.putar.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/** Tipografi khas PureWave — sedikit lebih rapat & tegas (kesan premium). */
private val PureWaveTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 0.3.sp
    )
)

/* ---------- token tema: dibaca per-komposisi dari palet aktif ---------- */

val Coral: Color get() = Palette.accent
val CoralBright: Color get() = Palette.accentBright
val Ink: Color get() = Palette.ink
val MutedInk: Color get() = Palette.mutedInk
val FaintInk: Color get() = Palette.faintInk
val Bg: Color get() = Palette.bg
val Surface: Color get() = Palette.surface
val SurfaceHigh: Color get() = Palette.surfaceHigh

/** Palet aktif — diisi tiap PutarTheme dipanggil (mode tema & aksen). */
object Palette {
    var accent: Color = Color(0xFFFF5A36)
    var accentBright: Color = Color(0xFFFF6F4F)
    var ink: Color = Color(0xFFF5F4F0)
    var mutedInk: Color = Color(0xFFAAA9A3)
    var faintInk: Color = Color(0xFF7C7D83)
    var bg: Color = Color(0xFF101218)
    var surface: Color = Color(0xFF171A21)
    var surfaceHigh: Color = Color(0xFF20242D)
    var outline: Color = Color(0xFF2F333B)
    var error: Color = Color(0xFFFF6F5E)
}

/* ---------- pilihan mode & aksen ---------- */

enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Ikuti sistem"),
    LIGHT("light", "Terang"),
    DARK("dark", "Gelap"),
    OLED("oled", "Gelap murni (OLED)")
}

enum class AccentChoice(val key: String, val label: String) {
    CORAL("coral", "Coral"),
    MINT("mint", "Mint"),
    SKY("sky", "Biru langit"),
    VIOLET("violet", "Violet"),
    GOLD("gold", "Emas")
}

fun themePrefs(context: Context) =
    context.getSharedPreferences("putar_prefs", Context.MODE_PRIVATE)

fun themeModeFromPrefs(context: Context): ThemeMode =
    themePrefs(context).getString("theme_mode", null)?.let { key ->
        ThemeMode.entries.firstOrNull { it.key == key }
    } ?: ThemeMode.DARK

fun accentFromPrefs(context: Context): AccentChoice =
    themePrefs(context).getString("theme_accent", null)?.let { key ->
        AccentChoice.entries.firstOrNull { it.key == key }
    } ?: AccentChoice.CORAL

private data class AccentSet(
    val main: Color, val bright: Color,
    val on: Color, val container: Color, val onContainer: Color
)

private fun accentSet(choice: AccentChoice, dark: Boolean): AccentSet = when (choice) {
    AccentChoice.CORAL -> AccentSet(
        Color(0xFFFF5A36), Color(0xFFFF6F4F),
        if (dark) Color(0xFF190902) else Color(0xFFFFFFFF),
        if (dark) Color(0xFF3A160C) else Color(0xFFFFD9CE),
        if (dark) Color(0xFFFF8A6B) else Color(0xFF571400)
    )
    AccentChoice.MINT -> AccentSet(
        Color(0xFF1FBF8F), Color(0xFF3ED4A6),
        if (dark) Color(0xFF00211A) else Color(0xFFFFFFFF),
        if (dark) Color(0xFF00382B) else Color(0xFFB7F4DE),
        if (dark) Color(0xFF6FE4C0) else Color(0xFF00382B)
    )
    AccentChoice.SKY -> AccentSet(
        Color(0xFF3D8BFF), Color(0xFF66A5FF),
        if (dark) Color(0xFF002A6B) else Color(0xFFFFFFFF),
        if (dark) Color(0xFF0A3E8C) else Color(0xFFD6E6FF),
        if (dark) Color(0xFF9CC6FF) else Color(0xFF002A6B)
    )
    AccentChoice.VIOLET -> AccentSet(
        Color(0xFF8B5CF6), Color(0xFFA78BFA),
        if (dark) Color(0xFF2A0A5C) else Color(0xFFFFFFFF),
        if (dark) Color(0xFF3B1D7A) else Color(0xFFE5D8FF),
        if (dark) Color(0xFFC5B3FF) else Color(0xFF2A0A5C)
    )
    AccentChoice.GOLD -> AccentSet(
        Color(0xFFE8A33D), Color(0xFFF5BD66),
        if (dark) Color(0xFF3B2400) else Color(0xFFFFFFFF),
        if (dark) Color(0xFF5C3A05) else Color(0xFFFCE6BD),
        if (dark) Color(0xFFFFD489) else Color(0xFF5C3A05)
    )
}

private fun applyPalette(
    dark: Boolean, oled: Boolean,
    accent: Color, bright: Color,
    container: Color, onContainer: Color
) {
    Palette.accent = accent
    Palette.accentBright = bright
    Palette.error = if (dark) Color(0xFFFF6F5E) else Color(0xFFB3261E)
    if (dark) {
        Palette.ink = Color(0xFFF5F4F0)
        Palette.mutedInk = Color(0xFFA9A8A2)
        Palette.faintInk = Color(0xFF7A7A82)
        Palette.bg = if (oled) Color(0xFF000000) else Color(0xFF0D0F14)
        Palette.surface = if (oled) Color(0xFF0A0A0C) else Color(0xFF151820)
        Palette.surfaceHigh = if (oled) Color(0xFF141416) else Color(0xFF1F232C)
        Palette.outline = Color(0xFF2B3038)
    } else {
        Palette.ink = Color(0xFF1B1B1E)
        Palette.mutedInk = Color(0xFF5E5E5A)
        Palette.faintInk = Color(0xFF94938D)
        Palette.bg = Color(0xFFF6F5F1)
        Palette.surface = Color(0xFFFFFFFF)
        Palette.surfaceHigh = Color(0xFFEBEAE4)
        Palette.outline = Color(0xFFC9C8C2)
    }
}

private fun schemeFor(
    dark: Boolean, accentSet: AccentSet
) = if (dark) darkColorScheme(
    primary = accentSet.main,
    onPrimary = accentSet.on,
    primaryContainer = accentSet.container,
    onPrimaryContainer = accentSet.onContainer,
    secondary = accentSet.bright,
    background = Palette.bg,
    onBackground = Palette.ink,
    surface = Palette.surface,
    onSurface = Palette.ink,
    surfaceVariant = Palette.surfaceHigh,
    onSurfaceVariant = Palette.mutedInk,
    outline = Palette.outline,
    error = Palette.error
) else lightColorScheme(
    primary = accentSet.main,
    onPrimary = accentSet.on,
    primaryContainer = accentSet.container,
    onPrimaryContainer = accentSet.onContainer,
    secondary = accentSet.bright,
    background = Palette.bg,
    onBackground = Palette.ink,
    surface = Palette.surface,
    onSurface = Palette.ink,
    surfaceVariant = Palette.surfaceHigh,
    onSurfaceVariant = Palette.mutedInk,
    outline = Palette.outline,
    error = Palette.error
)

@Composable
fun PutarTheme(content: @Composable () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mode = themeModeFromPrefs(context)
    val accentKey = accentFromPrefs(context)
    val useDynamic = themePrefs(context).getBoolean("theme_dynamic", false) &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.OLED -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val oled = mode == ThemeMode.OLED

    val scheme = if (useDynamic) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        val acc = accentSet(accentKey, dark)
        applyPalette(dark, oled, acc.main, acc.bright, acc.container, acc.onContainer)
        schemeFor(dark, acc)
    }
    if (useDynamic) {
        // ikut warna wallpaper + mode
        val d = scheme
        applyPalette(
            dark, oled,
            d.primary, d.secondary,
            d.primaryContainer, d.onPrimaryContainer
        )
    }

    // ikon sistem menyesuaikan tema app (bukan tema sistem)
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(dark, useDynamic) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val ctl = WindowCompat.getInsetsController(window, view)
                ctl.isAppearanceLightStatusBars = !dark
                ctl.isAppearanceLightNavigationBars = !dark
            }
            onDispose {}
        }
    }

    // Penting: MaterialTheme TIDAK menetapkan LocalContentColor. Bawaannya
    // hitam, sehingga Text tanpa `color =` menjadi hitam di atas latar gelap
    // dan tidak terlihat (kejadian di baris artis/folder/video). Ditetapkan di
    // sini supaya seluruh aplikasi punya tinta yang benar tanpa harus setiap
    // Text menyebut warnanya sendiri.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides scheme.onBackground
    ) {
        MaterialTheme(colorScheme = scheme, typography = PureWaveTypography, content = content)
    }
}
