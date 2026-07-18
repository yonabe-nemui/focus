package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase

/**
 * ユーザー設定(テーマ等)のキーバリュー永続化。
 * database が null の場合(Web)はメモリ保持となり、セッション内のみ有効
 * (ローカルミュートワードの Web 挙動と同じ)。
 */
class PreferenceStore(database: FocusDatabase?) {
    private val queries = database?.focusDatabaseQueries
    private val memory = mutableMapOf<String, String>()

    fun get(key: String): String? =
        queries?.getPreference(key)?.executeAsOneOrNull() ?: memory[key]

    fun put(key: String, value: String) {
        val queries = queries
        if (queries != null) {
            queries.upsertPreference(key, value)
        } else {
            memory[key] = value
        }
    }
}
