package app.focus.personal

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import app.focus.personal.ui.RssListScreen
import app.focus.personal.ui.WebViewScreen
import app.focus.personal.viewmodel.RssViewModel
import androidx.compose.ui.tooling.preview.Preview

sealed class Screen {
    object List : Screen()
    data class WebView(val url: String) : Screen()
}

@Composable
@Preview
fun App(
    viewModel: RssViewModel,
    onLinkClick: (String) -> Unit // Keep this for now, but we'll use internal navigation
) {
    LaunchedEffect(Unit) {
        // Simple initialization for debug builds. 
        // In a real app, this should be platform-specific.
        Napier.base(DebugAntilog())
    }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.List) }

    MaterialTheme {
        when (val screen = currentScreen) {
            is Screen.List -> {
                RssListScreen(
                    viewModel = viewModel,
                    onLinkClick = { url ->
                        currentScreen = Screen.WebView(url)
                    }
                )
            }
            is Screen.WebView -> {
                WebViewScreen(
                    url = screen.url,
                    onBack = { currentScreen = Screen.List }
                )
            }
        }
    }
}
