package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.engine.darwin.Darwin
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

// Swift 側(ContentView.swift)から呼ぶ UIViewController のファクトリ。名前は Swift 側と合わせる
@Suppress("ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController {
    remember { Napier.base(DebugAntilog()) }
    val scope = rememberCoroutineScope()
    val viewModel = remember {
        val database = FocusDatabase(DriverFactory().createDriver())
        createFeedViewModel(scope = scope, database = database, engine = Darwin)
    }

    App(
        viewModel = viewModel,
        onLinkClick = { url ->
            NSURL(string = url)?.let { nsUrl ->
                UIApplication.sharedApplication.openURL(
                    nsUrl,
                    options = emptyMap<Any?, Any?>(),
                    completionHandler = null,
                )
            }
        },
    )
}
