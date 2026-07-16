package app.focus.personal

import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.FocusApiClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.MisskeyClient
import app.focus.personal.repository.FeedRepository
import app.focus.personal.repository.RssRepository
import app.focus.personal.repository.ServerRssRepository
import app.focus.personal.viewmodel.FeedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json

/**
 * 各プラットフォームのエントリーポイントから呼ぶ共通ファクトリ。
 * HttpClient(JSON 設定込み)・Repository・ViewModel の組み立てを一元化する。
 *
 * @param engine プラットフォーム別 HTTP エンジン(OkHttp / Darwin 等)。null ならデフォルトエンジン(Web)。
 * @param database プラットフォーム別 DB。null なら永続化なし(Web)。
 * @param serverBaseUrl 指定すると自前 Ktor サーバー経由の ServerRssRepository を使う(Android)。
 *   null なら各ソースへ直接アクセスする RssRepository を使う。
 */
fun createFeedViewModel(
    scope: CoroutineScope,
    database: FocusDatabase? = null,
    engine: HttpClientEngineFactory<*>? = null,
    serverBaseUrl: String? = null,
): FeedViewModel {
    val client = createHttpClient(engine)
    val repository: FeedRepository = if (serverBaseUrl != null) {
        ServerRssRepository(
            database = database,
            apiClient = FocusApiClient(client, serverBaseUrl),
            blueskyApi = BlueskyClient(client),
        )
    } else {
        RssRepository(
            database = database,
            googleApi = GoogleRssClient(client),
            hatenaApi = HatenaRssClient(client),
            blueskyApi = BlueskyClient(client),
            misskeyApi = MisskeyClient(client),
        )
    }
    return FeedViewModel(repository, scope)
}

private fun createHttpClient(engine: HttpClientEngineFactory<*>?): HttpClient =
    if (engine != null) {
        HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    } else {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
