package com.zenn889.putar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Coral = Color(0xFFFF5A36)
val CoralBright = Color(0xFFFF6F4F)
val Ink = Color(0xFFF5F4F0)
val MutedInk = Color(0xFFA4A29A)
val FaintInk = Color(0xFF71706A)
val Bg = Color(0xFF0E0F11)
val Surface = Color(0xFF15161A)
val SurfaceHigh = Color(0xFF1C1E23)

private val PutarColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color(0xFF190902),
    primaryContainer = Color(0xFF3A160C),
    onPrimaryContainer = CoralBright,
    secondary = CoralBright,
    onSecondary = Color(0xFF190902),
    background = Bg,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFF2A2C31),
    error = Color(0xFFFF6F5E)
)

@Composable
fun PutarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PutarColors,
        content = content
    )
}
