package com.jsf.app.pravin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.jsf.app.pravin.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * Pentagon radar chart for 5 AI workout metrics (0–100 each).
 * metrics: list of (label, value) pairs — exactly 5 entries expected.
 */
@Composable
fun RadarChart(
    metrics: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    if (metrics.isEmpty()) return
    val n = metrics.size

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy) * 0.70f

        fun axisPoint(index: Int, scale: Float): Offset {
            val angle = (-PI / 2 + 2 * PI * index / n).toFloat()
            return Offset(cx + radius * scale * cos(angle), cy + radius * scale * sin(angle))
        }

        // Grid rings at 25 / 50 / 75 / 100 %
        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { level ->
            val path = Path()
            for (i in 0 until n) {
                val pt = axisPoint(i, level)
                if (i == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()
            drawPath(path, color = TextGray.copy(alpha = 0.15f), style = Stroke(1.5f))
        }

        // Axes from center to edge
        for (i in 0 until n) {
            val edge = axisPoint(i, 1f)
            drawLine(TextGray.copy(alpha = 0.25f), Offset(cx, cy), edge, strokeWidth = 1.5f)
        }

        // Value polygon
        val valuePath = Path()
        metrics.forEachIndexed { i, (_, v) ->
            val pt = axisPoint(i, (v / 100f).coerceIn(0f, 1f))
            if (i == 0) valuePath.moveTo(pt.x, pt.y) else valuePath.lineTo(pt.x, pt.y)
        }
        valuePath.close()
        drawPath(valuePath, color = CyanPrimary.copy(alpha = 0.22f))
        drawPath(valuePath, color = CyanPrimary, style = Stroke(2.5f))

        // Dots at each vertex
        metrics.forEachIndexed { i, (_, v) ->
            val pt = axisPoint(i, (v / 100f).coerceIn(0f, 1f))
            drawCircle(CyanPrimary, 5f, pt)
            drawCircle(LightCard, 2.5f, pt)
        }

        // Labels at each axis endpoint
        val paint = android.graphics.Paint().apply {
            color = TextGray.toArgb()
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        metrics.forEachIndexed { i, (label, _) ->
            val angle = (-PI / 2 + 2 * PI * i / n).toFloat()
            val labelRadius = radius * 1.22f
            val lx = cx + labelRadius * cos(angle)
            val ly = cy + labelRadius * sin(angle) + paint.textSize / 3f
            drawContext.canvas.nativeCanvas.drawText(label, lx, ly, paint)
        }
    }
}
