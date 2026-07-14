package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings

/**
 * BlueSky セッションと Misskey 設定の永続化を担う共通ストア。
 * RssRepository / ServerRssRepository の両方から利用される。
 * database が null の場合（Web）は何も保存しない。
 */
class SessionStore(database: FocusDatabase?) {
    private val queries = database?.focusDatabaseQueries

    fun getBlueskySession(): BlueskySession? =
        queries?.getActiveBlueskySession()?.executeAsOneOrNull()?.let { entity ->
            BlueskySession(
                accessJwt = entity.accessJwt,
                refreshJwt = entity.refreshJwt,
                handle = entity.handle,
                did = entity.did
            )
        }

    fun saveBlueskySession(session: BlueskySession) {
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

    fun clearBlueskySession() {
        queries?.clearActiveBlueskySession()
    }

    fun getMisskeySettings(): MisskeySettings? =
        queries?.getActiveMisskeySettings()?.executeAsOneOrNull()?.let { entity ->
            MisskeySettings(
                instanceUrl = entity.instanceUrl,
                apiToken = entity.apiToken
            )
        }

    fun saveMisskeySettings(settings: MisskeySettings) {
        queries?.upsertMisskeySettings(
            instanceUrl = settings.instanceUrl,
            apiToken = settings.apiToken
        )
    }

    fun clearMisskeySettings() {
        queries?.clearMisskeySettings()
    }
}
