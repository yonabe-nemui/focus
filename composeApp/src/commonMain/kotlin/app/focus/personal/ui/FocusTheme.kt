package app.focus.personal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Focus Green — derived from Android launcher #3DDC84, pulled toward forest/ink
private val AccentGreen = Color(0xFF2E7D5B)
private val AccentGreenLight = Color(0xFF8FBFA5)
private val AccentGreenContainer = Color(0xFFE6F1EB)
private val AccentGreenDark = Color(0xFF215F45)

// Neutrals (warm-cool paper)
private val Paper50 = Color(0xFFFAFAF7)
private val Paper100 = Color(0xFFF3F3EE)
private val Paper200 = Color(0xFFE5E5DE)
private val Paper700 = Color(0xFF3E3E39)
private val Paper900 = Color(0xFF14140F)

// Ink (dark surface tones)
private val Ink900 = Color(0xFF11120F)
private val Ink800 = Color(0xFF1C1D1A)
private val Ink700 = Color(0xFF2A2A25)
private val Ink500 = Color(0xFF6B6B64)

// Semantic
private val ErrorRed = Color(0xFFB3261E)
private val ErrorRedContainer = Color(0xFFFDECEB)

private val FocusLightColorScheme = lightColorScheme(
    primary = AccentGreen,
    onPrimary = Color.White,
    primaryContainer = AccentGreenContainer,
    onPrimaryContainer = Color(0xFF173F2E),
    secondary = Paper700,
    onSecondary = Color.White,
    secondaryContainer = Paper100,
    onSecondaryContainer = Paper900,
    tertiary = Ink500,
    onTertiary = Color.White,
    tertiaryContainer = Paper200,
    onTertiaryContainer = Paper900,
    background = Paper50,
    onBackground = Paper900,
    surface = Color.White,
    onSurface = Paper900,
    surfaceVariant = Paper100,
    onSurfaceVariant = Paper700,
    outline = Paper200,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFF410E0B),
)

private val FocusDarkColorScheme = darkColorScheme(
    primary = AccentGreenLight,
    onPrimary = Color(0xFF0F1A14),
    primaryContainer = AccentGreenDark,
    onPrimaryContainer = AccentGreenContainer,
    secondary = Color(0xFFCFCFC6),
    onSecondary = Color(0xFF1C1D1A),
    secondaryContainer = Ink700,
    onSecondaryContainer = Color(0xFFF3F3EE),
    tertiary = Color(0xFF8A8A82),
    onTertiary = Ink900,
    tertiaryContainer = Ink700,
    onTertiaryContainer = Color(0xFFCFCFC6),
    background = Ink900,
    onBackground = Color(0xFFF3F3EE),
    surface = Ink800,
    onSurface = Color(0xFFF3F3EE),
    surfaceVariant = Ink700,
    onSurfaceVariant = Color(0xFFCFCFC6),
    outline = Ink700,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun FocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FocusDarkColorScheme else FocusLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
