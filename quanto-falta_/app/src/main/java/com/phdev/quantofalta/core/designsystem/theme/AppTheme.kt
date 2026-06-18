package com.phdev.quantofalta.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = BlueSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = LightBlue,
    background = GrayBackground,
    onBackground = TextPrimaryLight,
    surface = androidx.compose.ui.graphics.Color.White,
    onSurface = TextPrimaryLight,
    surfaceVariant = PurpleBackground,
    onSurfaceVariant = TextPrimaryLight,
    outline = TextSecondaryLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = BlueSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    tertiary = LightBlue,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextPrimaryDark,
    outline = TextSecondaryDark
)

private val DeepPurpleColorScheme = darkColorScheme(
    primary = DeepPurplePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = DeepPurpleSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = DeepPurpleBackground,
    onBackground = TextPrimaryDark,
    surface = DeepPurpleSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DeepPurpleSurfaceVariant,
    onSurfaceVariant = TextPrimaryDark,
    outline = TextSecondaryDark
)

private val OceanBlueColorScheme = darkColorScheme(
    primary = OceanBluePrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = OceanBlueSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = OceanBlueBackground,
    onBackground = TextPrimaryDark,
    surface = OceanBlueSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = OceanBlueSurfaceVariant,
    onSurfaceVariant = TextPrimaryDark,
    outline = TextSecondaryDark
)

private val SereneGreenColorScheme = lightColorScheme(
    primary = SereneGreenPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = SereneGreenSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = SereneGreenBackground,
    onBackground = SereneGreenTextDark,
    surface = SereneGreenSurface,
    onSurface = SereneGreenTextDark,
    surfaceVariant = SereneGreenSurfaceVariant,
    onSurfaceVariant = SereneGreenTextMedium,
    outline = SereneGreenTextMedium.copy(alpha = 0.5f)
)

enum class AppThemeMode(val isPremium: Boolean, val displayName: String) {
    SYSTEM(false, "Sistema"),
    LIGHT(false, "Claro"),
    DARK(false, "Escuro"),
    DEEP_PURPLE(true, "Roxo Profundo"),
    OCEAN_BLUE(true, "Azul Oceano"),
    SERENE_GREEN(true, "Verde Sereno")
}

@Composable
fun AppTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    
    val colorScheme = when (themeMode) {
        AppThemeMode.SYSTEM -> if (isSystemDark) DarkColorScheme else LightColorScheme
        AppThemeMode.LIGHT -> LightColorScheme
        AppThemeMode.DARK -> DarkColorScheme
        AppThemeMode.DEEP_PURPLE -> DeepPurpleColorScheme
        AppThemeMode.OCEAN_BLUE -> OceanBlueColorScheme
        AppThemeMode.SERENE_GREEN -> SereneGreenColorScheme
    }

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT, AppThemeMode.SERENE_GREEN -> false
        AppThemeMode.DARK, AppThemeMode.DEEP_PURPLE, AppThemeMode.OCEAN_BLUE -> true
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
