package com.jsf.app.pravin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jsf.app.pravin.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScoreGauge(
    score: Float,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "score"
    )
    val color = when {
        score >= 80f -> NeonGreen
        score >= 60f -> OrangeWarning
        else -> RedDanger
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.toPx() * 0.08f
            val radius = (this.size.minDimension - strokeWidth) / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val startAngle = 150f
            val sweepTotal = 240f

            // Background arc
            drawArc(
                color = LightBorder,
                startAngle = startAngle,
                sweepAngle = sweepTotal,
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Score arc
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(color.copy(alpha = 0.7f), color),
                    center = center
                ),
                startAngle = startAngle,
                sweepAngle = sweepTotal * (animatedScore / 100f),
                useCenter = false,
                style = Stroke(strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // Needle tip dot
            val angle = Math.toRadians(
                (startAngle + sweepTotal * (animatedScore / 100f)).toDouble()
            )
            val dotX = center.x + radius * cos(angle).toFloat()
            val dotY = center.y + radius * sin(angle).toFloat()
            drawCircle(color = color, radius = strokeWidth / 2f, center = Offset(dotX, dotY))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                fontSize = (size.value * 0.1f).sp,
                color = TextGray
            )
        }
    }
}
