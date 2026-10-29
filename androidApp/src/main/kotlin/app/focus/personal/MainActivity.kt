package app.focus.personal

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.net.toUri
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 以降は OS 側で強制されるため、全バージョンで明示的に有効化して挙動を揃える
        enableEdgeToEdge()
        Napier.base(DebugAntilog())

        setContent {
            val scope = rememberCoroutineScope()
            val viewModel = remember {
                val database = FocusDatabase(DriverFactory(this@MainActivity).createDriver())
                // 接続先はビルドタイプ別の resolveServerConfig が決める
                // (debug はデバッグメニューの選択を反映、release は BuildConfig の値)
                val serverConfig = resolveServerConfig(this@MainActivity)
                createFeedViewModel(
                    scope = scope,
                    database = database,
                    engine = OkHttp,
                    serverBaseUrl = serverConfig.baseUrl,
                    serverApiKey = serverConfig.apiKey,
                )
            }

            // debug のみ接続先切り替えメニューを重ねる。切り替え時は再生成して Repository を作り直す
            DebugMenuHost(onTargetChanged = { recreate() }) {
                App(
                    viewModel = viewModel,
                    onLinkClick = { url ->
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        startActivity(intent)
                    },
                )
            }
        }
    }
}
