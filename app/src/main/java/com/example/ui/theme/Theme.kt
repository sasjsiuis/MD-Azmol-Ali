package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NewsPrimaryRed,
    secondary = NewsAccentTeal,
    tertiary = TextSecondaryDark,
    background = EditorialDarkBg,
    surface = EditorialDarkSurface,
    onPrimary = TextPrimaryDark,
    onSecondary = EditorialDarkBg,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

// Extra fallback color definition for light theme onText
private val ColorSecondaryForLightText = androidx.compose.ui.graphics.Color(0xFF24292E)

private val LightColorScheme = lightColorScheme(
    primary = NewsPrimaryLight,
    secondary = NewsSecondaryLight,
    tertiary = NewsAccentTeal,
    background = EditorialLightBg,
    surface = EditorialLightSurface,
    onPrimary = EditorialLightSurface,
    onSecondary = EditorialLightBg,
    onBackground = ColorSecondaryForLightText,
    onSurface = ColorSecondaryForLightText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to retain our highly aesthetic news brand colors
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
