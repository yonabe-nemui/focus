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
        return xml.decodeFromString(RssFeed.serializer(), body)
    }
}
