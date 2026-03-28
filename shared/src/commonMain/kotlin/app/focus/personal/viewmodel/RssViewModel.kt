package app.focus.personal.viewmodel

import app.focus.personal.model.RssItem
import app.focus.personal.repository.RssRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class RssUiState {
    object Loading : RssUiState()
    data class Success(val items: List<RssItem>) : RssUiState()
    data class Error(val message: String) : RssUiState()
}

enum class RssSource { YAHOO, GOOGLE }

class RssViewModel(
    private val repository: RssRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<RssUiState>(RssUiState.Loading)
    val uiState: StateFlow<RssUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _currentSource = MutableStateFlow(RssSource.YAHOO)
    val currentSource: StateFlow<RssSource> = _currentSource.asStateFlow()

    // メモリ上のキャッシュリスト
    private var yahooItems = listOf<RssItem>()
    private var googleItems = listOf<RssItem>()

    init {
        loadAllTopics()
    }

    fun setSource(source: RssSource) {
        if (_currentSource.value == source) return
        _currentSource.value = source
        val cachedItems = if (source == RssSource.YAHOO) yahooItems else googleItems
        
        if (cachedItems.isEmpty()) {
            loadAllTopics()
        } else {
            _uiState.value = RssUiState.Success(cachedItems)
        }
    }

    fun loadAllTopics() {
        scope.launch(Dispatchers.Default) {
            _uiState.value = RssUiState.Loading
            try {
                val source = _currentSource.value
                val newItems = if (source == RssSource.YAHOO) {
                    repository.fetchAllTopics()
                } else {
                    repository.fetchAllGoogleTopics()
                }
                updateList(newItems, source)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.Default) {
            _isRefreshing.value = true
            try {
                val source = _currentSource.value
                val newItems = if (source == RssSource.YAHOO) {
                    repository.fetchAllTopics()
                } else {
                    repository.fetchAllGoogleTopics()
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
        val currentItems = if (source == RssSource.YAHOO) yahooItems else googleItems
        // 重複を排除してマージ
        val merged = (newItems + currentItems)
            .distinctBy { it.guid ?: it.link }
            .sortedByDescending { app.focus.personal.util.DateUtils.parseRfc822ToMillis(it.pubDate) }
        
        if (source == RssSource.YAHOO) {
            yahooItems = merged
        } else {
            googleItems = merged
        }

        if (_currentSource.value == source) {
            _uiState.value = RssUiState.Success(merged)
        }
    }

    fun loadTopics(category: String = "top-picks") {
        scope.launch(Dispatchers.Default) {
            _uiState.value = RssUiState.Loading
            try {
                val items = repository.fetchTopics(category)
                _uiState.value = RssUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadCategory(category: String) {
        scope.launch(Dispatchers.Default) {
            _uiState.value = RssUiState.Loading
            try {
                val items = repository.fetchCategory(category)
                _uiState.value = RssUiState.Success(items)
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
