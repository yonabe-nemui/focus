package app.focus.personal

import androidx.compose.runtime.*
import app.focus.personal.ui.BindBackHandler
import app.focus.personal.ui.FocusTheme
import app.focus.personal.ui.MuteWordSettingsScreen
import app.focus.personal.ui.RssListScreen
import app.focus.personal.ui.SettingsScreen
import app.focus.personal.ui.WebViewScreen
import app.focus.personal.viewmodel.RssViewModel
import androidx.compose.ui.tooling.preview.Preview

sealed class Screen {
    object List : Screen()
    object Settings : Screen()
    object MuteWords : Screen()
    data class WebView(val url: String) : Screen()
}

@Composable
@Preview
fun App(
    viewModel: RssViewModel,
    onLinkClick: (String) -> Unit
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.List) }
    val currentScreen = backStack.lastOrNull() ?: Screen.List

    fun navigateTo(screen: Screen) { backStack.add(screen) }
    fun navigateBack() { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }

    if (backStack.size > 1) {
        BindBackHandler { navigateBack() }
    }

    FocusTheme {
        when (val screen = currentScreen) {
            is Screen.List -> {
                RssListScreen(
                    viewModel = viewModel,
                    onLinkClick = { url -> navigateTo(Screen.WebView(url)) },
                    onNavigateToSettings = { navigateTo(Screen.Settings) }
                )
            }
            is Screen.Settings -> {
                SettingsScreen(
                    onNavigateToMuteWords = { navigateTo(Screen.MuteWords) },
                    onBack = { navigateBack() }
                )
            }
            is Screen.MuteWords -> {
                MuteWordSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }
            is Screen.WebView -> {
                WebViewScreen(
                    url = screen.url,
                    onBack = { navigateBack() }
                )
            }
        }
    }
}
