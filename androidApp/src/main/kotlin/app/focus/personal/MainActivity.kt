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
                    serverBaseUrl = "http://10.0.2.2:$SERVER_PORT",
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
