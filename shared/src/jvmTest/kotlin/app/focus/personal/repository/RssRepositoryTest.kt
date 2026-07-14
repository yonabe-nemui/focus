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

    private fun createRepository(): RssRepository {
        val mockEngine = MockEngine { _ ->
            respond(
                content = mockXml,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/xml")
            )
        }
        val client = HttpClient(mockEngine)
        return RssRepository(
            database = database,
            googleApi = GoogleRssClient(client),
            hatenaApi = HatenaRssClient(client),
            blueskyApi = BlueskyClient(client),
            misskeyApi = MisskeyClient(client)
        )
    }

    @Test
    fun testFetchAllGoogleTopics() = runTest {
        val repository = createRepository()

        // データの取得を実行
        val items = repository.fetchAllGoogleTopics()

        // モックは全リクエストで同じ link1/link2 を返すため distinctBy で 2 件に縮退する
        assertEquals(2, items.size)
        // 最新順（News 2 -> News 1）になっていることを確認
        assertEquals("News 2 (Newer)", items[0].title)
        assertEquals("News 1 (Older)", items[1].title)
    }

    @Test
    fun testMuteWordFiltersFeed() = runTest {
        val repository = createRepository()
        repository.addMuteWord("Older")

        val items = repository.fetchAllGoogleTopics()

        assertEquals(1, items.size)
        assertEquals("News 2 (Newer)", items[0].title)
    }

    @Test
    fun testMuteWordsPersistedInDatabase() = runTest {
        val repository = createRepository()
        repository.addMuteWord("foo")
        repository.addMuteWord("foo") // 重複追加は無視される

        // 別インスタンスからも DB 経由で読めること（永続化の確認）
        val another = createRepository()
        assertEquals(listOf("foo"), another.fetchMuteWords())

        another.deleteMuteWord("foo")
        assertEquals(emptyList(), another.fetchMuteWords())

        // 削除も DB に反映されている
        assertEquals(emptyList(), createRepository().fetchMuteWords())
    }
}
