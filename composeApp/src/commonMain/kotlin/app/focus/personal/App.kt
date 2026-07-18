package app.focus.personal

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.focus.personal.model.ItemKind
import app.focus.personal.model.RssItem
import app.focus.personal.ui.BindBackHandler
import app.focus.personal.ui.MuteWordSettingsScreen
import app.focus.personal.ui.theme.FocusTheme
import app.focus.personal.ui.PostDetailScreen
import app.focus.personal.ui.FeedListScreen
import app.focus.personal.ui.MultiColumnFeedScreen
import app.focus.personal.ui.SettingsScreen
import app.focus.personal.ui.WebViewScreen
import app.focus.personal.viewmodel.FeedViewModel
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
    viewModel: FeedViewModel,
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

    val themeSettings by viewModel.themeSettings.collectAsState()

    CompositionLocalProvider(LocalAppImageLoader provides imageLoader) {
        FocusTheme(settings = themeSettings) {
            when (val screen = currentScreen) {
                is Screen.List -> {
                    // タブレット・折りたたみ展開時(Expanded 相当)はマルチカラム表示に切り替える
                    BoxWithConstraints {
                        val onItemClick: (RssItem) -> Unit = { item ->
                            if (item.kind == ItemKind.SNS_POST) {
                                navigateTo(Screen.PostDetail(item))
                            } else {
                                navigateTo(Screen.WebView(item.link))
                            }
                        }
                        if (maxWidth >= 840.dp) {
                            MultiColumnFeedScreen(
                                viewModel = viewModel,
                                onItemClick = onItemClick,
                                onNavigateToSettings = { navigateTo(Screen.Settings) },
                                onOpenInBrowser = onLinkClick,
                            )
                        } else {
                            FeedListScreen(
                                viewModel = viewModel,
                                onItemClick = onItemClick,
                                onNavigateToSettings = { navigateTo(Screen.Settings) },
                                onOpenInBrowser = onLinkClick,
                            )
                        }
                    }
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
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
                        onBack = { navigateBack() },
                        onOpenInBrowser = { onLinkClick(screen.url) },
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
