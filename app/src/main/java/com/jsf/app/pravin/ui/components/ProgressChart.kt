package com.jsf.app.pravin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import com.jsf.app.pravin.ui.theme.*

@Composable
fun ProgressChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padL = 40f; val padB = 30f; val padT = 20f; val padR = 20f
        val chartW = w - padL - padR
        val chartH = h - padB - padT
        val maxVal = data.maxOf { it.second }.coerceAtLeast(100f)
        val minVal = 0f
        val xStep = if (data.size > 1) chartW / (data.size - 1).toFloat() else chartW

        // Grid lines
        for (i in 0..4) {
            val y = padT + chartH * (1f - i / 4f)
            drawLine(CardDark, Offset(padL, y), Offset(w - padR, y), 1f)
        }

        // Area fill
        if (data.size >= 2) {
            val path = Path()
            val firstX = padL
            val firstY = padT + chartH * (1f - (data[0].second - minVal) / (maxVal - minVal))
            path.moveTo(firstX, h - padB)
            path.lineTo(firstX, firstY)
            data.forEachIndexed { idx, (_, v) ->
                val x = padL + idx * xStep
                val y = padT + chartH * (1f - (v - minVal) / (maxVal - minVal))
                path.lineTo(x, y)
            }
            val lastX = padL + (data.size - 1) * xStep
            path.lineTo(lastX, h - padB)
            path.close()
            drawPath(
                path,
                brush = Brush.verticalGradient(
                    colors = listOf(CyanPrimary.copy(alpha = 0.3f), Color.Transparent),
                    startY = padT, endY = h - padB
                )
            )
        }

        // Line
        val linePath = Path()
        data.forEachIndexed { idx, (_, v) ->
            val x = padL + idx * xStep
            val y = padT + chartH * (1f - (v - minVal) / (maxVal - minVal))
            if (idx == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(
            linePath,
            CyanPrimary,
            style = Stroke(3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Data points
        data.forEachIndexed { idx, (_, v) ->
            val x = padL + idx * xStep
            val y = padT + chartH * (1f - (v - minVal) / (maxVal - minVal))
            drawCircle(CyanPrimary, radius = 6f, center = Offset(x, y))
            drawCircle(DeepSpace, radius = 3f, center = Offset(x, y))
        }
    }
}
