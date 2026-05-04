package app.focus.personal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import app.focus.personal.model.RssItem
import app.focus.personal.ui.BindBackHandler
import app.focus.personal.ui.FocusTheme
import app.focus.personal.ui.MuteWordSettingsScreen
import app.focus.personal.ui.PostDetailScreen
import app.focus.personal.ui.RssListScreen
import app.focus.personal.ui.SettingsScreen
import app.focus.personal.ui.WebViewScreen
import app.focus.personal.viewmodel.RssViewModel
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory

val LocalAppImageLoader = compositionLocalOf<ImageLoader?> { null }

sealed class Screen {
    object List : Screen()
    object Settings : Screen()
    object MuteWords : Screen()
    data class WebView(val url: String) : Screen()
    data class PostDetail(val item: RssItem) : Screen()
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

    val context = LocalPlatformContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    CompositionLocalProvider(LocalAppImageLoader provides imageLoader) {
        FocusTheme {
            when (val screen = currentScreen) {
                is Screen.List -> {
                    RssListScreen(
                        viewModel = viewModel,
                        onItemClick = { item ->
                            if (item.authorName != null) {
                                navigateTo(Screen.PostDetail(item))
                            } else {
                                navigateTo(Screen.WebView(item.link))
                            }
                        },
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
                is Screen.PostDetail -> {
                    PostDetailScreen(
                        item = screen.item,
                        onBack = { navigateBack() },
                        onOpenInBrowser = { onLinkClick(screen.item.link) }
                    )
                }
            }
        }
    }
}
