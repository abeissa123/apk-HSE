package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HSEDarkColorScheme =
  darkColorScheme(
    primary = HSEBluePrimary,
    onPrimary = Color.White,
    primaryContainer = HSEBlueDark,
    onPrimaryContainer = Color.White,
    secondary = HSEGreenSafe,
    onSecondary = Color.White,
    tertiary = HSEOrangeAlert,
    background = HSEDarkBg,
    onBackground = HSETextPrimary,
    surface = HSECardBg,
    onSurface = HSETextPrimary,
    surfaceVariant = HSECardElevated,
    onSurfaceVariant = HSETextSecondary,
    error = HSERedUrgent,
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = HSEDarkColorScheme,
    typography = Typography,
    content = content
  )
}

