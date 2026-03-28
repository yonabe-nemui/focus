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
              <title>News 1</title>
              <link>link1</link>
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
    fun testRefreshAndGetItems() = runTest {
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

        assertEquals(1, items.size)
        assertEquals("News 1", items[0].title)
        assertEquals("link1", items[0].link)
    }
}
