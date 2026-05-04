package app.focus.personal.model

import kotlinx.serialization.Serializable

@Serializable
data class BlueskySession(
    val accessJwt: String,
    val refreshJwt: String,
    val handle: String,
    val did: String
)

@Serializable
data class BlueskyPreferences(
    val mutedWords: List<MutedWord> = emptyList()
)

@Serializable
data class MutedWord(
    val value: String,
    val targets: List<String> = listOf("content")
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
    val cid: String,
    val author: BlueskyProfile,
    val record: BlueskyRecord,
    val likeCount: Int = 0,
    val repostCount: Int = 0
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
    val langs: List<String> = emptyList()
)

fun BlueskyPost.toRssItem(): RssItem {
    return RssItem(
        title = author.displayName ?: author.handle,
        link = "https://bsky.app/profile/${author.did}/post/${uri.split("/").last()}",
        description = record.text,
        pubDate = record.createdAt,
        guid = uri
    )
}
