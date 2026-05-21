package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.White,
    secondary = CyberLime,
    onSecondary = Color.White,
    tertiary = NeonPink,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkCardBg,
    onSurface = TextPrimary,
    surfaceVariant = DarkCardBorder,
    onSurfaceVariant = TextSecondary,
    error = NeonPink,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFF6F8FA),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for premium Bento Grid aesthetic
    dynamicColor: Boolean = false, // Disable dynamic colors to preserve our tailored Cyber theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
