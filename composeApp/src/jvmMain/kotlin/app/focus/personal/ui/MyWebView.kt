package app.focus.personal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Desktop は記事を外部ブラウザで開く(App の isDesktop)ため、WebView 画面には遷移しない
@Composable
actual fun MyWebView(url: String, modifier: Modifier, onProgress: (Float) -> Unit, onTitle: (String) -> Unit) {
    Box(modifier = modifier)
}
