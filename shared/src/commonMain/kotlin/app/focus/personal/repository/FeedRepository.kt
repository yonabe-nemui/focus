package app.focus.personal.repository

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem

interface FeedRepository {
    suspend fun fetchAllGoogleTopics(): List<RssItem>
    suspend fun fetchAllHatenaEntries(): List<RssItem>
    suspend fun fetchBlueskyPage(query: String, session: BlueskySession? = null, cursor: String? = null): PagedFeedResponse
    suspend fun loginBluesky(handle: String, appPassword: String, authCode: String? = null): BlueskySession
    fun getSavedBlueskySession(): BlueskySession?
    fun saveBlueskySession(session: BlueskySession)
    fun clearBlueskySession()
    suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession

    suspend fun fetchMisskeyEntries(query: String, settings: MisskeySettings): List<RssItem>
    suspend fun fetchMisskeyPage(query: String, settings: MisskeySettings, untilId: String? = null): List<RssItem>
    fun getSavedMisskeySettings(): MisskeySettings?
    fun saveMisskeySettings(settings: MisskeySettings)
    fun clearMisskeySettings()

    suspend fun fetchMuteWords(): List<String>
    suspend fun addMuteWord(word: String)
    suspend fun deleteMuteWord(word: String)
}
