package app.focus.personal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/** デバッグビルド専用: 接続先(ローカル / Cloud Run)の選択を SharedPreferences に保持する */
object DebugServerSettings {
    const val TARGET_LOCAL = "local"
    const val TARGET_CLOUD = "cloud"
    private const val PREFS_NAME = "debug_server_settings"
    private const val KEY_TARGET = "target"

    fun target(context: Context): String = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_TARGET, TARGET_LOCAL) ?: TARGET_LOCAL

    fun setTarget(context: Context, value: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_TARGET, value) }
    }
}

fun resolveServerConfig(context: Context): ServerConfig = when (DebugServerSettings.target(context)) {
    DebugServerSettings.TARGET_CLOUD -> ServerConfig(
        baseUrl = BuildConfig.SERVER_BASE_URL_CLOUD.ifBlank { null },
        apiKey = BuildConfig.SERVER_API_KEY.ifBlank { null },
    )

    // ローカルサーバーは FOCUS_API_KEY なしで起動する想定。平文 HTTP にキーを乗せない
    else -> ServerConfig(
        baseUrl = BuildConfig.SERVER_BASE_URL.ifBlank { null },
        apiKey = null,
    )
}

/**
 * デバッグビルド専用: 画面左下に DEBUG チップを重ね、タップで接続先切り替えダイアログを開く。
 * release 側(src/release)は content をそのまま表示するだけの no-op。
 */
@Composable
fun DebugMenuHost(onTargetChanged: () -> Unit, content: @Composable () -> Unit) {
    // 許可された時点で初回の読み込みは失敗しているため、再生成して読み直す
    RequestLocalNetworkPermission(onGranted = onTargetChanged)

    Box(Modifier.fillMaxSize()) {
        content()

        var showDialog by remember { mutableStateOf(false) }
        Surface(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(8.dp),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
        ) {
            Text(
                text = stringResource(R.string.debug_menu_chip),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        if (showDialog) {
            DebugServerDialog(
                onDismiss = { showDialog = false },
                onTargetChanged = onTargetChanged,
            )
        }
    }
}

/**
 * Android 17 以降(targetSdk 37)は、ローカルサーバー(エミュレーターの 10.0.2.2・PC の LAN IP)への接続に
 * ACCESS_LOCAL_NETWORK の実行時許可が必要。接続先がローカルサーバーのときだけ要求する。
 * release の接続先(Cloud Run・各ソースの公開 API)はローカルネットワークではないため不要。
 */
@Composable
private fun RequestLocalNetworkPermission(onGranted: () -> Unit) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN) return
    val context = LocalContext.current
    if (DebugServerSettings.target(context) != DebugServerSettings.TARGET_LOCAL) return

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onGranted()
    }
    LaunchedEffect(Unit) {
        val permission = Manifest.permission.ACCESS_LOCAL_NETWORK
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(permission)
        }
    }
}

private data class DebugServerOption(val target: String, val label: String, val url: String)

@Composable
private fun DebugServerDialog(onDismiss: () -> Unit, onTargetChanged: () -> Unit) {
    val context = LocalContext.current
    val currentTarget = remember { DebugServerSettings.target(context) }
    val unsetLabel = stringResource(R.string.debug_server_unset)
    val options = listOf(
        DebugServerOption(
            target = DebugServerSettings.TARGET_LOCAL,
            label = stringResource(R.string.debug_server_local),
            url = BuildConfig.SERVER_BASE_URL.ifBlank { unsetLabel },
        ),
        DebugServerOption(
            target = DebugServerSettings.TARGET_CLOUD,
            label = stringResource(R.string.debug_server_cloud),
            url = BuildConfig.SERVER_BASE_URL_CLOUD.ifBlank { unsetLabel },
        ),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.debug_server_dialog_title)) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = currentTarget == option.target,
                                onClick = {
                                    if (option.target != currentTarget) {
                                        DebugServerSettings.setTarget(context, option.target)
                                        onTargetChanged()
                                    }
                                    onDismiss()
                                },
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = currentTarget == option.target, onClick = null)
                        Column(Modifier.padding(start = 8.dp)) {
                            Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = option.url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.debug_server_close))
            }
        },
    )
}
