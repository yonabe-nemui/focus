package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.RssFeed
import app.focus.personal.model.RssItem
import app.focus.personal.network.YahooRssClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RssRepository(
    private val database: FocusDatabase,
    private val api: YahooRssClient
) {
    private val queries = database.focusDatabaseQueries

    suspend fun refreshTopics(category: String = "top-picks") {
        val feed = api.fetchTopicRss(category)
        saveFeed(feed, "topic")
    }

    suspend fun refreshCategory(category: String) {
        val feed = api.fetchCategoryRss(category)
        saveFeed(feed, "category")
    }

    private fun saveFeed(feed: RssFeed, dbCategory: String) {
        database.transaction {
            queries.deleteChannelByCategory(dbCategory)
            queries.insertChannel(
                title = feed.channel.title,
                link = feed.channel.link,
                description = feed.channel.description,
                category = dbCategory
            )
            val channelId = queries.lastInsertedId().executeAsOne()
            feed.channel.items.forEach { item ->
                queries.insertItem(
                    channelId = channelId,
                    title = item.title,
                    link = item.link,
                    description = item.description,
                    pubDate = item.pubDate,
                    guid = item.guid
                )
            }
        }
    }

    fun getItemsByCategory(dbCategory: String): Flow<List<RssItem>> = flow {
        val channel = queries.selectAllChannelsByCategory(dbCategory).executeAsOneOrNull()
        if (channel != null) {
            val items = queries.selectItemsByChannelId(channel.id).executeAsList().map { entity ->
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
