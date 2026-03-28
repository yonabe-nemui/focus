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

class RssViewModel(
    private val repository: RssRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow<RssUiState>(RssUiState.Loading)
    val uiState: StateFlow<RssUiState> = _uiState.asStateFlow()

    init {
        loadTopics()
    }

    fun loadTopics(category: String = "top-picks") {
        scope.launch(Dispatchers.Default) {
            _uiState.value = RssUiState.Loading
            try {
                repository.refreshTopics(category)
                repository.getItemsByCategory("topic").collectLatest { items ->
                    _uiState.value = RssUiState.Success(items)
                }
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun loadCategory(category: String) {
        scope.launch(Dispatchers.Default) {
            _uiState.value = RssUiState.Loading
            try {
                repository.refreshCategory(category)
                repository.getItemsByCategory("category").collectLatest { items ->
                    _uiState.value = RssUiState.Success(items)
                }
            } catch (e: Exception) {
                _uiState.value = RssUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
