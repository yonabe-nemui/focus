package app.focus.personal.repository

import app.focus.personal.db.FocusDatabase
import app.focus.personal.model.RssItem

/**
 * ローカルミュートワードの永続化とフィルタ適用を担う共通ストア。
 * RssRepository / ServerRssRepository の両方から利用される。
 * database が null の場合（Web）はメモリのみ保持でセッション内のみ有効。
 */
class MuteWordStore(database: FocusDatabase?) {
    private val queries = database?.focusDatabaseQueries
    private var cache: MutableList<String>? = null

    private fun load(): MutableList<String> {
        cache?.let { return it }
        val loaded = queries?.selectAllMuteWords()?.executeAsList()?.toMutableList() ?: mutableListOf()
        cache = loaded
        return loaded
    }

    fun getAll(): List<String> = load().toList()

    fun add(word: String) {
        val words = load()
        if (word !in words) {
            words.add(word)
            queries?.insertMuteWord(word)
        }
    }

    fun delete(word: String) {
        load().remove(word)
        queries?.deleteMuteWord(word)
    }

    /** タイトル + 本文にローカルミュートワードを適用する（BlueSky 公式ミュートワードとは独立）。 */
    fun filter(items: List<RssItem>): List<RssItem> {
        val words = load()
        if (words.isEmpty()) return items
        return items.filter { item ->
            val text = "${item.title} ${item.description.orEmpty()}"
            words.none { matchesMutedWord(text, it) }
        }
    }

    companion object {
        // ASCII のみのワードは単語境界マッチで誤検出を防ぐ ("book" が "facebook" にマッチしない)
        // CJK 等を含む場合は単語境界の概念がないため部分一致を許容する
        private val asciiOnly = Regex("^[\\x00-\\x7F]+$")

        /** ミュートワード判定。ローカル・BlueSky 公式の両系統で共通に使う。 */
        fun matchesMutedWord(text: String, word: String): Boolean {
            if (word.isEmpty()) return false
            return if (asciiOnly.matches(word)) {
                Regex("\\b${Regex.escape(word)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
            } else {
                text.contains(word, ignoreCase = true)
            }
        }
    }
}
