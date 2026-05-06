package app.focus.personal.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.focus.personal.LocalAppImageLoader
import app.focus.personal.model.RssItem
import app.focus.personal.ui.components.ArticleHeader
import app.focus.personal.ui.theme.FocusShape
import app.focus.personal.ui.theme.FocusSpacing
import coil3.compose.AsyncImage

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenInBrowser) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "ブラウザで開く")
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
                pubDate = item.pubDate,
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    fullImages.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            imageLoader = imageLoader,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(FocusShape.imageCornerDetail)),
                        )
                        Spacer(modifier = Modifier.height(FocusSpacing.sm))
                    }
                }
            }
        }
    }
}
