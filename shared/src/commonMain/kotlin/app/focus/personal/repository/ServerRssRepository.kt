package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.FocusApiClient

class ServerRssRepository(
    database: FocusDatabase?,
    private val apiClient: FocusApiClient,
    private val blueskyApi: BlueskyClient
) : FeedRepository {

    private val sessionStore = SessionStore(database)
    private val muteWordStore = MuteWordStore(database)

    override suspend fun fetchAllGoogleTopics(): List<RssItem> =
        muteWordStore.filter(apiClient.fetchGoogleFeed())

    override suspend fun fetchAllHatenaEntries(): List<RssItem> =
        muteWordStore.filter(apiClient.fetchHatenaFeed())

    override suspend fun fetchBlueskyPage(query: String, session: BlueskySession?, cursor: String?): PagedFeedResponse {
        val page = apiClient.fetchBlueskyPage(session?.accessJwt, query, cursor)
        return PagedFeedResponse(muteWordStore.filter(page.items), page.nextCursor)
    }

    override suspend fun loginBluesky(
        handle: String,
        appPassword: String,
        authCode: String?
    ): BlueskySession {
        val session = blueskyApi.createSession(handle, appPassword, authCode)
        saveBlueskySession(session)
        return session
    }

    override fun getSavedBlueskySession(): BlueskySession? = sessionStore.getBlueskySession()

    override fun saveBlueskySession(session: BlueskySession) = sessionStore.saveBlueskySession(session)

    override fun clearBlueskySession() = sessionStore.clearBlueskySession()

    override suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession {
        val session = blueskyApi.refreshSession(refreshJwt)
        saveBlueskySession(session)
        return session
    }

    override suspend fun fetchMisskeyEntries(query: String, settings: MisskeySettings): List<RssItem> =
        fetchMisskeyPage(query, settings, null)

    override suspend fun fetchMisskeyPage(query: String, settings: MisskeySettings, untilId: String?): List<RssItem> =
        muteWordStore.filter(apiClient.fetchMisskeyPage(settings.instanceUrl, settings.apiToken, query, untilId))

    override fun getSavedMisskeySettings(): MisskeySettings? = sessionStore.getMisskeySettings()

    override fun saveMisskeySettings(settings: MisskeySettings) = sessionStore.saveMisskeySettings(settings)

    override fun clearMisskeySettings() = sessionStore.clearMisskeySettings()

    // ローカルミュートワードは他プラットフォームと同様にクライアント側で管理・適用する
    override suspend fun fetchMuteWords(): List<String> = muteWordStore.getAll()

    override suspend fun addMuteWord(word: String) = muteWordStore.add(word)

    override suspend fun deleteMuteWord(word: String) = muteWordStore.delete(word)
}
