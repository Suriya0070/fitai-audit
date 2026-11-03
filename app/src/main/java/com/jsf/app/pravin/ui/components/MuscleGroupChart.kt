package com.jsf.app.pravin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.jsf.app.pravin.domain.MuscleGroup
import com.jsf.app.pravin.ui.theme.*

private val muscleColors = mapOf(
    MuscleGroup.CHEST      to Color(0xFFF67E2D),
    MuscleGroup.BACK       to Color(0xFF6366F1),
    MuscleGroup.SHOULDERS  to Color(0xFF06B6D4),
    MuscleGroup.BICEPS     to Color(0xFF10B981),
    MuscleGroup.TRICEPS    to Color(0xFFF59E0B),
    MuscleGroup.ABS        to Color(0xFFEF4444),
    MuscleGroup.OBLIQUES   to Color(0xFFEC4899),
    MuscleGroup.LOWER_BACK to Color(0xFF8B5CF6),
    MuscleGroup.QUADS      to Color(0xFF14B8A6),
    MuscleGroup.HAMSTRINGS to Color(0xFFF97316),
    MuscleGroup.GLUTES     to Color(0xFFE11D48),
    MuscleGroup.CALVES     to Color(0xFF84CC16),
    MuscleGroup.FULL_BODY  to Color(0xFF64748B)
)

/**
 * Horizontal bar chart showing workout volume (reps) per muscle group.
 * Shows top 8 groups sorted by volume descending.
 */
@Composable
fun MuscleGroupChart(
    data: Map<MuscleGroup, Int>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val sorted = data.entries
        .sortedByDescending { it.value }
        .take(8)
    val maxVolume = sorted.maxOf { it.value }.coerceAtLeast(1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sorted.forEach { (muscle, volume) ->
            val fraction = volume.toFloat() / maxVolume
            val barColor = muscleColors[muscle] ?: CyanPrimary

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Color dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(barColor, CircleShape)
                )

                // Muscle name
                Text(
                    muscle.displayName,
                    fontSize = 12.sp,
                    color = TextDark,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(90.dp)
                )

                // Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(LightBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(barColor.copy(alpha = 0.85f))
                    )
                }

                // Volume label
                Text(
                    "$volume",
                    fontSize = 11.sp,
                    color = TextGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp)
                )
            }
        }
    }
}
