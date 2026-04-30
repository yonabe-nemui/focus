package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MutedWord
import app.focus.personal.model.RssItem
import app.focus.personal.model.toRssItem
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.FocusApiClient
import app.focus.personal.util.DateUtils

class ServerRssRepository(
    private val database: FocusDatabase?,
    private val apiClient: FocusApiClient,
    private val blueskyApi: BlueskyClient
) : FeedRepository {

    private val queries = database?.focusDatabaseQueries

    override suspend fun fetchAllGoogleTopics(): List<RssItem> = apiClient.fetchGoogleFeed()

    override suspend fun fetchAllHatenaEntries(): List<RssItem> = apiClient.fetchHatenaFeed()

    override suspend fun fetchBlueskyEntries(
        query: String,
        session: BlueskySession?,
        mutedWords: List<MutedWord>
    ): List<RssItem> {
        val response = blueskyApi.searchPosts(query, session = session)
        return response.posts
            .filter { post ->
                val text = post.record.text.lowercase()
                mutedWords.none { word ->
                    word.value.lowercase().isNotEmpty() && text.contains(word.value.lowercase())
                }
            }
            .map { it.toRssItem() }
            .sortedByDescending { DateUtils.parseIso8601ToMillis(it.pubDate) }
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

    override suspend fun getBlueskyMutedWords(session: BlueskySession): List<MutedWord> {
        return try {
            blueskyApi.getMutedWords(session)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
