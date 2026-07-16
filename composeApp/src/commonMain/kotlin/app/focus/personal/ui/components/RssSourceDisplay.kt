package app.focus.personal.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.focus.personal.viewmodel.RssSource
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.source_bluesky
import focus.composeapp.generated.resources.source_google
import focus.composeapp.generated.resources.source_hatena
import focus.composeapp.generated.resources.source_misskey
import org.jetbrains.compose.resources.stringResource

/** ソースの表示名。 */
@Composable
fun sourceDisplayName(source: RssSource): String = when (source) {
    RssSource.GOOGLE -> stringResource(Res.string.source_google)
    RssSource.HATENA -> stringResource(Res.string.source_hatena)
    RssSource.BLUESKY -> stringResource(Res.string.source_bluesky)
    RssSource.MISSKEY -> stringResource(Res.string.source_misskey)
}

/** ソースのアイコン。 */
val RssSource.icon: ImageVector
    get() = when (this) {
        RssSource.GOOGLE -> Icons.Default.RssFeed
        RssSource.HATENA -> Icons.Default.Bookmark
        RssSource.BLUESKY -> Icons.Default.Cloud
        RssSource.MISSKEY -> Icons.AutoMirrored.Filled.Message
    }
