package app.focus.personal.model

import kotlinx.serialization.Serializable

@Serializable
data class MisskeyNote(
    val id: String,
    val createdAt: String,
    val text: String? = null,
    val cw: String? = null,
    val user: MisskeyUser,
    val renoteCount: Int = 0,
    val repliesCount: Int = 0,
    val tags: List<String> = emptyList(),
    val visibility: String = "public"
)

@Serializable
data class MisskeyUser(
    val id: String,
    val username: String,
    val name: String? = null,
    val host: String? = null,
    val avatarUrl: String? = null
)

data class MisskeySettings(
    val instanceUrl: String,
    val apiToken: String? = null
)

fun MisskeyNote.toRssItem(instanceUrl: String): RssItem {
    val noteUrl = "https://$instanceUrl/notes/$id"
    val displayText = if (cw != null) "[$cw]\n${text ?: ""}" else (text ?: "")
    return RssItem(
        title = user.name ?: "@${user.username}",
        link = noteUrl,
        description = displayText,
        pubDate = createdAt,
        guid = id
    )
}