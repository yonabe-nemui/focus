package app.focus.personal.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Focus Green ──────────────────────────────────────────────────────────────
private val AccentGreen          = Color(0xFF2E7D5B)
private val AccentGreenLight     = Color(0xFF8FBFA5)
private val AccentGreenContainer = Color(0xFFE6F1EB)
private val AccentGreenDark      = Color(0xFF215F45)

// ── Neutrals (warm-cool paper) ───────────────────────────────────────────────
private val Paper50  = Color(0xFFFAFAF7)
private val Paper100 = Color(0xFFF3F3EE)
private val Paper200 = Color(0xFFE5E5DE)
private val Paper700 = Color(0xFF3E3E39)
private val Paper900 = Color(0xFF14140F)

// ── Ink (dark surface tones) ─────────────────────────────────────────────────
private val Ink900 = Color(0xFF11120F)
private val Ink800 = Color(0xFF1C1D1A)
private val Ink700 = Color(0xFF2A2A25)
private val Ink500 = Color(0xFF6B6B64)

// ── Semantic ─────────────────────────────────────────────────────────────────
private val ErrorRed          = Color(0xFFB3261E)
private val ErrorRedContainer = Color(0xFFFDECEB)

// ── Color schemes ────────────────────────────────────────────────────────────

private val FocusLightColorScheme = lightColorScheme(
    primary             = AccentGreen,
    onPrimary           = Color.White,
    primaryContainer    = AccentGreenContainer,
    onPrimaryContainer  = Color(0xFF173F2E),
    secondary           = Paper700,
    onSecondary         = Color.White,
    secondaryContainer  = Paper100,
    onSecondaryContainer = Paper900,
    tertiary            = Ink500,
    onTertiary          = Color.White,
    tertiaryContainer   = Paper200,
    onTertiaryContainer = Paper900,
    background          = Paper50,
    onBackground        = Paper900,
    surface             = Color.White,
    onSurface           = Paper900,
    surfaceVariant      = Paper100,
    onSurfaceVariant    = Paper700,
    outline             = Paper200,
    error               = ErrorRed,
    onError             = Color.White,
    errorContainer      = ErrorRedContainer,
    onErrorContainer    = Color(0xFF410E0B),
)

private val FocusDarkColorScheme = darkColorScheme(
    primary             = AccentGreenLight,
    onPrimary           = Color(0xFF0F1A14),
    primaryContainer    = AccentGreenDark,
    onPrimaryContainer  = AccentGreenContainer,
    secondary           = Color(0xFFCFCFC6),
    onSecondary         = Ink800,
    secondaryContainer  = Ink700,
    onSecondaryContainer = Color(0xFFF3F3EE),
    tertiary            = Color(0xFF8A8A82),
    onTertiary          = Ink900,
    tertiaryContainer   = Ink700,
    onTertiaryContainer = Color(0xFFCFCFC6),
    background          = Ink900,
    onBackground        = Color(0xFFF3F3EE),
    surface             = Ink800,
    onSurface           = Color(0xFFF3F3EE),
    surfaceVariant      = Ink700,
    onSurfaceVariant    = Color(0xFFCFCFC6),
    outline             = Ink700,
    error               = Color(0xFFFFB4AB),
    onError             = Color(0xFF690005),
    errorContainer      = Color(0xFF93000A),
    onErrorContainer    = Color(0xFFFFDAD6),
)

// ── Typography ───────────────────────────────────────────────────────────────
// Scale matches the Focus design system (colors_and_type.css).
// Font families: Inter (Latin) + Noto Sans JP (CJK).
// To activate custom fonts, add TTF files to composeResources/font/ and
// replace FontWeight.* TextStyle calls with FontFamily references.

private val FocusTypography = Typography(
    // Display — not customised from M3 defaults
    // Headline — bump weight to SemiBold for stronger hierarchy
    headlineLarge = TextStyle(
        fontSize = 32.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    // Title
    titleLarge = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.1.sp
    ),
    // Body
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.4.sp
    ),
    // Label
    labelLarge = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    ),
)

// ── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun FocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) FocusDarkColorScheme else FocusLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FocusTypography,
        content = content
    )
}
