package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ChromaPulseDarkColorScheme = darkColorScheme(
    primary = ElectricViolet,
    onPrimary = TextPrimary,
    primaryContainer = ElectricVioletDeep,
    onPrimaryContainer = ElectricVioletBright,
    secondary = NeonEmerald,
    onSecondary = ObsidianCanvas,
    secondaryContainer = NeonEmeraldDeep,
    onSecondaryContainer = NeonEmeraldBright,
    tertiary = CoinGold,
    onTertiary = ObsidianCanvas,
    tertiaryContainer = ObsidianCardElevated,
    onTertiaryContainer = CoinGoldSoft,
    background = ObsidianCanvas,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianCanvas,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianCardElevated,
    surfaceContainerHighest = ObsidianBorder,
    outline = ObsidianBorder,
    outlineVariant = ObsidianBorder.copy(alpha = 0.55f),
    error = RubyRed,
    onError = TextPrimary,
    errorContainer = RubyRedDeep,
    onErrorContainer = RubyRedBright
)

val ChromaPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ChromaPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChromaPulseDarkColorScheme,
        typography = Typography,
        shapes = ChromaPulseShapes,
        content = content
    )
}
