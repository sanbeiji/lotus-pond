package com.example.lotuspondreader.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = LotusPrimaryDark,
    onPrimary = Color.White,
    secondary = LotusAccent,
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2C2C2C),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF3C3C3C),
    onSurfaceVariant = Color(0xFFAAAAAA)
)

private val LightColorScheme = lightColorScheme(
    primary = LotusPrimary,
    onPrimary = Color.White,
    secondary = LotusAccent,
    background = LotusBackground,
    surface = LotusCardBackground,
    onBackground = LotusText,
    onSurface = LotusText,
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = LotusTextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set to false to use our custom Lotus Pond colors by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
