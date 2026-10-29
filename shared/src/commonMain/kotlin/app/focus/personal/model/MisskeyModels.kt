package app.focus.personal.model

import app.focus.personal.util.DateUtils
import kotlinx.serialization.Serializable

// Misskey API のレスポンスのうち、アプリで使う項目だけを定義する(未知のキーは無視してデコード)

@Serializable
data class MisskeyNote(
    val id: String,
    val createdAt: String,
    val text: String? = null,
    val cw: String? = null,
    val user: MisskeyUser,
    val files: List<MisskeyFile> = emptyList(),
)

@Serializable
data class MisskeyFile(
    val type: String = "",
    val url: String = "",
    val thumbnailUrl: String? = null,
    // 画像の代替テキスト（Misskey ではキャプション）
    val comment: String? = null,
)

@Serializable
data class MisskeyUser(val username: String, val name: String? = null, val avatarUrl: String? = null)

data class MisskeySettings(val instanceUrl: String, val apiToken: String? = null)

fun MisskeyNote.toRssItem(instanceUrl: String): RssItem {
    val displayText = if (cw != null) "[$cw]\n${text.orEmpty()}" else text.orEmpty()
    val name = user.name ?: "@${user.username}"
    // URL が空のファイルは除外し、サムネイル・原寸・alt のインデックスを揃える
    val imageFiles = files.filter { it.type.startsWith("image/") && it.url.isNotEmpty() }.takeIf { it.isNotEmpty() }
    return RssItem(
        title = name,
        link = "https://$instanceUrl/notes/$id",
        description = displayText,
        pubDate = createdAt,
        guid = id,
        authorName = name,
        authorAvatarUrl = user.avatarUrl,
        imageUrls = imageFiles?.map { it.thumbnailUrl?.takeIf { url -> url.isNotEmpty() } ?: it.url },
        imageFullUrls = imageFiles?.map { it.url },
        imageAlts = imageFiles?.map { it.comment.orEmpty() },
        pubDateMillis = DateUtils.parseIso8601ToMillis(createdAt),
        kind = ItemKind.SNS_POST,
    )
}
