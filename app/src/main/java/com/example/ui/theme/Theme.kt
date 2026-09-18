package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = SophisticatedPrimary,
    onPrimary = SophisticatedOnPrimary,
    background = SophisticatedBackground,
    onBackground = SophisticatedOnBackground,
    surface = SophisticatedSurface,
    onSurface = SophisticatedOnBackground,
    surfaceVariant = SophisticatedSurface,
    onSurfaceVariant = SophisticatedSecondary,
    secondary = SophisticatedSecondary,
    outline = SophisticatedOutline
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(colorScheme = DarkColorScheme, typography = Typography, content = content)
}
