package app.focus.personal.network

import app.focus.personal.model.RssFeed
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy

class YahooRssClient(private val client: HttpClient) {
    private val xml = XML {
        autoPolymorphic = true
        policy = DefaultXmlSerializationPolicy {
            ignoreUnknownChildren()
        }
    }

    suspend fun fetchTopicRss(category: String = "top-picks"): RssFeed {
        val url = "https://news.yahoo.co.jp/rss/topics/$category.xml"
        return fetchAndParse(url)
    }

    suspend fun fetchCategoryRss(category: String): RssFeed {
        val url = "https://news.yahoo.co.jp/rss/categories/$category.xml"
        return fetchAndParse(url)
    }

    private suspend fun fetchAndParse(url: String): RssFeed {
        val response = client.get(url)
        val body = response.bodyAsText()
        println("DEBUG: RSS Response Body: ${body.take(500)}...") // 最初の500文字
        val feed = xml.decodeFromString(RssFeed.serializer(), body)
        println("DEBUG: Parsed Feed Channel Title: '${feed.channel.title}'")
        println("DEBUG: Parsed Feed Items Count: ${feed.channel.items.size}")
        return feed
    }
}
