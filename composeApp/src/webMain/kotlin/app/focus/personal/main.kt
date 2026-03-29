package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.network.GoogleRssClient
import app.focus.personal.network.HatenaRssClient
import app.focus.personal.repository.RssRepository
import app.focus.personal.viewmodel.RssViewModel
import io.ktor.client.HttpClient
import kotlinx.browser.window

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
                val client = HttpClient()
                val googleApi = GoogleRssClient(client)
                val hatenaApi = HatenaRssClient(client)
                
                // DB がない場合はダミーのデータを扱う仕組みが必要ですが、
                // まずは ViewModel の作成を完遂させます
                val repository = RssRepository(database, googleApi, hatenaApi)
                RssViewModel(repository, scope)
            } catch (e: Exception) {
                println("CRITICAL ERROR in initialization: ${e.message}")
                // ここで throw せず、何らかの形で UI を継続させる
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
