package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.BlueskyClient
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.MisskeyClient
import app.focus.personal.repository.RssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

fun MainViewController() = ComposeUIViewController {
    remember { Napier.base(DebugAntilog()) }
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        val driver = DriverFactory().createDriver()
        val database = FocusDatabase(driver)
        val client = HttpClient(Darwin) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val googleApi = GoogleRssClient(client)
        val hatenaApi = HatenaRssClient(client)
        val blueskyApi = BlueskyClient(client)
        val misskeyApi = MisskeyClient(client)
        val repository = RssRepository(database, googleApi, hatenaApi, blueskyApi, misskeyApi)
        RssViewModel(repository, scope)
    }

    App(
        viewModel = viewModel,
        onLinkClick = { url ->
            NSURL(string = url)?.let { nsUrl ->
                UIApplication.sharedApplication.openURL(
                    nsUrl,
                    options = emptyMap<Any?, Any?>(),
                    completionHandler = null
                )
            }
        }
    )
}
