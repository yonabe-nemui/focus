package app.focus.personal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.focus.personal.model.ThemeMode
import app.focus.personal.model.ThemeSettings

/**
 * Focus アプリの MaterialTheme ラッパー。ユーザーのテーマ設定([ThemeSettings])から配色を解決する。
 * - ダイナミックカラー有効時はプラットフォームのスキーム(Android 12+)を使用
 * - OLED ブラックはダークテーマ時のみ適用し、ダイナミックカラーとも併用できる
 */
@Composable
fun FocusTheme(settings: ThemeSettings = ThemeSettings(), content: @Composable () -> Unit) {
    val darkTheme = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dynamicScheme = if (settings.dynamicColor) dynamicColorSchemeOrNull(darkTheme) else null
    val oled = settings.oledBlack && darkTheme
    val colorScheme = when {
        dynamicScheme != null && oled -> dynamicScheme.copy(background = OledBackground, surface = OledSurface)
        dynamicScheme != null -> dynamicScheme
        oled -> FocusOledColorScheme
        darkTheme -> FocusDarkColorScheme
        else -> FocusLightColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = focusTypography(),
        shapes = FocusShapeScheme,
        content = content,
    )
}
