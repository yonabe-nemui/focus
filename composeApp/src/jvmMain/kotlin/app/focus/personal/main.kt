package app.focus.personal

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.focus.personal.db.DriverFactory
import app.focus.personal.db.FocusDatabase
import app.focus.personal.repository.PreferenceStore
import app.focus.personal.viewmodel.FeedSource
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.engine.okhttp.OkHttp
import java.awt.Desktop
import java.net.URI

// ウィンドウサイズ・位置の永続化キー
private const val KEY_WINDOW_WIDTH = "window_width"
private const val KEY_WINDOW_HEIGHT = "window_height"
private const val KEY_WINDOW_X = "window_x"
private const val KEY_WINDOW_Y = "window_y"

fun main() = application {
    Napier.base(DebugAntilog())

    val scope = rememberCoroutineScope()
    val database = remember { FocusDatabase(DriverFactory().createDriver()) }
    val preferences = remember { PreferenceStore(database) }
    val viewModel = remember {
        createFeedViewModel(scope = scope, database = database, engine = OkHttp)
    }

    // 前回終了時のウィンドウサイズ・位置を復元する
    val windowState = rememberWindowState(
        size = DpSize(
            width = (preferences.get(KEY_WINDOW_WIDTH)?.toFloatOrNull() ?: 800f).dp,
            height = (preferences.get(KEY_WINDOW_HEIGHT)?.toFloatOrNull() ?: 600f).dp,
        ),
        position = run {
            val x = preferences.get(KEY_WINDOW_X)?.toFloatOrNull()
            val y = preferences.get(KEY_WINDOW_Y)?.toFloatOrNull()
            if (x != null && y != null) WindowPosition(x.dp, y.dp) else WindowPosition.PlatformDefault
        },
    )

    fun saveWindowState() {
        preferences.put(KEY_WINDOW_WIDTH, windowState.size.width.value.toString())
        preferences.put(KEY_WINDOW_HEIGHT, windowState.size.height.value.toString())
        val position = windowState.position
        if (position.x.isSpecified && position.y.isSpecified) {
            preferences.put(KEY_WINDOW_X, position.x.value.toString())
            preferences.put(KEY_WINDOW_Y, position.y.value.toString())
        }
    }

    Window(
        onCloseRequest = {
            saveWindowState()
            exitApplication()
        },
        state = windowState,
        title = "Focus",
        onPreviewKeyEvent = { event ->
            // Ctrl+R: 全カラムをリフレッシュ(r 単独は検索欄の入力と衝突するため修飾キー付き)
            if (event.type == KeyEventType.KeyDown && event.isCtrlPressed && event.key == Key.R) {
                FeedSource.entries.forEach { viewModel.refreshColumn(it) }
                true
            } else {
                false
            }
        },
    ) {
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
