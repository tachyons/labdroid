package `in`.aboobacker.labdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun ContributionHeatmap(
    calendar: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val weeksToShow = 20
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Calculate start date: Sunday of the week weeksToShow ago
            val startDate = today.minusWeeks(weeksToShow.toLong())
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))

            repeat(weeksToShow) { weekIndex ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { dayIndex ->
                        val date = startDate.plusDays((weekIndex * 7 + dayIndex).toLong())
                        val dateString = date.format(formatter)
                        val count = calendar[dateString] ?: 0

                        val alpha = when {
                            count == 0 -> 0.1f
                            count < 3 -> 0.3f
                            count < 6 -> 0.5f
                            count < 10 -> 0.8f
                            else -> 1.0f
                        }

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Less",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0.1f, 0.3f, 0.5f, 0.8f, 1.0f).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
                    )
                }
            }
            Text(
                "More",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
