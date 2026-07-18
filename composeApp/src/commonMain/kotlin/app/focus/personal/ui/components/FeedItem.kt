package app.focus.personal.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.focus.personal.LocalAppImageLoader
import app.focus.personal.model.ItemKind
import app.focus.personal.model.RssItem
import app.focus.personal.ui.theme.FocusShape
import app.focus.personal.ui.theme.FocusSpacing
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.dialog_cancel
import focus.composeapp.generated.resources.hatena_user_count
import focus.composeapp.generated.resources.menu_add_mute_word
import focus.composeapp.generated.resources.menu_copy_link
import focus.composeapp.generated.resources.menu_open_in_browser
import focus.composeapp.generated.resources.mute_word_dialog_title
import focus.composeapp.generated.resources.mute_words_add_button
import focus.composeapp.generated.resources.mute_words_input_label
import org.jetbrains.compose.resources.stringResource

/**
 * フィードの1アイテム。Card・elevation を使わずフラットなレイアウト。
 * 下端に SectionDivider を内包し、LazyColumn 側での追加処理が不要。
 *
 * - SNS 投稿（kind = SNS_POST）: 左アバター + 右コンテンツ（Twitter 形式）
 * - ニュース/RSS（kind = NEWS）: タイトル + 説明 + 画像 + メタ情報
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedItem(
    item: RssItem,
    onClick: (RssItem) -> Unit,
    modifier: Modifier = Modifier,
    onOpenInBrowser: ((RssItem) -> Unit)? = null,
    onAddMuteWord: ((String) -> Unit)? = null,
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var isMuteDialogOpen by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onClick(item) },
                    onLongClick = { isMenuOpen = true },
                ),
        ) {
            if (item.kind == ItemKind.SNS_POST) {
                SnsPostItem(item = item)
            } else {
                NewsItem(item = item)
            }
            SectionDivider()
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = { isMenuOpen = false },
        ) {
            if (onOpenInBrowser != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.menu_open_in_browser)) },
                    onClick = {
                        isMenuOpen = false
                        onOpenInBrowser(item)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.menu_copy_link)) },
                onClick = {
                    isMenuOpen = false
                    clipboardManager.setText(AnnotatedString(item.link))
                },
            )
            if (onAddMuteWord != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.menu_add_mute_word)) },
                    onClick = {
                        isMenuOpen = false
                        isMuteDialogOpen = true
                    },
                )
            }
        }
    }

    if (isMuteDialogOpen && onAddMuteWord != null) {
        AddMuteWordDialog(
            onConfirm = { word ->
                isMuteDialogOpen = false
                onAddMuteWord(word)
            },
            onDismiss = { isMuteDialogOpen = false },
        )
    }
}

/** フィードから直接ミュートワードを追加するダイアログ。 */
@Composable
private fun AddMuteWordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var word by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.mute_word_dialog_title)) },
        text = {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it.replace("\n", "") },
                label = { Text(stringResource(Res.string.mute_words_input_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(word.trim()) },
                enabled = word.isNotBlank(),
            ) {
                Text(stringResource(Res.string.mute_words_add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        },
    )
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
                FeedImageGallery(imageUrls = imageUrls, imageAlts = item.imageAlts)
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
            FeedImageGallery(imageUrls = imageUrls, imageAlts = item.imageAlts)
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
                    // ブックマーク数は小型の tonal チップで表示
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = RoundedCornerShape(percent = 50),
                    ) {
                        Text(
                            text = stringResource(Res.string.hatena_user_count, count),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = FocusSpacing.sm,
                                vertical = FocusSpacing.xxs,
                            ),
                        )
                    }
                }
            }
        }
    }
}

/** 1 枚: フル幅 180dp / 複数: 横スクロールギャラリー。角丸は imageCornerDetail (12dp)。 */
@Composable
private fun FeedImageGallery(
    imageUrls: List<String>,
    imageAlts: List<String>?,
    modifier: Modifier = Modifier,
) {
    val imageLoader = LocalAppImageLoader.current ?: return
    fun altAt(index: Int): String? = imageAlts?.getOrNull(index)?.takeIf { it.isNotBlank() }

    if (imageUrls.size == 1) {
        FeedAsyncImage(
            url = imageUrls[0],
            contentDescription = altAt(0),
            imageLoader = imageLoader,
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(FocusShape.imageCornerDetail)),
        )
    } else {
        // Twitter/Bluesky 風の 2×2 グリッド(最大4枚)。奇数最終行は横長1枚で埋める。
        val shownUrls = imageUrls.take(4)
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
        ) {
            shownUrls.chunked(2).forEachIndexed { rowIndex, rowUrls ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(FocusSpacing.xs),
                ) {
                    rowUrls.forEachIndexed { colIndex, url ->
                        FeedAsyncImage(
                            url = url,
                            contentDescription = altAt(rowIndex * 2 + colIndex),
                            imageLoader = imageLoader,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(if (rowUrls.size == 1) 3f else 1.5f)
                                .clip(RoundedCornerShape(FocusShape.imageCornerDetail)),
                        )
                    }
                }
            }
        }
    }
}
