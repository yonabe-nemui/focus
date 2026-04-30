package app.focus.personal.repository

import app.focus.personal.db.BlueskySessionEntity
import app.focus.personal.db.FocusDatabase
import app.focus.personal.db.RssChannelEntity
import app.focus.personal.db.RssItemEntity
import app.focus.personal.model.BlueskySearchResponse
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MutedWord
import app.focus.personal.model.RssFeed
import app.focus.personal.model.RssItem
import app.focus.personal.model.toRssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RssRepository(
    private val database: FocusDatabase?,
    private val googleApi: GoogleRssClient,
    private val hatenaApi: HatenaRssClient,
    private val blueskyApi: BlueskyClient
) : FeedRepository {
    private val queries = database?.focusDatabaseQueries

    private val googleTopics = listOf(
        "WORLD", "NATION", "BUSINESS", "TECHNOLOGY", 
        "ENTERTAINMENT", "SPORTS", "SCIENCE", "HEALTH"
    )

    override suspend fun fetchAllGoogleTopics(): List<RssItem> = coroutineScope {
        val deferredTop = async {
            try { googleApi.fetchTopStories().channel.items } catch (e: Exception) { emptyList() }
        }
        val deferredOthers = googleTopics.map { topic ->
            async {
                try { googleApi.fetchTopicRss(topic).channel.items } catch (e: Exception) { emptyList() }
            }
        }

        (listOf(deferredTop) + deferredOthers).awaitAll()
            .flatten()
            .distinctBy { it.guid ?: it.link }
            .sortedByDescending { DateUtils.parseRfc822ToMillis(it.pubDate) }
    }

    override suspend fun fetchAllHatenaEntries(): List<RssItem> = coroutineScope {
        val deferredHot = async { try { hatenaApi.fetchHotEntry().items } catch (e: Exception) { emptyList() } }
        val deferredNew = async { try { hatenaApi.fetchEntryList().items } catch (e: Exception) { emptyList() } }
        val deferredIt = async { try { hatenaApi.fetchItHotEntry().items } catch (e: Exception) { emptyList() } }

        awaitAll(deferredHot, deferredNew, deferredIt)
            .flatten()
            .distinctBy { it.link }
            .map { it.toRssItem() }
            .sortedByDescending { DateUtils.parseIso8601ToMillis(it.pubDate) }
    }

    suspend fun fetchHatenaHotEntries(): List<RssItem> {
        return try {
            hatenaApi.fetchHotEntry().items.map { it.toRssItem() }
                .sortedByDescending { DateUtils.parseIso8601ToMillis(it.pubDate) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchHatenaNewEntries(): List<RssItem> {
        return try {
            hatenaApi.fetchEntryList().items.map { it.toRssItem() }
                .sortedByDescending { DateUtils.parseIso8601ToMillis(it.pubDate) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchHatenaItEntries(): List<RssItem> {
        return try {
            hatenaApi.fetchItHotEntry().items.map { it.toRssItem() }
                .sortedByDescending { DateUtils.parseIso8601ToMillis(it.pubDate) }
        } catch (e: Exception) {
            emptyList()
        }
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
    }

    override suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession {
        val session = blueskyApi.refreshSession(refreshJwt)
        saveBlueskySession(session)
        return session
    }

    override suspend fun getBlueskyMutedWords(session: BlueskySession): List<MutedWord> {
        return try {
            blueskyApi.getMutedWords(session)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun fetchBlueskyEntries(
        query: String,
        session: BlueskySession?,
        mutedWords: List<MutedWord>
    ): List<RssItem> {
        val response = try {
            blueskyApi.searchPosts(query, session = session)
        } catch (e: Exception) {
            BlueskySearchResponse(emptyList())
        }
        
        return response.posts
            .filter { post ->
                // 「見たくないものは見ない」フィルタリング
                val text = post.record.text.lowercase()
                mutedWords.none { word -> 
                    word.value.lowercase().isNotEmpty() && text.contains(word.value.lowercase())
                }
            }
            .map { it.toRssItem() }
            .sortedByDescending { DateUtils.parseIso8601ToMillis(it.pubDate) }
    }

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
