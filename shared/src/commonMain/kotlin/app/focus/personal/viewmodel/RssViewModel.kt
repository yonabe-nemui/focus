package app.focus.personal.viewmodel

import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
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
                    RssSource.BLUESKY -> fetchBlueskyWithRetry()
                    RssSource.MISSKEY -> fetchMisskeyEntries()
                }
                updateList(newItems, source)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun fetchMisskeyEntries(): List<RssItem> {
        val settings = _misskeySettings.value ?: return emptyList()
        return try {
            repository.fetchMisskeyEntries(query = "IT", settings = settings)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchBlueskyWithRetry(): List<RssItem> {
        val session = _blueskySession.value ?: return emptyList()
        return try {
            repository.fetchBlueskyEntries(query = "IT", session = session)
        } catch (e: Exception) {
            try {
                val newSession = repository.refreshBlueskySession(session.refreshJwt)
                _blueskySession.value = newSession
                repository.fetchBlueskyEntries(query = "IT", session = newSession)
            } catch (retryEx: Exception) {
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
                    RssSource.BLUESKY -> fetchBlueskyWithRetry()
                    RssSource.MISSKEY -> fetchMisskeyEntries()
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
