package app.focus.personal.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.MisskeyClient
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
    fun testFetchAllGoogleTopics() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                content = mockXml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/xml")
            )
        }
        val client = HttpClient(mockEngine)
        val googleApi = GoogleRssClient(client)
        val hatenaApi = HatenaRssClient(client)
        val blueskyApi = BlueskyClient(client)
        val misskeyApi = MisskeyClient(client)
        val repository = RssRepository(database, googleApi, hatenaApi, blueskyApi, misskeyApi)

        // データの取得を実行
        val items = repository.fetchAllGoogleTopics()

        // モックは全リクエストで同じ link1/link2 を返すため distinctBy で 2 件に縮退する
        assertEquals(2, items.size)
        // 最新順（News 2 -> News 1）になっていることを確認
        assertEquals("News 2 (Newer)", items[0].title)
        assertEquals("News 1 (Older)", items[1].title)
    }
}
