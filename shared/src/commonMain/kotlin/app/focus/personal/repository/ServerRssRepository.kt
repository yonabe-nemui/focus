package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.RssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.FocusApiClient

class ServerRssRepository(
    private val database: FocusDatabase?,
    private val apiClient: FocusApiClient,
    private val blueskyApi: BlueskyClient
) : FeedRepository {

    private val queries = database?.focusDatabaseQueries

    override suspend fun fetchAllGoogleTopics(): List<RssItem> = apiClient.fetchGoogleFeed()

    override suspend fun fetchAllHatenaEntries(): List<RssItem> = apiClient.fetchHatenaFeed()

    override suspend fun fetchBlueskyEntries(query: String, session: BlueskySession?): List<RssItem> {
        return try {
            apiClient.fetchBlueskyFeed(session?.accessJwt ?: "", query)
        } catch (e: Exception) {
            emptyList()
        }
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

    override fun getSavedBlueskySession(): BlueskySession? {
        return queries?.getActiveBlueskySession()?.executeAsOneOrNull()?.let { entity ->
            BlueskySession(
                accessJwt = entity.accessJwt,
                refreshJwt = entity.refreshJwt,
                handle = entity.handle,
                did = entity.did
            )
        }
    }

    override fun saveBlueskySession(session: BlueskySession) {
        queries?.transaction {
            queries.clearActiveBlueskySession()
            queries.upsertBlueskySession(
                handle = session.handle,
                accessJwt = session.accessJwt,
                refreshJwt = session.refreshJwt,
                did = session.did,
                isActive = 1L
            )
        }
    }

    override fun clearBlueskySession() {
        queries?.clearActiveBlueskySession()
    }

    override suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession {
        val session = blueskyApi.refreshSession(refreshJwt)
        saveBlueskySession(session)
        return session
    }

    override suspend fun fetchMisskeyEntries(query: String, settings: MisskeySettings): List<RssItem> {
        return try {
            apiClient.fetchMisskeyFeed(settings.instanceUrl, settings.apiToken, query)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun getSavedMisskeySettings(): MisskeySettings? {
        return database?.focusDatabaseQueries?.getActiveMisskeySettings()?.executeAsOneOrNull()?.let { entity ->
            MisskeySettings(
                instanceUrl = entity.instanceUrl,
                apiToken = entity.apiToken
            )
        }
    }

    override fun saveMisskeySettings(settings: MisskeySettings) {
        database?.focusDatabaseQueries?.upsertMisskeySettings(
            instanceUrl = settings.instanceUrl,
            apiToken = settings.apiToken
        )
    }

    override fun clearMisskeySettings() {
        database?.focusDatabaseQueries?.clearMisskeySettings()
    }

    override suspend fun fetchMuteWords(): List<String> = apiClient.getMuteWords()

    override suspend fun addMuteWord(word: String) = apiClient.addMuteWord(word)

    override suspend fun deleteMuteWord(word: String) = apiClient.deleteMuteWord(word)
}
