package app.focus.personal.model

import app.focus.personal.util.DateUtils
import kotlinx.serialization.Serializable

// BlueSky XRPC API のレスポンスのうち、アプリで使う項目だけを定義する(未知のキーは無視してデコード)

@Serializable
data class BlueskySession(
    val accessJwt: String,
    val refreshJwt: String,
    val handle: String,
    val did: String
)

@Serializable
data class BlueskySearchResponse(
    val posts: List<BlueskyPost> = emptyList(),
    val cursor: String? = null
)

@Serializable
data class BlueskyTimelineResponse(
    val feed: List<BlueskyFeedViewPost> = emptyList(),
    val cursor: String? = null
)

@Serializable
data class BlueskyFeedViewPost(
    val post: BlueskyPost
)

@Serializable
data class BlueskyPost(
    val uri: String,
    val author: BlueskyProfile,
    val record: BlueskyRecord,
    val embed: BlueskyEmbed? = null,
)

@Serializable
data class BlueskyEmbed(
    val images: List<BlueskyEmbedImage> = emptyList()
)

@Serializable
data class BlueskyEmbedImage(
    val thumb: String = "",
    val fullsize: String = "",
    val alt: String = ""
)

@Serializable
data class BlueskyProfile(
    val did: String,
    val handle: String,
    val displayName: String? = null,
    val avatar: String? = null
)

@Serializable
data class BlueskyRecord(
    val text: String = "",
    val createdAt: String = "",
)

fun BlueskyPost.toRssItem(): RssItem {
    val images = embed?.images?.takeIf { it.isNotEmpty() }
    val name = author.displayName ?: author.handle
    return RssItem(
        title = name,
        link = "https://bsky.app/profile/${author.did}/post/${uri.substringAfterLast("/")}",
        description = record.text,
        pubDate = record.createdAt,
        guid = uri,
        authorName = name,
        authorAvatarUrl = author.avatar,
        imageUrls = images?.map { it.thumb },
        imageFullUrls = images?.map { it.fullsize },
        imageAlts = images?.map { it.alt },
        pubDateMillis = DateUtils.parseIso8601ToMillis(record.createdAt),
        kind = ItemKind.SNS_POST,
    )
}
