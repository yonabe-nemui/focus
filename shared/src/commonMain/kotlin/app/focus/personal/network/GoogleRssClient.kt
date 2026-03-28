package app.focus.personal.network

import app.focus.personal.model.RssFeed
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy

class GoogleRssClient(private val client: HttpClient) {
    private val xml = XML {
        autoPolymorphic = true
        policy = DefaultXmlSerializationPolicy {
            ignoreUnknownChildren()
        }
    }

    private val baseParams = "hl=ja&gl=JP&ceid=JP:ja"

    suspend fun fetchTopStories(): RssFeed {
        val url = "https://news.google.com/rss?$baseParams"
        return fetchAndParse(url)
    }

    suspend fun fetchTopicRss(topic: String): RssFeed {
        val url = "https://news.google.com/rss/headlines/section/topic/$topic?$baseParams"
        return fetchAndParse(url)
    }

    private suspend fun fetchAndParse(url: String): RssFeed {
        val response = client.get(url)
        val body = response.bodyAsText()
        return xml.decodeFromString(RssFeed.serializer(), body)
    }
}
