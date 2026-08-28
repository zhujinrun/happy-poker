package com.happy.poker.app.ui.theme

import android.app.Activity
import android.graphics.Color.TRANSPARENT
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Green400,
    secondary = Gold500,
    tertiary = Red500,
    background = TableGradientStart,
    surface = Green800,
    onPrimary = CardBlack,
    onSecondary = CardBlack,
    onTertiary = CardWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val LightColorScheme = lightColorScheme(
    primary = Green600,
    secondary = Gold700,
    tertiary = Red700,
    background = Green400,
    surface = CardWhite,
    onPrimary = CardWhite,
    onSecondary = CardBlack,
    onTertiary = CardWhite,
    onBackground = CardBlack,
    onSurface = CardBlack
)

@Composable
fun HappyPokerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        @Suppress("DEPRECATION")
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TRANSPARENT
            window.navigationBarColor = TRANSPARENT
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
