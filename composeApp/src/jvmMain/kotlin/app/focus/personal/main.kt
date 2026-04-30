package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
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

fun main() = application {
    Napier.base(DebugAntilog())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Focus",
    ) {
        val scope = rememberCoroutineScope()
        val viewModel = remember {
            val driver = DriverFactory().createDriver()
            val database = FocusDatabase(driver)
            val client = HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
            val apiClient = FocusApiClient(client, "http://localhost:$SERVER_PORT")
            val blueskyApi = BlueskyClient(client)
            val repository = ServerRssRepository(database, apiClient, blueskyApi)
            RssViewModel(repository, scope)
        }

        App(
            viewModel = viewModel,
            onLinkClick = { /* Handle link click if needed */ }
        )
    }
}
