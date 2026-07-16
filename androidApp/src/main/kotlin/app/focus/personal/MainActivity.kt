package app.focus.personal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Napier.base(DebugAntilog())

        setContent {
            val scope = rememberCoroutineScope()
            val viewModel = remember {
                val database = FocusDatabase(DriverFactory(this@MainActivity).createDriver())
                createRssViewModel(
                    scope = scope,
                    database = database,
                    engine = OkHttp,
                    // 空文字列(URL 未設定)なら null にして各ソースへ直接アクセスする
                    serverBaseUrl = BuildConfig.SERVER_BASE_URL.ifBlank { null },
                )
            }

            App(
                viewModel = viewModel,
                onLinkClick = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            )
        }
    }
}
