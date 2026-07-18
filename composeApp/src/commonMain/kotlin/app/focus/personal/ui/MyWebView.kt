package app.focus.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * プラットフォーム別 WebView。
 *
 * @param onProgress 読み込み進捗(0f..1f)。1f で完了。WebView を持たないプラットフォーム
 *   (Desktop = 外部ブラウザ、Web = 新規タブ)は即座に 1f を通知する。
 * @param onTitle 読み込んだページのタイトル。取得できないプラットフォームでは呼ばれない。
 */
@Composable
expect fun MyWebView(
    url: String,
    modifier: Modifier = Modifier,
    onProgress: (Float) -> Unit = {},
    onTitle: (String) -> Unit = {},
)
