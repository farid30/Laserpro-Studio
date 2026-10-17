package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LaserDarkColorScheme = darkColorScheme(
    primary = LaserPrimary,
    onPrimary = LaserOnPrimary,
    primaryContainer = LaserPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = LaserSecondary,
    onSecondary = Color.Black,
    tertiary = LaserTertiary,
    onTertiary = Color.Black,
    background = LaserDarkBg,
    onBackground = LaserTextPrimary,
    surface = LaserSurface,
    onSurface = LaserTextPrimary,
    surfaceVariant = LaserSurfaceElevated,
    onSurfaceVariant = LaserTextSecondary,
    outline = LaserBorder,
    outlineVariant = LaserBorderActive,
    error = LaserEmergencyRed,
    onError = Color.White
)

@Composable
fun LaserProStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LaserDarkColorScheme,
        typography = Typography,
        content = content
    )
}
