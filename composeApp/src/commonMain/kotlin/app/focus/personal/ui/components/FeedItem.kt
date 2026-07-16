package app.focus.personal.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.focus.personal.LocalAppImageLoader
import app.focus.personal.model.ItemKind
import app.focus.personal.model.RssItem
import app.focus.personal.ui.theme.FocusShape
import app.focus.personal.ui.theme.FocusSpacing
import coil3.compose.AsyncImage
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.hatena_user_count
import org.jetbrains.compose.resources.stringResource

/**
 * フィードの1アイテム。Card・elevation を使わずフラットなレイアウト。
 * 下端に SectionDivider を内包し、LazyColumn 側での追加処理が不要。
 *
 * - SNS 投稿（kind = SNS_POST）: 左アバター + 右コンテンツ（Twitter 形式）
 * - ニュース/RSS（kind = NEWS）: タイトル + 説明 + 画像 + メタ情報
 */
@Composable
fun FeedItem(
    item: RssItem,
    onClick: (RssItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
    ) {
        if (item.kind == ItemKind.SNS_POST) {
            SnsPostItem(item = item)
        } else {
            NewsItem(item = item)
        }
        SectionDivider()
    }
}

/** SNS 投稿: 左アバター + 右コンテンツ（Twitter/Bluesky 形式） */
@Composable
private fun SnsPostItem(item: RssItem) {
    val imageUrls = item.imageUrls
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        AuthorAvatar(
            name = item.authorName ?: "",
            avatarUrl = item.authorAvatarUrl,
            size = 40.dp,
        )
        Spacer(Modifier.width(FocusSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            // 著者名 + 時刻（同一行）
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.authorName ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                relativeTimeLabel(item.pubDateMillis, item.pubDate)?.let { date ->
                    Text(
                        text = " · $date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            // 本文（最大 4 行、ellipsis）
            item.description?.let { text ->
                Spacer(Modifier.height(FocusSpacing.xxs))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // 画像（本文の下）
            if (!imageUrls.isNullOrEmpty()) {
                Spacer(Modifier.height(FocusSpacing.sm))
                FeedImageGallery(imageUrls = imageUrls)
            }
        }
    }
}

/** ニュース / RSS: タイトル + 説明 + 画像 + メタ情報行 */
@Composable
private fun NewsItem(item: RssItem) {
    val imageUrls = item.imageUrls
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.md),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        item.description?.let { desc ->
            Spacer(Modifier.height(FocusSpacing.xxs))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // 画像（本文の下、角丸 12dp）
        if (!imageUrls.isNullOrEmpty()) {
            Spacer(Modifier.height(FocusSpacing.sm))
            FeedImageGallery(imageUrls = imageUrls)
        }
        // メタ情報行（日時・ブックマーク数）
        if (item.pubDate != null || item.bookmarkCount != null) {
            Spacer(Modifier.height(FocusSpacing.xs))
            Row(
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                relativeTimeLabel(item.pubDateMillis, item.pubDate)?.let { date ->
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item.bookmarkCount?.let { count ->
                    Text(
                        text = stringResource(Res.string.hatena_user_count, count),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/** 1 枚: フル幅 180dp / 複数: 横スクロールギャラリー。角丸は imageCornerDetail (12dp)。 */
@Composable
private fun FeedImageGallery(
    imageUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalAppImageLoader.current ?: return
    if (imageUrls.size == 1) {
        AsyncImage(
            model = imageUrls[0],
            contentDescription = null,
            imageLoader = imageLoader,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(FocusShape.imageCornerDetail)),
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
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
                        .widthIn(min = 120.dp, max = 240.dp)
                        .clip(RoundedCornerShape(FocusShape.imageCornerDetail)),
                )
            }
        }
    }
}
