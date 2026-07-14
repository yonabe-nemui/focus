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
import app.focus.personal.viewmodel.FeedErrorKind
import app.focus.personal.viewmodel.RssUiState
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.bluesky_2fa_description
import focus.composeapp.generated.resources.bluesky_2fa_label
import focus.composeapp.generated.resources.bluesky_auth_code_invalid_error
import focus.composeapp.generated.resources.bluesky_cd_hide_password
import focus.composeapp.generated.resources.bluesky_cd_show_password
import focus.composeapp.generated.resources.bluesky_handle_label
import focus.composeapp.generated.resources.bluesky_login_button
import focus.composeapp.generated.resources.bluesky_login_description
import focus.composeapp.generated.resources.bluesky_login_title
import focus.composeapp.generated.resources.bluesky_password_label
import focus.composeapp.generated.resources.bluesky_rate_limit_error
import focus.composeapp.generated.resources.bluesky_verify_button
import org.jetbrains.compose.resources.stringResource

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
            .padding(top = FocusSpacing.xxxl, bottom = FocusSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.bluesky_login_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = FocusSpacing.sm),
        )
        Text(
            text = stringResource(Res.string.bluesky_login_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FocusSpacing.xl),
        )

        if (uiState is RssUiState.Error) {
            val errorMessage = when (uiState.kind) {
                FeedErrorKind.RATE_LIMITED -> stringResource(Res.string.bluesky_rate_limit_error)
                FeedErrorKind.AUTH_CODE_INVALID -> stringResource(Res.string.bluesky_auth_code_invalid_error)
                FeedErrorKind.GENERIC -> uiState.message
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
                label = { Text(stringResource(Res.string.bluesky_handle_label)) },
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
                label = { Text(stringResource(Res.string.bluesky_password_label)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, enabled = !isLoading) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = stringResource(
                                if (passwordVisible) Res.string.bluesky_cd_hide_password else Res.string.bluesky_cd_show_password
                            ),
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
                    Text(stringResource(Res.string.bluesky_login_button))
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.bluesky_2fa_description),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = FocusSpacing.lg),
            )
            TextField(
                value = authCode,
                onValueChange = { authCode = it.replace("\n", "").replace(" ", "").uppercase() },
                label = { Text(stringResource(Res.string.bluesky_2fa_label)) },
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
                    Text(stringResource(Res.string.bluesky_verify_button))
                }
            }
        }
    }
}
