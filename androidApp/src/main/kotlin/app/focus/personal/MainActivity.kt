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
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.FocusApiClient
import app.focus.personal.repository.ServerRssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Napier.base(DebugAntilog())

        setContent {
            val scope = rememberCoroutineScope()
            val viewModel = remember {
                val driver = DriverFactory(this@MainActivity).createDriver()
                val database = FocusDatabase(driver)
                val client = HttpClient(OkHttp) {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
                val apiClient = FocusApiClient(client, "http://10.0.2.2:$SERVER_PORT")
                val blueskyApi = BlueskyClient(client)
                val repository = ServerRssRepository(database, apiClient, blueskyApi)
                RssViewModel(repository, scope)
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
