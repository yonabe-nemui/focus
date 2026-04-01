package app.focus.personal.viewmodel

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MutedWord
import app.focus.personal.model.RssItem
import app.focus.personal.repository.RssRepository
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

enum class RssSource { GOOGLE, HATENA, BLUESKY }

class RssViewModel(
    private val repository: RssRepository,
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

    private val _mutedWords = MutableStateFlow<List<MutedWord>>(emptyList())

    // メモリ上のキャッシュリスト
    private var googleItems = listOf<RssItem>()
    private var hatenaItems = listOf<RssItem>()
    private var blueskyItems = listOf<RssItem>()

    init {
        checkSavedSession()
        loadAllTopics()
    }

    private fun checkSavedSession() {
        val savedSession = repository.getSavedBlueskySession()
        if (savedSession != null) {
            _blueskySession.value = savedSession
            // 非同期でプレファレンスを取得（セッション有効チェックも兼ねる）
            scope.launch(dispatcher) {
                try {
                    _mutedWords.value = repository.getBlueskyMutedWords(savedSession)
                } catch (e: Exception) {
                    // セッション切れの可能性。リフレッシュを試みる
                    tryRefreshSession(savedSession.refreshJwt)
                }
            }
        }
    }

    private suspend fun tryRefreshSession(refreshJwt: String) {
        try {
            val newSession = repository.refreshBlueskySession(refreshJwt)
            _blueskySession.value = newSession
            _mutedWords.value = repository.getBlueskyMutedWords(newSession)
        } catch (e: Exception) {
            // リフレッシュも失敗した場合はログアウト状態にする
            _blueskySession.value = null
            repository.clearBlueskySession()
        }
    }

    fun setSource(source: RssSource) {
        if (_currentSource.value == source) return
        _currentSource.value = source
        
        val cachedItems = when (source) {
            RssSource.GOOGLE -> googleItems
            RssSource.HATENA -> hatenaItems
            RssSource.BLUESKY -> blueskyItems
        }
        
        if (cachedItems.isEmpty() && source != RssSource.BLUESKY) {
            loadAllTopics()
        } else if (source == RssSource.BLUESKY && _blueskySession.value != null && cachedItems.isEmpty()) {
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
                _mutedWords.value = repository.getBlueskyMutedWords(session)
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

    fun logoutBluesky() {
        _blueskySession.value = null
        _mutedWords.value = emptyList()
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
                        fetchBlueskyWithRetry()
                    }
                }
                updateList(newItems, source)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchBlueskyWithRetry(): List<RssItem> {
        val session = _blueskySession.value ?: return emptyList()
        return try {
            repository.fetchBlueskyEntries(
                query = "IT",
                session = session,
                mutedWords = _mutedWords.value
            )
        } catch (e: Exception) {
            // エラー時（セッション切れ等）に1回だけリフレッシュして再試行
            try {
                val newSession = repository.refreshBlueskySession(session.refreshJwt)
                _blueskySession.value = newSession
                repository.fetchBlueskyEntries(
                    query = "IT",
                    session = newSession,
                    mutedWords = repository.getBlueskyMutedWords(newSession)
                )
            } catch (retryEx: Exception) {
                // リトライも失敗した場合は空リスト（またはエラー）
                throw retryEx
            }
        }
    }

    fun refresh() {
        scope.launch(dispatcher) {
            _isRefreshing.value = true
            try {
                val source = _currentSource.value
                val newItems = when (source) {
                    RssSource.GOOGLE -> repository.fetchAllGoogleTopics()
                    RssSource.HATENA -> repository.fetchAllHatenaEntries()
                    RssSource.BLUESKY -> {
                        fetchBlueskyWithRetry()
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
        }

        // 重複を排除してマージ
        val merged = (newItems + currentItems)
            .distinctBy { it.link }
            .sortedByDescending { 
                if (source == RssSource.HATENA || source == RssSource.BLUESKY) {
                    app.focus.personal.util.DateUtils.parseIso8601ToMillis(it.pubDate)
                } else {
                    app.focus.personal.util.DateUtils.parseRfc822ToMillis(it.pubDate)
                }
            }
        
        when (source) {
            RssSource.GOOGLE -> googleItems = merged
            RssSource.HATENA -> hatenaItems = merged
            RssSource.BLUESKY -> blueskyItems = merged
        }

        if (_currentSource.value == source) {
            _uiState.value = RssUiState.Success(merged)
        }
    }
}
