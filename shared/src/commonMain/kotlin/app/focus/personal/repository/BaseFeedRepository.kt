package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.network.BlueskyClient

/**
 * RssRepository / ServerRssRepository で共通の処理。
 * BlueSky 認証・Misskey 設定・ローカルミュートワード・ユーザー設定はどちらもクライアント側で扱う。
 */
abstract class BaseFeedRepository(
    database: FocusDatabase?,
    protected val blueskyApi: BlueskyClient,
) : FeedRepository {
    private val sessionStore = SessionStore(database)
    protected val muteWordStore = MuteWordStore(database)
    private val preferenceStore = PreferenceStore(database)

    override suspend fun loginBluesky(handle: String, appPassword: String, authCode: String?): BlueskySession =
        blueskyApi.createSession(handle, appPassword, authCode).also(::saveBlueskySession)

    override suspend fun refreshBlueskySession(refreshJwt: String): BlueskySession =
        blueskyApi.refreshSession(refreshJwt).also(::saveBlueskySession)

    override fun getSavedBlueskySession(): BlueskySession? = sessionStore.getBlueskySession()

    override fun saveBlueskySession(session: BlueskySession) = sessionStore.saveBlueskySession(session)

    override fun clearBlueskySession() = sessionStore.clearBlueskySession()

    override fun getSavedMisskeySettings(): MisskeySettings? = sessionStore.getMisskeySettings()

    override fun saveMisskeySettings(settings: MisskeySettings) = sessionStore.saveMisskeySettings(settings)

    override fun clearMisskeySettings() = sessionStore.clearMisskeySettings()

    override suspend fun fetchMuteWords(): List<String> = muteWordStore.getAll()

    override suspend fun addMuteWord(word: String) = muteWordStore.add(word)

    override suspend fun deleteMuteWord(word: String) = muteWordStore.delete(word)

    override fun getPreference(key: String): String? = preferenceStore.get(key)

    override fun setPreference(key: String, value: String) = preferenceStore.put(key, value)
}
