package app.focus.personal.util

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

object DateUtils {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /**
     * Parses RFC 822 date string (e.g., "Fri, 28 Mar 2025 15:45:00 +0900") to epoch millis.
     * Returns 0 if parsing fails.
     */
    fun parseRfc822ToMillis(dateString: String?): Long {
        if (dateString == null) return 0
        try {
            // Remove day of week if present: "Fri, 28 Mar 2025 15:45:00 +0900" -> "28 Mar 2025 15:45:00 +0900"
            val cleanDate = if (dateString.contains(",")) {
                dateString.substringAfter(",").trim()
            } else {
                dateString.trim()
            }

            val parts = cleanDate.split(" ")
            if (parts.size < 4) return 0

            val day = parts[0].padStart(2, '0').toInt()
            val monthStr = parts[1]
            val month = months.indexOf(monthStr) + 1
            if (month == 0) return 0
            val year = parts[2].toInt()
            
            val timeParts = parts[3].split(":")
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val second = if (timeParts.size > 2) timeParts[2].toInt() else 0

            val ldt = LocalDateTime(year, month, day, hour, minute, second)
            
            // Handle timezone offset (e.g., "+0900")
            val offsetStr = if (parts.size > 4) parts[4] else "+0000"
            val offsetMinutes = if (offsetStr.length == 5) {
                val sign = if (offsetStr.startsWith("-")) -1 else 1
                val h = offsetStr.substring(1, 3).toInt()
                val m = offsetStr.substring(3, 5).toInt()
                sign * (h * 60 + m)
            } else {
                0
            }

            // Convert to UTC Instant
            val utcInstant = ldt.toInstant(TimeZone.UTC)
            return utcInstant.toEpochMilliseconds() - (offsetMinutes * 60 * 1000)
        } catch (e: Exception) {
            println("Error parsing date: $dateString, ${e.message}")
            return 0
        }
    }
}
