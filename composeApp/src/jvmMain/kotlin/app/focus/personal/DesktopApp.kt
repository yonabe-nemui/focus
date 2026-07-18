package app.focus.personal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.focus.personal.model.ItemKind
import app.focus.personal.ui.MultiColumnFeedScreen
import app.focus.personal.ui.theme.FocusTheme
import app.focus.personal.ui.MuteWordSettingsScreen
import app.focus.personal.ui.PostDetailScreen
import app.focus.personal.ui.SettingsScreen
import app.focus.personal.viewmodel.FeedViewModel
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DesktopApp(
    viewModel: FeedViewModel,
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

    val themeSettings by viewModel.themeSettings.collectAsState()

    CompositionLocalProvider(LocalAppImageLoader provides imageLoader) {
        FocusTheme(settings = themeSettings) {
            when (val screen = currentScreen) {
                is Screen.List -> MultiColumnFeedScreen(
                    viewModel = viewModel,
                    onOpenInBrowser = onLinkClick,
                    onItemClick = { item ->
                        if (item.kind == ItemKind.SNS_POST) navigateTo(Screen.PostDetail(item))
                        else onLinkClick(item.link)
                    },
                    onNavigateToSettings = { navigateTo(Screen.Settings) }
                )
                is Screen.Settings -> SettingsScreen(
                    viewModel = viewModel,
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
                        LoadingIndicator()
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
