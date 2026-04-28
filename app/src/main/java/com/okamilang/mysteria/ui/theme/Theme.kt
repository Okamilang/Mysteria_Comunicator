package com.okamilang.mysteria.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MysteriaColorScheme = darkColorScheme(
    primary = LaitonClair,
    onPrimary = Encre,
    primaryContainer = Laiton,
    onPrimaryContainer = Encre,
    secondary = RougeSceau,
    onSecondary = Papier,
    secondaryContainer = RougeVif,
    onSecondaryContainer = Papier,
    tertiary = VertOcculte,
    onTertiary = Papier,
    background = BrunTresSombre,
    onBackground = Papier,
    surface = BrunFonce,
    onSurface = Papier,
    surfaceVariant = Papier,
    onSurfaceVariant = Encre,
    error = RougeVif,
    onError = Papier,
    outline = LaitonSombre
)

@Composable
fun MysteriaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MysteriaColorScheme,
        typography = MysteriaTypography,
        content = content
    )
}
