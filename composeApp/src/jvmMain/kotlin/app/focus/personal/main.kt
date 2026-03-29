package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.repository.RssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Focus",
    ) {
        val scope = rememberCoroutineScope()
        val viewModel = remember {
            val driver = DriverFactory().createDriver()
            val database = FocusDatabase(driver)
            val client = HttpClient(OkHttp)
            val googleApi = GoogleRssClient(client)
            val hatenaApi = HatenaRssClient(client)
            val repository = RssRepository(database, googleApi, hatenaApi)
            RssViewModel(repository, scope)
        }

        App(
            viewModel = viewModel,
            onLinkClick = { /* Handle link click if needed */ }
        )
    }
}
