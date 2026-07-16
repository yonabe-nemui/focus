package app.focus.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.focus.personal.ui.components.feedErrorMessage
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.viewmodel.FeedUiState
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.misskey_api_token_label
import focus.composeapp.generated.resources.misskey_connect_button
import focus.composeapp.generated.resources.misskey_instance_url_label
import focus.composeapp.generated.resources.misskey_settings_description
import focus.composeapp.generated.resources.misskey_settings_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun MisskeySettingsScreen(
    uiState: FeedUiState,
    onSave: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var instanceUrl by remember { mutableStateOf("misskey.io") }
    var apiToken by remember { mutableStateOf("") }
    var tokenVisible by remember { mutableStateOf(false) }
    val isLoading = uiState is FeedUiState.Loading

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = FocusSpacing.xl)
            .padding(top = FocusSpacing.xxxl, bottom = FocusSpacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = stringResource(Res.string.misskey_settings_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = FocusSpacing.sm),
        )
        Text(
            text = stringResource(Res.string.misskey_settings_description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FocusSpacing.xl),
        )

        if (uiState is FeedUiState.Error) {
            Text(
                text = feedErrorMessage(uiState),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = FocusSpacing.lg),
            )
        }

        TextField(
            value = instanceUrl,
            onValueChange = { instanceUrl = it.replace("\n", "") },
            label = { Text(stringResource(Res.string.misskey_instance_url_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FocusSpacing.sm),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false),
            singleLine = true,
            enabled = !isLoading,
        )
        TextField(
            value = apiToken,
            onValueChange = { apiToken = it.replace("\n", "") },
            label = { Text(stringResource(Res.string.misskey_api_token_label)) },
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { tokenVisible = !tokenVisible }, enabled = !isLoading) {
                    Icon(
                        imageVector = if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = FocusSpacing.lg),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
            singleLine = true,
            enabled = !isLoading,
        )
        Button(
            onClick = { onSave(instanceUrl.trim(), apiToken.trim().takeIf { it.isNotEmpty() }) },
            modifier = Modifier.fillMaxWidth(),
            enabled = instanceUrl.isNotEmpty() && !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(Res.string.misskey_connect_button))
            }
        }
    }
}
