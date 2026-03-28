package app.focus.personal.network

import app.focus.personal.model.RssFeed
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.XML

class YahooRssClient(private val client: HttpClient) {
    private val xml = XML {
        autoPolymorphic = true
        // XMLUtil の戻り値の型に合わせて emptyList() を返すように修正
        unknownChildHandler = { _, _, _, _, _ -> emptyList() } 
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
        return xml.decodeFromString(RssFeed.serializer(), body)
    }
}
