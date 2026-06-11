package app.focus.personal.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.focus.personal.model.BlueskySession
import app.focus.personal.model.MisskeySettings
import app.focus.personal.model.RssItem
import app.focus.personal.ui.components.FeedItem
import app.focus.personal.ui.components.SectionDivider
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.viewmodel.RssSource
import app.focus.personal.viewmodel.RssUiState
import app.focus.personal.viewmodel.RssViewModel
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.app_name
import focus.composeapp.generated.resources.cd_clear
import focus.composeapp.generated.resources.cd_refresh
import focus.composeapp.generated.resources.cd_settings
import focus.composeapp.generated.resources.search_placeholder
import focus.composeapp.generated.resources.source_bluesky
import focus.composeapp.generated.resources.source_google
import focus.composeapp.generated.resources.source_hatena
import focus.composeapp.generated.resources.source_misskey
import org.jetbrains.compose.resources.stringResource

@Composable
private fun sourceDisplayName(source: RssSource): String = when (source) {
    RssSource.GOOGLE  -> stringResource(Res.string.source_google)
    RssSource.HATENA  -> stringResource(Res.string.source_hatena)
    RssSource.BLUESKY -> stringResource(Res.string.source_bluesky)
    RssSource.MISSKEY -> stringResource(Res.string.source_misskey)
}

private val RssSource.icon: ImageVector
    get() = when (this) {
        RssSource.GOOGLE  -> Icons.Default.RssFeed
        RssSource.HATENA  -> Icons.Default.Bookmark
        RssSource.BLUESKY -> Icons.Default.Cloud
        RssSource.MISSKEY -> Icons.AutoMirrored.Filled.Message
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopRssScreen(
    viewModel: RssViewModel,
    onItemClick: (RssItem) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val columnStates by viewModel.columnStates.collectAsState()
    val blueskySession by viewModel.blueskySession.collectAsState()
    val is2faRequired by viewModel.is2faRequired.collectAsState()
    val misskeySettings by viewModel.misskeySettings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadAllSourcesParallel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(Res.string.cd_settings))
                    }
                },
            )
        },
    ) { padding ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            val minColumnWidth = 280.dp
            val sourceCount = RssSource.entries.size
            val fitsAll = maxWidth >= minColumnWidth * sourceCount
            val columnWidth = if (fitsAll) maxWidth / sourceCount else minColumnWidth

            val rowModifier = if (fitsAll) Modifier.fillMaxSize()
            else Modifier.fillMaxSize().horizontalScroll(rememberScrollState())

            Row(modifier = rowModifier) {
                RssSource.entries.forEachIndexed { index, source ->
                    FeedColumn(
                        source = source,
                        uiState = columnStates[source] ?: RssUiState.Loading,
                        loginUiState = uiState,
                        blueskySession = blueskySession,
                        is2faRequired = is2faRequired,
                        misskeySettings = misskeySettings,
                        onItemClick = onItemClick,
                        onRefresh = { viewModel.refreshColumn(source) },
                        onLogin = { handle, password, code -> viewModel.loginBluesky(handle, password, code) },
                        onSaveMisskey = { url, token -> viewModel.saveMisskeySettings(url, token) },
                        onSearch = { query -> viewModel.searchColumnFeed(source, query) },
                        modifier = Modifier
                            .width(columnWidth)
                            .fillMaxHeight(),
                    )
                    if (index < sourceCount - 1) VerticalDivider()
                }
            }
        }
    }
}

@Composable
private fun FeedColumn(
    source: RssSource,
    uiState: RssUiState,
    loginUiState: RssUiState,
    blueskySession: BlueskySession?,
    is2faRequired: Boolean,
    misskeySettings: MisskeySettings?,
    onItemClick: (RssItem) -> Unit,
    onRefresh: () -> Unit,
    onLogin: (String, String, String?) -> Unit,
    onSaveMisskey: (String, String?) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSearch = source == RssSource.BLUESKY || source == RssSource.MISSKEY
    val needsAuth = (source == RssSource.BLUESKY && blueskySession == null) ||
                    (source == RssSource.MISSKEY && misskeySettings == null)
    val isLoading = uiState is RssUiState.Loading
    var localSearchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier) {
        // カラムヘッダー（フラット、elevation なし）
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = FocusSpacing.md, vertical = FocusSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = source.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(FocusSpacing.sm))
                    Text(
                        text = sourceDisplayName(source),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (!needsAuth) {
                        IconButton(onClick = onRefresh, enabled = !isLoading) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(Res.string.cd_refresh),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                if (hasSearch && !needsAuth) {
                    OutlinedTextField(
                        value = localSearchQuery,
                        onValueChange = { localSearchQuery = it.replace("\n", "") },
                        placeholder = { Text(stringResource(Res.string.search_placeholder), style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (localSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { localSearchQuery = ""; onSearch("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.cd_clear), modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = FocusSpacing.sm)
                            .padding(bottom = FocusSpacing.sm),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch(localSearchQuery) }),
                    )
                }
            }
        }

        SectionDivider()

        when {
            source == RssSource.BLUESKY && blueskySession == null ->
                BlueskyLoginScreen(
                    is2faRequired = is2faRequired,
                    uiState = loginUiState,
                    onLogin = onLogin,
                )
            source == RssSource.MISSKEY && misskeySettings == null ->
                MisskeySettingsScreen(
                    uiState = loginUiState,
                    onSave = onSaveMisskey,
                )
            else -> ColumnFeedList(uiState = uiState, onItemClick = onItemClick)
        }
    }
}

@Composable
private fun ColumnFeedList(
    uiState: RssUiState,
    onItemClick: (RssItem) -> Unit,
) {
    when (uiState) {
        is RssUiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        is RssUiState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(uiState.items) { item ->
                FeedItem(item = item, onClick = onItemClick)
            }
        }
        is RssUiState.Error -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(FocusSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
