package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AstraIndigo,
    onPrimary = Color.White,
    primaryContainer = AstraIndigoContainer,
    onPrimaryContainer = AstraOnIndigoContainer,
    secondary = NebulaViolet,
    onSecondary = Color.White,
    secondaryContainer = NebulaVioletContainer,
    onSecondaryContainer = NebulaOnVioletContainer,
    tertiary = ScholarAmber,
    onTertiary = Color.White,
    tertiaryContainer = ScholarAmberContainer,
    onTertiaryContainer = ScholarOnAmberContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceContainer,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = CoralError,
    onError = Color.White,
    errorContainer = CoralErrorContainer,
    onErrorContainer = CoralError
)

private val DarkColorScheme = darkColorScheme(
    primary = AstraIndigoLight,
    onPrimary = Color(0xFF0A104D),
    primaryContainer = Color(0xFF252F8F),
    onPrimaryContainer = Color(0xFFE0E4FF),
    secondary = NebulaVioletLight,
    onSecondary = Color(0xFF240B59),
    secondaryContainer = Color(0xFF3E1C8A),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = ScholarAmberLight,
    onTertiary = Color(0xFF3E1D00),
    tertiaryContainer = Color(0xFF5C3100),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceContainer,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = CoralErrorLight,
    onError = Color(0xFF450A0A),
    errorContainer = CoralDarkErrorContainer,
    onErrorContainer = Color(0xFFFEE2E2)
)

val AstraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AstraMacTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AstraShapes,
        content = content
    )
}
