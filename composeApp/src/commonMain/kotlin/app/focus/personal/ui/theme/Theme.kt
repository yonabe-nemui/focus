package app.focus.personal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

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
            typography  = FocusTypography,
            shapes      = FocusShapeScheme,
            content     = content,
        )
    }
}
