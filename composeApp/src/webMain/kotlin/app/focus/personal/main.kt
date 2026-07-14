package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("compose-app") {
        val scope = rememberCoroutineScope()
        val viewModel = remember {
            // Web では WebWorkerDriver が非同期のため DB は使用しない（セッションはメモリのみ保持）
            createRssViewModel(scope = scope)
        }

        App(
            viewModel = viewModel,
            onLinkClick = { url ->
                window.open(url, "_blank")
            }
        )
    }
}
