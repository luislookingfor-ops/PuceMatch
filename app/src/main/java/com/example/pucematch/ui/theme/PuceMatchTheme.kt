package com.example.pucematch.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Paleta de Colores Premium - Inspirada en la PUCE (Azul y Dorado)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF003554),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD2E4FF),
    onPrimaryContainer = Color(0xFF001D35),
    secondary = Color(0xFFD4AF37),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFF0CB),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF006B9A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD1E8FF),
    onTertiaryContainer = Color(0xFF001D2D),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF003554),
    primaryContainer = Color(0xFF004D74),
    onPrimaryContainer = Color(0xFFD2E4FF),
    secondary = Color(0xFFF3C63F),
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = Color(0xFF564500),
    onSecondaryContainer = Color(0xFFFFF0CB),
    tertiary = Color(0xFF90D0FF),
    onTertiary = Color(0xFF003553),
    tertiaryContainer = Color(0xFF004F75),
    onTertiaryContainer = Color(0xFFD1E8FF),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/**
 * Tema Material3 de PuceMatch.
 * Soporta Dynamic Color (Material You) en Android 12+.
 */
@Composable
fun PuceMatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
