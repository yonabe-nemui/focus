package app.focus.personal.model

import kotlinx.serialization.Serializable

@Serializable
data class MisskeyNote(
    val id: String,
    val createdAt: String,
    val text: String? = null,
    val cw: String? = null,
    val user: MisskeyUser,
    val files: List<MisskeyFile> = emptyList(),
    val renoteCount: Int = 0,
    val repliesCount: Int = 0,
    val tags: List<String> = emptyList(),
    val visibility: String = "public"
)

@Serializable
data class MisskeyFile(
    val id: String,
    val type: String = "",
    val url: String = "",
    val thumbnailUrl: String? = null,
    // 画像の代替テキスト（Misskey ではキャプション）
    val comment: String? = null
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
    // URL が空のファイルは除外し、サムネイル・原寸・alt のインデックスを揃える
    val imageFiles = files.filter { it.type.startsWith("image/") && it.url.isNotEmpty() }
    val imageUrls = imageFiles
        .map { it.thumbnailUrl?.takeIf { url -> url.isNotEmpty() } ?: it.url }
        .takeIf { it.isNotEmpty() }
    val imageFullUrls = imageFiles
        .map { it.url }
        .takeIf { it.isNotEmpty() }
    val imageAlts = imageFiles
        .map { it.comment.orEmpty() }
        .takeIf { imageFiles.isNotEmpty() }
    return RssItem(
        title = user.name ?: "@${user.username}",
        link = noteUrl,
        description = displayText,
        pubDate = createdAt,
        guid = id,
        authorName = user.name ?: "@${user.username}",
        authorAvatarUrl = user.avatarUrl,
        imageUrls = imageUrls,
        imageFullUrls = imageFullUrls,
        imageAlts = imageAlts,
        pubDateMillis = app.focus.personal.util.DateUtils.parseIso8601ToMillis(createdAt),
        kind = ItemKind.SNS_POST,
    )
}