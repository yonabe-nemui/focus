package app.focus.personal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.focus.personal.ui.components.SectionDivider
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.viewmodel.FeedViewModel
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.cd_back
import focus.composeapp.generated.resources.cd_delete
import focus.composeapp.generated.resources.mute_words_add_button
import focus.composeapp.generated.resources.mute_words_description
import focus.composeapp.generated.resources.mute_words_empty
import focus.composeapp.generated.resources.mute_words_input_label
import focus.composeapp.generated.resources.screen_title_mute_words
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuteWordSettingsScreen(
    viewModel: FeedViewModel,
    onBack: () -> Unit,
) {
    val muteWords by viewModel.muteWords.collectAsState()
    val isLoading by viewModel.muteWordsLoading.collectAsState()
    var newWord by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadMuteWords() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_title_mute_words)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = FocusSpacing.lg),
        ) {
            Text(
                text = stringResource(Res.string.mute_words_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = FocusSpacing.md),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(FocusSpacing.sm),
            ) {
                OutlinedTextField(
                    value = newWord,
                    onValueChange = { newWord = it.replace("\n", "") },
                    label = { Text(stringResource(Res.string.mute_words_input_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                TextButton(
                    onClick = {
                        if (newWord.isNotBlank()) {
                            viewModel.addMuteWord(newWord)
                            newWord = ""
                        }
                    },
                    enabled = newWord.isNotBlank() && !isLoading,
                ) {
                    Text(stringResource(Res.string.mute_words_add_button))
                }
            }
            SectionDivider(modifier = Modifier.padding(vertical = FocusSpacing.md))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FocusSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (muteWords.isEmpty()) {
                Text(
                    text = stringResource(Res.string.mute_words_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = FocusSpacing.sm),
                )
            } else {
                LazyColumn {
                    items(muteWords) { word ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = FocusSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = word,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { viewModel.deleteMuteWord(word) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.cd_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        SectionDivider()
                    }
                }
            }
        }
    }
}
