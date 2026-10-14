package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FokusElectricLime,
    onPrimary = FokusOnLime,
    primaryContainer = FokusLimeContainer,
    onPrimaryContainer = FokusElectricLime,
    secondary = FokusBlue,
    onSecondary = FokusDarkBackground,
    secondaryContainer = FokusBlueContainer,
    onSecondaryContainer = FokusBlue,
    tertiary = FokusAmber,
    onTertiary = FokusDarkBackground,
    tertiaryContainer = FokusAmberContainer,
    onTertiaryContainer = FokusAmber,
    error = FokusCoral,
    onError = FokusDarkBackground,
    errorContainer = FokusCoralContainer,
    onErrorContainer = FokusCoral,
    background = FokusDarkBackground,
    onBackground = FokusTextPrimaryDark,
    surface = FokusDarkSurface,
    onSurface = FokusTextPrimaryDark,
    surfaceVariant = FokusDarkSurfaceVariant,
    onSurfaceVariant = FokusTextSecondaryDark,
    outline = FokusDarkBorder,
    outlineVariant = FokusDarkBorderSubtle
)

private val LightColorScheme = lightColorScheme(
    primary = FokusLimeLightPrimary,
    onPrimary = FokusLightSurface,
    primaryContainer = FokusLimeLightContainer,
    onPrimaryContainer = FokusOnLimeLightContainer,
    secondary = FokusBlue,
    onSecondary = FokusLightSurface,
    secondaryContainer = Color(0xFFD6E7FF),
    onSecondaryContainer = Color(0xFF003D82),
    tertiary = Color(0xFFB37400),
    onTertiary = FokusLightSurface,
    tertiaryContainer = Color(0xFFFFECC7),
    onTertiaryContainer = Color(0xFF422800),
    error = Color(0xFFBA1A1A),
    onError = FokusLightSurface,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = FokusLightBackground,
    onBackground = FokusTextPrimaryLight,
    surface = FokusLightSurface,
    onSurface = FokusTextPrimaryLight,
    surfaceVariant = FokusLightSurfaceVariant,
    onSurfaceVariant = FokusTextSecondaryLight,
    outline = FokusLightBorder,
    outlineVariant = FokusLightBorderSubtle
)

@Composable
fun FokusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
