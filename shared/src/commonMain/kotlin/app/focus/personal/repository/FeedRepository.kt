package app.focus.personal.repository

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MutedWord
import app.focus.personal.model.RssItem

interface FeedRepository {
    suspend fun fetchAllGoogleTopics(): List<RssItem>
    suspend fun fetchAllHatenaEntries(): List<RssItem>
    suspend fun fetchBlueskyEntries(
        query: String,
        session: BlueskySession? = null,
        mutedWords: List<MutedWord> = emptyList()
    ): List<RssItem>
    suspend fun loginBluesky(handle: String, appPassword: String, authCode: String? = null): BlueskySession
    fun getSavedBlueskySession(): BlueskySession?
    fun saveBlueskySession(session: BlueskySession)
    fun clearBlueskySession()
    suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession
    suspend fun getBlueskyMutedWords(session: BlueskySession): List<MutedWord>
}
