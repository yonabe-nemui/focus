package app.focus.personal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.focus.personal.model.ThemeMode
import app.focus.personal.model.ThemeSettings

/**
 * Focus アプリの MaterialTheme ラッパー。
 *
 * ダイナミックカラー対応: プラットフォーム側（Android の MainActivity 等）から
 * `colorScheme = dynamicDarkColorScheme(context)` を渡すだけで動的配色に対応できる。
 * 共通コードの変更は不要。
 *
 * OLED Black: [oledBlack] = true でバックグラウンド/サーフェスを純黒化し AMOLED 省電力モードに。
 * 設定画面の UserPreference と連動させることを想定。
 *
 * @param colorScheme 外部から注入する ColorScheme（ダイナミックカラー等）。
 *   省略時は [darkTheme] / [oledBlack] に基づいて自動選択される。
 */
/**
 * ユーザーのテーマ設定([ThemeSettings])から配色を解決するオーバーロード。
 * - ダイナミックカラー有効時はプラットフォームのスキーム(Android 12+)を使用
 * - OLED ブラックはダークテーマ時のみ適用し、ダイナミックカラーとも併用できる
 */
@Composable
fun FocusTheme(
    settings: ThemeSettings,
    content: @Composable () -> Unit,
) {
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
    FocusTheme(darkTheme = darkTheme, colorScheme = colorScheme, content = content)
}

@Composable
fun FocusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    oledBlack: Boolean = false,
    colorScheme: ColorScheme = when {
        oledBlack -> FocusOledColorScheme
        darkTheme -> FocusDarkColorScheme
        else      -> FocusLightColorScheme
    },
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalFocusSpacing provides FocusSpacing,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = focusTypography(),
            shapes      = FocusShapeScheme,
            content     = content,
        )
    }
}
