package com.jsf.app.pravin.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CyanPrimary,
    onPrimary = TextDark,
    primaryContainer = CyanPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = CyanPrimary,
    secondary = GreenSuccess,
    onSecondary = Color.White,
    secondaryContainer = GreenSuccess.copy(alpha = 0.12f),
    onSecondaryContainer = GreenSuccess,
    tertiary = OrangeWarning,
    background = LightBg,
    onBackground = TextDark,
    surface = LightCard,
    onSurface = TextDark,
    surfaceVariant = LightSurface,
    onSurfaceVariant = TextGray,
    outline = LightBorder,
    error = RedDanger,
    onError = Color.White
)

@Composable
fun PravinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
