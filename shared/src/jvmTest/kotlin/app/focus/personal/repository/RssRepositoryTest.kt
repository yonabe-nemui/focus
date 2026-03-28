package app.focus.personal.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.YahooRssClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RssRepositoryTest {

    private lateinit var database: FocusDatabase
    private val mockXml = """
        <rss version="2.0">
          <channel>
            <title>Test Channel</title>
            <link>link</link>
            <description>desc</description>
            <item>
              <title>News 1 (Older)</title>
              <link>link1</link>
              <pubDate>Fri, 28 Mar 2025 15:45:00 +0900</pubDate>
            </item>
            <item>
              <title>News 2 (Newer)</title>
              <link>link2</link>
              <pubDate>Fri, 28 Mar 2025 15:46:00 +0900</pubDate>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FocusDatabase.Schema.create(driver)
        database = FocusDatabase(driver)
    }

    @Test
    fun testRefreshAndGetItemsSorted() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = mockXml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/xml")
            )
        }
        val api = YahooRssClient(HttpClient(mockEngine))
        val repository = RssRepository(database, api)

        // データの取得と保存を実行
        repository.refreshTopics("top-picks")

        // DBから取得
        val items = repository.getItemsByCategory("topic").first()

        assertEquals(2, items.size)
        // 最新順（News 2 -> News 1）になっていることを確認
        assertEquals("News 2 (Newer)", items[0].title)
        assertEquals("News 1 (Older)", items[1].title)
    }
}
