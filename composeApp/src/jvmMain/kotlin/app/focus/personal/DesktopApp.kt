package app.focus.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.focus.personal.ui.DesktopRssScreen
import app.focus.personal.ui.FocusTheme
import app.focus.personal.ui.MuteWordSettingsScreen
import app.focus.personal.ui.PostDetailScreen
import app.focus.personal.ui.SettingsScreen
import app.focus.personal.viewmodel.RssViewModel
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory

@Composable
fun DesktopApp(
    viewModel: RssViewModel,
    onLinkClick: (String) -> Unit
) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.List) }
    val currentScreen = backStack.lastOrNull() ?: Screen.List

    fun navigateTo(screen: Screen) { backStack.add(screen) }
    fun navigateBack() { if (backStack.size > 1) backStack.removeAt(backStack.size - 1) }

    val context = LocalPlatformContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    CompositionLocalProvider(LocalAppImageLoader provides imageLoader) {
        FocusTheme {
            when (val screen = currentScreen) {
                is Screen.List -> DesktopRssScreen(
                    viewModel = viewModel,
                    onItemClick = { item ->
                        if (item.authorName != null) navigateTo(Screen.PostDetail(item))
                        else onLinkClick(item.link)
                    },
                    onNavigateToSettings = { navigateTo(Screen.Settings) }
                )
                is Screen.Settings -> SettingsScreen(
                    onNavigateToMuteWords = { navigateTo(Screen.MuteWords) },
                    onBack = { navigateBack() }
                )
                is Screen.MuteWords -> MuteWordSettingsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
                is Screen.WebView -> {
                    // Desktop ではブラウザで直接開く
                    LaunchedEffect(screen.url) {
                        onLinkClick(screen.url)
                        navigateBack()
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is Screen.PostDetail -> PostDetailScreen(
                    item = screen.item,
                    onBack = { navigateBack() },
                    onOpenInBrowser = { onLinkClick(screen.item.link) }
                )
            }
        }
    }
}
