package app.focus.personal.repository

import app.focus.personal.db.BlueskySessionEntity
import app.focus.personal.db.FocusDatabase
import app.focus.personal.db.RssChannelEntity
import app.focus.personal.db.RssItemEntity
import app.focus.personal.model.BlueskyPost
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.HatenaItem
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.MutedWord
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssFeed
import app.focus.personal.model.RssItem
import app.focus.personal.model.toRssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.MisskeyClient
import app.focus.personal.util.DateUtils
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class RssRepository(
    private val database: FocusDatabase?,
    private val googleApi: GoogleRssClient,
    private val hatenaApi: HatenaRssClient,
    private val blueskyApi: BlueskyClient,
    private val misskeyApi: MisskeyClient
) : FeedRepository {
    private val queries = database?.focusDatabaseQueries

    private val googleTopics = listOf(
        "WORLD", "NATION", "BUSINESS", "TECHNOLOGY",
        "ENTERTAINMENT", "SPORTS", "SCIENCE", "HEALTH"
    )

    // BlueSky ミュートワードのキャッシュ（セッション DID をキー、5 分 TTL）
    private var mutedWordsCache: Pair<String, List<MutedWord>>? = null
    private var mutedWordsCacheAt: Long = 0L
    private val mutedWordsCacheTtlMs = 5 * 60 * 1000L

    @OptIn(ExperimentalTime::class)
    private suspend fun getCachedMutedWords(session: BlueskySession): List<MutedWord> {
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

    // ASCII のみのワードは単語境界マッチで誤検出を防ぐ ("book" が "facebook" にマッチしない)
    // CJK 等を含む場合は単語境界の概念がないため部分一致を許容する
    private val asciiOnly = Regex("^[\\x00-\\x7F]+$")

    private fun matchesMutedWord(text: String, word: String): Boolean {
        if (word.isEmpty()) return false
        return if (asciiOnly.matches(word)) {
            Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        } else {
            text.contains(word, ignoreCase = true)
        }
    }

    private suspend fun fetchGoogleTopicSafe(label: String, block: suspend () -> List<RssItem>): List<RssItem> = try {
        block()
    } catch (e: Exception) {
        Napier.w("Google $label fetch failed: ${e.message}")
        emptyList()
    }

    private suspend fun fetchHatenaSafe(label: String, block: suspend () -> List<HatenaItem>): List<HatenaItem> = try {
        block()
    } catch (e: Exception) {
        Napier.w("Hatena $label fetch failed: ${e.message}")
        emptyList()
    }

    override suspend fun fetchAllGoogleTopics(): List<RssItem> = coroutineScope {
        val deferredTop = async { fetchGoogleTopicSafe("top") { googleApi.fetchTopStories().channel.items } }
        val deferredOthers = googleTopics.map { topic ->
            async { fetchGoogleTopicSafe(topic) { googleApi.fetchTopicRss(topic).channel.items } }
        }

        (listOf(deferredTop) + deferredOthers).awaitAll()
            .flatten()
            .map { it.copy(pubDateMillis = DateUtils.parseRfc822ToMillis(it.pubDate)) }
            .distinctBy { it.guid ?: it.link }
            .sortedByDescending { it.pubDateMillis }
    }

    override suspend fun fetchAllHatenaEntries(): List<RssItem> = coroutineScope {
        val deferredHot = async { fetchHatenaSafe("hot") { hatenaApi.fetchHotEntry().items } }
        val deferredNew = async { fetchHatenaSafe("new") { hatenaApi.fetchEntryList().items } }
        val deferredIt = async { fetchHatenaSafe("it") { hatenaApi.fetchItHotEntry().items } }

        awaitAll(deferredHot, deferredNew, deferredIt)
            .flatten()
            .distinctBy { it.link }
            .map { it.toRssItem() }
            .sortedByDescending { it.pubDateMillis }
    }

    override suspend fun loginBluesky(
        handle: String,
        appPassword: String,
        authCode: String?
    ): BlueskySession {
        val session = blueskyApi.createSession(handle, appPassword, authCode)
        saveBlueskySession(session)
        return session
    }

    override fun getSavedBlueskySession(): BlueskySession? {
        return queries?.getActiveBlueskySession()?.executeAsOneOrNull()?.let { entity ->
            BlueskySession(
                accessJwt = entity.accessJwt,
                refreshJwt = entity.refreshJwt,
                handle = entity.handle,
                did = entity.did
            )
        }
    }

    override fun saveBlueskySession(session: BlueskySession) {
        queries?.transaction {
            queries.clearActiveBlueskySession()
            queries.upsertBlueskySession(
                handle = session.handle,
                accessJwt = session.accessJwt,
                refreshJwt = session.refreshJwt,
                did = session.did,
                isActive = 1L
            )
        }
    }

    override fun clearBlueskySession() {
        queries?.clearActiveBlueskySession()
        mutedWordsCache = null
        mutedWordsCacheAt = 0L
    }

    override suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession {
        val session = blueskyApi.refreshSession(refreshJwt)
        saveBlueskySession(session)
        return session
    }

    override suspend fun fetchBlueskyEntries(query: String, session: BlueskySession?): List<RssItem> =
        fetchBlueskyPage(query, session, null).items

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
            .filter { post ->
                val text = post.record.text
                mutedWords.none { matchesMutedWord(text, it.value) }
            }
            .map { it.toRssItem() }
            .sortedByDescending { it.pubDateMillis }

        return PagedFeedResponse(items, nextCursor)
    }

    override suspend fun fetchMisskeyEntries(query: String, settings: MisskeySettings): List<RssItem> =
        fetchMisskeyPage(query, settings, null)

    override suspend fun fetchMisskeyPage(query: String, settings: MisskeySettings, untilId: String?): List<RssItem> {
        val token = settings.apiToken
        val notes = when {
            token != null && query.isEmpty() ->
                fetchMisskeySafe("home") { misskeyApi.getHomeTimeline(settings.instanceUrl, token, untilId = untilId) }
            token != null && query.isNotEmpty() ->
                fetchMisskeySafe("search-auth") { misskeyApi.searchNotes(settings.instanceUrl, query, token = token, untilId = untilId) }
            query.isNotEmpty() ->
                fetchMisskeySafe("search") { misskeyApi.searchNotes(settings.instanceUrl, query, untilId = untilId) }
            else -> emptyList()
        }
        return notes
            .map { it.toRssItem(settings.instanceUrl) }
            .sortedByDescending { it.pubDateMillis }
    }

    private suspend fun <T> fetchMisskeySafe(label: String, block: suspend () -> List<T>): List<T> = try {
        block()
    } catch (e: Exception) {
        Napier.w("Misskey $label fetch failed: ${e.message}")
        emptyList()
    }

    override fun getSavedMisskeySettings(): MisskeySettings? {
        return queries?.getActiveMisskeySettings()?.executeAsOneOrNull()?.let { entity ->
            MisskeySettings(
                instanceUrl = entity.instanceUrl,
                apiToken = entity.apiToken
            )
        }
    }

    override fun saveMisskeySettings(settings: MisskeySettings) {
        queries?.upsertMisskeySettings(
            instanceUrl = settings.instanceUrl,
            apiToken = settings.apiToken
        )
    }

    override fun clearMisskeySettings() {
        queries?.clearMisskeySettings()
    }

    override suspend fun fetchMuteWords(): List<String> = emptyList()
    override suspend fun addMuteWord(word: String) = Unit
    override suspend fun deleteMuteWord(word: String) = Unit

    private fun saveFeed(feed: RssFeed, dbCategory: String) {
        val db = database ?: return
        val q = queries ?: return
        db.transaction {
            q.deleteChannelByCategory(dbCategory)
            q.insertChannel(
                title = feed.channel.title,
                link = feed.channel.link,
                description = feed.channel.description ?: "",
                category = dbCategory
            )
            val channelId = q.lastInsertedId().executeAsOne()
            feed.channel.items.forEach { item ->
                q.insertItem(
                    channelId = channelId,
                    title = item.title,
                    link = item.link,
                    description = item.description ?: "",
                    pubDate = item.pubDate ?: "",
                    pubDateMillis = DateUtils.parseRfc822ToMillis(item.pubDate),
                    guid = item.guid ?: ""
                )
            }
        }
    }

    fun getPagedItemsByCategory(
        dbCategory: String,
        limit: Long = 20,
        offset: Long = 0
    ): Flow<List<RssItem>> = flow {
        val q = queries
        if (q == null) {
            emit(emptyList())
            return@flow
        }
        val items = q.selectPagedItemsByCategory(dbCategory, limit, offset).executeAsList().map { entity ->
            RssItem(
                title = entity.title,
                link = entity.link,
                description = entity.description,
                pubDate = entity.pubDate,
                guid = entity.guid
            )
        }
        emit(items)
    }

    fun getItemsByCategory(dbCategory: String): Flow<List<RssItem>> = flow {
        val q = queries
        if (q == null) {
            emit(emptyList())
            return@flow
        }
        val channel = q.selectAllChannelsByCategory(dbCategory).executeAsOneOrNull()
        if (channel != null) {
            val items = q.selectItemsByChannelId(channel.id).executeAsList().map { entity ->
                RssItem(
                    title = entity.title,
                    link = entity.link,
                    description = entity.description,
                    pubDate = entity.pubDate,
                    guid = entity.guid
                )
            }
            emit(items)
        } else {
            emit(emptyList())
        }
    }
}
