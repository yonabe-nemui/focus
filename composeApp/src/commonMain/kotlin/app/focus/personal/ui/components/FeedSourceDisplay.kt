package app.focus.personal.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.focus.personal.viewmodel.FeedSource
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.source_bluesky
import focus.composeapp.generated.resources.source_google
import focus.composeapp.generated.resources.source_hatena
import focus.composeapp.generated.resources.source_misskey
import org.jetbrains.compose.resources.stringResource

/** ソースの表示名。 */
@Composable
fun sourceDisplayName(source: FeedSource): String = when (source) {
    FeedSource.GOOGLE -> stringResource(Res.string.source_google)
    FeedSource.HATENA -> stringResource(Res.string.source_hatena)
    FeedSource.BLUESKY -> stringResource(Res.string.source_bluesky)
    FeedSource.MISSKEY -> stringResource(Res.string.source_misskey)
}

/** ソースのアイコン。 */
val FeedSource.icon: ImageVector
    get() = when (this) {
        FeedSource.GOOGLE -> Icons.Default.RssFeed
        FeedSource.HATENA -> Icons.Default.Bookmark
        FeedSource.BLUESKY -> Icons.Default.Cloud
        FeedSource.MISSKEY -> Icons.AutoMirrored.Filled.Message
    }
