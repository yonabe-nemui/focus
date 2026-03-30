package app.focus.personal

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import app.focus.personal.ui.BindBackHandler
import app.focus.personal.ui.RssListScreen
import app.focus.personal.ui.WebViewScreen
import app.focus.personal.viewmodel.RssViewModel
import androidx.compose.ui.tooling.preview.Preview

sealed class Screen {
    object List : Screen()
    data class WebView(val url: String) : Screen()
@Composable
@Preview
fun App(
    viewModel: RssViewModel,
    onLinkClick: (String) -> Unit // Keep this for now
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.List) }
    val currentScreen = backStack.last()

    // 画面が2枚以上あるときだけバックハンドラーを有効にする
    // これにより、1枚目のときはシステムデフォルト（アプリ終了）に任せられる
    if (backStack.size > 1) {
        BindBackHandler {
            backStack.removeAt(backStack.size - 1)
        }
    }

    MaterialTheme {
        when (val screen = currentScreen) {
            is Screen.List -> {
                RssListScreen(
                    viewModel = viewModel,
                    onLinkClick = { url ->
                        backStack.add(Screen.WebView(url))
                    }
                )
            }
            is Screen.WebView -> {
                WebViewScreen(
                    url = screen.url,
                    onBack = { 
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
        }
    }
}
