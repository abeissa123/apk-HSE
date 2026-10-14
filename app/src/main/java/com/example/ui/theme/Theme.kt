package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HSELightColorScheme =
  lightColorScheme(
    primary = HSEBluePrimary,           // #1B7F4B Vert forêt
    onPrimary = Color.White,
    primaryContainer = HSEBlueDark,     // #0F5132 Vert profond
    onPrimaryContainer = Color.White,
    secondary = HSEBlueLight,           // #1565C0 Bleu secondaire
    onSecondary = Color.White,
    secondaryContainer = HSEHealthBg,   // #E3F2FD Fond bleu très clair
    onSecondaryContainer = HSETextPrimary,
    tertiary = HSEOrangeAlert,          // #F57C00 Orange sécurité
    onTertiary = Color.White,
    background = HSEDarkBg,             // #F5F7F6 Fond de l'écran
    onBackground = HSETextPrimary,      // #1C1F1E Texte principal
    surface = HSECardBg,                // #FFFFFF Cartes
    onSurface = HSETextPrimary,         // #1C1F1E Texte principal
    surfaceVariant = HSECardElevated,   // #E8F5E9 Fond vert clair
    onSurfaceVariant = HSETextSecondary,// #5F6B66 Texte secondaire
    outline = HSECardBorder,            // #DDE3E0 Bordures
    error = HSERedUrgent,               // #D32F2F Rouge danger
    onError = Color.White,
  )

private val HSEDarkColorScheme =
  darkColorScheme(
    primary = HSEDarkModeGreen,         // #66BB6A Vert éclairci mode sombre
    onPrimary = Color(0xFF1C1F1E),
    primaryContainer = HSEBlueDark,     // #0F5132 Vert profond
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF0288D1),      // #0288D1 Bleu clair
    onSecondary = Color.White,
    tertiary = HSEOrangeAlert,          // #F57C00 Orange sécurité
    onTertiary = Color.White,
    background = Color(0xFF121715),
    onBackground = Color(0xFFF5F7F6),
    surface = Color(0xFF1C2420),
    onSurface = Color(0xFFF5F7F6),
    surfaceVariant = Color(0xFF26332D),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF37474F),
    error = HSEDarkModeRed,             // #EF5350 Rouge éclairci mode sombre
    onError = Color.White,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) HSEDarkColorScheme else HSELightColorScheme,
    typography = Typography,
    content = content
  )
}


