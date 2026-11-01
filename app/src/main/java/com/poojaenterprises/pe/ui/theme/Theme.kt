package com.poojaenterprises.pe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = RedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = RedDark,
    secondary = GoldPrimary,
    onSecondary = Color.White,
    secondaryContainer = GoldBg,
    onSecondaryContainer = GoldDark,
    background = Color(0xFFF9F9FA),
    onBackground = OnSurfaceBlack,
    surface = SurfaceWhite,
    onSurface = OnSurfaceBlack,
    surfaceVariant = SurfaceVariantGray,
    onSurfaceVariant = Color(0xFF444444),
    outline = OutlineGray
)

@Composable
fun PETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // For optimal contrast in daytime shop lighting, use clean light theme with warm accents
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
