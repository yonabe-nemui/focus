package app.focus.personal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.focus.personal.viewmodel.RssSource
import app.focus.personal.viewmodel.RssUiState
import app.focus.personal.viewmodel.RssViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssListScreen(
    viewModel: RssViewModel,
    onLinkClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val currentSource by viewModel.currentSource.collectAsState()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("News Feed") })
                TabRow(selectedTabIndex = currentSource.ordinal) {
                    Tab(
                        selected = currentSource == RssSource.GOOGLE,
                        onClick = { viewModel.setSource(RssSource.GOOGLE) },
                        text = { Text("Google") }
                    )
                    Tab(
                        selected = currentSource == RssSource.HATENA,
                        onClick = { viewModel.setSource(RssSource.HATENA) },
                        text = { Text("はてな") }
                    )
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RssUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is RssUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp)
                    ) {
                        items(state.items) { item ->
                            RssItemCard(
                                item = item,
                                onClick = onLinkClick
                            )
                        }
                    }
                }
                is RssUiState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
