package app.focus.personal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.webview_opened_in_new_tab
import kotlinx.browser.window
import org.jetbrains.compose.resources.stringResource

// Web は記事をブラウザの新規タブで開き、この画面には案内文だけを表示する
@Composable
actual fun MyWebView(
    url: String,
    modifier: Modifier,
    onProgress: (Float) -> Unit,
    onTitle: (String) -> Unit,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(stringResource(Res.string.webview_opened_in_new_tab))
    }

    LaunchedEffect(url) {
        window.open(url, "_blank")
        onProgress(1f)
    }
}
