package app.focus.personal.viewmodel

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import app.focus.personal.network.BlueskyException
import app.focus.personal.repository.FeedRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** エラーの種別。文言化は UI 層がこの種別に応じてリソースから行う。 */
enum class FeedErrorKind { GENERIC, LOGIN_FAILED, AUTH_CODE_INVALID, RATE_LIMITED }

sealed class RssUiState {
    object Loading : RssUiState()
    data class Success(val items: List<RssItem>) : RssUiState()
    data class Error(
        val message: String,
        val kind: FeedErrorKind = FeedErrorKind.GENERIC,
    ) : RssUiState()
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

    private val _blueskySession = MutableStateFlow<BlueskySession?>(null)
    val blueskySession: StateFlow<BlueskySession?> = _blueskySession.asStateFlow()

    private val _is2faRequired = MutableStateFlow(false)
    val is2faRequired: StateFlow<Boolean> = _is2faRequired.asStateFlow()

    private val _misskeySettings = MutableStateFlow<MisskeySettings?>(null)
    val misskeySettings: StateFlow<MisskeySettings?> = _misskeySettings.asStateFlow()

    private val _muteWords = MutableStateFlow<List<String>>(emptyList())
    val muteWords: StateFlow<List<String>> = _muteWords.asStateFlow()

    private val _muteWordsLoading = MutableStateFlow(false)
    val muteWordsLoading: StateFlow<Boolean> = _muteWordsLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    // Desktop マルチカラム用: ソースごとの UI 状態
    private val _columnStates = MutableStateFlow(
        RssSource.entries.associateWith<RssSource, RssUiState> { RssUiState.Loading }
    )
    val columnStates: StateFlow<Map<RssSource, RssUiState>> = _columnStates.asStateFlow()

    // ソース別の内部状態（items, ページネーション cursor/untilId）
    private val cachedItems: MutableMap<RssSource, List<RssItem>> =
        RssSource.entries.associateWith<RssSource, List<RssItem>> { emptyList() }.toMutableMap()
    private val paginationCursor: MutableMap<RssSource, String?> =
        RssSource.entries.associateWith<RssSource, String?> { null }.toMutableMap()

    // 検索クエリ。入力欄の表示値も ViewModel を single source of truth とする。
    private val _searchQueries = MutableStateFlow(RssSource.entries.associateWith { "" })
    val searchQueries: StateFlow<Map<RssSource, String>> = _searchQueries.asStateFlow()

    // モバイル単一カラムの進行中 fetch Job。タブ高速切り替え時の race を防ぐ。
    private var currentLoadJob: Job? = null

    init {
        checkSavedSession()
        checkSavedMisskeySettings()
        loadAllTopics()
    }

    private fun checkSavedSession() {
        repository.getSavedBlueskySession()?.let { _blueskySession.value = it }
    }

    private fun checkSavedMisskeySettings() {
        repository.getSavedMisskeySettings()?.let { _misskeySettings.value = it }
    }

    private fun resetSourceCache(source: RssSource) {
        cachedItems[source] = emptyList()
        paginationCursor[source] = null
    }

