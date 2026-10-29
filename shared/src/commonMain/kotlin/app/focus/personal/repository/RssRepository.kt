package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskyPost
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import app.focus.personal.model.toRssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.MisskeyClient
import app.focus.personal.util.DateUtils
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.Clock

/** 各ソースへ直接アクセスする Repository(iOS・Desktop・Web・サーバーで使用)。 */
class RssRepository(
    database: FocusDatabase?,
    private val googleApi: GoogleRssClient,
    blueskyApi: BlueskyClient,
    private val misskeyApi: MisskeyClient,
) : BaseFeedRepository(database, blueskyApi) {

    private val googleTopics = listOf(
        "WORLD",
        "NATION",
        "BUSINESS",
        "TECHNOLOGY",
        "ENTERTAINMENT",
        "SPORTS",
        "SCIENCE",
        "HEALTH",
    )

    // BlueSky 公式ミュートワードのキャッシュ（セッション DID をキー、5 分 TTL）
    private var mutedWordsCache: Pair<String, List<String>>? = null
    private var mutedWordsCacheAt: Long = 0L
    private val mutedWordsCacheTtlMs = 5 * 60 * 1000L

    private suspend fun getCachedMutedWords(session: BlueskySession): List<String> {
        val now = Clock.System.now().toEpochMilliseconds()
        val cached = mutedWordsCache
        if (cached != null && cached.first == session.did && now - mutedWordsCacheAt < mutedWordsCacheTtlMs) {
            return cached.second
        }
        return try {
            val fresh = blueskyApi.getMutedWords(session)
            mutedWordsCache = session.did to fresh
            mutedWordsCacheAt = now
            fresh
        } catch (e: Exception) {
            Napier.w("BlueSky getMutedWords failed: ${e.message}")
            cached?.second ?: emptyList()
        }
    }

    /** 失敗したソースは空リストとして扱い、他のソースの結果は返す。 */
    private suspend fun <T> fetchSafe(label: String, block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: Exception) {
        Napier.w("$label fetch failed: ${e.message}")
        emptyList()
    }

    override suspend fun fetchAllGoogleTopics(): List<RssItem> = coroutineScope {
        val deferredTop = async { fetchSafe("Google top") { googleApi.fetchTopStories().channel.items } }
        val deferredOthers = googleTopics.map { topic ->
            async { fetchSafe("Google $topic") { googleApi.fetchTopicRss(topic).channel.items } }
        }

        (listOf(deferredTop) + deferredOthers).awaitAll()
            .flatten()
            .map { it.copy(pubDateMillis = DateUtils.parseRfc822ToMillis(it.pubDate)) }
            .distinctBy { it.guid ?: it.link }
            .sortedByDescending { it.pubDateMillis }
            .let(muteWordStore::filter)
    }

    override fun clearBlueskySession() {
        super.clearBlueskySession()
        mutedWordsCache = null
        mutedWordsCacheAt = 0L
    }

    override suspend fun fetchBlueskyPage(query: String, session: BlueskySession?, cursor: String?): PagedFeedResponse {
        val mutedWords = if (session != null) getCachedMutedWords(session) else emptyList()

        var posts: List<BlueskyPost> = emptyList()
        var nextCursor: String? = null
        when {
            session != null && query.isEmpty() -> {
                val response = blueskyApi.getTimeline(session, cursor = cursor)
                posts = response.feed.map { it.post }
                nextCursor = response.cursor
            }

            query.isNotEmpty() -> {
                val response = blueskyApi.searchPosts(query, session = session, cursor = cursor)
                posts = response.posts
                nextCursor = response.cursor
            }
        }

        val items = posts
            .filter { post -> mutedWords.none { MuteWordStore.matchesMutedWord(post.record.text, it) } }
            .map { it.toRssItem() }
            .sortedByDescending { it.pubDateMillis }
            .let(muteWordStore::filter)

        return PagedFeedResponse(items, nextCursor)
    }

    override suspend fun fetchMisskeyPage(query: String, settings: MisskeySettings, untilId: String?): List<RssItem> {
        val token = settings.apiToken
        val notes = when {
            token != null && query.isEmpty() ->
                fetchSafe("Misskey home") { misskeyApi.getHomeTimeline(settings.instanceUrl, token, untilId = untilId) }

            query.isNotEmpty() ->
                fetchSafe("Misskey search") { misskeyApi.searchNotes(settings.instanceUrl, query, token = token, untilId = untilId) }

            else -> emptyList()
        }
        return notes
            .map { it.toRssItem(settings.instanceUrl) }
            .sortedByDescending { it.pubDateMillis }
            .let(muteWordStore::filter)
    }
}
