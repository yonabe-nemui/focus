package app.focus.personal.model

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

// RSS 2.0 のうちアプリで使う項目だけを定義する(未知の要素・属性は無視してパース)

@Serializable
@XmlSerialName("rss", "", "")
data class RssFeed(
    val channel: RssChannel = RssChannel()
)

@Serializable
@XmlSerialName("channel", "", "")
data class RssChannel(
    @XmlSerialName("item", "", "")
    val items: List<RssItem> = emptyList()
)

@Serializable
@XmlSerialName("item", "", "")
data class RssItem(
    @XmlElement(true)
    val title: String = "",
    @XmlElement(true)
    val link: String = "",
    @XmlElement(true)
    val description: String? = null,
    @XmlElement(true)
    val pubDate: String? = null,
    @XmlElement(true)
    val guid: String? = null,
    @XmlElement(false)
    val authorName: String? = null,
    @XmlElement(false)
    val authorAvatarUrl: String? = null,
    val imageUrls: List<String>? = null,
    val imageFullUrls: List<String>? = null,
    // 画像の代替テキスト。imageUrls / imageFullUrls とインデックスで対応する
    val imageAlts: List<String>? = null,
    // 各クライアントが pubDate をパースして正規化した値を入れる。0 = パース失敗・未設定。
    val pubDateMillis: Long = 0L,
    // XML(RSS)には存在しないため、パース時はデフォルトの NEWS になる。
    val kind: ItemKind = ItemKind.NEWS,
)

/** アイテムの種別。UI の詳細画面遷移や表示レイアウトの分岐に使う。 */
@Serializable
enum class ItemKind { NEWS, SNS_POST }

@Serializable
data class PagedFeedResponse(val items: List<RssItem>, val nextCursor: String? = null)
