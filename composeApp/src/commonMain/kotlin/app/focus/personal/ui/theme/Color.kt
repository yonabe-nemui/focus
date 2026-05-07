package app.focus.personal.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Focus Green ───────────────────────────────────────────────────────────────
private val AccentGreen          = Color(0xFF2E7D5B)
private val AccentGreenLight     = Color(0xFF8FBFA5)
private val AccentGreenContainer = Color(0xFFE6F1EB)
private val AccentGreenDark      = Color(0xFF215F45)

// ── Neutrals (warm paper tones) ───────────────────────────────────────────────
private val Paper50  = Color(0xFFFAFAF7)
private val Paper100 = Color(0xFFF3F3EE)
private val Paper200 = Color(0xFFE5E5DE)
private val Paper700 = Color(0xFF3E3E39)
private val Paper900 = Color(0xFF14140F)

// ── Ink (dark surface tones, warm near-black) ─────────────────────────────────
private val Ink900 = Color(0xFF11120F)  // #121212 相当（純黒は使わない）
private val Ink800 = Color(0xFF1C1D1A)
private val Ink700 = Color(0xFF2A2A25)
private val Ink500 = Color(0xFF6B6B64)

// ── Semantic ──────────────────────────────────────────────────────────────────
private val ErrorRed          = Color(0xFFB3261E)
private val ErrorRedContainer = Color(0xFFFDECEB)

// ── Light scheme ──────────────────────────────────────────────────────────────
internal val FocusLightColorScheme = lightColorScheme(
    primary              = AccentGreen,
    onPrimary            = Color.White,
    primaryContainer     = AccentGreenContainer,
    onPrimaryContainer   = Color(0xFF173F2E),
    secondary            = Paper700,
    onSecondary          = Color.White,
    secondaryContainer   = Paper100,
    onSecondaryContainer = Paper900,
    tertiary             = Ink500,
    onTertiary           = Color.White,
    tertiaryContainer    = Paper200,
    onTertiaryContainer  = Paper900,
    background           = Paper50,
    onBackground         = Paper900,
    surface              = Color.White,
    onSurface            = Paper900,
    surfaceVariant       = Paper100,
    onSurfaceVariant     = Paper700,
    outline              = Paper200,
    error                = ErrorRed,
    onError              = Color.White,
    errorContainer       = ErrorRedContainer,
    onErrorContainer     = Color(0xFF410E0B),
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
// ベースは純黒ではなく Ink900 (#121212 相当) でフラットな階層を保つ
internal val FocusDarkColorScheme = darkColorScheme(
    primary              = AccentGreenLight,
    onPrimary            = Color(0xFF0F1A14),
    primaryContainer     = AccentGreenDark,
    onPrimaryContainer   = AccentGreenContainer,
    secondary            = Color(0xFFCFCFC6),
    onSecondary          = Ink800,
    secondaryContainer   = Ink700,
    onSecondaryContainer = Color(0xFFF3F3EE),
    tertiary             = Color(0xFF8A8A82),
    onTertiary           = Ink900,
    tertiaryContainer    = Ink700,
    onTertiaryContainer  = Color(0xFFCFCFC6),
    background           = Ink900,
    onBackground         = Color(0xFFF3F3EE),
    surface              = Ink800,
    onSurface            = Color(0xFFF3F3EE),
    surfaceVariant       = Ink700,
    onSurfaceVariant     = Color(0xFFCFCFC6),
    outline              = Ink700,
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
)

// ── OLED Black scheme ─────────────────────────────────────────────────────────
// Dark scheme の派生。background/surface を純黒にして AMOLED 省電力を最大化。
// ユーザー設定で切り替え可能にする想定（デフォルト OFF）。
internal val FocusOledColorScheme = FocusDarkColorScheme.copy(
    background = Color(0xFF000000),
    surface    = Color(0xFF0A0A0A),
)
