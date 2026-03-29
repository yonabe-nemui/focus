package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.repository.RssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.window
import kotlinx.serialization.json.Json

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport("compose-app") {
        val scope = rememberCoroutineScope()
        val viewModel = remember {
            try {
                // JS環境では worker.js がないと DB 初期化に失敗するため、
                // 失敗してもクラッシュさせないようにガードする
                val driver = try {
                    DriverFactory().createDriver()
                } catch (e: Exception) {
                    println("LOG: SQLDelight Driver creation failed, falling back to dummy. ${e.message}")
                    null
                }
                
                val database = driver?.let { FocusDatabase(it) }
                val client = HttpClient {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
                val googleApi = GoogleRssClient(client)
                val hatenaApi = HatenaRssClient(client)
                val blueskyApi = BlueskyClient(client)
                
                val repository = RssRepository(database, googleApi, hatenaApi, blueskyApi)
                RssViewModel(repository, scope)
            } catch (e: Exception) {
                println("CRITICAL ERROR in initialization: ${e.message}")
                throw e
            }
        }

        App(
            viewModel = viewModel,
            onLinkClick = { url ->
                window.open(url, "_blank")
            }
        )
    }
}
