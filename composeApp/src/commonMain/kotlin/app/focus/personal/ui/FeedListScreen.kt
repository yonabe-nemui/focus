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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import app.focus.personal.model.RssItem
import app.focus.personal.ui.components.FeedItem
import app.focus.personal.ui.components.feedErrorMessage
import app.focus.personal.ui.components.sourceDisplayName
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.viewmodel.FeedSource
import app.focus.personal.viewmodel.FeedUiState
import app.focus.personal.viewmodel.FeedViewModel
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.cd_clear
import focus.composeapp.generated.resources.cd_settings
import focus.composeapp.generated.resources.screen_title_feed
import focus.composeapp.generated.resources.search_placeholder
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
    viewModel: FeedViewModel,
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
    val searchQueries by viewModel.searchQueries.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(Res.string.screen_title_feed)) },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.cd_settings))
                        }
                    },
                )
                PrimaryTabRow(selectedTabIndex = currentSource.ordinal) {
                    FeedSource.entries.forEach { source ->
                        Tab(
                            selected = currentSource == source,
                            onClick = { viewModel.setSource(source) },
                            text = { Text(sourceDisplayName(source)) },
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        if (currentSource == FeedSource.BLUESKY && blueskySession == null) {
            BlueskyLoginScreen(
                is2faRequired = is2faRequired,
                uiState = uiState,
                onLogin = { handle, password, code -> viewModel.loginBluesky(handle, password, code) },
                modifier = Modifier.padding(paddingValues),
            )
        } else if (currentSource == FeedSource.MISSKEY && misskeySettings == null) {
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
                if (currentSource == FeedSource.BLUESKY || currentSource == FeedSource.MISSKEY) {
                    val query = searchQueries[currentSource].orEmpty()

                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.setSearchQuery(currentSource, it.replace("\n", "")) },
                        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchFeed("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.cd_clear))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FocusSpacing.sm, vertical = FocusSpacing.xs),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { viewModel.searchFeed(query) }),
                    )
                }
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.weight(1f),
                ) {
                    when (val state = uiState) {
                        is FeedUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is FeedUiState.Success -> {
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
                        is FeedUiState.Error -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = feedErrorMessage(state),
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
