package app.focus.personal.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * 投稿日時の相対時刻の構造化表現。
 * 文言化(「3分前」等)は UI 層がこの型に応じてリソースから行う。
 */
sealed class RelativeTime {
    /** 1分未満(未来の日時も含む) */
    data object JustNow : RelativeTime()

    /** 1分以上60分未満 */
    data class MinutesAgo(val minutes: Int) : RelativeTime()

    /** 1時間以上24時間未満 */
    data class HoursAgo(val hours: Int) : RelativeTime()

    /** 1日以上7日未満 */
    data class DaysAgo(val days: Int) : RelativeTime()

    /** 7日以上。同年なら「7月10日」、年が違えば「2025年7月10日」形式を想定 */
    data class AbsoluteDate(val year: Int, val month: Int, val day: Int, val withYear: Boolean) : RelativeTime()

    companion object {
        private const val MINUTE_MILLIS = 60_000L
        private const val HOUR_MILLIS = 60 * MINUTE_MILLIS
        private const val DAY_MILLIS = 24 * HOUR_MILLIS

        /**
         * epoch ミリ秒から相対時刻を求める。
         * [epochMillis] が 0 以下(パース失敗・未設定)の場合は null を返す。
         */
        fun of(
            epochMillis: Long,
            nowMillis: Long = Clock.System.now().toEpochMilliseconds(),
            timeZone: TimeZone = TimeZone.currentSystemDefault(),
        ): RelativeTime? {
            if (epochMillis <= 0L) return null
            val diff = nowMillis - epochMillis
            return when {
                diff < MINUTE_MILLIS -> JustNow
                diff < HOUR_MILLIS -> MinutesAgo((diff / MINUTE_MILLIS).toInt())
                diff < DAY_MILLIS -> HoursAgo((diff / HOUR_MILLIS).toInt())
                diff < 7 * DAY_MILLIS -> DaysAgo((diff / DAY_MILLIS).toInt())
                else -> {
                    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).date
                    val nowYear = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date.year
                    AbsoluteDate(
                        year = date.year,
                        month = date.monthNumber,
                        day = date.dayOfMonth,
                        withYear = date.year != nowYear,
                    )
                }
            }
        }
    }
}
