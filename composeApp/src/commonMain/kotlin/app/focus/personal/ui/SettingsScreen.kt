package app.focus.personal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.focus.personal.model.ThemeMode
import app.focus.personal.ui.components.SectionDivider
import app.focus.personal.ui.components.sourceDisplayName
import app.focus.personal.ui.theme.FocusSpacing
import app.focus.personal.ui.theme.supportsDynamicColor
import app.focus.personal.viewmodel.FeedSource
import app.focus.personal.viewmodel.FeedViewModel
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.cd_back
import focus.composeapp.generated.resources.dialog_cancel
import focus.composeapp.generated.resources.screen_title_licenses
import focus.composeapp.generated.resources.screen_title_mute_words
import focus.composeapp.generated.resources.screen_title_settings
import focus.composeapp.generated.resources.settings_account_section
import focus.composeapp.generated.resources.settings_bluesky_logged_in
import focus.composeapp.generated.resources.settings_bluesky_logout
import focus.composeapp.generated.resources.settings_bluesky_logout_confirm
import focus.composeapp.generated.resources.settings_licenses_summary
import focus.composeapp.generated.resources.settings_misskey_connected
import focus.composeapp.generated.resources.settings_misskey_disconnect
import focus.composeapp.generated.resources.settings_misskey_disconnect_confirm
import focus.composeapp.generated.resources.settings_mute_words_summary
import focus.composeapp.generated.resources.settings_theme_section
import focus.composeapp.generated.resources.theme_dynamic_summary
import focus.composeapp.generated.resources.theme_dynamic_title
import focus.composeapp.generated.resources.theme_mode_dark
import focus.composeapp.generated.resources.theme_mode_light
import focus.composeapp.generated.resources.theme_mode_system
import focus.composeapp.generated.resources.theme_oled_summary
import focus.composeapp.generated.resources.theme_oled_title
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: FeedViewModel, onNavigateToMuteWords: () -> Unit, onNavigateToLicenses: () -> Unit, onBack: () -> Unit) {
    val themeSettings by viewModel.themeSettings.collectAsState()
    val blueskySession by viewModel.blueskySession.collectAsState()
    val misskeySettings by viewModel.misskeySettings.collectAsState()
    var pendingConfirm by remember { mutableStateOf<AccountAction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.screen_title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.screen_title_mute_words)) },
                supportingContent = { Text(stringResource(Res.string.settings_mute_words_summary)) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToMuteWords() },
            )
            SectionDivider()

            // ── アカウント(ログイン・接続中のときだけ表示) ──
            val session = blueskySession
            val misskey = misskeySettings
            if (session != null || misskey != null) {
                SettingsSectionHeader(stringResource(Res.string.settings_account_section))
                if (session != null) {
                    AccountItem(
                        name = sourceDisplayName(FeedSource.BLUESKY),
                        status = stringResource(Res.string.settings_bluesky_logged_in, "@${session.handle}"),
                        actionLabel = stringResource(Res.string.settings_bluesky_logout),
                        onAction = { pendingConfirm = AccountAction.BLUESKY_LOGOUT },
                    )
                }
                if (misskey != null) {
                    AccountItem(
                        name = sourceDisplayName(FeedSource.MISSKEY),
                        status = stringResource(Res.string.settings_misskey_connected, misskey.instanceUrl),
                        actionLabel = stringResource(Res.string.settings_misskey_disconnect),
                        onAction = { pendingConfirm = AccountAction.MISSKEY_DISCONNECT },
                    )
                }
                SectionDivider()
            }

            // ── テーマ ──
            SettingsSectionHeader(stringResource(Res.string.settings_theme_section))
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FocusSpacing.lg, vertical = FocusSpacing.sm),
            ) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = themeSettings.mode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                    ) {
                        Text(themeModeLabel(mode))
                    }
                }
            }
            ListItem(
                headlineContent = { Text(stringResource(Res.string.theme_oled_title)) },
                supportingContent = { Text(stringResource(Res.string.theme_oled_summary)) },
                trailingContent = {
                    Switch(
                        checked = themeSettings.oledBlack,
                        onCheckedChange = { viewModel.setOledBlack(it) },
                    )
                },
            )
            if (supportsDynamicColor) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.theme_dynamic_title)) },
                    supportingContent = { Text(stringResource(Res.string.theme_dynamic_summary)) },
                    trailingContent = {
                        Switch(
                            checked = themeSettings.dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                        )
                    },
                )
            }
            SectionDivider()

            // ── ライセンス ──
            ListItem(
                headlineContent = { Text(stringResource(Res.string.screen_title_licenses)) },
                supportingContent = { Text(stringResource(Res.string.settings_licenses_summary)) },
                trailingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToLicenses() },
            )
            SectionDivider()
        }
    }

    pendingConfirm?.let { action ->
        AccountConfirmDialog(
            action = action,
            onConfirm = {
                pendingConfirm = null
                when (action) {
                    AccountAction.BLUESKY_LOGOUT -> viewModel.logoutBluesky()
                    AccountAction.MISSKEY_DISCONNECT -> viewModel.clearMisskeySettings()
                }
            },
            onDismiss = { pendingConfirm = null },
        )
    }
}

/** ログアウト・接続解除の確認待ちの操作。 */
private enum class AccountAction { BLUESKY_LOGOUT, MISSKEY_DISCONNECT }

@Composable
private fun AccountConfirmDialog(action: AccountAction, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val (message, confirmLabel) = when (action) {
        AccountAction.BLUESKY_LOGOUT ->
            Res.string.settings_bluesky_logout_confirm to Res.string.settings_bluesky_logout

        AccountAction.MISSKEY_DISCONNECT ->
            Res.string.settings_misskey_disconnect_confirm to Res.string.settings_misskey_disconnect
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(stringResource(message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(confirmLabel)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun SettingsSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = FocusSpacing.lg,
            top = FocusSpacing.lg,
            end = FocusSpacing.lg,
        ),
    )
}

@Composable
private fun AccountItem(name: String, status: String, actionLabel: String, onAction: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = { Text(status) },
        trailingContent = {
            TextButton(onClick = onAction) { Text(actionLabel) }
        },
    )
}

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(Res.string.theme_mode_system)
    ThemeMode.LIGHT -> stringResource(Res.string.theme_mode_light)
    ThemeMode.DARK -> stringResource(Res.string.theme_mode_dark)
}
