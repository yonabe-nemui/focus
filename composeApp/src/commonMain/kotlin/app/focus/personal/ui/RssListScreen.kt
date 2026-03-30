package app.focus.personal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
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
    val is2faRequired by viewModel.is2faRequired.collectAsState()

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
                is2faRequired = is2faRequired,
                uiState = uiState,
                onLogin = { handle, password, code ->
                    viewModel.loginBluesky(
                        handle,
                        password,
                        code
                    )
                },
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            Box(
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BlueskyLoginScreen(
    is2faRequired: Boolean,
    uiState: RssUiState,
    onLogin: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var handle by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val isLoading = uiState is RssUiState.Loading

    // Autofill support
    val autofill = LocalAutofill.current
    val autofillTree = LocalAutofillTree.current

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

        if (uiState is RssUiState.Error) {
            val errorMessage =
                if (uiState.message.contains("429") || uiState.message.contains("RateLimitExceeded")) {
                    "アクセス制限がかかりました。数分〜数十分待ってから再度お試しください。"
                } else {
                    uiState.message
                }

            SelectionContainer {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .combinedClickable(
                            onClick = { /* Do nothing on single click */ },
                            onLongClick = {
                                clipboardManager.setText(AnnotatedString(uiState.message))
                            }
                        )
                )
            }
        }

        if (!is2faRequired) {
            TextField(
                value = handle,
                onValueChange = { handle = it.replace("\n", "") },
                label = { Text("Handle or Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        val node = AutofillNode(
                            autofillTypes = listOf(
                                AutofillType.Username,
                                AutofillType.EmailAddress
                            ),
                            onFill = { handle = it }
                        )
                        autofillTree += node
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    autoCorrect = false
                ),
                singleLine = true,
                enabled = !isLoading
            )
            TextField(
                value = password,
                onValueChange = { password = it.replace("\n", "") },
                label = { Text("App Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image =
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        enabled = !isLoading
                    ) {
                        Icon(imageVector = image, contentDescription = description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .onGloballyPositioned { coordinates ->
                        val node = AutofillNode(
                            autofillTypes = listOf(AutofillType.Password),
                            onFill = { password = it }
                        )
                        autofillTree += node
                    },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    autoCorrect = false
                ),
                singleLine = true,
                enabled = !isLoading
            )
            Button(
                onClick = { onLogin(handle.trim(), password.trim(), null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = handle.isNotEmpty() && password.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Login")
                }
            }
        } else {
            Text(
                text = "認証コードがメールで送信されました。入力してください。",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            TextField(
                value = authCode,
                onValueChange = { authCode = it.replace("\n", "").replace(" ", "").uppercase() },
                label = { Text("Verification Code") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    autoCorrect = false
                ),
                singleLine = true,
                enabled = !isLoading
            )
            Button(
                onClick = { 
                    // ハイフンを除去して送信する
                    val cleanedCode = authCode.replace("-", "").trim()
                    onLogin(handle.trim(), password.trim(), cleanedCode) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authCode.isNotEmpty() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Verify Code")
                }
            }
        }
    }
}
