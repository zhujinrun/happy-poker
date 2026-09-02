package com.happy.poker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Green400,
    secondary = Gold500,
    tertiary = Red500,
    background = PokerAppBackground,
    surface = PokerAppBackground,
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
    background = PokerAppBackground,
    surface = PokerAppBackground,
    onPrimary = CardWhite,
    onSecondary = CardBlack,
    onTertiary = CardWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
