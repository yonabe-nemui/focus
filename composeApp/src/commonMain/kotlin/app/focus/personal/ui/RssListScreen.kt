package app.focus.personal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import app.focus.personal.model.RssItem
import app.focus.personal.ui.components.FeedItem
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.viewmodel.RssSource
import app.focus.personal.viewmodel.RssUiState
import app.focus.personal.viewmodel.RssViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssListScreen(
    viewModel: RssViewModel,
    onItemClick: (RssItem) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val currentSource by viewModel.currentSource.collectAsState()
    val blueskySession by viewModel.blueskySession.collectAsState()
    val is2faRequired by viewModel.is2faRequired.collectAsState()
    val misskeySettings by viewModel.misskeySettings.collectAsState()

    var localBlueskyQuery by remember { mutableStateOf("") }
    var localMisskeyQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("News Feed") },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "設定")
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = currentSource.ordinal) {
                    Tab(
                        selected = currentSource == RssSource.GOOGLE,
                        onClick = { viewModel.setSource(RssSource.GOOGLE) },
                        text = { Text("Google") },
                    )
                    Tab(
                        selected = currentSource == RssSource.HATENA,
                        onClick = { viewModel.setSource(RssSource.HATENA) },
                        text = { Text("はてな") },
                    )
                    Tab(
                        selected = currentSource == RssSource.BLUESKY,
                        onClick = { viewModel.setSource(RssSource.BLUESKY) },
                        text = { Text("BlueSky") },
                    )
                    Tab(
                        selected = currentSource == RssSource.MISSKEY,
                        onClick = { viewModel.setSource(RssSource.MISSKEY) },
                        text = { Text("Misskey") },
                    )
                }
            }
        },
    ) { paddingValues ->
        if (currentSource == RssSource.BLUESKY && blueskySession == null) {
            BlueskyLoginScreen(
                is2faRequired = is2faRequired,
                uiState = uiState,
                onLogin = { handle, password, code -> viewModel.loginBluesky(handle, password, code) },
                modifier = Modifier.padding(paddingValues),
            )
        } else if (currentSource == RssSource.MISSKEY && misskeySettings == null) {
            MisskeySettingsScreen(
                uiState = uiState,
                onSave = { instanceUrl, token -> viewModel.saveMisskeySettings(instanceUrl, token) },
                modifier = Modifier.padding(paddingValues),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                if (currentSource == RssSource.BLUESKY || currentSource == RssSource.MISSKEY) {
                    val localQuery = if (currentSource == RssSource.BLUESKY) localBlueskyQuery else localMisskeyQuery
                    val onQueryChange: (String) -> Unit =
                        if (currentSource == RssSource.BLUESKY) { q -> localBlueskyQuery = q }
                        else { q -> localMisskeyQuery = q }

                    OutlinedTextField(
                        value = localQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("検索...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (localQuery.isNotEmpty()) {
                                IconButton(onClick = { onQueryChange(""); viewModel.searchFeed("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "クリア")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FocusSpacing.sm, vertical = FocusSpacing.xs),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.searchFeed(localQuery) }),
                    )
                }
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                ) {
                    when (val state = uiState) {
                        is RssUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is RssUiState.Success -> {
                            val lazyListState = rememberLazyListState()
                            val shouldLoadMore by remember {
                                derivedStateOf {
                                    val layoutInfo = lazyListState.layoutInfo
                                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                                        ?: return@derivedStateOf false
                                    lastVisible >= layoutInfo.totalItemsCount - 3
                                }
                            }
                            LaunchedEffect(shouldLoadMore) {
                                if (shouldLoadMore && !isLoadingMore) viewModel.loadMore()
                            }
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                items(state.items) { item ->
                                    FeedItem(item = item, onClick = onItemClick)
                                }
                                if (isLoadingMore) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(FocusSpacing.lg),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(FocusSpacing.xl))
                                        }
                                    }
                                }
                            }
                        }
                        is RssUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Error: ${state.message}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
