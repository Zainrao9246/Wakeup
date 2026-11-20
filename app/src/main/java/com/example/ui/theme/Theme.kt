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

private val DarkColorScheme = darkColorScheme(
    primary = DawnAmber,
    onPrimary = DawnAmberDeep,
    primaryContainer = DawnAmberContainer,
    onPrimaryContainer = DawnAmberBright,
    secondary = TwilightTeal,
    onSecondary = Color(0xFF04221F),
    secondaryContainer = TwilightTealContainer,
    onSecondaryContainer = TwilightTeal,
    tertiary = DawnAmberBright,
    onTertiary = DawnAmberDeep,
    error = DawnCoral,
    onError = Color(0xFF2D0610),
    errorContainer = DawnCoralContainer,
    onErrorContainer = Color(0xFFFFCCD5),
    background = MidnightBg,
    onBackground = IvoryText,
    surface = MidnightSurface,
    onSurface = IvoryText,
    surfaceVariant = MidnightSurfaceHigh,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = MidnightBg,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = MidnightSurfaceHigh,
    surfaceContainerHigh = MidnightSurfaceHighest,
    outline = MidnightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = AmberGold,
    onPrimary = Color.White,
    primaryContainer = AmberGoldSoft,
    onPrimaryContainer = Color(0xFF451A03),
    secondary = DeepTeal,
    onSecondary = Color.White,
    secondaryContainer = DeepTealSoft,
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = IndigoInk,
    onTertiary = Color.White,
    error = AlertCrimson,
    onError = Color.White,
    errorContainer = AlertCrimsonSoft,
    onErrorContainer = Color(0xFF4C0519),
    background = AlabasterBg,
    onBackground = IndigoInk,
    surface = AlabasterSurface,
    onSurface = IndigoInk,
    surfaceVariant = AlabasterSurfaceHigh,
    onSurfaceVariant = WarmSlate,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = AlabasterBg,
    surfaceContainer = AlabasterSurfaceHigh,
    surfaceContainerHigh = Color(0xFFE7DFD1),
    outline = AlabasterOutline
)

val WakeProofShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun WakeProofTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = WakeProofTypography,
        shapes = WakeProofShapes,
        content = content
    )
}
