package app.focus.personal.ui.components

import androidx.compose.runtime.Composable
import app.focus.personal.viewmodel.FeedErrorKind
import app.focus.personal.viewmodel.RssUiState
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.bluesky_auth_code_invalid_error
import focus.composeapp.generated.resources.bluesky_login_failed_error
import focus.composeapp.generated.resources.bluesky_rate_limit_error
import focus.composeapp.generated.resources.error_detail_suffix
import focus.composeapp.generated.resources.error_generic
import org.jetbrains.compose.resources.stringResource

/**
 * エラー種別([FeedErrorKind])を表示文言に解決する。
 * 例外由来の詳細メッセージがあれば文言に併記する。
 */
@Composable
fun feedErrorMessage(state: RssUiState.Error): String {
    val base = when (state.kind) {
        FeedErrorKind.RATE_LIMITED -> stringResource(Res.string.bluesky_rate_limit_error)
        FeedErrorKind.AUTH_CODE_INVALID -> stringResource(Res.string.bluesky_auth_code_invalid_error)
        FeedErrorKind.LOGIN_FAILED -> stringResource(Res.string.bluesky_login_failed_error)
        FeedErrorKind.GENERIC -> stringResource(Res.string.error_generic)
    }
    val showDetail = state.message.isNotBlank() &&
        (state.kind == FeedErrorKind.GENERIC || state.kind == FeedErrorKind.LOGIN_FAILED)
    return if (showDetail) stringResource(Res.string.error_detail_suffix, base, state.message) else base
}
