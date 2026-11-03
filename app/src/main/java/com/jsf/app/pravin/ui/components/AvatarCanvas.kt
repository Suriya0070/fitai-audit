package com.jsf.app.pravin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jsf.app.pravin.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AvatarCanvas(
    level: Int,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    val breatheAnim = rememberInfiniteTransition(label = "breathe")
    val scale by breatheAnim.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by breatheAnim.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val bodyColor = when (level) {
        0 -> TextSecondary
        1 -> CyanPrimary
        2 -> NeonGreen
        3 -> OrangeWarning
        4 -> ScoreGold
        else -> TextSecondary
    }
    val glowColor = bodyColor.copy(alpha = glowAlpha * 0.3f)

    Canvas(modifier = modifier.size(size)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val unit = this.size.height / 12f * scale

        // Glow effect
        if (level > 0) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = this.size.minDimension * 0.5f
                ),
                radius = this.size.minDimension * 0.5f,
                center = Offset(cx, cy)
            )
        }

        when (level) {
            0 -> drawBasicFigure(cx, cy, unit, bodyColor)
            1 -> drawWarriorFigure(cx, cy, unit, bodyColor)
            2 -> drawAthleteFigure(cx, cy, unit, bodyColor)
            3 -> drawChampionFigure(cx, cy, unit, bodyColor)
            4 -> drawLegendFigure(cx, cy, unit, bodyColor)
        }
    }
}

private fun DrawScope.drawBasicFigure(cx: Float, cy: Float, u: Float, color: Color) {
    val sw = u * 0.25f
    // Head
    drawCircle(
        color = color,
        radius = u * 0.8f,
        center = Offset(cx, cy - u * 4.5f),
        style = Stroke(sw)
    )
    // Body
    drawLine(color, Offset(cx, cy - u * 3.7f), Offset(cx, cy), sw)
    // Arms
    drawLine(color, Offset(cx - u * 1.5f, cy - u * 2.5f), Offset(cx + u * 1.5f, cy - u * 2.5f), sw)
    // Legs
    drawLine(color, Offset(cx, cy), Offset(cx - u * 1.2f, cy + u * 2.5f), sw)
    drawLine(color, Offset(cx, cy), Offset(cx + u * 1.2f, cy + u * 2.5f), sw)
}

private fun DrawScope.drawWarriorFigure(cx: Float, cy: Float, u: Float, color: Color) {
    val stroke = u * 0.3f
    // Head with armor hints
    drawCircle(color = color, radius = u * 0.9f, center = Offset(cx, cy - u * 4.5f), style = Stroke(stroke))
    drawCircle(color = color.copy(alpha = 0.3f), radius = u * 0.7f, center = Offset(cx, cy - u * 4.5f))
    // Torso (wider)
    drawLine(color, Offset(cx, cy - u * 3.6f), Offset(cx, cy), stroke)
    // Wider shoulders
    drawLine(color, Offset(cx - u * 2f, cy - u * 3f), Offset(cx + u * 2f, cy - u * 3f), stroke)
    // Upper arms
    drawLine(color, Offset(cx - u * 2f, cy - u * 3f), Offset(cx - u * 2f, cy - u * 1.5f), stroke)
    drawLine(color, Offset(cx + u * 2f, cy - u * 3f), Offset(cx + u * 2f, cy - u * 1.5f), stroke)
    // Forearms
    drawLine(color, Offset(cx - u * 2f, cy - u * 1.5f), Offset(cx - u * 1.5f, cy - u * 0.5f), stroke)
    drawLine(color, Offset(cx + u * 2f, cy - u * 1.5f), Offset(cx + u * 1.5f, cy - u * 0.5f), stroke)
    // Legs
    drawLine(color, Offset(cx, cy), Offset(cx - u * 1.3f, cy + u * 2.8f), stroke)
    drawLine(color, Offset(cx, cy), Offset(cx + u * 1.3f, cy + u * 2.8f), stroke)
}

