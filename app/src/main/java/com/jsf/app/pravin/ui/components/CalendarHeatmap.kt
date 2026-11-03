package com.jsf.app.pravin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsf.app.pravin.ui.theme.*
import java.util.Calendar

/**
 * 5-week × 7-day heatmap showing workout activity for the last 35 days.
 * data: Map<daysSinceEpoch, avgScore>
 */
@Composable
fun CalendarHeatmap(
    data: Map<Long, Float>,
    modifier: Modifier = Modifier
) {
    val dayMs = 86_400_000L
    val today = System.currentTimeMillis() / dayMs
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

    Column(modifier = modifier) {
        // Day-of-week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dayLabels.forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, fontSize = 10.sp, color = TextGray, fontWeight = FontWeight.Medium)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Find the start of the current week (Sunday)
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        // Move back to most recent Sunday
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1  // 0=Sun
        cal.add(Calendar.DAY_OF_YEAR, -dayOfWeek)
        // Move back 4 more weeks to show 5 rows total
        cal.add(Calendar.DAY_OF_YEAR, -28)

        val startDay = cal.timeInMillis / dayMs

        // 5 rows of 7 cells = 35 days
        for (week in 0 until 5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dow in 0 until 7) {
                    val day = startDay + week * 7 + dow
                    val score = data[day]
                    val isFuture = day > today

                    val cellColor = when {
                        isFuture          -> Color.Transparent
                        score == null     -> LightBorder.copy(alpha = 0.6f)
                        score >= 80f      -> GreenSuccess.copy(alpha = 0.80f)
                        score >= 60f      -> CyanPrimary.copy(alpha = 0.65f)
                        else              -> CyanPrimary.copy(alpha = 0.30f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(cellColor)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Legend
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", fontSize = 10.sp, color = TextGray)
            listOf(
                LightBorder.copy(alpha = 0.6f),
                CyanPrimary.copy(alpha = 0.30f),
                CyanPrimary.copy(alpha = 0.65f),
                GreenSuccess.copy(alpha = 0.80f)
            ).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
            Text("More", fontSize = 10.sp, color = TextGray)
        }
    }
}
