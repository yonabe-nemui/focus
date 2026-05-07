package app.focus.personal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.focus.personal.LocalAppImageLoader
import app.focus.personal.ui.theme.FocusSpacing
import coil3.compose.AsyncImage

/**
 * Avatar + 著者名 + 日付 の共通ヘッダー行。
 * FeedItem（36dp）と PostDetailScreen（48dp）で avatarSize を切り替えて使う。
 */
@Composable
fun ArticleHeader(
    authorName: String,
    authorAvatarUrl: String?,
    pubDate: String?,
    avatarSize: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        AuthorAvatar(name = authorName, avatarUrl = authorAvatarUrl, size = avatarSize)
        Spacer(modifier = Modifier.width(FocusSpacing.sm))
        Column {
            Text(
                text = authorName,
                style = MaterialTheme.typography.titleSmall,
            )
            if (pubDate != null) {
                Text(
                    text = pubDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun AuthorAvatar(name: String, avatarUrl: String?, size: Dp) {
    val imageLoader = LocalAppImageLoader.current
    if (avatarUrl != null && imageLoader != null) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.first().uppercaseChar().toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