    fun setSource(source: RssSource) {
        if (_currentSource.value == source) return
        _currentSource.value = source

        val cached = cachedItems[source].orEmpty()
        val needsFetch = when (source) {
            RssSource.BLUESKY -> _blueskySession.value != null && cached.isEmpty()
            RssSource.MISSKEY -> _misskeySettings.value != null && cached.isEmpty()
            else -> cached.isEmpty()
        }

        if (needsFetch) {
            _uiState.value = RssUiState.Loading
            loadAllTopics()
        } else {
            _uiState.value = RssUiState.Success(cached)
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
                when (e) {
                    is BlueskyException.AuthFactorRequired -> {
                        Napier.i("2FA required for BlueSky login")
                        _is2faRequired.value = true
                        _uiState.value = RssUiState.Success(emptyList())
                    }
                    is BlueskyException.AuthFactorInvalid -> {
                        _uiState.value = RssUiState.Error(e.message.orEmpty(), FeedErrorKind.AUTH_CODE_INVALID)
                    }
                    is BlueskyException.RateLimited -> {
                        _uiState.value = RssUiState.Error(e.message.orEmpty(), FeedErrorKind.RATE_LIMITED)
                    }
                    else -> {
                        _uiState.value = RssUiState.Error(e.message.orEmpty(), FeedErrorKind.LOGIN_FAILED)
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
        resetSourceCache(RssSource.MISSKEY)
        if (_currentSource.value == RssSource.MISSKEY) {
            loadAllTopics()
        }
    }

    fun clearMisskeySettings() {
        _misskeySettings.value = null
        resetSourceCache(RssSource.MISSKEY)
        repository.clearMisskeySettings()
        if (_currentSource.value == RssSource.MISSKEY) {
            _uiState.value = RssUiState.Success(emptyList())
        }
    }

    fun logoutBluesky() {
        _blueskySession.value = null
        resetSourceCache(RssSource.BLUESKY)
        repository.clearBlueskySession()
        if (_currentSource.value == RssSource.BLUESKY) {
            _uiState.value = RssUiState.Success(emptyList())
        }
    }

    fun loadAllTopics() {
        currentLoadJob?.cancel()
        currentLoadJob = scope.launch(dispatcher) {
            _uiState.value = RssUiState.Loading
            try {
                val source = _currentSource.value
                val newItems = fetchFirstPage(source)
                updateList(newItems, source)
            } catch (e: Exception) {
                Napier.w("loadAllTopics failed: ${e.message}")
                _uiState.value = RssUiState.Error(e.message.orEmpty())
            }
        }
    }

    // ソースの初回ページを取得し、ページネーションカーソルを保存する。
    // 未ログイン(BlueSky)・未設定(Misskey)のソースは空リストを返す。
    // モバイル単一カラム・デスクトップマルチカラムの両方から使う。
    private suspend fun fetchFirstPage(source: RssSource): List<RssItem> = when (source) {
        RssSource.GOOGLE -> repository.fetchAllGoogleTopics()
        RssSource.HATENA -> repository.fetchAllHatenaEntries()
        RssSource.BLUESKY -> {
            val result = fetchBlueskyPageWithRetry(null)
            paginationCursor[RssSource.BLUESKY] = result.nextCursor
            result.items
        }
        RssSource.MISSKEY -> {
            val settings = _misskeySettings.value
            if (settings == null) {
                emptyList()
            } else {
                val items = repository.fetchMisskeyEntries(queryOf(RssSource.MISSKEY), settings)
                paginationCursor[RssSource.MISSKEY] = items.lastOrNull()?.guid
                items
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
                val newItems: List<RssItem>
                val nextCursor: String?
                when (source) {
                    RssSource.BLUESKY -> {
                        val cursor = paginationCursor[source] ?: return@launch
                        val result = fetchBlueskyPageWithRetry(cursor)
                        newItems = result.items
                        nextCursor = result.nextCursor
                    }
                    RssSource.MISSKEY -> {
                        val untilId = paginationCursor[source] ?: return@launch
                        val settings = _misskeySettings.value ?: return@launch
                        newItems = repository.fetchMisskeyPage(queryOf(source), settings, untilId)
                        nextCursor = newItems.lastOrNull()?.guid
                    }
                    RssSource.GOOGLE, RssSource.HATENA -> return@launch
                }

                if (newItems.isNotEmpty()) {
                    val combined = (cachedItems[source].orEmpty() + newItems).distinctBy { it.link }
                    cachedItems[source] = combined
                    paginationCursor[source] = nextCursor
                    if (_currentSource.value == source) {
                        _uiState.value = RssUiState.Success(combined)
                    }
                } else {
                    paginationCursor[source] = null
                }
            } catch (e: Exception) {
                Napier.e("loadMore failed", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    private suspend fun fetchBlueskyPageWithRetry(cursor: String?): PagedFeedResponse {
        val session = _blueskySession.value ?: run {
            Napier.w("BlueSky: no session, skipping fetch")
            return PagedFeedResponse(emptyList())
        }
        val query = queryOf(RssSource.BLUESKY)
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

    private fun queryOf(source: RssSource): String = _searchQueries.value[source].orEmpty()

    /** 入力欄の値を更新する(検索は実行しない)。 */
    fun setSearchQuery(source: RssSource, query: String) {
        _searchQueries.value = _searchQueries.value + (source to query)
    }

    fun searchFeed(query: String) {
        val source = _currentSource.value
        if (source != RssSource.BLUESKY && source != RssSource.MISSKEY) return
        setSearchQuery(source, query)
        resetSourceCache(source)
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

    // --- Desktop マルチカラム ---

    fun loadAllSourcesParallel() {
        RssSource.entries.forEach { source -> loadColumn(source) }
    }

    fun refreshColumn(source: RssSource) {
        resetSourceCache(source)
        loadColumn(source)
    }

    fun searchColumnFeed(source: RssSource, query: String) {
        if (source != RssSource.BLUESKY && source != RssSource.MISSKEY) return
        setSearchQuery(source, query)
        resetSourceCache(source)
        loadColumn(source)
    }

    // デスクトップマルチカラムの状態更新。モバイル側 _uiState には影響させない。
    // cachedItems は両モードで共有（再フェッチを避ける）。
    private fun setColumnState(source: RssSource, state: RssUiState) {
        _columnStates.value = _columnStates.value + (source to state)
        if (state is RssUiState.Success) {
            cachedItems[source] = state.items
        }
    }

    private fun loadColumn(source: RssSource) {
        scope.launch(dispatcher) {
            setColumnState(source, RssUiState.Loading)
            try {
                setColumnState(source, RssUiState.Success(fetchFirstPage(source)))
            } catch (e: Exception) {
                setColumnState(source, RssUiState.Error(e.message.orEmpty()))
            }
        }
    }

    fun refresh() {
        currentLoadJob?.cancel()
        currentLoadJob = scope.launch(dispatcher) {
            _isRefreshing.value = true
            val source = _currentSource.value
            resetSourceCache(source)
            try {
                val newItems = fetchFirstPage(source)
                updateList(newItems, source)
            } catch (e: Exception) {
                Napier.w("refresh failed: ${e.message}")
                _uiState.value = RssUiState.Error(e.message.orEmpty())
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun updateList(newItems: List<RssItem>, source: RssSource) {
        val current = cachedItems[source].orEmpty()
        val merged = (newItems + current)
            .distinctBy { it.link }
            .sortedByDescending { it.pubDateMillis }

        cachedItems[source] = merged
        if (_currentSource.value == source) {
            _uiState.value = RssUiState.Success(merged)
        }
    }
}
