package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.RssFeed
import app.focus.personal.model.RssItem
import app.focus.personal.network.YahooRssClient
import app.focus.personal.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RssRepository(
    private val database: FocusDatabase?,
    private val api: YahooRssClient
) {
    private val queries = database?.focusDatabaseQueries

    suspend fun refreshTopics(category: String = "top-picks") {
        val feed = api.fetchTopicRss(category)
        saveFeed(feed, "topic")
    }

    suspend fun refreshCategory(category: String) {
        val feed = api.fetchCategoryRss(category)
        saveFeed(feed, "category")
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
