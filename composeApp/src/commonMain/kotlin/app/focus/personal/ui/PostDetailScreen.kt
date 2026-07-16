package app.focus.personal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.focus.personal.LocalAppImageLoader
import app.focus.personal.model.RssItem
import app.focus.personal.ui.components.ArticleHeader
import app.focus.personal.ui.components.FeedAsyncImage
import app.focus.personal.ui.components.ImageViewerDialog
import app.focus.personal.ui.components.relativeTimeLabel
import app.focus.personal.ui.theme.FocusShape
import app.focus.personal.ui.theme.FocusSpacing
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.cd_back
import focus.composeapp.generated.resources.cd_open_in_browser
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    item: RssItem,
    onBack: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    val imageLoader = LocalAppImageLoader.current
    val authorName = item.authorName ?: item.title
    val authorAvatarUrl = item.authorAvatarUrl
    val fullImages = item.imageFullUrls ?: item.imageUrls

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInBrowser) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = stringResource(Res.string.cd_open_in_browser))
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm),
        ) {
            ArticleHeader(
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                pubDate = relativeTimeLabel(item.pubDateMillis, item.pubDate),
                avatarSize = 48.dp,
                modifier = Modifier.padding(bottom = FocusSpacing.md),
            )

            item.description?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = FocusSpacing.md),
                )
            }

            if (!fullImages.isNullOrEmpty() && imageLoader != null) {
                var viewerIndex by remember { mutableStateOf<Int?>(null) }
                Column(modifier = Modifier.fillMaxWidth()) {
                    fullImages.forEachIndexed { index, url ->
                        val alt = item.imageAlts?.getOrNull(index)?.takeIf { it.isNotBlank() }
                        // 高さ固定をやめ、読み込み後は画像のアスペクト比どおりに表示する
                        FeedAsyncImage(
                            url = url,
                            contentDescription = alt,
                            imageLoader = imageLoader,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .clip(RoundedCornerShape(FocusShape.imageCornerDetail))
                                .clickable { viewerIndex = index },
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.sm))
                    }
                }
                viewerIndex?.let { index ->
                    ImageViewerDialog(
                        url = fullImages[index],
                        contentDescription = item.imageAlts?.getOrNull(index)?.takeIf { it.isNotBlank() },
                        imageLoader = imageLoader,
                        onDismiss = { viewerIndex = null },
                    )
                }
            }
        }
    }
}
