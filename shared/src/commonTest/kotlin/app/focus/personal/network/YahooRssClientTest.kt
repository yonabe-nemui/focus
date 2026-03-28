package app.focus.personal.network

import app.focus.personal.model.RssFeed
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class YahooRssClientTest {

    private val mockXml = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <rss version="2.0">
          <channel>
            <title>Yahoo!ニュース・トピックス - 主要</title>
            <link>https://news.yahoo.co.jp/topics/top-picks?source=rss</link>
            <description>Yahoo! JAPANのニュース・トピックスで取り扱っている最新ニュースを配信しています。</description>
            <item>
              <title>Test News Title</title>
              <link>https://news.yahoo.co.jp/articles/test</link>
              <description>Test Description</description>
              <pubDate>Sat, 28 Mar 2026 12:00:00 JST</pubDate>
              <guid isPermaLink="false">test-guid</guid>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun testFetchAndParseRss() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = mockXml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/xml")
            )
        }
        val client = HttpClient(mockEngine)
        val yahooClient = YahooRssClient(client)

        val feed = yahooClient.fetchTopicRss("top-picks")

        assertEquals("Yahoo!ニュース・トピックス - 主要", feed.channel.title)
        assertEquals(1, feed.channel.items.size)
        assertEquals("Test News Title", feed.channel.items[0].title)
        assertEquals("https://news.yahoo.co.jp/articles/test", feed.channel.items[0].link)
    }
}
