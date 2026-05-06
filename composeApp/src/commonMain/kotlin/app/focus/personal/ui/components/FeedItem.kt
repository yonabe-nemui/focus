package app.focus.personal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.focus.personal.LocalAppImageLoader
import app.focus.personal.model.RssItem
import app.focus.personal.ui.theme.FocusShape
import app.focus.personal.ui.theme.FocusSpacing
import coil3.compose.AsyncImage

/**
 * フィードの1アイテム。Card・elevation を使わずフラットなレイアウト。
 * 下端に SectionDivider を内包し、LazyColumn 側での追加処理が不要。
 *
 * - SNS 投稿（authorName あり）: ArticleHeader + 本文 + 画像
 * - ニュース/RSS（authorName なし）: タイトル + ブックマーク数 + 説明 + 日付
 */
@Composable
fun FeedItem(
    item: RssItem,
    onClick: (RssItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalAppImageLoader.current
    val authorName = item.authorName
    val imageUrls  = item.imageUrls

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = FocusSpacing.lg,
                vertical = FocusSpacing.sm,
            ),
        ) {
            if (authorName != null) {
                // ── SNS 投稿レイアウト ────────────────────────────────────
                ArticleHeader(
                    authorName = authorName,
                    authorAvatarUrl = item.authorAvatarUrl,
                    pubDate = item.pubDate,
                    modifier = Modifier.padding(bottom = FocusSpacing.xs),
                )
                item.description?.let { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                // ── ニュース / RSS レイアウト ─────────────────────────────
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                item.bookmarkCount?.let { count ->
                    Text(
                        text = "$count users",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = FocusSpacing.xxs),
                    )
                }
                item.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = FocusSpacing.xs),
                    )
                }
                item.pubDate?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = FocusSpacing.xs),
                    )
                }
            }

            // ── 画像ギャラリー（SNS・ニュース共通）──────────────────────
            if (!imageUrls.isNullOrEmpty() && imageLoader != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = FocusSpacing.sm)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
                ) {
                    imageUrls.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            imageLoader = imageLoader,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(160.dp)
                                .widthIn(min = 120.dp, max = 280.dp)
                                .clip(RoundedCornerShape(FocusShape.imageCorner)),
                        )
                    }
                }
            }
        }

        SectionDivider()
    }
}
