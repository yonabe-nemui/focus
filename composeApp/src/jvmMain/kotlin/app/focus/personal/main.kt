package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.engine.okhttp.OkHttp
import java.awt.Desktop
import java.net.URI

fun main() = application {
    Napier.base(DebugAntilog())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Focus",
    ) {
        val scope = rememberCoroutineScope()
        val viewModel = remember {
            val database = FocusDatabase(DriverFactory().createDriver())
            createFeedViewModel(scope = scope, database = database, engine = OkHttp)
        }

        DesktopApp(
            viewModel = viewModel,
            onLinkClick = { url ->
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(url))
                }
            }
        )
    }
}
