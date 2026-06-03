package `in`.aboobacker.labdroid.ui.components

import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun formatTimeAgo(createdAt: String): String {
    return try {
        val eventTime = ZonedDateTime.parse(createdAt)
        val now = ZonedDateTime.now()
        val minutes = ChronoUnit.MINUTES.between(eventTime, now)
        val hours = ChronoUnit.HOURS.between(eventTime, now)
        val days = ChronoUnit.DAYS.between(eventTime, now)

        when {
            minutes < 1 -> "just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> eventTime.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (_: Exception) {
        createdAt.take(10)
    }
}

fun formatCommitDate(dateStr: String): String {
    return try {
        val date = ZonedDateTime.parse(dateStr).toLocalDate()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        when (date) {
            today -> "TODAY"
            yesterday -> "YESTERDAY"
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")).uppercase()
        }
    } catch (_: Exception) {
        dateStr
    }
}
