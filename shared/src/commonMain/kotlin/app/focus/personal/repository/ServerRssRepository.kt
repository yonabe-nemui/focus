package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.FocusApiClient

/** フィード取得を自前 Ktor サーバー経由で行う Repository(Android で使用)。 */
class ServerRssRepository(
    database: FocusDatabase?,
    private val apiClient: FocusApiClient,
    blueskyApi: BlueskyClient,
) : BaseFeedRepository(database, blueskyApi) {

    override suspend fun fetchAllGoogleTopics(): List<RssItem> =
        muteWordStore.filter(apiClient.fetchGoogleFeed())

    override suspend fun fetchBlueskyPage(query: String, session: BlueskySession?, cursor: String?): PagedFeedResponse {
        val page = apiClient.fetchBlueskyPage(session?.accessJwt, query, cursor)
        return PagedFeedResponse(muteWordStore.filter(page.items), page.nextCursor)
    }

    override suspend fun fetchMisskeyPage(query: String, settings: MisskeySettings, untilId: String?): List<RssItem> =
        muteWordStore.filter(apiClient.fetchMisskeyPage(settings.instanceUrl, settings.apiToken, query, untilId))
}
