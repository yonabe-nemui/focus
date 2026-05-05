package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.MisskeyClient
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
                // Web では WebWorkerDriver が非同期のため DB は使用しない（セッションはメモリのみ保持）
                val client = HttpClient {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
                }
                val googleApi = GoogleRssClient(client)
                val hatenaApi = HatenaRssClient(client)
                val blueskyApi = BlueskyClient(client)
                val misskeyApi = MisskeyClient(client)

                val repository = RssRepository(null, googleApi, hatenaApi, blueskyApi, misskeyApi)
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
