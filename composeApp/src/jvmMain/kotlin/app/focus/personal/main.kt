package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.repository.RssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun main() = application {
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
            val googleApi = GoogleRssClient(client)
            val hatenaApi = HatenaRssClient(client)
            val blueskyApi = BlueskyClient(client)
            val repository = RssRepository(database, googleApi, hatenaApi, blueskyApi)
            RssViewModel(repository, scope)
        }

        App(
            viewModel = viewModel,
            onLinkClick = { /* Handle link click if needed */ }
        )
    }
}
