package app.focus.personal.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RelativeTimeTest {

    // 2026-07-15T12:00:00Z を「現在時刻」として固定
    private val now = 1_784_116_800_000L
    private val tz = TimeZone.UTC

    private fun at(millisAgo: Long): RelativeTime? = RelativeTime.of(now - millisAgo, now, tz)

    @Test
    fun testInvalidMillisReturnsNull() {
        assertNull(RelativeTime.of(0L, now, tz))
        assertNull(RelativeTime.of(-1L, now, tz))
    }

    @Test
    fun testJustNow() {
        assertEquals(RelativeTime.JustNow, at(0L))
        assertEquals(RelativeTime.JustNow, at(59_999L))
        // 未来の日時(サーバー時刻ずれ等)も JustNow 扱い
        assertEquals(RelativeTime.JustNow, at(-10_000L))
    }

    @Test
    fun testMinutesAgo() {
        assertEquals(RelativeTime.MinutesAgo(1), at(60_000L))
        assertEquals(RelativeTime.MinutesAgo(3), at(3 * 60_000L))
        assertEquals(RelativeTime.MinutesAgo(59), at(60 * 60_000L - 1))
    }

    @Test
    fun testHoursAgo() {
        assertEquals(RelativeTime.HoursAgo(1), at(60 * 60_000L))
        assertEquals(RelativeTime.HoursAgo(2), at(2 * 60 * 60_000L))
        assertEquals(RelativeTime.HoursAgo(23), at(24 * 60 * 60_000L - 1))
    }

    @Test
    fun testDaysAgo() {
        val day = 24 * 60 * 60_000L
        assertEquals(RelativeTime.DaysAgo(1), at(day))
        assertEquals(RelativeTime.DaysAgo(6), at(7 * day - 1))
    }

    @Test
    fun testAbsoluteDateSameYear() {
        val day = 24 * 60 * 60_000L
        // 7日前 = 2026-07-08(同年なので withYear = false)
        assertEquals(
            RelativeTime.AbsoluteDate(year = 2026, month = 7, day = 8, withYear = false),
            at(7 * day),
        )
    }

    @Test
    fun testAbsoluteDateDifferentYear() {
        val day = 24 * 60 * 60_000L
        // 365日前 = 2025-07-15(年が違うので withYear = true)
        assertEquals(
            RelativeTime.AbsoluteDate(year = 2025, month = 7, day = 15, withYear = true),
            at(365 * day),
        )
    }
}
