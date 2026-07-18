package app.focus.personal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.focus.personal.ui.components.SectionDivider
import app.focus.personal.ui.theme.FocusSpacing
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.cd_back
import focus.composeapp.generated.resources.licenses_description
import focus.composeapp.generated.resources.screen_title_licenses
import org.jetbrains.compose.resources.stringResource

/** 利用ライブラリ・フォントのライセンス情報。名称・ライセンス識別子は固有名詞のためリソース化しない。 */
private data class LicenseEntry(
    val name: String,
    val license: String,
    val url: String,
)

private val licenseEntries = listOf(
    LicenseEntry("Kotlin / kotlinx (coroutines, serialization, datetime)", "Apache License 2.0", "https://github.com/JetBrains/kotlin"),
    LicenseEntry("Compose Multiplatform", "Apache License 2.0", "https://github.com/JetBrains/compose-multiplatform"),
    LicenseEntry("AndroidX (Jetpack)", "Apache License 2.0", "https://developer.android.com/jetpack"),
    LicenseEntry("Ktor", "Apache License 2.0", "https://github.com/ktorio/ktor"),
    LicenseEntry("OkHttp", "Apache License 2.0", "https://github.com/square/okhttp"),
    LicenseEntry("SQLDelight", "Apache License 2.0", "https://github.com/sqldelight/sqldelight"),
    LicenseEntry("xmlutil", "Apache License 2.0", "https://github.com/pdvrieze/xmlutil"),
    LicenseEntry("Napier", "Apache License 2.0", "https://github.com/AAkira/Napier"),
    LicenseEntry("Coil", "Apache License 2.0", "https://github.com/coil-kt/coil"),
    LicenseEntry("Logback (server)", "EPL 1.0 / LGPL 2.1", "https://logback.qos.ch/"),
    LicenseEntry("Inter", "SIL Open Font License 1.1", "https://github.com/rsms/inter"),
    LicenseEntry("Noto Sans JP", "SIL Open Font License 1.1", "https://github.com/googlefonts/noto-cjk"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_title_licenses)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                Text(
                    text = stringResource(Res.string.licenses_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = FocusSpacing.lg,
                        vertical = FocusSpacing.sm,
                    ),
                )
            }
            items(licenseEntries) { entry ->
                Column {
                    ListItem(
                        headlineContent = { Text(entry.name, style = MaterialTheme.typography.titleSmall) },
                        supportingContent = {
                            Column {
                                Text(entry.license, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = entry.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenInBrowser(entry.url) },
                    )
                    SectionDivider()
                }
            }
        }
    }
}
