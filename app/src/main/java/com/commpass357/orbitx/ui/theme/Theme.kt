package com.commpass357.orbitx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OrbitDarkScheme = darkColorScheme(
    primary = Color(0xFF7ED7FF),
    onPrimary = Color(0xFF041B2B),
    secondary = Color(0xFFFFD36A),
    onSecondary = Color(0xFF231A00),
    tertiary = Color(0xFFFF8DBB),
    onTertiary = Color(0xFF32101E),
    background = Color(0xFF050711),
    onBackground = Color(0xFFEAF2FF),
    surface = Color(0xFF101624),
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = Color(0xFF1B2535),
    onSurfaceVariant = Color(0xFFC8D3E2),
    outline = Color(0xFF6F7C90)
)

@Composable
fun OrbitXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrbitDarkScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
