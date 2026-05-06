package app.focus.personal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.viewmodel.RssUiState

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
    val autofillTree = LocalAutofillTree.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = FocusSpacing.xl)
            .padding(top = FocusSpacing.xxl, bottom = FocusSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = "BlueSky Login",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = FocusSpacing.sm),
        )
        Text(
            text = "「見たくないものは見ない」設定（ミュートワード等）を反映するためにログインが必要です。",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FocusSpacing.xl),
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
                        .padding(bottom = FocusSpacing.lg)
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { clipboardManager.setText(AnnotatedString(uiState.message)) },
                        ),
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
                    .padding(bottom = FocusSpacing.sm)
                    .onGloballyPositioned {
                        autofillTree += AutofillNode(
                            autofillTypes = listOf(AutofillType.Username, AutofillType.EmailAddress),
                            onFill = { handle = it },
                        )
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                singleLine = true,
                enabled = !isLoading,
            )
            TextField(
                value = password,
                onValueChange = { password = it.replace("\n", "") },
                label = { Text("App Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FocusSpacing.lg)
                    .onGloballyPositioned {
                        autofillTree += AutofillNode(
                            autofillTypes = listOf(AutofillType.Password),
                            onFill = { password = it },
                        )
                    },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                singleLine = true,
                enabled = !isLoading,
            )
            Button(
                onClick = { onLogin(handle.trim(), password.trim(), null) },
                modifier = Modifier.fillMaxWidth(),
                enabled = handle.isNotEmpty() && password.isNotEmpty() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Login")
                }
            }
        } else {
            Text(
                text = "認証コードがメールで送信されました。入力してください。",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = FocusSpacing.lg),
            )
            TextField(
                value = authCode,
                onValueChange = { authCode = it.replace("\n", "").replace(" ", "").uppercase() },
                label = { Text("Verification Code") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FocusSpacing.lg),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, autoCorrectEnabled = false),
                singleLine = true,
                enabled = !isLoading,
            )
            Button(
                onClick = { onLogin(handle.trim(), password.trim(), authCode.trim()) },
                modifier = Modifier.fillMaxWidth(),
                enabled = authCode.isNotEmpty() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Verify Code")
                }
            }
        }
    }
}
