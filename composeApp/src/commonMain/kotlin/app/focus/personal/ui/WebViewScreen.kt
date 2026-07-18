package app.focus.personal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.cd_back
import focus.composeapp.generated.resources.cd_open_in_browser
import focus.composeapp.generated.resources.screen_title_web_content
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit,
    onOpenInBrowser: (() -> Unit)? = null,
) {
    var pageTitle by remember(url) { mutableStateOf<String?>(null) }
    var progress by remember(url) { mutableFloatStateOf(0f) }

    // タイトル未取得の間は URL のホスト名を表示する
    val hostName = remember(url) {
        url.substringAfter("://").substringBefore("/").takeIf { it.isNotBlank() }
    }
    val title = pageTitle ?: hostName ?: stringResource(Res.string.screen_title_web_content)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back),
                            )
                        }
                    },
                    actions = {
                        if (onOpenInBrowser != null) {
                            IconButton(onClick = onOpenInBrowser) {
                                Icon(
                                    imageVector = Icons.Default.OpenInBrowser,
                                    contentDescription = stringResource(Res.string.cd_open_in_browser),
                                )
                            }
                        }
                    },
                )
                if (progress < 1f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            MyWebView(
                url = url,
                modifier = Modifier.fillMaxSize(),
                onProgress = { progress = it },
                onTitle = { pageTitle = it },
            )
        }
    }
}
