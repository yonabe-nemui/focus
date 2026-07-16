package app.focus.personal.viewmodel

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.PagedFeedResponse
import app.focus.personal.model.RssItem
import app.focus.personal.network.BlueskyException
import app.focus.personal.repository.FeedRepository
import app.focus.personal.repository.MuteWordStore
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

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Success(val items: List<RssItem>) : FeedUiState()
    data class Error(
        val message: String,
        val kind: FeedErrorKind = FeedErrorKind.GENERIC,
    ) : FeedUiState()
}

enum class FeedSource { GOOGLE, HATENA, BLUESKY, MISSKEY }

class FeedViewModel(
    private val repository: FeedRepository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineContext = Dispatchers.Default
) {
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentSource = MutableStateFlow(FeedSource.GOOGLE)
    val currentSource: StateFlow<FeedSource> = _currentSource.asStateFlow()

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
        FeedSource.entries.associateWith<FeedSource, FeedUiState> { FeedUiState.Loading }
    )
    val columnStates: StateFlow<Map<FeedSource, FeedUiState>> = _columnStates.asStateFlow()

    // ソース別の内部状態（items, ページネーション cursor/untilId）
    private val cachedItems: MutableMap<FeedSource, List<RssItem>> =
        FeedSource.entries.associateWith<FeedSource, List<RssItem>> { emptyList() }.toMutableMap()
    private val paginationCursor: MutableMap<FeedSource, String?> =
        FeedSource.entries.associateWith<FeedSource, String?> { null }.toMutableMap()

    // 検索クエリ。入力欄の表示値も ViewModel を single source of truth とする。
    private val _searchQueries = MutableStateFlow(FeedSource.entries.associateWith { "" })
    val searchQueries: StateFlow<Map<FeedSource, String>> = _searchQueries.asStateFlow()

    // モバイル単一カラムの進行中 fetch Job。タブ高速切り替え時の race を防ぐ。
    private var currentLoadJob: Job? = null

    init {
        checkSavedSession()
        checkSavedMisskeySettings()
        loadCurrentSource()
    }

    private fun checkSavedSession() {
        repository.getSavedBlueskySession()?.let { _blueskySession.value = it }
    }

    private fun checkSavedMisskeySettings() {
        repository.getSavedMisskeySettings()?.let { _misskeySettings.value = it }
    }

    private fun resetSourceCache(source: FeedSource) {
        cachedItems[source] = emptyList()
        paginationCursor[source] = null
    }

    fun setSource(source: FeedSource) {
        if (_currentSource.value == source) return
        _currentSource.value = source

        val cached = cachedItems[source].orEmpty()
        val needsFetch = when (source) {
            FeedSource.BLUESKY -> _blueskySession.value != null && cached.isEmpty()
            FeedSource.MISSKEY -> _misskeySettings.value != null && cached.isEmpty()
            else -> cached.isEmpty()
        }

        if (needsFetch) {
            _uiState.value = FeedUiState.Loading
            loadCurrentSource()
        } else {
            _uiState.value = FeedUiState.Success(cached)
        }
    }

    fun loginBluesky(handle: String, appPassword: String, authCode: String? = null) {
        scope.launch(dispatcher) {
            _uiState.value = FeedUiState.Loading
            try {
                Napier.d("Attempting BlueSky login for: $handle, hasAuthCode: ${authCode != null}")
                val session = repository.loginBluesky(handle, appPassword, authCode)
                Napier.i("BlueSky login successful for: ${session.handle}")
                _blueskySession.value = session
                _is2faRequired.value = false
                loadCurrentSource()
            } catch (e: Exception) {
                Napier.e("BlueSky login failed", e)
                when (e) {
                    is BlueskyException.AuthFactorRequired -> {
                        Napier.i("2FA required for BlueSky login")
                        _is2faRequired.value = true
                        _uiState.value = FeedUiState.Success(emptyList())
                    }
                    is BlueskyException.AuthFactorInvalid -> {
                        _uiState.value = FeedUiState.Error(e.message.orEmpty(), FeedErrorKind.AUTH_CODE_INVALID)
                    }
                    is BlueskyException.RateLimited -> {
                        _uiState.value = FeedUiState.Error(e.message.orEmpty(), FeedErrorKind.RATE_LIMITED)
                    }
                    else -> {
                        _uiState.value = FeedUiState.Error(e.message.orEmpty(), FeedErrorKind.LOGIN_FAILED)
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
        resetSourceCache(FeedSource.MISSKEY)
        if (_currentSource.value == FeedSource.MISSKEY) {
            loadCurrentSource()
        }
    }

    fun clearMisskeySettings() {
        _misskeySettings.value = null
        resetSourceCache(FeedSource.MISSKEY)
        repository.clearMisskeySettings()
        if (_currentSource.value == FeedSource.MISSKEY) {
            _uiState.value = FeedUiState.Success(emptyList())
        }
    }

    fun logoutBluesky() {
        _blueskySession.value = null
        resetSourceCache(FeedSource.BLUESKY)
        repository.clearBlueskySession()
        if (_currentSource.value == FeedSource.BLUESKY) {
            _uiState.value = FeedUiState.Success(emptyList())
        }
    }

    fun loadCurrentSource() {
        currentLoadJob?.cancel()
        currentLoadJob = scope.launch(dispatcher) {
            _uiState.value = FeedUiState.Loading
            try {
                val source = _currentSource.value
                val newItems = fetchFirstPage(source)
                updateList(newItems, source)
            } catch (e: Exception) {
                Napier.w("loadCurrentSource failed: ${e.message}")
                _uiState.value = FeedUiState.Error(e.message.orEmpty())
            }
        }
    }

    // ソースの初回ページを取得し、ページネーションカーソルを保存する。
    // 未ログイン(BlueSky)・未設定(Misskey)のソースは空リストを返す。
    // モバイル単一カラム・デスクトップマルチカラムの両方から使う。
    private suspend fun fetchFirstPage(source: FeedSource): List<RssItem> = when (source) {
        FeedSource.GOOGLE -> repository.fetchAllGoogleTopics()
        FeedSource.HATENA -> repository.fetchAllHatenaEntries()
        FeedSource.BLUESKY -> {
            val result = fetchBlueskyPageWithRetry(null)
            paginationCursor[FeedSource.BLUESKY] = result.nextCursor
            result.items
        }
        FeedSource.MISSKEY -> {
            val settings = _misskeySettings.value
            if (settings == null) {
                emptyList()
            } else {
                val items = repository.fetchMisskeyEntries(queryOf(FeedSource.MISSKEY), settings)
                paginationCursor[FeedSource.MISSKEY] = items.lastOrNull()?.guid
                items
            }
        }
    }

    fun loadMore() {
        val source = _currentSource.value
        if (_isLoadingMore.value) return
        if (source != FeedSource.BLUESKY && source != FeedSource.MISSKEY) return

        scope.launch(dispatcher) {
            _isLoadingMore.value = true
            try {
                val newItems: List<RssItem>
                val nextCursor: String?
                when (source) {
                    FeedSource.BLUESKY -> {
                        val cursor = paginationCursor[source] ?: return@launch
                        val result = fetchBlueskyPageWithRetry(cursor)
                        newItems = result.items
                        nextCursor = result.nextCursor
                    }
                    FeedSource.MISSKEY -> {
                        val untilId = paginationCursor[source] ?: return@launch
                        val settings = _misskeySettings.value ?: return@launch
                        newItems = repository.fetchMisskeyPage(queryOf(source), settings, untilId)
                        nextCursor = newItems.lastOrNull()?.guid
                    }
                    FeedSource.GOOGLE, FeedSource.HATENA -> return@launch
                }

                if (newItems.isNotEmpty()) {
                    val combined = (cachedItems[source].orEmpty() + newItems).distinctBy { it.link }
                    cachedItems[source] = combined
                    paginationCursor[source] = nextCursor
                    if (_currentSource.value == source) {
                        _uiState.value = FeedUiState.Success(combined)
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
        val query = queryOf(FeedSource.BLUESKY)
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

    private fun queryOf(source: FeedSource): String = _searchQueries.value[source].orEmpty()

    /** 入力欄の値を更新する(検索は実行しない)。 */
    fun setSearchQuery(source: FeedSource, query: String) {
        _searchQueries.value = _searchQueries.value + (source to query)
    }

    fun searchFeed(query: String) {
        val source = _currentSource.value
        if (source != FeedSource.BLUESKY && source != FeedSource.MISSKEY) return
        setSearchQuery(source, query)
        resetSourceCache(source)
        loadCurrentSource()
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

    /** フィードのコンテキストメニューからミュートワードを追加し、表示中のフィードへ即時反映する。 */
    fun addMuteWordAndRefreshFeed(word: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        scope.launch(dispatcher) {
            try {
                repository.addMuteWord(trimmed)
                _muteWords.value = repository.fetchMuteWords()
                applyMuteWordToLoadedFeeds(trimmed)
            } catch (e: Exception) {
                Napier.e("Failed to add mute word from feed", e)
            }
        }
    }

    // 読み込み済みのキャッシュと表示状態からミュートワードに一致するアイテムを除外する
    private fun applyMuteWordToLoadedFeeds(word: String) {
        FeedSource.entries.forEach { source ->
            cachedItems[source] = cachedItems[source].orEmpty().filterNot {
                MuteWordStore.matchesMutedWord("${it.title} ${it.description.orEmpty()}", word)
            }
        }
        val current = _uiState.value
        if (current is FeedUiState.Success) {
            _uiState.value = FeedUiState.Success(cachedItems[_currentSource.value].orEmpty())
        }
        _columnStates.value = _columnStates.value.mapValues { (source, state) ->
            if (state is FeedUiState.Success) FeedUiState.Success(cachedItems[source].orEmpty()) else state
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
        FeedSource.entries.forEach { source -> loadColumn(source) }
    }

    fun refreshColumn(source: FeedSource) {
        resetSourceCache(source)
        loadColumn(source)
    }

    fun searchColumnFeed(source: FeedSource, query: String) {
        if (source != FeedSource.BLUESKY && source != FeedSource.MISSKEY) return
        setSearchQuery(source, query)
        resetSourceCache(source)
        loadColumn(source)
    }

    // デスクトップマルチカラムの状態更新。モバイル側 _uiState には影響させない。
    // cachedItems は両モードで共有（再フェッチを避ける）。
    private fun setColumnState(source: FeedSource, state: FeedUiState) {
        _columnStates.value = _columnStates.value + (source to state)
        if (state is FeedUiState.Success) {
            cachedItems[source] = state.items
        }
    }

    private fun loadColumn(source: FeedSource) {
        scope.launch(dispatcher) {
            setColumnState(source, FeedUiState.Loading)
            try {
                setColumnState(source, FeedUiState.Success(fetchFirstPage(source)))
            } catch (e: Exception) {
                setColumnState(source, FeedUiState.Error(e.message.orEmpty()))
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
                _uiState.value = FeedUiState.Error(e.message.orEmpty())
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun updateList(newItems: List<RssItem>, source: FeedSource) {
        val current = cachedItems[source].orEmpty()
        val merged = (newItems + current)
            .distinctBy { it.link }
            .sortedByDescending { it.pubDateMillis }

        cachedItems[source] = merged
        if (_currentSource.value == source) {
            _uiState.value = FeedUiState.Success(merged)
        }
    }
}
