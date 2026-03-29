package app.focus.personal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
    val blueskySession by viewModel.blueskySession.collectAsState()

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
                    Tab(
                        selected = currentSource == RssSource.BLUESKY,
                        onClick = { viewModel.setSource(RssSource.BLUESKY) },
                        text = { Text("BlueSky") }
                    )
                }
            }
        }
    ) { paddingValues ->
        if (currentSource == RssSource.BLUESKY && blueskySession == null) {
            BlueskyLoginScreen(
                onLogin = { handle, password -> viewModel.loginBluesky(handle, password) },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
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
}

@Composable
fun BlueskyLoginScreen(
    onLogin: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var handle by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "BlueSky Login",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "「見たくないものは見ない」設定（ミュートワード等）を反映するためにログインが必要です。",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        TextField(
            value = handle,
            onValueChange = { handle = it },
            label = { Text("Handle (e.g. user.bsky.social)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("App Password") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                val description = if (passwordVisible) "Hide password" else "Show password"

                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = description)
                }
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        Button(
            onClick = { onLogin(handle, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = handle.isNotEmpty() && password.isNotEmpty()
        ) {
            Text("Login")
        }
    }
}
