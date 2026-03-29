package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.network.YahooRssClient
import app.focus.personal.repository.RssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

fun MainViewController() = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        val driver = DriverFactory().createDriver()
        val database = FocusDatabase(driver)
        val client = HttpClient(Darwin)
        val yahooApi = YahooRssClient(client)
        val googleApi = GoogleRssClient(client)
        val hatenaApi = HatenaRssClient(client)
        val repository = RssRepository(database, yahooApi, googleApi, hatenaApi)
        RssViewModel(repository, scope)
    }

    App(
        viewModel = viewModel,
        onLinkClick = { /* Handle link click if needed */ }
    )
}