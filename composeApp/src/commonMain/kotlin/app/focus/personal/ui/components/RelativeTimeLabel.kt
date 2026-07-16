package app.focus.personal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.focus.personal.util.RelativeTime
import focus.composeapp.generated.resources.Res
import focus.composeapp.generated.resources.time_date
import focus.composeapp.generated.resources.time_date_with_year
import focus.composeapp.generated.resources.time_days_ago
import focus.composeapp.generated.resources.time_hours_ago
import focus.composeapp.generated.resources.time_just_now
import focus.composeapp.generated.resources.time_minutes_ago
import org.jetbrains.compose.resources.stringResource

/**
 * 投稿日時の表示文言を返す。
 * pubDateMillis から相対時刻(たった今 / 3分前 / 2時間前 / 7月10日)を生成し、
 * パース失敗時(0L)は生の pubDate 文字列にフォールバックする。
 */
@Composable
fun relativeTimeLabel(pubDateMillis: Long, fallback: String?): String? {
    val relative = remember(pubDateMillis) { RelativeTime.of(pubDateMillis) } ?: return fallback
    return when (relative) {
        is RelativeTime.JustNow -> stringResource(Res.string.time_just_now)
        is RelativeTime.MinutesAgo -> stringResource(Res.string.time_minutes_ago, relative.minutes)
        is RelativeTime.HoursAgo -> stringResource(Res.string.time_hours_ago, relative.hours)
        is RelativeTime.DaysAgo -> stringResource(Res.string.time_days_ago, relative.days)
        is RelativeTime.AbsoluteDate ->
            if (relative.withYear) {
                stringResource(Res.string.time_date_with_year, relative.year, relative.month, relative.day)
            } else {
                stringResource(Res.string.time_date, relative.month, relative.day)
            }
    }
}
