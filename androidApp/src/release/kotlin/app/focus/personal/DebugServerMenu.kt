package app.focus.personal

import android.content.Context
import androidx.compose.runtime.Composable

// src/debug 側と対になる release 実装。デバッグメニューは含めず BuildConfig の値をそのまま使う。

fun resolveServerConfig(context: Context): ServerConfig = ServerConfig(
    // 空文字列(URL 未設定)なら null にして各ソースへ直接アクセスする
    baseUrl = BuildConfig.SERVER_BASE_URL.ifBlank { null },
    apiKey = BuildConfig.SERVER_API_KEY.ifBlank { null },
)

@Composable
fun DebugMenuHost(onTargetChanged: () -> Unit, content: @Composable () -> Unit) {
    content()
}
