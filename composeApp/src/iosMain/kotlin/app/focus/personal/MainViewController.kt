package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
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
        val api = YahooRssClient(client)
        val repository = RssRepository(database, api)
        RssViewModel(repository, scope)
    }

    App(
        viewModel = viewModel,
        onLinkClick = { /* Handle link click if needed */ }
    )
}