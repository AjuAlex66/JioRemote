package com.jioremote.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3D4B9C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E4FF),
    onPrimaryContainer = Color(0xFF202B73),
    secondary = Color(0xFF41645D),
    secondaryContainer = Color(0xFFCAEDE1),
    onSecondaryContainer = Color(0xFF123E34),
    background = Color(0xFFF6F6FC),
    onBackground = Color(0xFF191B2A),
    surface = Color(0xFFFEFBFF),
    onSurface = Color(0xFF191B2A),
    surfaceVariant = Color(0xFFE6E5F0),
    onSurfaceVariant = Color(0xFF474A60),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFEEF8),
    surfaceContainerHigh = Color(0xFFE8E8F3),
    outline = Color(0xFF767990),
    outlineVariant = Color(0xFFD1D2E1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF222F79),
    primaryContainer = Color(0xFF35438E),
    onPrimaryContainer = Color(0xFFE1E5FF),
    secondary = Color(0xFFA8D2C5),
    secondaryContainer = Color(0xFF234F45),
    onSecondaryContainer = Color(0xFFC5F1E3),
    background = Color(0xFF111322),
    onBackground = Color(0xFFF0EFFF),
    surface = Color(0xFF181B2D),
    onSurface = Color(0xFFF0EFFF),
    surfaceVariant = Color(0xFF31354E),
    onSurfaceVariant = Color(0xFFC6C8E0),
    surfaceContainerLow = Color(0xFF181B2D),
    surfaceContainer = Color(0xFF202337),
    surfaceContainerHigh = Color(0xFF282C42),
    outline = Color(0xFF9195AF),
    outlineVariant = Color(0xFF424762),
)

private val RemoteShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
internal fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = RemoteShapes,
        content = content,
    )
}
