package app.focus.personal.ui.components

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.focus.personal.model.RssItem
import app.focus.personal.ui.theme.FocusTheme

// ── サンプルデータ ────────────────────────────────────────────────────────────

private val previewSnsShort = RssItem(
    authorName = "Alice",
    authorAvatarUrl = null,
    description = "Kotlin Multiplatform 1.0 がついにリリースされました！",
    pubDate = "2m",
)

private val previewSnsWithImages = RssItem(
    authorName = "Bob Photographer",
    authorAvatarUrl = null,
    description = "今日の夕焼けは格別だった。毎年この季節が好き。",
    pubDate = "1h",
    imageUrls = listOf(
        "https://picsum.photos/seed/focus1/400/300",
        "https://picsum.photos/seed/focus2/400/300",
    ),
)

private val previewSnsLong = RssItem(
    authorName = "Carol Dev",
    authorAvatarUrl = null,
    description = "Compose Multiplatform について長文を書きます。"
        + " Android・iOS・Desktop・Web の全プラットフォームで Compose UI を共有できるのは非常に魅力的です。"
        + " 特にビジネスロジックだけでなく UI レイヤーまで共有できる点が他の KMP アプローチと大きく異なります。"
        + " 学習コストはあるものの、一度慣れれば開発速度が劇的に向上します。ぜひ試してみてください。",
    pubDate = "3h",
)

private val previewNews = RssItem(
    title = "Kotlin Multiplatform が iOS App Store に対応—Apple シリコン向け最適化も完了",
    description = "JetBrains が発表した最新リリースでは、iOS App Store への配布フローが大幅に改善された。"
        + " Compose Multiplatform と組み合わせることで、共通 UI コードを iOS に展開できる。",
    pubDate = "2024-01-15",
    bookmarkCount = 342,
    imageUrls = listOf("https://picsum.photos/seed/focus3/800/400"),
)

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview
@Composable
private fun FeedItemSnsPreview() {
    FocusTheme {
        Surface {
            FeedItem(item = previewSnsShort, onClick = {})
        }
    }
}

@Preview
@Composable
private fun FeedItemWithImagesPreview() {
    FocusTheme {
        Surface {
            FeedItem(item = previewSnsWithImages, onClick = {})
        }
    }
}

@Preview
@Composable
private fun FeedItemLongTextPreview() {
    FocusTheme {
        Surface {
            FeedItem(item = previewSnsLong, onClick = {})
        }
    }
}

@Preview
@Composable
private fun FeedItemDarkModePreview() {
    FocusTheme(darkTheme = true) {
        Surface {
            FeedItem(item = previewNews, onClick = {})
        }
    }
}