private fun DrawScope.drawAthleteFigure(cx: Float, cy: Float, u: Float, color: Color) {
    val stroke = u * 0.35f
    // Head
    drawCircle(color = color, radius = u, center = Offset(cx, cy - u * 4.5f), style = Stroke(stroke))
    drawCircle(color = color.copy(alpha = 0.2f), radius = u * 0.8f, center = Offset(cx, cy - u * 4.5f))
    // Neck
    drawLine(color, Offset(cx, cy - u * 3.5f), Offset(cx, cy - u * 3f), stroke)
    // Shoulders (V-shape chest)
    drawLine(color, Offset(cx - u * 2.2f, cy - u * 3f), Offset(cx, cy - u * 2f), stroke)
    drawLine(color, Offset(cx + u * 2.2f, cy - u * 3f), Offset(cx, cy - u * 2f), stroke)
    // Body
    drawLine(color, Offset(cx, cy - u * 2f), Offset(cx, cy - u * 0.5f), stroke)
    // Hips
    drawLine(color, Offset(cx - u * 1.2f, cy - u * 0.5f), Offset(cx + u * 1.2f, cy - u * 0.5f), stroke)
    // Arms
    drawLine(color, Offset(cx - u * 2.2f, cy - u * 3f), Offset(cx - u * 2.5f, cy - u * 1f), stroke)
    drawLine(color, Offset(cx + u * 2.2f, cy - u * 3f), Offset(cx + u * 2.5f, cy - u * 1f), stroke)
    // Bicep bulge
    drawCircle(color = color.copy(alpha = 0.5f), radius = u * 0.5f, center = Offset(cx - u * 2.3f, cy - u * 2f))
    drawCircle(color = color.copy(alpha = 0.5f), radius = u * 0.5f, center = Offset(cx + u * 2.3f, cy - u * 2f))
    // Legs
    drawLine(color, Offset(cx - u * 1.2f, cy - u * 0.5f), Offset(cx - u * 1.5f, cy + u * 2.8f), stroke)
    drawLine(color, Offset(cx + u * 1.2f, cy - u * 0.5f), Offset(cx + u * 1.5f, cy + u * 2.8f), stroke)
    // Calves
    drawLine(color, Offset(cx - u * 1.5f, cy + u * 1.5f), Offset(cx - u * 1.2f, cy + u * 2.8f), stroke * 1.2f)
    drawLine(color, Offset(cx + u * 1.5f, cy + u * 1.5f), Offset(cx + u * 1.2f, cy + u * 2.8f), stroke * 1.2f)
}

private fun DrawScope.drawChampionFigure(cx: Float, cy: Float, u: Float, color: Color) {
    drawAthleteFigure(cx, cy, u, color)
    // Crown
    val crownY = cy - u * 5.8f
    val crownPoints = listOf(
        Offset(cx - u, crownY + u * 0.5f),
        Offset(cx - u * 0.7f, crownY),
        Offset(cx - u * 0.35f, crownY + u * 0.3f),
        Offset(cx, crownY - u * 0.2f),
        Offset(cx + u * 0.35f, crownY + u * 0.3f),
        Offset(cx + u * 0.7f, crownY),
        Offset(cx + u, crownY + u * 0.5f)
    )
    val path = Path().apply {
        moveTo(crownPoints[0].x, crownPoints[0].y)
        crownPoints.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path,
        color = ScoreGold,
        style = Stroke(u * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    // Trophy badge
    drawCircle(
        color = ScoreGold.copy(alpha = 0.4f),
        radius = u * 0.4f,
        center = Offset(cx + u * 3f, cy - u * 2f)
    )
}

private fun DrawScope.drawLegendFigure(cx: Float, cy: Float, u: Float, color: Color) {
    // Aura rings
    for (i in 1..3) {
        drawCircle(
            color = color.copy(alpha = 0.1f * (4 - i)),
            radius = u * (3f + i * 0.8f),
            center = Offset(cx, cy - u),
            style = Stroke(u * 0.15f)
        )
    }
    drawChampionFigure(cx, cy, u * 1.1f, color)
    // Star burst
    for (i in 0..7) {
        val angle = Math.toRadians(i * 45.0)
        val starX = cx + (u * 3.5f * cos(angle)).toFloat()
        val starY = (cy - u) + (u * 3.5f * sin(angle)).toFloat()
        drawLine(color.copy(alpha = 0.7f), Offset(cx, cy - u), Offset(starX, starY), u * 0.15f)
    }
}
