package app.focus.personal.util

import io.github.aakira.napier.Napier
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

object DateUtils {
    private val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    // RFC 822 で使われる代表的なタイムゾーン名 → UTC からのオフセット(分)
    private val zoneNameOffsets = mapOf(
        "GMT" to 0, "UT" to 0, "UTC" to 0, "Z" to 0,
        "EST" to -5 * 60, "EDT" to -4 * 60,
        "CST" to -6 * 60, "CDT" to -5 * 60,
        "MST" to -7 * 60, "MDT" to -6 * 60,
        "PST" to -8 * 60, "PDT" to -7 * 60,
    )

    /**
     * Parses ISO 8601 date string (e.g., "2026-03-29T21:44:02+09:00") to epoch millis.
     * Returns 0 if parsing fails.
     */
    fun parseIso8601ToMillis(dateString: String?): Long {
        if (dateString == null) return 0
        return try {
            Instant.parse(dateString).toEpochMilliseconds()
        } catch (e: Exception) {
            Napier.w("Error parsing ISO 8601 date: $dateString, ${e.message}")
            0
        }
    }

    /**
     * Parses RFC 822 date string (e.g., "Fri, 28 Mar 2025 15:45:00 +0900") to epoch millis.
     * Timezone は数値オフセット(+0900)と代表的なゾーン名(GMT 等)に対応。
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

            // Handle timezone: numeric offset ("+0900") or zone name ("GMT")
            val offsetStr = if (parts.size > 4) parts[4] else "+0000"
            val offsetMinutes = when {
                offsetStr.length == 5 && (offsetStr[0] == '+' || offsetStr[0] == '-') -> {
                    val sign = if (offsetStr.startsWith("-")) -1 else 1
                    val h = offsetStr.substring(1, 3).toInt()
                    val m = offsetStr.substring(3, 5).toInt()
                    sign * (h * 60 + m)
                }
                else -> zoneNameOffsets[offsetStr.uppercase()] ?: 0
            }

            // Convert to UTC Instant
            val utcInstant = ldt.toInstant(TimeZone.UTC)
            return utcInstant.toEpochMilliseconds() - (offsetMinutes * 60 * 1000)
        } catch (e: Exception) {
            Napier.w("Error parsing RFC 822 date: $dateString, ${e.message}")
            return 0
        }
    }
}
