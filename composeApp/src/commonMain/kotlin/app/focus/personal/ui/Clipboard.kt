package app.focus.personal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import kotlinx.coroutines.launch

/** プレーンテキストの ClipEntry。生成方法がプラットフォームごとに異なる。 */
expect fun plainTextClipEntry(text: String): ClipEntry

/** テキストをクリップボードにコピーする関数を返す。 */
@Composable
fun rememberCopyToClipboard(): (String) -> Unit {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    return remember(clipboard, scope) {
        { text -> scope.launch { clipboard.setClipEntry(plainTextClipEntry(text)) } }
    }
}
