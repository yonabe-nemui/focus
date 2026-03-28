package app.focus.personal.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateUtilsTest {

    @Test
    fun testParseRfc822ToMillis() {
        val date1 = "Fri, 28 Mar 2025 15:45:00 +0900"
        val millis1 = DateUtils.parseRfc822ToMillis(date1)
        
        val date2 = "Fri, 28 Mar 2025 15:46:00 +0900"
        val millis2 = DateUtils.parseRfc822ToMillis(date2)
        
        assertTrue(millis2 > millis1, "Later date should have larger millis")
        
        // Test different timezone
        val date3 = "Fri, 28 Mar 2025 06:45:00 +0000" // Same as 15:45 +0900
        val millis3 = DateUtils.parseRfc822ToMillis(date3)
        assertEquals(millis1, millis3, "Same time in different timezones should have same millis")
    }

    @Test
    fun testParseRfc822WithoutDayOfWeek() {
        val date = "28 Mar 2025 15:45:00 +0900"
        val millis = DateUtils.parseRfc822ToMillis(date)
        assertTrue(millis > 0)
    }

    @Test
    fun testParseInvalidDate() {
        assertEquals(0, DateUtils.parseRfc822ToMillis("invalid date"))
        assertEquals(0, DateUtils.parseRfc822ToMillis(null))
    }
}
