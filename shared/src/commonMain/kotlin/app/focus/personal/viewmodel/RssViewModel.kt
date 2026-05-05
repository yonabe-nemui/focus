package app.focus.personal.viewmodel

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import app.focus.personal.repository.FeedRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RssUiState {
    object Loading : RssUiState()
    data class Success(val items: List<RssItem>) : RssUiState()
    data class Error(val message: String) : RssUiState()
}

enum class RssSource { GOOGLE, HATENA, BLUESKY, MISSKEY }

class RssViewModel(
    private val repository: FeedRepository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineContext = Dispatchers.Default
) {
    private val _uiState = MutableStateFlow<RssUiState>(RssUiState.Loading)
    val uiState: StateFlow<RssUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentSource = MutableStateFlow(RssSource.GOOGLE)
    val currentSource: StateFlow<RssSource> = _currentSource.asStateFlow()

    // Bluesky Session
    private val _blueskySession = MutableStateFlow<BlueskySession?>(null)
    val blueskySession: StateFlow<BlueskySession?> = _blueskySession.asStateFlow()

    private val _is2faRequired = MutableStateFlow(false)
    val is2faRequired: StateFlow<Boolean> = _is2faRequired.asStateFlow()

    private val _misskeySettings = MutableStateFlow<MisskeySettings?>(null)
    val misskeySettings: StateFlow<MisskeySettings?> = _misskeySettings.asStateFlow()

    private val _blueskySearchQuery = MutableStateFlow("")
    private val _misskeySearchQuery = MutableStateFlow("")

    private val _muteWords = MutableStateFlow<List<String>>(emptyList())
    val muteWords: StateFlow<List<String>> = _muteWords.asStateFlow()

    private val _muteWordsLoading = MutableStateFlow(false)
    val muteWordsLoading: StateFlow<Boolean> = _muteWordsLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Desktop マルチカラム用: ソースごとの状態
    private val _columnStates = MutableStateFlow<Map<RssSource, RssUiState>>(
        mapOf(
            RssSource.GOOGLE  to RssUiState.Loading,
            RssSource.HATENA  to RssUiState.Loading,
            RssSource.BLUESKY to RssUiState.Loading,
            RssSource.MISSKEY to RssUiState.Loading
        )
    )
    val columnStates: StateFlow<Map<RssSource, RssUiState>> = _columnStates.asStateFlow()

    // ページネーション用カーソル・ID
    private var blueskyNextCursor: String? = null
    private var misskeyLastItemId: String? = null

    // メモリ上のキャッシュリスト
    private var googleItems = listOf<RssItem>()
    private var hatenaItems = listOf<RssItem>()
    private var blueskyItems = listOf<RssItem>()
    private var misskeyItems = listOf<RssItem>()

    init {
        checkSavedSession()
        checkSavedMisskeySettings()
        loadAllTopics()
    }

    private fun checkSavedSession() {
        val savedSession = repository.getSavedBlueskySession()
        if (savedSession != null) {
            _blueskySession.value = savedSession
        }
    }

    private fun checkSavedMisskeySettings() {
        val saved = repository.getSavedMisskeySettings()
        if (saved != null) {
            _misskeySettings.value = saved
        }
    }

    fun setSource(source: RssSource) {
        if (_currentSource.value == source) return
        _currentSource.value = source

        val cachedItems = when (source) {
            RssSource.GOOGLE -> googleItems
            RssSource.HATENA -> hatenaItems
            RssSource.BLUESKY -> blueskyItems
            RssSource.MISSKEY -> misskeyItems
        }

        val needsFetch = when (source) {
            RssSource.BLUESKY -> _blueskySession.value != null && cachedItems.isEmpty()
            RssSource.MISSKEY -> _misskeySettings.value != null && cachedItems.isEmpty()
            else -> cachedItems.isEmpty()
        }

        if (needsFetch) {
            _uiState.value = RssUiState.Loading
            loadAllTopics()
        } else {
            _uiState.value = RssUiState.Success(cachedItems)
        }
    }

    fun loginBluesky(handle: String, appPassword: String, authCode: String? = null) {
        scope.launch(dispatcher) {
            _uiState.value = RssUiState.Loading
            try {
                Napier.d("Attempting BlueSky login for: $handle, hasAuthCode: ${authCode != null}")
                val session = repository.loginBluesky(handle, appPassword, authCode)
                Napier.i("BlueSky login successful for: ${session.handle}")
                _blueskySession.value = session
                _is2faRequired.value = false
                loadAllTopics()
            } catch (e: Exception) {
                Napier.e("BlueSky login failed", e)
                when (e.message) {
                    "AuthFactorRequired" -> {
                        Napier.i("2FA required for BlueSky login")
                        _is2faRequired.value = true
                        _uiState.value = RssUiState.Success(emptyList())
                    }
                    "AuthFactorInvalid" -> {
                        _uiState.value = RssUiState.Error("認証コードが正しくないか、期限が切れています。")
                    }
                    else -> {
                        _uiState.value = RssUiState.Error("Login failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun saveMisskeySettings(instanceUrl: String, apiToken: String?) {
        val settings = MisskeySettings(
            instanceUrl = instanceUrl.trim().trimEnd('/'),
            apiToken = apiToken?.trim()?.takeIf { it.isNotEmpty() }
        )
        repository.saveMisskeySettings(settings)
        _misskeySettings.value = settings
        misskeyItems = emptyList()
        if (_currentSource.value == RssSource.MISSKEY) {
            loadAllTopics()
        }
    }

    fun clearMisskeySettings() {
        _misskeySettings.value = null
        misskeyItems = emptyList()
        repository.clearMisskeySettings()
        if (_currentSource.value == RssSource.MISSKEY) {
            _uiState.value = RssUiState.Success(emptyList())
        }
    }

    fun logoutBluesky() {
        _blueskySession.value = null
        blueskyItems = emptyList()
        repository.clearBlueskySession()
        if (_currentSource.value == RssSource.BLUESKY) {
            _uiState.value = RssUiState.Success(emptyList())
        }
    }

    fun loadAllTopics() {
        scope.launch(dispatcher) {
            _uiState.value = RssUiState.Loading
            try {
                val source = _currentSource.value
                val newItems = when (source) {
                    RssSource.GOOGLE -> repository.fetchAllGoogleTopics()
                    RssSource.HATENA -> repository.fetchAllHatenaEntries()
                    RssSource.BLUESKY -> {
                        val result = fetchBlueskyPageWithRetry(null)
                        blueskyNextCursor = result.nextCursor
                        result.items
                    }
                    RssSource.MISSKEY -> {
                        val items = fetchMisskeyEntries()
                        misskeyLastItemId = items.lastOrNull()?.guid
                        items
                    }
                }
                updateList(newItems, source)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadMore() {
        val source = _currentSource.value
        if (_isLoadingMore.value) return
        if (source != RssSource.BLUESKY && source != RssSource.MISSKEY) return

        scope.launch(dispatcher) {
            _isLoadingMore.value = true
            try {
                when (source) {
                    RssSource.BLUESKY -> {
                        val cursor = blueskyNextCursor ?: return@launch
                        val result = fetchBlueskyPageWithRetry(cursor)
                        if (result.items.isNotEmpty()) {
                            val combined = (blueskyItems + result.items).distinctBy { it.link }
                            blueskyItems = combined
                            blueskyNextCursor = result.nextCursor
                            if (_currentSource.value == RssSource.BLUESKY) {
                                _uiState.value = RssUiState.Success(combined)
                            }
                        } else {
                            blueskyNextCursor = null
                        }
                    }
                    RssSource.MISSKEY -> {
                        val untilId = misskeyLastItemId ?: return@launch
                        val settings = _misskeySettings.value ?: return@launch
                        val newItems = repository.fetchMisskeyPage(_misskeySearchQuery.value, settings, untilId)
                        if (newItems.isNotEmpty()) {
                            val combined = (misskeyItems + newItems).distinctBy { it.link }
                            misskeyItems = combined
                            misskeyLastItemId = combined.lastOrNull()?.guid
                            if (_currentSource.value == RssSource.MISSKEY) {
                                _uiState.value = RssUiState.Success(combined)
                            }
                        } else {
                            misskeyLastItemId = null
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Napier.e("loadMore failed", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun fetchMisskeyEntries(): List<RssItem> {
        val settings = _misskeySettings.value ?: return emptyList()
        return try {
            repository.fetchMisskeyEntries(query = _misskeySearchQuery.value, settings = settings)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchBlueskyPageWithRetry(cursor: String?): PagedFeedResponse {
        val session = _blueskySession.value ?: run {
            Napier.w("BlueSky: no session, skipping fetch")
            return PagedFeedResponse(emptyList())
        }
        val query = _blueskySearchQuery.value
        Napier.d("BlueSky: fetching page query='$query' cursor=$cursor")
        return try {
            val result = repository.fetchBlueskyPage(query = query, session = session, cursor = cursor)
            Napier.i("BlueSky: fetched ${result.items.size} items")
            result
        } catch (e: Exception) {
            Napier.w("BlueSky: fetch failed (${e.message}), refreshing token")
            try {
                val newSession = repository.refreshBlueskySession(session.refreshJwt)
                _blueskySession.value = newSession
                Napier.i("BlueSky: token refreshed, retrying fetch")
                val result = repository.fetchBlueskyPage(query = query, session = newSession, cursor = cursor)
                Napier.i("BlueSky: retry fetched ${result.items.size} items")
                result
            } catch (retryEx: Exception) {
                Napier.e("BlueSky: retry also failed: ${retryEx.message}")
                throw retryEx
            }
        }
    }

    fun searchFeed(query: String) {
        when (_currentSource.value) {
            RssSource.BLUESKY -> {
                _blueskySearchQuery.value = query
                blueskyItems = emptyList()
                blueskyNextCursor = null
            }
            RssSource.MISSKEY -> {
                _misskeySearchQuery.value = query
                misskeyItems = emptyList()
                misskeyLastItemId = null
            }
            else -> return
        }
        loadAllTopics()
    }

    fun loadMuteWords() {
        scope.launch(dispatcher) {
            _muteWordsLoading.value = true
            try {
                _muteWords.value = repository.fetchMuteWords()
            } catch (e: Exception) {
                Napier.e("Failed to load mute words", e)
            } finally {
                _muteWordsLoading.value = false
            }
        }
    }

    fun addMuteWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        scope.launch(dispatcher) {
            try {
                repository.addMuteWord(trimmed)
                _muteWords.value = repository.fetchMuteWords()
            } catch (e: Exception) {
                Napier.e("Failed to add mute word", e)
            }
        }
    }

    fun deleteMuteWord(word: String) {
        scope.launch(dispatcher) {
            try {
                repository.deleteMuteWord(word)
                _muteWords.value = _muteWords.value.filter { it != word }
            } catch (e: Exception) {
                Napier.e("Failed to delete mute word", e)
            }
        }
    }

    // --- Desktop マルチカラム用 ---

    fun loadAllSourcesParallel() {
        RssSource.values().forEach { source -> loadColumn(source) }
    }

    fun refreshColumn(source: RssSource) {
        when (source) {
            RssSource.BLUESKY -> { blueskyNextCursor = null; blueskyItems = emptyList() }
            RssSource.MISSKEY -> { misskeyLastItemId = null; misskeyItems = emptyList() }
            else -> {}
        }
        loadColumn(source)
    }

    fun searchColumnFeed(source: RssSource, query: String) {
        when (source) {
            RssSource.BLUESKY -> { _blueskySearchQuery.value = query; blueskyItems = emptyList(); blueskyNextCursor = null }
            RssSource.MISSKEY -> { _misskeySearchQuery.value = query; misskeyItems = emptyList(); misskeyLastItemId = null }
            else -> return
        }
        loadColumn(source)
    }

    private fun setColumnState(source: RssSource, state: RssUiState) {
        _columnStates.value = _columnStates.value + (source to state)
        if (state is RssUiState.Success) {
            when (source) {
                RssSource.GOOGLE  -> googleItems  = state.items
                RssSource.HATENA  -> hatenaItems  = state.items
                RssSource.BLUESKY -> blueskyItems = state.items
                RssSource.MISSKEY -> misskeyItems = state.items
            }
            if (_currentSource.value == source) _uiState.value = state
        }
    }

    private fun loadColumn(source: RssSource) {
        scope.launch(dispatcher) {
            setColumnState(source, RssUiState.Loading)
            try {
                val items: List<RssItem> = when (source) {
                    RssSource.GOOGLE -> repository.fetchAllGoogleTopics()
                    RssSource.HATENA -> repository.fetchAllHatenaEntries()
                    RssSource.BLUESKY -> {
                        if (_blueskySession.value == null) {
                            setColumnState(source, RssUiState.Success(emptyList()))
                            return@launch
                        }
                        val result = fetchBlueskyPageWithRetry(null)
                        blueskyNextCursor = result.nextCursor
                        result.items
                    }
                    RssSource.MISSKEY -> {
                        val settings = _misskeySettings.value ?: run {
                            setColumnState(source, RssUiState.Success(emptyList()))
                            return@launch
                        }
                        val notes = repository.fetchMisskeyEntries(_misskeySearchQuery.value, settings)
                        misskeyLastItemId = notes.lastOrNull()?.guid
                        notes
                    }
                }
                setColumnState(source, RssUiState.Success(items))
            } catch (e: Exception) {
                setColumnState(source, RssUiState.Error(e.message ?: "Error"))
            }
        }
    }

    fun refresh() {
        scope.launch(dispatcher) {
            _isRefreshing.value = true
            val source = _currentSource.value
            when (source) {
                RssSource.BLUESKY -> { blueskyNextCursor = null; blueskyItems = emptyList() }
                RssSource.MISSKEY -> { misskeyLastItemId = null; misskeyItems = emptyList() }
                else -> {}
            }
            try {
                val newItems = when (source) {
                    RssSource.GOOGLE -> repository.fetchAllGoogleTopics()
                    RssSource.HATENA -> repository.fetchAllHatenaEntries()
                    RssSource.BLUESKY -> {
                        val result = fetchBlueskyPageWithRetry(null)
                        blueskyNextCursor = result.nextCursor
                        result.items
                    }
                    RssSource.MISSKEY -> {
                        val items = fetchMisskeyEntries()
                        misskeyLastItemId = items.lastOrNull()?.guid
                        items
                    }
                }
                updateList(newItems, source)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun updateList(newItems: List<RssItem>, source: RssSource) {
        val currentItems = when (source) {
            RssSource.GOOGLE -> googleItems
            RssSource.HATENA -> hatenaItems
            RssSource.BLUESKY -> blueskyItems
            RssSource.MISSKEY -> misskeyItems
        }

        // 重複を排除してマージ
        val merged = (newItems + currentItems)
            .distinctBy { it.link }
            .sortedByDescending {
                if (source == RssSource.HATENA || source == RssSource.BLUESKY || source == RssSource.MISSKEY) {
                    app.focus.personal.util.DateUtils.parseIso8601ToMillis(it.pubDate)
                } else {
                    app.focus.personal.util.DateUtils.parseRfc822ToMillis(it.pubDate)
                }
            }

        when (source) {
            RssSource.GOOGLE -> googleItems = merged
            RssSource.HATENA -> hatenaItems = merged
            RssSource.BLUESKY -> blueskyItems = merged
            RssSource.MISSKEY -> misskeyItems = merged
        }

        if (_currentSource.value == source) {
            _uiState.value = RssUiState.Success(merged)
        }
    }
}
